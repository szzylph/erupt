package xyz.erupt.core.controller;import com.google.gson.Gson;import com.google.gson.JsonElement;import com.google.gson.JsonObject;import lombok.SneakyThrows;import lombok.extern.slf4j.Slf4j;import org.apache.commons.lang3.StringUtils;import org.springframework.web.bind.annotation.*;import xyz.erupt.annotation.constant.AnnotationConst;import xyz.erupt.annotation.fun.OperationHandler;import xyz.erupt.annotation.fun.PowerObject;import xyz.erupt.annotation.sub_erupt.Filter;import xyz.erupt.annotation.sub_erupt.RowOperation;import xyz.erupt.annotation.sub_erupt.Tree;import xyz.erupt.annotation.sub_field.Edit;import xyz.erupt.annotation.sub_field.sub_edit.CheckboxType;import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTreeType;import xyz.erupt.core.annotation.EruptRouter;import xyz.erupt.core.config.GsonFactory;import xyz.erupt.core.constant.EruptRestPath;import xyz.erupt.core.exception.EruptNoLegalPowerException;import xyz.erupt.core.exception.EruptWebApiRuntimeException;import xyz.erupt.core.invoke.DataProxyInvoke;import xyz.erupt.core.invoke.PowerInvoke;import xyz.erupt.core.query.Column;import xyz.erupt.core.query.Condition;import xyz.erupt.core.query.EruptQuery;import xyz.erupt.core.service.EruptCoreService;import xyz.erupt.core.service.EruptService;import xyz.erupt.core.service.PreEruptDataService;import xyz.erupt.core.util.AnnotationUtil;import xyz.erupt.core.util.EruptSpringUtil;import xyz.erupt.core.util.EruptUtil;import xyz.erupt.core.view.*;import javax.annotation.Resource;import java.io.Serializable;import java.util.ArrayList;import java.util.Collection;import java.util.List;import java.util.Map;/** * @author liyuepeng * @date 9/28/18. */@RestController@RequestMapping(EruptRestPath.ERUPT_DATA)@Slf4jpublic class EruptDataController {    @Resource    private EruptService eruptService;    @Resource    private PreEruptDataService preEruptDataService;    private final Gson gson = GsonFactory.getGson();    @PostMapping({"/table/{erupt}"})    @EruptRouter(authIndex = 2, verifyType = EruptRouter.VerifyType.ERUPT)    @ResponseBody    public Page getEruptData(@PathVariable("erupt") String eruptName, @RequestBody TableQueryVo tableQueryVo) {        return eruptService.getEruptData(EruptCoreService.getErupt(eruptName), tableQueryVo, null);    }    @GetMapping("/tree/{erupt}")    @ResponseBody    @EruptRouter(authIndex = 2, verifyType = EruptRouter.VerifyType.ERUPT)    public Collection<TreeModel> getEruptTreeData(@PathVariable("erupt") String eruptName) {        EruptModel eruptModel = EruptCoreService.getErupt(eruptName);        if (PowerInvoke.getPowerObject(eruptModel).isQuery()) {            Tree tree = eruptModel.getErupt().tree();            return preEruptDataService.geneTree(eruptModel, tree.id(), tree.label(), tree.pid(), tree.rootPid(), EruptQuery.builder().build());        } else {            throw new EruptNoLegalPowerException();        }    }    //获取初始化数据    @GetMapping("/init-value/{erupt}")    @ResponseBody    @EruptRouter(authIndex = 2, verifyType = EruptRouter.VerifyType.ERUPT)    public Map<String, Object> initEruptValue(@PathVariable("erupt") String eruptName) throws IllegalAccessException, InstantiationException {        EruptModel eruptModel = EruptCoreService.getErupt(eruptName);        Object obj = eruptModel.getClazz().newInstance();        DataProxyInvoke.invoke(eruptModel, (dataProxy -> dataProxy.addBehavior(obj)));        return EruptUtil.generateEruptDataMap(eruptModel, obj);    }    @GetMapping("/{erupt}/{id}")    @ResponseBody    @EruptRouter(authIndex = 1, verifyType = EruptRouter.VerifyType.ERUPT)    public Map<String, Object> getEruptDataById(@PathVariable("erupt") String eruptName, @PathVariable("id") String id) {        EruptModel eruptModel = EruptCoreService.getErupt(eruptName);        PowerObject powerObject = PowerInvoke.getPowerObject(eruptModel);        if (powerObject.isEdit() || powerObject.isViewDetails()) {            if (!eruptService.verifyIdPermissions(eruptModel, id)) {                throw new EruptNoLegalPowerException();            }            Object data = AnnotationUtil.getEruptDataProcessor(eruptModel.getClazz())                    .findDataById(eruptModel, EruptUtil.toEruptId(eruptModel, id));            DataProxyInvoke.invoke(eruptModel, (dataProxy -> dataProxy.editBehavior(data)));            return EruptUtil.generateEruptDataMap(eruptModel, data);        } else {            throw new EruptNoLegalPowerException();        }    }    @PostMapping("/{erupt}/operator/{code}")    @ResponseBody    @EruptRouter(authIndex = 1, verifyType = EruptRouter.VerifyType.ERUPT)    public EruptApiModel execEruptOperator(@PathVariable("erupt") String eruptName, @PathVariable("code") String code,                                           @RequestBody JsonObject body) {        EruptModel eruptModel = EruptCoreService.getErupt(eruptName);        for (RowOperation rowOperation : eruptModel.getErupt().rowOperation()) {            if (code.equals(rowOperation.code())) {                if (!AnnotationUtil.getExprBool(rowOperation.show())) {                    throw new EruptNoLegalPowerException();                }                if (rowOperation.eruptClass() != void.class) {                    EruptModel erupt = EruptCoreService.getErupt(rowOperation.eruptClass().getSimpleName());                    EruptApiModel eruptApiModel = EruptUtil.validateEruptValue(erupt, body.getAsJsonObject("param"));                    if (eruptApiModel.getStatus() == EruptApiModel.Status.ERROR) return eruptApiModel;                }                if (rowOperation.operationHandler().isInterface()) {                    return EruptApiModel.errorApi("请为" + rowOperation.title() + "实现OperationHandler接口");                }                OperationHandler<Object, Object> operationHandler = EruptSpringUtil.getBean(rowOperation.operationHandler());                Object param = null;                if (!body.get("param").isJsonNull()) {                    param = gson.fromJson(body.getAsJsonObject("param"), rowOperation.eruptClass());                }                if (rowOperation.mode() == RowOperation.Mode.BUTTON) {                    operationHandler.exec(null, param, rowOperation.operationParam());                } else {                    if (body.get("ids").isJsonArray() && body.getAsJsonArray("ids").size() > 0) {                        List<Object> list = new ArrayList<>();                        for (JsonElement id : body.getAsJsonArray("ids")) {                            Object obj = AnnotationUtil.getEruptDataProcessor(eruptModel.getClazz())                                    .findDataById(eruptModel, EruptUtil.toEruptId(eruptModel, id.getAsString()));                            list.add(obj);                        }                        operationHandler.exec(list, param, rowOperation.operationParam());                    } else {                        return EruptApiModel.errorApi("执行该操作时请至少选中一条数据");                    }                }                return EruptApiModel.successApi("执行成功", null);            }        }        throw new EruptNoLegalPowerException();    }    @GetMapping("/tab/tree/{erupt}/{tabFieldName}")    @ResponseBody    @EruptRouter(authIndex = 3, verifyType = EruptRouter.VerifyType.ERUPT)    public Collection<TreeModel> findTabTree(@PathVariable("erupt") String eruptName, @PathVariable("tabFieldName") String tabFieldName) {        EruptModel eruptModel = EruptCoreService.getErupt(eruptName);        PowerObject powerObject = PowerInvoke.getPowerObject(eruptModel);        if (powerObject.isViewDetails() || powerObject.isEdit()) {            EruptModel tabEruptModel = EruptCoreService.getErupt(eruptModel.getEruptFieldMap().get(tabFieldName).getFieldReturnName());            Tree tree = tabEruptModel.getErupt().tree();            return preEruptDataService.geneTree(tabEruptModel, tree.id(), tree.label(), tree.pid(), tree.rootPid(), EruptQuery.builder().build());        } else {            throw new EruptNoLegalPowerException();        }    }    @GetMapping("/{erupt}/checkbox/{fieldName}")    @ResponseBody    @EruptRouter(authIndex = 1, verifyType = EruptRouter.VerifyType.ERUPT)    public Collection<CheckboxModel> findCheckbox(@PathVariable("erupt") String eruptName, @PathVariable("fieldName") String fieldName) {        EruptModel eruptModel = EruptCoreService.getErupt(eruptName);        PowerObject powerObject = PowerInvoke.getPowerObject(eruptModel);        if (powerObject.isViewDetails() || powerObject.isEdit()) {            EruptFieldModel eruptFieldModel = eruptModel.getEruptFieldMap().get(fieldName);            EruptModel tabEruptModel = EruptCoreService.getErupt(eruptFieldModel.getFieldReturnName());            List<Column> columns = new ArrayList<>();            CheckboxType checkboxType = eruptFieldModel.getEruptField().edit().checkboxType();            columns.add(new Column(checkboxType.id(), AnnotationConst.ID));            columns.add(new Column(checkboxType.label(), AnnotationConst.LABEL));            Collection<Map<String, Object>> collection = preEruptDataService.createColumnQuery(tabEruptModel, columns, EruptQuery.builder().build());            Collection<CheckboxModel> checkboxModels = new ArrayList<>(collection.size());            for (Map<String, Object> map : collection) {                checkboxModels.add(new CheckboxModel(map.get(AnnotationConst.ID), map.get(AnnotationConst.LABEL)));            }            return checkboxModels;        } else {            throw new EruptNoLegalPowerException();        }    }    // REFERENCE API    @PostMapping("/{erupt}/reference-table/{fieldName}")    @ResponseBody    @EruptRouter(authIndex = 1, verifyType = EruptRouter.VerifyType.ERUPT)    public Page getReferenceTable(@PathVariable("erupt") String eruptName,                                  @PathVariable("fieldName") String fieldName,                                  @RequestParam(value = "dependValue", required = false) Serializable dependValue,                                  @RequestParam(value = "tabRef", required = false) Boolean tabRef,                                  @RequestBody TableQueryVo tableQueryVo) {        EruptModel eruptModel = EruptCoreService.getErupt(eruptName);        EruptFieldModel eruptFieldModel = eruptModel.getEruptFieldMap().get(fieldName);        PowerObject powerObject = PowerInvoke.getPowerObject(eruptModel);        if (powerObject.isEdit() || powerObject.isAdd()                || eruptFieldModel.getEruptField().edit().search().value()) {            Edit edit = eruptFieldModel.getEruptField().edit();            String dependField = edit.referenceTableType().dependField();            String dependCondition = "";            if (!AnnotationConst.EMPTY_STR.equals(dependField)) {                if (null == dependValue) {                    throw new EruptWebApiRuntimeException("请先选择" + eruptModel.getEruptFieldMap().get(dependField)                            .getEruptField().edit().title());                } else {                    dependCondition = eruptFieldModel.getFieldReturnName() + '.' + edit.referenceTableType().dependColumn() + '=' + dependValue;                }            }            String[] conditions = new String[edit.filter().length + 1];            for (int i = 0; i < edit.filter().length; i++) {                conditions[i] = AnnotationUtil.switchFilterConditionToStr(edit.filter()[i]);            }            conditions[conditions.length - 1] = dependCondition;            EruptModel eruptReferenceModel = EruptCoreService.getErupt(eruptFieldModel.getFieldReturnName());            if (!tabRef) {                //由于类加载顺序问题，并没有选择在启动时检测                ReferenceTableType referenceTableType = eruptFieldModel.getEruptField().edit().referenceTableType();                if (!eruptReferenceModel.getEruptFieldMap().containsKey(referenceTableType.label())) {                    throw new EruptWebApiRuntimeException(eruptReferenceModel.getEruptName() + " not found '"                            + referenceTableType.label() + "' field，please use @ReferenceTableType annotation config");                }            }            return eruptService.getEruptData(eruptReferenceModel, tableQueryVo,                    null, conditions);        } else {            throw new EruptNoLegalPowerException();        }    }    @SneakyThrows    @GetMapping("/depend-tree/{erupt}")    @ResponseBody    @EruptRouter(authIndex = 2, verifyType = EruptRouter.VerifyType.ERUPT)    public Collection<TreeModel> getDependTree(@PathVariable("erupt") String erupt) {        EruptModel eruptModel = EruptCoreService.getErupt(erupt);        String field = eruptModel.getErupt().linkTree().field();        if (null == eruptModel.getEruptFieldMap().get(field)) {            String treeErupt = eruptModel.getClazz().getDeclaredField(field).getType().getSimpleName();            return this.getEruptTreeData(treeErupt);        } else {            return this.getReferenceTree(eruptModel.getEruptName(), field, null);        }    }    @GetMapping("/{erupt}/reference-tree/{fieldName}")    @ResponseBody    @EruptRouter(authIndex = 1, verifyType = EruptRouter.VerifyType.ERUPT)    public Collection<TreeModel> getReferenceTree(@PathVariable("erupt") String erupt,                                                  @PathVariable("fieldName") String fieldName,                                                  @RequestParam(value = "dependValue", required = false) Serializable dependValue) {        EruptModel eruptModel = EruptCoreService.getErupt(erupt);        EruptFieldModel eruptFieldModel = eruptModel.getEruptFieldMap().get(fieldName);        PowerObject powerObject = PowerInvoke.getPowerObject(eruptModel);        if (powerObject.isEdit() || powerObject.isAdd()                || eruptFieldModel.getEruptField().edit().search().value()                || StringUtils.isNotBlank(eruptModel.getErupt().linkTree().field())) {            String dependField = eruptFieldModel.getEruptField().edit().referenceTreeType().dependField();            if (!AnnotationConst.EMPTY_STR.equals(dependField)) {                if (null == dependValue) {                    throw new EruptWebApiRuntimeException("请先选择" + eruptModel.getEruptFieldMap().get(dependField).getEruptField().edit().title());                }            }            Edit edit = eruptFieldModel.getEruptField().edit();            ReferenceTreeType treeType = edit.referenceTreeType();            EruptModel referenceEruptModel = EruptCoreService.getErupt(eruptFieldModel.getFieldReturnName());            if (!referenceEruptModel.getEruptFieldMap().containsKey(treeType.label())) {                throw new EruptWebApiRuntimeException(referenceEruptModel.getEruptName() + " not found " + treeType.label() + " field");            }            List<Condition> conditions = new ArrayList<>();            //处理depend参数代码            if (StringUtils.isNotBlank(treeType.dependField()) && null != dependValue) {                conditions.add(new Condition(edit.referenceTreeType().dependColumn(), dependValue));            }            List<String> conditionStrings = new ArrayList<>();            for (Filter filter : edit.filter()) {                conditionStrings.add(AnnotationUtil.switchFilterConditionToStr(filter));            }            return preEruptDataService.geneTree(referenceEruptModel, treeType.id(), treeType.label(), treeType.pid(), treeType.rootPid(),                    EruptQuery.builder().orderBy(edit.orderBy()).conditionStrings(conditionStrings).conditions(conditions).build());        } else {            throw new EruptNoLegalPowerException();        }    }    @PostMapping("/validate-erupt/{erupt}")    @ResponseBody    @EruptRouter(authIndex = 2, verifyType = EruptRouter.VerifyType.ERUPT)    public EruptApiModel validateErupt(@PathVariable("erupt") String erupt, @RequestBody JsonObject data) {        EruptModel eruptModel = EruptCoreService.getErupt(erupt);        EruptApiModel eruptApiModel = EruptUtil.validateEruptValue(eruptModel, data);        if (eruptApiModel.getStatus() == EruptApiModel.Status.SUCCESS) {            DataProxyInvoke.invoke(eruptModel, (dataProxy -> dataProxy.beforeAdd(gson.fromJson(data.toString(), eruptModel.getClazz()))));        }        eruptApiModel.setErrorIntercept(false);        eruptApiModel.setPromptWay(EruptApiModel.PromptWay.MESSAGE);        return eruptApiModel;    }}
package xyz.erupt.core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.model.BoolAndReason;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.VL;
import xyz.erupt.core.constant.RestPath;
import xyz.erupt.core.model.EruptFieldModel;
import xyz.erupt.core.model.EruptModel;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by liyuepeng on 11/1/18.
 */
public class EruptUtil {

    //数据项与erupt注解中描述不相符时使用
    private static final String NOT_ERUPT_REF = "@NOT_REF@";


    public static String handleNoRightVariable(String pathVariable) {
        if (pathVariable.startsWith(RestPath.NO_RIGHT_SYMBOL)) {
            return pathVariable.substring(2);
        } else {
            throw new RuntimeException("数据参数异常");
        }
    }

    //清空一对多关系数据，防止循环引用导致内存溢出
    public static void rinseEruptObj(Object eruptObj) throws IllegalAccessException {
        Erupt erupt = eruptObj.getClass().getAnnotation(Erupt.class);
        for (Field field : eruptObj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            EruptField eruptField = field.getAnnotation(EruptField.class);
            if (field.getName().equalsIgnoreCase(erupt.primaryKeyCol())) {
                continue;
            }
            if (null != eruptField) {
                if (eruptField.edit().type() == EditType.TAB) {
                    field.set(eruptObj, null);
                }
            } else {
                field.set(eruptObj, null);
            }
        }
    }

    public static Object jsonElementToObject(EruptFieldModel eruptFieldModel, JsonElement jsonElement) {
        if (EruptFieldModel.NUMBER_TYPE.equalsIgnoreCase(eruptFieldModel.getFieldReturnName())) {
            try {
                return jsonElement.getAsInt();
            } catch (Exception e) {
                return 0;
            }
        }
        switch (eruptFieldModel.getEruptField().edit().type()) {
            case SLIDER:
                return jsonElement.getAsInt();
            case BOOLEAN:
                return jsonElement.getAsBoolean();
            case DATE:
                return DateUtil.getDate(jsonElement.getAsString());
            default:
                return jsonElement.getAsString();
        }
    }

    public static BoolAndReason eruptDataToViewData(EruptModel eruptModel, Map<String, Object> dataMap) {
        for (EruptFieldModel field : eruptModel.getEruptFieldModels()) {
            if (field.getEruptField().edit().notNull()) {
                if (!dataMap.containsKey(field.getFieldName())) {
                    return new BoolAndReason(false, field.getEruptField().edit().title() + "必填");
                }
            }
        }
        return new BoolAndReason(true, "");
    }

    public static BoolAndReason eruptDataToViewData(EruptModel eruptModel, JsonObject data) {
        for (EruptFieldModel field : eruptModel.getEruptFieldModels()) {
            if (field.getEruptField().edit().notNull()) {
                if (!data.has(field.getFieldName())) {
                    return new BoolAndReason(false, field.getEruptField().edit().title() + "必填");
                }
            }
        }
        return new BoolAndReason(true, "");
    }


    public Map<String, Object> eruptDataToViewData(Object data) {
        Map<String, Object> result = new HashMap<>();
        try {
            for (Field field : data.getClass().getDeclaredFields()) {
                EruptField eruptField = field.getAnnotation(EruptField.class);
                Object fieldData = field.get(data);
                if (null != eruptField) {
                    switch (eruptField.edit().type()) {
                        case INPUT:
                            result.put(field.getName(), fieldData);
                            break;
                        case CHOICE:
                            if (StringUtils.isNotBlank(fieldData.toString())) {
                                for (VL vl : eruptField.edit().choiceType()[0].vl()) {
                                    if ((vl.value() + "").equals(fieldData.toString())) {
                                        result.put(field.getName(), fieldData);
                                        break;
                                    }
                                }
                                //如果与VL注解无匹配项则注入该标识信息
                                if (StringUtils.isBlank(result.get(field.getName()).toString())) {
                                    result.put(field.getName(), NOT_ERUPT_REF);
                                }
                            } else {
                                result.put(field.getName(), NOT_ERUPT_REF);
                            }
                            break;
                        case BOOLEAN:
                            if (StringUtils.isNotBlank(fieldData.toString())) {
                                Boolean boolField = Boolean.valueOf(fieldData.toString());
                                if (boolField) {
                                    result.put(field.getName(), eruptField.edit().boolType()[0].trueText());
                                } else {
                                    result.put(field.getName(), eruptField.edit().boolType()[0].falseText());
                                }
                            } else {
                                result.put(field.getName(), NOT_ERUPT_REF);
                            }
                            break;
                        case REFERENCE:
                            if (StringUtils.isNotBlank(fieldData.toString())) {
                                for (View view : eruptField.views()) {
                                    result.put(field.getName() + "_" + view.column(),
                                            fieldData.getClass().getDeclaredField(view.column()).get(fieldData));
                                }
                            } else {
                                result.put(field.getName(), NOT_ERUPT_REF);
                            }
                            break;
                        default:
                            result.put(field.getName(), field.get(data));
                            break;
                    }
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return result;
    }


}

package xyz.erupt.core.proxy;

import lombok.SneakyThrows;
import org.aopalliance.intercept.MethodInvocation;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.core.proxy.erupt_field.EditProxy;
import xyz.erupt.core.proxy.erupt_field.ViewProxy;

import java.util.ArrayList;
import java.util.List;

/**
 * @author YuePeng
 * date 2022/2/5 14:19
 */
public class EruptFieldProxy extends AnnotationProxy<EruptField, Void> {

    private final AnnotationProxy<Edit, EruptField> editAnnotationProxy = new EditProxy();

    @Override
    @SneakyThrows
    protected Object invocation(MethodInvocation invocation) {
        switch (invocation.getMethod().getName()) {
            case "views":
                View[] views = this.rawAnnotation.views();
                List<View> proxyViews = new ArrayList<>();
                for (View view : views) {
                    proxyViews.add(AnnotationProxyPool.getOrPut(view, annotation ->
                            new ViewProxy().newProxy(annotation, this)
                    ));
                }
                return proxyViews.toArray(new View[0]);
            case "edit":
                return AnnotationProxyPool.getOrPut(this.rawAnnotation.edit(), annotation ->
                        editAnnotationProxy.newProxy(annotation, this)
                );
        }
        return this.invoke(invocation);
    }

}

package com.e16din.screensadapter.annotation.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface View {
    //todo: как сделать вызов элементов списка с привязкой к основному Screen,
    //todo:  с привязкой к основному Binder?

    //todo: Как сделать презентер с логикой для любого View?
    //todo: Кто будет вызывать этот презентер? Зачем он нужен когда есть саппорт биндеры? Не нужен.

    //todo: Как сделать презентер с логикой для любого элемента списка?
    //todo: Кто будет вызывать этот презентер? Этот презентер будет вызывать адаптер на onBind()
}
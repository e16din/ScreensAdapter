package com.e16din.screensadapter.annotation.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Screen {
    Class data() default Object.class;

    boolean isDataNullable() default false;

    Class parent() default Object.class;

    boolean saveOnHide() default true;
}
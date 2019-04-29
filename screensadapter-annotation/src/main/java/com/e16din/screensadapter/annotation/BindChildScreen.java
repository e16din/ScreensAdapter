package com.e16din.screensadapter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BindChildScreen {
    Class parentScreen();

    Class screen();

    Class startAfter() default Object.class;

    Class startBefore() default Object.class;
}
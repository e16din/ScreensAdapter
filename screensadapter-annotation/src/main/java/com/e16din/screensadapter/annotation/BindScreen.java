package com.e16din.screensadapter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BindScreen {
    Class screen();

    Class[] supportScreens() default {};

    Class startAfter() default Object.class;

    Class startBefore() default Object.class;
}
package com.bytearch.nebula.orm.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface VID {
    String type() default "string";
}

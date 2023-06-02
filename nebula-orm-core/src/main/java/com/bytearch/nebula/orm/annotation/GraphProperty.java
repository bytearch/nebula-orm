package com.bytearch.nebula.orm.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface GraphProperty {
    String name() default "";
}

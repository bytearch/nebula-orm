package com.bytearch.nebula.orm.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphVertex {
    /**
     * 空间
     * @return
     */
    String space() default "default";

    String tag()  default "";
}

package com.bytearch.nebula.orm.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GraphEdge {
    /**
     * 空间
     * @return
     */
    String space() default "default";

    String edge();

}

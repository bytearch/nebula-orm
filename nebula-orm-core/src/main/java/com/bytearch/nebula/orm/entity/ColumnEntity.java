package com.bytearch.nebula.orm.entity;

import lombok.Data;

@Data
public class ColumnEntity {
    /**
     * 字段名
     */
    private String column;
    /**
     * 值
     */
    private Object value;

    private OperatorEnum operatorEnum;
}

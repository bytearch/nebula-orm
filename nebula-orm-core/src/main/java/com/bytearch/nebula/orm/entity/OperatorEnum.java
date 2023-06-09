package com.bytearch.nebula.orm.entity;

//操作枚举
public enum OperatorEnum {
    EQUAL("="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    NOT_EQUAL("<>"),
    IN("in"),
    NOT_IN("not in");
    //BETWEEN("between"),
    //NOT_BETWEEN("not between");

    private final String symbol;

    OperatorEnum(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}

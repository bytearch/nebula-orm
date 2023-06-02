package com.bytearch.nebula.orm.exception;


public class NebulaOrmException extends RuntimeException {

    private String msg;

    public NebulaOrmException(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "NebulaOrmException{" +
                "msg='" + msg + '\'' +
                '}';
    }
}
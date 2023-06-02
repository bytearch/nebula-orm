package com.bytearch.nebula.orm.constant;
import java.util.List;

public class NebulaResult<T> {
    private Integer code;
    private String message;
    private List<T> data;

    public boolean isSuccessed(){
        return code == 0;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
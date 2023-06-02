package com.bytearch.nebula.orm.curd;

import com.bytearch.nebula.orm.constant.NebulaResult;

public interface VertexCurdDao<T, ID> {
    void insert(T entity);

    int update(T entity);

    T getOne(ID vid);

    public <R> NebulaResult<R> execute(String stmt, Class<R> tClass);
}

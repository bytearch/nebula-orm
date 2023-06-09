package com.bytearch.nebula.orm.curd;

import java.util.Map;

public interface GraphQuery {
    public String buildSql();


    /**
     * limit
     *
     * @param size 条数
     * @return 查询API
     */
    public GraphQuery limit(int size);

    /**
     * limit
     * @param offset 偏移量
     * @param size 条数
     * @return 查询API
     */
    public GraphQuery limit(int offset, int size);


    /**
     * 添加yield关键字
     * @return 查询API
     */
    public GraphQuery yield();


    /**
     * 查询哪个标签的哪些属性
     * @param clazz 类类型
     * @param fields 字段
     * @return 查询API
     */
    public GraphQuery yield(Class clazz, String... fields);


    /**
     * 查询哪些属性
     *
     * @param fields 字段
     * @return 查询API
     */
    public GraphQuery yield(String... fields);


    /**
     * 查询哪些属性
     *
     * @param fieldAlias 字段与别名映射
     * @return 查询API
     */
    public GraphQuery yield(Map<String, String> fieldAlias);

    public GraphQuery pipe();


}

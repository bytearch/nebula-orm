package com.bytearch.nebula.orm.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GraphEdgeEntity {
    private String space;
    private String edgeName;
    /**
     * 边类型属性集合
     */
    private List<String> edgeList;
    /**
     * 边类型属性值集合
     */
    private List<Object> edgeValueList;
}

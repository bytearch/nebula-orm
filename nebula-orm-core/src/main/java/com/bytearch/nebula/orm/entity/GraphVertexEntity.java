package com.bytearch.nebula.orm.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GraphVertexEntity {
    private String space;
    private String tagName;
    private List<String> tagList;
    private Object vid;
    private List<Object> tagValueList;
}

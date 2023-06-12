package com.bytearch.nebula.orm.config;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class GraphGroupConfig implements Serializable {
    /**
     * 配置对象  key: name 名称 value: 配置
     */
    private Map<String, NebulaGraphProperties> group;
}

package com.bytearch.nebula.orm.config;

import lombok.Data;

import java.util.Map;

@Data
public class GraphGroupConfig {
    /**
     * 配置对象  key: name 名称 value: 配置
     */
    private Map<String, NebulaGraphProperties> properties;
}

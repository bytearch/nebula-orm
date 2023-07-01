package com.bytearch.nebula.orm.pool;

import com.bytearch.nebula.orm.config.GraphGroupConfig;
import com.bytearch.nebula.orm.config.NebulaGraphProperties;
import com.bytearch.nebula.orm.exception.NebulaOrmException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GraphSessionContainer {
    /**
     * 对象池
     */
    private final static ConcurrentHashMap<String, GraphSessionMapper> CP = new ConcurrentHashMap<>();
    /**
     * 属性
     */
    private final static Map<String, NebulaGraphProperties> nebulaGraphPropertiesMap = new HashMap<>();


    /**
     * 初始化 多个
     * @param graphGroupConfig
     */
    public static void sessionManagerInit(GraphGroupConfig graphGroupConfig) {
        for (Map.Entry<String, NebulaGraphProperties> nebulaGraphPropertiesEntry : graphGroupConfig.getGroup().entrySet()) {
            CP.put(nebulaGraphPropertiesEntry.getKey(), GraphSessionMapperFactory.create(nebulaGraphPropertiesEntry.getValue(), nebulaGraphPropertiesEntry.getKey()));
            nebulaGraphPropertiesMap.put(nebulaGraphPropertiesEntry.getKey(), nebulaGraphPropertiesEntry.getValue());
        }
    }
    public static GraphSessionMapper getSession(String groupName) {
        if (StringUtils.isBlank(groupName)) {
            throw new NebulaOrmException("space cannot be empty!");
        }
        GraphSessionMapper graphSessionMapper = CP.get(groupName);
        if (graphSessionMapper == null) {
            NebulaGraphProperties nebulaGraphProperties = nebulaGraphPropertiesMap.get(groupName);
            if (nebulaGraphProperties == null) {
                throw new NebulaOrmException("nebulaGraphProperties cannot be empty");
            }
            synchronized (GraphSessionContainer.class) {
                graphSessionMapper = CP.get(groupName);
                if (graphSessionMapper == null) {
                   graphSessionMapper = GraphSessionMapperFactory.create(nebulaGraphProperties, groupName);
                }
            }
        }
       return graphSessionMapper;
    }

    public static void  clearAll() {
        for (Map.Entry<String, GraphSessionMapper> graphSessionMapperEntry : CP.entrySet()) {
            graphSessionMapperEntry.getValue().destroy();
            log.info("执行graph session清理成功 group:{}", graphSessionMapperEntry.getKey());
        }
        CP.clear();
    }
}

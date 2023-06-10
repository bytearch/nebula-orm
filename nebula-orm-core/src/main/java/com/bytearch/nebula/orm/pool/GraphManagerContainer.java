package com.bytearch.nebula.orm.pool;

import com.bytearch.nebula.orm.config.GraphGroupConfig;
import com.bytearch.nebula.orm.config.NebulaGraphProperties;
import com.bytearch.nebula.orm.exception.NebulaOrmException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GraphManagerContainer {
    /**
     * 对象池
     */
    private final static ConcurrentHashMap<String, GraphSessionManager> CP = new ConcurrentHashMap<>();
    /**
     * 属性
     */
    private final static Map<String, NebulaGraphProperties> nebulaGraphPropertiesMap = new HashMap<>();


    public static void sessionManagerInit(GraphGroupConfig graphGroupConfig) {
        for (Map.Entry<String, NebulaGraphProperties> nebulaGraphPropertiesEntry : graphGroupConfig.getProperties().entrySet()) {
            GraphSessionManager graphSessionManager = new GraphSessionManager(nebulaGraphPropertiesEntry.getKey(), nebulaGraphPropertiesEntry.getValue());
            CP.put(nebulaGraphPropertiesEntry.getKey(), graphSessionManager);
            nebulaGraphPropertiesMap.put(nebulaGraphPropertiesEntry.getKey(), nebulaGraphPropertiesEntry.getValue());
        }
    }
    public static void release(GraphSession session) {
        GraphSessionManager graphSessionManager = CP.get(session.getGroupName());
        if (graphSessionManager == null) {
            return;
        }
        graphSessionManager.releaseSession(session);
    }

    public static void close(GraphSession session) {
        GraphSessionManager graphSessionManager = CP.get(session.getGroupName());
        if (graphSessionManager == null) {
            return;
        }
        graphSessionManager.closeSession(session);
    }
    public static GraphSession getSession(String groupName) {
        if (StringUtils.isBlank(groupName)) {
            throw new NebulaOrmException("space cannot be empty!");
        }
        GraphSessionManager graphSessionManager = CP.get(groupName);
        if (graphSessionManager == null) {
            NebulaGraphProperties nebulaGraphProperties = nebulaGraphPropertiesMap.get(groupName);
            if (nebulaGraphProperties == null) {
                throw new NebulaOrmException("nebulaGraphProperties cannot be empty");
            }
            synchronized (GraphManagerContainer.class) {
                graphSessionManager = CP.get(groupName);
                if (graphSessionManager == null) {
                    graphSessionManager = new GraphSessionManager(groupName, nebulaGraphProperties);
                }
            }
        }
       return graphSessionManager.getSession();
    }

    public static GraphSessionManager getGraphSessionManager(String name) {
       return CP.get(name);
    }
}

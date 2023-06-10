package com.bytearch.nebula.orm.pool;

import com.bytearch.nebula.orm.config.NebulaGraphProperties;
import com.bytearch.nebula.orm.exception.NebulaOrmException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GraphManagerPool {
    /**
     * 对象池
     */
    private final static ConcurrentHashMap<String, GraphSessionManager> CP = new ConcurrentHashMap<>();
    /**
     * 属性
     */
    private final static Map<String, NebulaGraphProperties> nebulaGraphPropertiesMap = new HashMap<>();


    public static void initNebulaSessionManager(List<NebulaGraphProperties> nebulaGraphProperties) {
        for (NebulaGraphProperties nebulaGraphProperty : nebulaGraphProperties) {
            GraphSessionManager graphSessionManager = new GraphSessionManager();
            graphSessionManager.setNebulaGraphProperties(nebulaGraphProperty);
            graphSessionManager.init();
            CP.put(nebulaGraphProperty.getName(), graphSessionManager);
            nebulaGraphPropertiesMap.put(nebulaGraphProperty.getName(), nebulaGraphProperty);
        }
    }
    public static void release(GraphSession session) {
        GraphSessionManager graphSessionManager = CP.get(session.getName());
        if (graphSessionManager == null) {
            return;
        }
        graphSessionManager.releaseSession(session);
    }

    public static void close(GraphSession session) {
        GraphSessionManager graphSessionManager = CP.get(session.getName());
        if (graphSessionManager == null) {
            return;
        }
        graphSessionManager.closeSession(session);
    }
    public static GraphSession getSession(String name) {
        if (StringUtils.isBlank(name)) {
            throw new NebulaOrmException("space cannot be empty!");
        }
        GraphSessionManager graphSessionManager = CP.get(name);
        if (graphSessionManager == null) {
            NebulaGraphProperties nebulaGraphProperties = nebulaGraphPropertiesMap.get(name);
            if (nebulaGraphProperties == null) {
                throw new NebulaOrmException("nebulaGraphProperties cannot be empty");
            }
            synchronized (GraphManagerPool.class) {
                graphSessionManager = CP.get(name);
                if (graphSessionManager == null) {
                    graphSessionManager = new GraphSessionManager();
                    //创建nebulaSessionManager
                    graphSessionManager.init(nebulaGraphProperties);
                }
            }
        }
       return graphSessionManager.getSession();
    }

    public static GraphSessionManager getGraphSessionManager(String name) {
       return CP.get(name);
    }
}

package com.bytearch.nebula.orm.pool;

import com.bytearch.nebula.orm.config.NebulaGraphProperties;
import com.bytearch.nebula.orm.exception.NebulaOrmException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GraphManagerPool {
    private final static ConcurrentHashMap<String, GraphSessionManager> CP = new ConcurrentHashMap<>();

    private final static Map<String, NebulaGraphProperties> nebulaGraphPropertiesMap = new HashMap<>();


    public void initNebulaSessionManager(List<NebulaGraphProperties> nebulaGraphProperties) {
        for (NebulaGraphProperties nebulaGraphProperty : nebulaGraphProperties) {
            GraphSessionManager graphSessionManager = new GraphSessionManager();
            graphSessionManager.setNebulaGraphProperties(nebulaGraphProperty);
            graphSessionManager.init();
        }
    }
    public static GraphSessionManager getSession(String space) {
        if (StringUtils.isBlank(space)) {
            throw new NebulaOrmException("space cannot be empty!");
        }
        NebulaGraphProperties nebulaGraphProperties = nebulaGraphPropertiesMap.get(space);
        GraphSessionManager graphSessionManager = CP.get(space);
        if (graphSessionManager == null) {
            synchronized (GraphManagerPool.class) {
                graphSessionManager = CP.get(space);
                if (graphSessionManager == null) {
                    //创建nebulaSessionManager
                    graphSessionManager.init(nebulaGraphProperties);
                }
            }
        }
        return graphSessionManager;
    }
}

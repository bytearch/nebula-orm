package com.bytearch.nebula.orm.pool;

import com.bytearch.nebula.orm.config.NebulaGraphProperties;
import com.bytearch.nebula.orm.exception.NebulaOrmException;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Session工厂
 */
@Slf4j
public class SessionFactory {
    private static ConcurrentHashMap<String, NebulaPool> concurrentHashMap = new ConcurrentHashMap<>();
    public static GraphSession create(NebulaGraphProperties nebulaGraphProperties, String groupName, NebulaPool nebulaPool) {
        try {
            Session session = nebulaPool.getSession(nebulaGraphProperties.getUserName(), nebulaGraphProperties.getPassword(), true);
            session.execute("USE " + nebulaGraphProperties.getSpace() + ";");
            return new GraphSession(session, groupName, nebulaGraphProperties.getSpace());
        } catch (Exception e) {
            log.error("create session error e:", e);
            throw new NebulaOrmException(e.getMessage());
        }
    }
}

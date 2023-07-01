package com.bytearch.nebula.orm.pool;

import com.bytearch.nebula.orm.config.NebulaGraphProperties;
import com.bytearch.nebula.orm.exception.NebulaOrmException;
import com.vesoft.nebula.client.graph.SessionPool;
import com.vesoft.nebula.client.graph.SessionPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Session工厂
 */
@Slf4j
public class GraphSessionMapperFactory {
    private static List<HostAddress> getGraphHostPort(String hostAndPortStr) {
        String[] hostAndPort= hostAndPortStr.split(";");
        List<HostAddress> hostAddresses = new ArrayList<>();
        for (String address : hostAndPort) {
            String[] split = address.split(":");
            HostAddress hostAddress = new HostAddress(split[0],
                    Integer.parseInt(split[1]));
            hostAddresses.add(hostAddress);
        }
        return hostAddresses;
    }
    public static GraphSessionMapper create(NebulaGraphProperties nebulaGraphProperties, String groupName) {
        try {
            List<HostAddress> addresses = getGraphHostPort(nebulaGraphProperties.getHostAddresses());
            SessionPoolConfig sessionPoolConfig = new SessionPoolConfig(addresses, nebulaGraphProperties.getSpace(), nebulaGraphProperties.getUserName(), nebulaGraphProperties.getPassword());
            if (nebulaGraphProperties.getMinConnsSize() != 0) sessionPoolConfig.setMinSessionSize(nebulaGraphProperties.getMinConnsSize());
            if (nebulaGraphProperties.getMaxConnsSize() != 0) sessionPoolConfig.setMaxSessionSize(nebulaGraphProperties.getMaxConnsSize());
            SessionPool sessionPool = new SessionPool(sessionPoolConfig);
            log.info("init session pool config:{}", sessionPoolConfig);
            if (!sessionPool.init()) {
                log.error("session pool init failed.");
                throw new Exception("session pool init failed.");
            }
            return GraphSessionMapper.builder().sessionPool(sessionPool)
                    .space(nebulaGraphProperties.getSpace())
                    .groupName(groupName)
                    .build();
        } catch (Exception e) {
            log.error("create session error e:", e);
            throw new NebulaOrmException(e.getMessage());
        }
    }
}

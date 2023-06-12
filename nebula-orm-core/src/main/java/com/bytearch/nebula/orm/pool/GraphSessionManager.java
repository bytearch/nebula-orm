package com.bytearch.nebula.orm.pool;

import com.bytearch.nebula.orm.config.NebulaGraphProperties;
import com.bytearch.nebula.orm.exception.NebulaOrmException;
import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class GraphSessionManager implements Serializable {
    private static Integer maxIdeTimeSecond = 60 * 1000;
    private static Integer maxConnectionSize = 500;
    private static Integer minConnectionSize = 50;
    private static Integer sessionGetMaxWaitTime = 60 * 1000;

    private static Integer cacheTime = 60000;

    /**
     * 组名
     */
    private String groupName;

    private NebulaGraphProperties nebulaGraphProperties;

    private NebulaPool pool;

    /**
     * total create connection pool
     */
    private AtomicInteger totalCount = new AtomicInteger();

    /**
     * free connection count
     */
    private AtomicInteger freeCount = new AtomicInteger();

    /**
     * lock
     */
    private ReentrantLock lock = new ReentrantLock(true);

    private Condition condition = lock.newCondition();

    /**
     * free connections
     */
    private LinkedList<GraphSession> freeSessions = new LinkedList<GraphSession>();

    public GraphSessionManager(String groupName, NebulaGraphProperties nebulaGraphProperties) {
        this.groupName = groupName;
        this.nebulaGraphProperties = nebulaGraphProperties;
        init();
    }

    private List<HostAddress> getGraphHostPort(List<String> hostAndPort) {
        List<HostAddress> hostAddresses = new ArrayList<>();
        for (String address : hostAndPort) {
            String[] split = address.split(":");
            HostAddress hostAddress = new HostAddress(split[0],
                    Integer.parseInt(split[1]));
            hostAddresses.add(hostAddress);
        }
        return hostAddresses;
    }

    private void init() {
        init(this.nebulaGraphProperties);
    }

    private void init(NebulaGraphProperties nebulaGraphProperties) {
        //check param
        checkParam(nebulaGraphProperties);
        this.nebulaGraphProperties = nebulaGraphProperties;
        List<HostAddress> hostAndPorts = getGraphHostPort(nebulaGraphProperties.getHostAddresses());
        if (nebulaGraphProperties.getMinConnsSize() != 0) minConnectionSize = nebulaGraphProperties.getMinConnsSize();
        if (nebulaGraphProperties.getMaxConnsSize() != 0) maxConnectionSize = nebulaGraphProperties.getMaxConnsSize();
        this.pool = new NebulaPool();
        NebulaPoolConfig nebulaPoolConfig = new NebulaPoolConfig();
        nebulaPoolConfig = nebulaPoolConfig.setMaxConnSize(maxConnectionSize);
        nebulaPoolConfig = nebulaPoolConfig.setMinConnSize(minConnectionSize);
        if (nebulaGraphProperties.getIdleTime() > 0) {
            nebulaPoolConfig.setIdleTime(nebulaGraphProperties.getIdleTime() + 1000);
        }
        boolean init = false;
        try {
            init = pool.init(hostAndPorts, nebulaPoolConfig);
            log.info("nebula group:{} 连接池init, 是否成功:{}", this.getGroupName(), init);
            if (!init) {
                throw new RuntimeException("Nebula连接初始化失败");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        if (nebulaGraphProperties.isUseCache()) {
            for (int i = 0; i < minConnectionSize; i++) {
                GraphSession graphSession = SessionFactory.create(nebulaGraphProperties, groupName, pool);
                freeSessions.add(graphSession);
                freeCount.incrementAndGet();
                totalCount.incrementAndGet();
            }
        }
    }

    private void checkParam(NebulaGraphProperties nebulaGraphProperties) {
        if (StringUtils.isBlank(nebulaGraphProperties.getSpace())) {
            throw new NebulaOrmException("space cannot be empty");
        }
        if (CollectionUtils.isEmpty(nebulaGraphProperties.getHostAddresses())) {
            throw new NebulaOrmException("nebula host address cannot be empty");
        }
    }

    public GraphSession getSession() throws NebulaOrmException {
        if (nebulaGraphProperties.isUseCache()) {
            Long start = System.currentTimeMillis();
            lock.lock();
            try {
                GraphSession graphSession = null;
                while (true) {
                    if (freeCount.get() > 0) {
                        //has free session
                        freeCount.decrementAndGet();
                        graphSession = freeSessions.poll();
                        if (graphSession != null) {
                            if ((System.currentTimeMillis() - graphSession.getLastAccessTime()) > maxIdeTimeSecond || !graphSession.ping()) {
                                closeSession(graphSession);
                                continue;
                            }
                           /* if (graphSession.isNeedActiveTest()) {
                                boolean isActive = false;
                                try {
                                    isActive = graphSession.ping();
                                } catch (NebulaOrmException e) {
                                    HostAddress graphAddress = graphSession.getSession().getGraphHost();
                                    log.error("ping to server[" + graphAddress.getHost() + ":" + graphAddress.getPort() + "] active test error ,emsg:" + e.getMessage());
                                    isActive = false;
                                }
                                if (!isActive) {
                                    closeSession(graphSession);
                                    continue;
                                } else {
                                    graphSession.setNeedActiveTest(false);
                                }
                            }*/
                        } else if (totalCount.get() < maxConnectionSize) {
                            //free is empty but cannot create
                            graphSession = SessionFactory.create(nebulaGraphProperties, groupName, pool);
                            totalCount.incrementAndGet();
                        } else {
                            //wait single to free
                            try {
                                if (condition.await(sessionGetMaxWaitTime, TimeUnit.MILLISECONDS)) {
                                    //wait single success
                                    continue;
                                }
                                throw new NebulaOrmException(String.format("get nebula session timeout:%s, properties:%s", sessionGetMaxWaitTime, nebulaGraphProperties));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                throw new NebulaOrmException(String.format("get nebula session error:%s, properties:%s", sessionGetMaxWaitTime, nebulaGraphProperties));

                            }
                        }
                    }
                    log.debug("获取Session:{} 耗时 :{} ms", graphSession, System.currentTimeMillis() - start);
                    return graphSession;
                }
            } finally {
                lock.unlock();
            }
        } else {
            return SessionFactory.create(nebulaGraphProperties, groupName, pool);
        }
    }

    public void releaseSession(GraphSession connection) {
        if (connection == null) {
            return;
        }
        if (connection.getUseCache()) {
            lock.lock();
            try {
                connection.setLastAccessTime(System.currentTimeMillis());
                freeSessions.add(connection);
                freeCount.incrementAndGet();
                condition.signal();
            } finally {
                lock.unlock();
            }
        } else {
            connection.getSession().release();
        }
        log.info("release session :{}, useCache:{}", connection.getSession(), connection.getUseCache());

    }

    public void closeSession(GraphSession graphSession) {
        if (graphSession == null) {
            return;
        }
        //不管是否启用cache 都是release
        try {
            if (graphSession.getUseCache()) {
                totalCount.decrementAndGet();
            }
            graphSession.getSession().release();
        } catch (Exception e) {
            log.error("close nebula session error space:[{}], host:[{}:{}] emsg:" + e.getMessage(), graphSession.getGroupName(), graphSession.getSession().getGraphHost().getHost(), graphSession.getSession().getGraphHost().getPort());
            e.printStackTrace();
        }
        log.info("close session :{}, useCache:{}", graphSession.getSession(), graphSession.getUseCache());
    }

    public void setActiveTestFlag() {
        if (freeCount.get() > 0) {
            lock.lock();
            try {
                for (GraphSession freeSession : freeSessions) {
                    freeSession.setNeedActiveTest(true);
                }
            } finally {
                lock.unlock();
            }
        }
    }


    @Override
    public String toString() {
        return "ConnectionManager{" +
                "groupName" + groupName +
                ", totalCount=" + totalCount +
                ", freeCount=" + freeCount +
                ", freeConnections =" + freeSessions +
                '}';
    }

    public NebulaGraphProperties getNebulaGraphProperties() {
        return nebulaGraphProperties;
    }

    public void setNebulaGraphProperties(NebulaGraphProperties nebulaGraphProperties) {
        this.nebulaGraphProperties = nebulaGraphProperties;
    }

    public String getGroupName() {
        return groupName;
    }
}

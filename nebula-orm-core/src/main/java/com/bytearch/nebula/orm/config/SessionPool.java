package com.bytearch.nebula.orm.config;

import com.bytearch.nebula.orm.exception.NebulaOrmException;
import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.exception.AuthFailedException;
import com.vesoft.nebula.client.graph.exception.ClientServerIncompatibleException;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.exception.NotValidConnectionException;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.annotation.PreDestroy;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * @ClassName: SessionPool
 */
@Slf4j
public class SessionPool {

    /**
     * 创建连接池
     *
     * @param maxSessionCount 默认创建连接数
     * @param minSessionCount 最大创建连接数
     * @param hostAndPort     机器端口列表
     * @param userName        用户名
     * @param passWord        密码
     * @throws UnknownHostException
     * @throws NotValidConnectionException
     * @throws IOErrorException
     * @throws AuthFailedException
     */
    public SessionPool(int maxSessionCount, int minSessionCount, List<String> hostAndPort, String userName, String passWord) throws UnknownHostException, NotValidConnectionException, IOErrorException, AuthFailedException, ClientServerIncompatibleException {
        try {
            if (CollectionUtils.isEmpty(hostAndPort)) {
                throw new NebulaOrmException("nebula host cannot be empty! please check your nebula config!");
            }
            this.minCountSession = minSessionCount;
            this.maxCountSession = maxSessionCount;
            this.userName = userName;
            this.passWord = passWord;
            this.queue = new LinkedBlockingQueue<>(minSessionCount);
            this.pool = this.initGraphClient(hostAndPort, maxSessionCount, minSessionCount);
            initSession();
        } catch (UnknownHostException | NotValidConnectionException | IOErrorException | AuthFailedException| ClientServerIncompatibleException e) {
            log.error("nebula init session error! e{}", e.getMessage());
            throw e;
        }
    }

    public SessionPool(NebulaGraphProperties graphProperties) throws UnknownHostException, IOErrorException, AuthFailedException, ClientServerIncompatibleException, NotValidConnectionException {
        this(graphProperties.getMaxConnsSize(), graphProperties.getMinConnsSize(), graphProperties.getHostAddresses(), graphProperties.getUserName(), graphProperties.getPassword());
    }

    public Session borrow() {
        Session se = queue.poll();
        if (se != null) {
            return se;
        }
        try {
            return this.pool.getSession(userName, passWord, true);
        } catch (Exception e) {
            log.error("execute borrow session fail, detail: ", e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void release() {
        Queue<Session> queue = this.queue;
        for (Session se : queue) {
            if (se != null) {
                boolean success = this.queue.offer(se);
                if (!success) {
                    se.release();
                }
            }
        }
    }

    public void close() {
        this.pool.close();
    }

    private void initSession() throws NotValidConnectionException, IOErrorException, AuthFailedException, ClientServerIncompatibleException {
        for (int i = 0; i < minCountSession; i++) {
            queue.offer(this.pool.getSession(userName, passWord, true));
        }
    }

    private NebulaPool initGraphClient(List<String> hostAndPort, int maxConnSize, int minCount) throws UnknownHostException {
        List<HostAddress> hostAndPorts = getGraphHostPort(hostAndPort);
        NebulaPool pool = new NebulaPool();
        NebulaPoolConfig nebulaPoolConfig = new NebulaPoolConfig();
        nebulaPoolConfig = nebulaPoolConfig.setMaxConnSize(maxConnSize);
        nebulaPoolConfig = nebulaPoolConfig.setMinConnSize(minCount);
        nebulaPoolConfig = nebulaPoolConfig.setIdleTime(1000 * 600);
        boolean init = pool.init(hostAndPorts, nebulaPoolConfig);
        if (!init) {
            throw new RuntimeException("Nebula连接初始化失败");
        }
        return pool;
    }

    private List<HostAddress> getGraphHostPort(List<String> hostAndPort) {
        return hostAndPort.stream().map(item -> {
            String[] splitList = item.split(":");
            return new HostAddress(splitList[0], Integer.parseInt(splitList[1]));
        }).collect(Collectors.toList());
    }

    private Queue<Session> queue;

    private String userName;

    private String passWord;

    private int minCountSession;

    private int maxCountSession;

    private NebulaPool pool;

}

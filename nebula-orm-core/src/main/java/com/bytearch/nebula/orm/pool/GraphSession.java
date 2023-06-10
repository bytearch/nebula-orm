package com.bytearch.nebula.orm.pool;

import com.vesoft.nebula.client.graph.net.Session;
import lombok.Data;

import java.io.Serializable;

@Data
public class GraphSession implements Serializable {
    private Session session;
    private String groupName;
    private String space;
    private Long lastAccessTime = System.currentTimeMillis();

    /**
     * 是否需要探活
     */
    private boolean needActiveTest = false;


    public  void release() {
        GraphManagerPool.release(this);
    }

    public  void close() {
        GraphManagerPool.close(this);
    }

    private GraphSession() {

    }
    public GraphSession(Session session, String groupName, String space) {
        this.session = session;
        this.groupName = groupName;
        this.space = space;
    }

    public boolean ping() {
        return this.session.ping();
    }

    public static GraphSessionManager getSessionManager(String name) {
        return GraphManagerPool.getGraphSessionManager(name);
    }

    @Override
    public String toString() {
        return "GraphSession{" +
                "session=" + session +
                ", name='" + groupName + '\'' +
                ", space='" + space + '\'' +
                ", lastAccessTime=" + lastAccessTime +
                ", needActiveTest=" + needActiveTest +
                '}';
    }
}

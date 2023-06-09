package com.bytearch.nebula.orm.pool;

import com.vesoft.nebula.client.graph.net.Session;
import lombok.Data;

import java.io.Serializable;

@Data
public class GraphSession implements Serializable {
    private Session session;
    private String groupName;
    private String space;
    private Boolean useCache;
    private Long lastAccessTime = System.currentTimeMillis();

    /**
     * 是否需要探活
     */
    private boolean needActiveTest = false;


    public  void release() {
        GraphSessionContainer.release(this);
    }

    public  void close() {
        GraphSessionContainer.close(this);
    }

    private GraphSession() {

    }
    public GraphSession(Session session, String groupName, String space, Boolean useCache) {
        this.session = session;
        this.groupName = groupName;
        this.space = space;
        this.useCache = useCache;
    }

    public boolean ping() {
        return this.session.ping();
    }

    public static GraphSessionManager getSessionManager(String name) {
        return GraphSessionContainer.getGraphSessionManager(name);
    }

    @Override
    public String toString() {
        return "GraphSession{" +
                "session=" + session +
                ", name='" + groupName + '\'' +
                ", space='" + space + '\'' +
                ", lastAccessTime=" + lastAccessTime +
                ", needActiveTest=" + needActiveTest +
                ", useCache=" + useCache +
                '}';
    }
}

package com.bytearch.nebula.orm.pool;

import com.vesoft.nebula.client.graph.net.Session;
import lombok.Data;

import java.io.Serializable;

@Data
public class GraphSession implements Serializable {
    private Session session;
    private String space;
    private Long lastAccessTime = System.currentTimeMillis();
    private GraphSessionManager manager;
    /**
     * 是否需要探活
     */
    private boolean needActiveTest = false;


    public  void release() {
        manager.releaseSession(this);
    }

    public  void close() {
        manager.closeSession(this);
    }

    public GraphSession(Session session, GraphSessionManager manager) {
        this.session = session;
        this.manager = manager;
        this.space = manager.getSpace();
    }

    public boolean ping() {
        return this.session.ping();
    }

    public GraphSessionManager getManager() {
        return manager;
    }

    public void setManager(GraphSessionManager manager) {
        this.manager = manager;
    }
}

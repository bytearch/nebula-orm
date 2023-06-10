package com.bytearch.nebula.orm.pool;

import com.vesoft.nebula.client.graph.net.Session;
import lombok.Data;

import java.io.Serializable;

@Data
public class GraphSession implements Serializable {
    private Session session;
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

    public GraphSession(Session session, GraphSessionManager manager) {
        this.session = session;
        this.space = manager.getSpace();
    }

    public boolean ping() {
        return this.session.ping();
    }

    public static GraphSessionManager getSessionManager(String space) {
        return GraphManagerPool.getGraphSessionManager(space);
    }
}

package com.bytearch.nebula.orm.pool;

import com.vesoft.nebula.client.graph.SessionPool;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class GraphSessionMapper implements Serializable {
    //private SessionWrapper session;
    //private GraphSessionManager graphSessionManager;
    private SessionPool sessionPool;
    private String groupName;
    private String space;

    @Override
    public String toString() {
        return "GraphSessionTemplate{" +
                "sessionPool=" + sessionPool +
                ", groupName='" + groupName + '\'' +
                ", space='" + space + '\'' +
                '}';
    }

    public void destroy() {
        if (sessionPool != null) {
            sessionPool.close();
        }
    }
}

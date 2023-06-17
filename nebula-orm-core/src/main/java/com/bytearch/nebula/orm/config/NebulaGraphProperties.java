package com.bytearch.nebula.orm.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class NebulaGraphProperties implements Serializable {
    private String userName;
    private String password;
    /**
     * 格式：ip:port
     */
    private String hostAddresses;
    private int minConnsSize = 5;
    private int MaxConnsSize = 500;
    private int timeout;
    private int idleTime;
    private String space;
    /**
     * 是否启用cache
     */
    private boolean useCache;
}

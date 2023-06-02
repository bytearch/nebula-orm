package com.bytearch.nebula.orm.config;

import lombok.Data;

import java.util.List;

@Data
public class NebulaGraphProperties {
    private String userName;
    private String password;
    /**
     * 格式：ip:port
     */
    private List<String> hostAddresses;
    private int minConnsSize;
    private int maxConnSize;
    private int timeout;
    private int idleTime;
    private String space;
}

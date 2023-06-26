package com.bytearch.nebula.orm.config;

import lombok.Data;

@Data
public class PoolProperties {
   private int activeConnNum;
   private int idleConnNum;
   private int waitersNum;

   private int minConnNum;

   private int maxConnNum;

}

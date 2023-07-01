package com.bytearch.nebula.orm.config;

import lombok.Data;

@Data
public class PoolProperties {
   private int sessionNums;
   private int idleSessionNums;

   private int minConnNum;

   private int maxConnNum;

}

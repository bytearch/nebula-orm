package com.bytearch.nebula.orm.facade;

import com.alibaba.fastjson.JSONObject;
import com.bytearch.nebula.orm.constant.NebulaConstant;
import com.bytearch.nebula.orm.constant.NebulaResult;
import com.bytearch.nebula.orm.exception.NebulaOrmException;
import com.bytearch.nebula.orm.pool.GraphSessionMapper;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.AuthFailedException;
import com.vesoft.nebula.client.graph.exception.BindSpaceFailedException;
import com.vesoft.nebula.client.graph.exception.ClientServerIncompatibleException;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class NebulaExecute implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(NebulaExecute.class);
    public interface Executor {
        Object run(Session session, int ct);
    }

    static public ResultSet executeSql(GraphSessionMapper graphSessionMapper, String sql) {
        long t1 = System.currentTimeMillis();
        ResultSet r = null;
        try {
             r = graphSessionMapper.getSessionPool().execute(sql);
        } catch (IOErrorException | ClientServerIncompatibleException |AuthFailedException | BindSpaceFailedException e) {
            logger.error("[nebula-orm] execute sql error sql:{} e:", sql, e);
           throw new NebulaOrmException(e.getMessage());
        }
        logger.info("[nebula-orm] executeSql cost:{} ms sql:{}", System.currentTimeMillis() - t1, sql);
        return r;
    }

    public static  <T> NebulaResult<T> queryObject(GraphSessionMapper graphSessionMapper, String sql, Class<T> tClass) {
        NebulaResult<T> NebulaResult = executeObject(graphSessionMapper, sql);
        if (Objects.isNull(NebulaResult.getData())) {
            return NebulaResult;
        }
        Optional.ofNullable(NebulaResult.getData()).ifPresent(data -> NebulaResult.setData(data.stream().map(d -> JSONObject.toJavaObject(((JSONObject) d), tClass)).collect(Collectors.toList())));
        return NebulaResult;
    }

    public static NebulaResult executeObject(GraphSessionMapper session, String sql) {
        return JSONObject.toJavaObject(executeJson(session, sql), NebulaResult.class);
    }



    public static JSONObject executeJson(GraphSessionMapper session, String sql) {
        long t1 = System.currentTimeMillis();
        JSONObject restJson = new JSONObject();
        try {
            restJson = session.getSessionPool().executeJson(sql);
            logger.error("[nebula-orm] execute sql success   cost:{}ms  sql:{}", sql, System.currentTimeMillis() - t1);
        } catch (IOErrorException | AuthFailedException |  BindSpaceFailedException e) {
            restJson.put(NebulaConstant.NebulaJson.CODE.getKey(), NebulaConstant.ERROR_CODE);
            restJson.put(NebulaConstant.NebulaJson.MESSAGE.getKey(), e.toString());
            logger.error("[nebula-orm] execute sql error sql:{} e:{}  cost:{}", sql, e, System.currentTimeMillis() - t1);
            throw new   NebulaOrmException(e.getMessage());
        }
        return restJson;
    }

}

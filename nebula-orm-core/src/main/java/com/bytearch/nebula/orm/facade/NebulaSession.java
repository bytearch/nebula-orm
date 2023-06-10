package com.bytearch.nebula.orm.facade;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bytearch.nebula.orm.constant.NebulaConstant;
import com.bytearch.nebula.orm.constant.NebulaResult;
import com.bytearch.nebula.orm.utils.ExceptionUtil;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.Session;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class NebulaSession implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(NebulaSession.class);
    public interface Executor {
        Object run(Session session, int ct);
    }

    private static Object execute(Session session, Executor executor) {
        Object r = null;
        try {
            for (int retry = 0; retry < 5; ++retry) {
                try {
                    r = executor.run(session, retry);
                    boolean isSuccess = true;
                    if (r instanceof ResultSet) {
                        ResultSet rs = (ResultSet) r;
                        if (!rs.isSucceeded() && !StringUtils.isBlank(rs.getErrorMessage()) && rs.getErrorMessage().indexOf("More than one request trying to") != -1) {
                            //Storage Error: More than one request trying to add/update/delete one edge/vertex at the same time.
                            isSuccess = false;
                            Thread.sleep(500);
                        }
                    }
                    if (isSuccess) {
                        break;
                    }
                } catch (Exception e) {
                    logger.error("executor.run exception: " + e.toString());
                    ExceptionUtil.printStackTrace(e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    static public ResultSet executeSql(Session session, String sql) {
        long t1 = System.currentTimeMillis();
        ResultSet r = (ResultSet) execute(session, (session2, ct) -> {
            ResultSet resp = null;
            try {
                resp = session.execute(sql);
                if (!resp.isSucceeded()) {
                    logger.warn("executeSql failed: {}, sql: [{}]", resp.getErrorMessage(), sql);
                    return resp;
                }
                return resp;
            } catch (IOErrorException e) {
                logger.error("executeSql exception: {}, sql: [{}]", e.toString(), sql);
                throw new RuntimeException(e);
            }
        });
        logger.info("executeSql cost:{} ms, sql[{}]", System.currentTimeMillis() - t1, sql);
        return r;
    }

    static public ResultSet executeSqlWithParams(Session session, String stmt, Map<String, Object> parmas) {
        long t1 = System.currentTimeMillis();
        ResultSet r = (ResultSet) execute(session, (session2, ct) -> {
            ResultSet resp = null;
            try {
                resp = session.executeWithParameter(stmt, parmas);
                if (!resp.isSucceeded()) {
                    logger.warn("executeSql failed: {}, sql: [{}]", resp.getErrorMessage(), stmt);
                    return resp;
                }
                return resp;
            } catch (IOErrorException e) {
                logger.error("executeSql exception: {}, sql: [{}]", e.toString(), stmt);
                throw new RuntimeException(e);
            }
        });
        logger.info("executeSql cost:{} ms, sql[{}]", System.currentTimeMillis() - t1, stmt);
        return r;
    }

    public static  <T> NebulaResult<T> queryObject(Session session, String sql, Class<T> tClass) {
        NebulaResult<T> NebulaResult = executeObject(session, sql);
        if (Objects.isNull(NebulaResult.getData())) {
            return NebulaResult;
        }
        Optional.ofNullable(NebulaResult.getData()).ifPresent(data -> NebulaResult.setData(data.stream().map(d -> JSONObject.toJavaObject(((JSONObject) d), tClass)).collect(Collectors.toList())));
        return NebulaResult;
    }

    public static NebulaResult executeObject(Session session, String sql) {
        JSONObject jsonObject = executeJson(session, sql);
        return JSONObject.toJavaObject(jsonObject, NebulaResult.class);
    }



    public static JSONObject executeJson(Session session, String sql) {
        long t1 = System.currentTimeMillis();
        JSONObject restJson = new JSONObject();
        try {
            JSONObject jsonObject = JSON.parseObject(Objects.requireNonNull(session).executeJson(sql));
            JSONObject errors = jsonObject.getJSONArray(NebulaConstant.NebulaJson.ERRORS.getKey()).getJSONObject(0);
            restJson.put(NebulaConstant.NebulaJson.CODE.getKey(), errors.getInteger(NebulaConstant.NebulaJson.CODE.getKey()));
            if (errors.getInteger(NebulaConstant.NebulaJson.CODE.getKey()) != 0) {
                restJson.put(NebulaConstant.NebulaJson.MESSAGE.getKey(), errors.getString(NebulaConstant.NebulaJson.MESSAGE.getKey()));
                return restJson;
            }
            JSONObject results = jsonObject.getJSONArray(NebulaConstant.NebulaJson.RESULTS.getKey()).getJSONObject(0);
            JSONArray columns = results.getJSONArray(NebulaConstant.NebulaJson.COLUMNS.getKey());
            if (Objects.isNull(columns)) {
                return restJson;
            }
            JSONArray data = results.getJSONArray(NebulaConstant.NebulaJson.DATA.getKey());
            if (Objects.isNull(data)) {
                return restJson;
            }
            List<JSONObject> resultList = new ArrayList<>();
            data.stream().map(d -> (JSONObject) d).forEach(d -> {
                JSONArray row = d.getJSONArray(NebulaConstant.NebulaJson.ROW.getKey());
                JSONObject map = new JSONObject();
                for (int i = 0; i < columns.size(); i++) {
                    map.put(columns.getString(i), row.get(i));
                }
                resultList.add(map);
            });
            restJson.put(NebulaConstant.NebulaJson.DATA.getKey(), resultList);
            logger.debug("nebula execute success! sql:{} cost:{} ms", sql, System.currentTimeMillis() - t1);
        } catch (Exception e) {
            restJson.put(NebulaConstant.NebulaJson.CODE.getKey(), NebulaConstant.ERROR_CODE);
            restJson.put(NebulaConstant.NebulaJson.MESSAGE.getKey(), e.toString());
            logger.error("nebula execute err：", e);
        }
        return restJson;
    }

}

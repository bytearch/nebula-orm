package com.bytearch.nebula.orm.utils;
;
import com.bytearch.nebula.orm.annotation.GraphEdge;
import com.bytearch.nebula.orm.annotation.GraphProperty;
import com.bytearch.nebula.orm.annotation.GraphVertex;
import com.bytearch.nebula.orm.annotation.VID;
import com.bytearch.nebula.orm.entity.ColumnEntity;
import com.bytearch.nebula.orm.entity.GraphEdgeEntity;
import com.bytearch.nebula.orm.entity.GraphVertexEntity;
import com.bytearch.nebula.orm.entity.OperatorEnum;
import com.bytearch.nebula.orm.exception.NebulaOrmException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SQL创建工具类
 */
@Slf4j
public class SQLBuildUtils {

    /**
     * 构建边属性
     *
     * @param obj
     * @return
     */
    private static GraphEdgeEntity buildGraphEdge(Object obj) {
        Class<?> objClass = obj.getClass();
        GraphEdge annotation = objClass.getAnnotation(GraphEdge.class);
        if (annotation == null) {
            throw new NebulaOrmException("@GraphEdge cannot be empty!");
        }
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        // 获取属性和对应的值
        Field[] fields = objClass.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;  // 跳过静态字段
            }
            field.setAccessible(true);
            GraphProperty graphProperty = field.getAnnotation(GraphProperty.class);
            if (graphProperty == null) {
                continue;
            }
            Object value;
            try {
                value = field.get(obj);
            } catch (IllegalAccessException e) {
                value = null;
            }
            if (value != null) {
                columns.add(graphProperty.name());
                values.add(value);
            }
        }
        return GraphEdgeEntity.builder()
                .edgeList(columns)
                .edgeValueList(values)
                .space(annotation.space())
                .edgeName(annotation.edge())
                .build();

    }

    private static GraphVertexEntity buildGraphVertex(Object obj) {
        Class<?> objClass = obj.getClass();
        GraphVertex annotation = objClass.getAnnotation(GraphVertex.class);
        if (annotation == null) {
            throw new NebulaOrmException("@GraphVertex cannot be empty!");
        }
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        Object pointKey = null;
        // 获取属性和对应的值
        Field[] fields = objClass.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;  // 跳过静态字段
            }
            field.setAccessible(true);
            GraphProperty graphProperty = field.getAnnotation(GraphProperty.class);
            if (graphProperty == null) {
                continue;
            }
            Object value;
            try {
                value = field.get(obj);
            } catch (IllegalAccessException e) {
                value = null;
            }
            if (value != null) {
                columns.add(graphProperty.name());
                values.add(value);
            }
            //获取vid
            VID vidAnnotation = field.getAnnotation(VID.class);
            if (vidAnnotation != null) {
                pointKey = value;
            }

        }
        return GraphVertexEntity.builder()
                .space(annotation.space())
                .tagName(annotation.tag())
                .tagList(columns)
                .vid(pointKey)
                .tagValueList(values).build();

    }

    public static String buildInsertEdgeSQL(Object obj, Object srcVid, Object dstVid) {
        if (Objects.isNull(srcVid)) {
            throw new NebulaOrmException("srcVid cannot be empty!");
        }
        if (Objects.isNull(dstVid)) {
            throw new NebulaOrmException("descVid cannot be empty!");
        }
        GraphEdgeEntity graphEdgeEntity = buildGraphEdge(obj);
        StringBuffer stringBuffer = getStringBuffer(graphEdgeEntity.getEdgeValueList());
        String bufferString = stringBuffer.toString();
        if (srcVid instanceof String) {
            srcVid = "'" + srcVid + "'";
        }
        if (dstVid instanceof String) {
            dstVid = "'" + dstVid + "'";
        }
        StringBuffer stringBufferEdge = new StringBuffer();
        for (String edge : graphEdgeEntity.getEdgeList()) {
            stringBufferEdge.append("`").append(edge).append("`").append(",");
        }
        String str = stringBufferEdge.toString();
        String insertEdge = String.format("USE `%s`; INSERT EDGE IF NOT EXISTS `%s` (%s) VALUES %s->%s:( %s );"
                , graphEdgeEntity.getSpace(), graphEdgeEntity.getEdgeName(), StringUtils.isNotBlank(str) ? str.substring(0, str.length() - 1) : "",
                srcVid, dstVid, bufferString);
        log.info("插入边 -gql语句:{}", insertEdge);
        return insertEdge;

    }

    public static String buildUpdateEdgeSQL(Object obj, Object srcVid, Object dstVid) {
        if (Objects.isNull(srcVid)) {
            throw new NebulaOrmException("srcVid cannot be empty!");
        }
        if (Objects.isNull(dstVid)) {
            throw new NebulaOrmException("descVid cannot be empty!");
        }
        GraphEdgeEntity graphEdgeEntity = buildGraphEdge(obj);
        if (srcVid instanceof String) {
            srcVid = "'" + srcVid + "'";
        }
        if (dstVid instanceof String) {
            dstVid = "'" + dstVid + "'";
        }
        List<String> edgeList = graphEdgeEntity.getEdgeList();
        List<Object> edgeValueList = graphEdgeEntity.getEdgeValueList();
        StringBuffer stringBuffer = getUpdateStringBuffer(edgeList, edgeValueList);
        String updateVertex = String.format("USE `%s`; UPDATE EDGE ON `%s` %s -> %s@0 SET %s;"
                , graphEdgeEntity.getSpace(), graphEdgeEntity.getEdgeName(), srcVid, dstVid
                , stringBuffer);
        log.info("边更新 -gql语句:{}", updateVertex);
        return updateVertex;

    }

    public static String buildInsertVertexSQL(Object obj) {
        GraphVertexEntity graphVertexEntity = buildGraphVertex(obj);
        List<Object> tagValueList = graphVertexEntity.getTagValueList();
        StringBuffer stringBuffer = getStringBuffer(tagValueList);
        String bufferString = stringBuffer.toString();
        StringBuffer stringBufferTagList = new StringBuffer();
        List<String> tagList = graphVertexEntity.getTagList();
        if (!CollectionUtils.isEmpty(tagList)) {
            for (int i = 0; i < tagList.size(); i++) {
                String tagPropertyName = tagList.get(i);
                stringBufferTagList.append(" `" + tagPropertyName + "` ");
                if (tagList.size() > 1 && (i + 1) != tagList.size()) {
                    stringBufferTagList.append(",");
                }
            }
        } else {
            throw new NebulaOrmException("insert column cannot be empty!");
        }
        Object pointKey = graphVertexEntity.getVid();
        if (pointKey instanceof String) {
            pointKey = "'" + StringEscapeUtils.unescapeHtml(pointKey.toString()) + "'";
        }
        String createPointSQL = String.format("USE `%s`;INSERT VERTEX IF NOT EXISTS `%s`(%s) VALUES %s: (" + bufferString + ");"
                , graphVertexEntity.getSpace(), graphVertexEntity.getTagName(), stringBufferTagList,
                pointKey);
        log.info("创建vertex-gql语句:{}", createPointSQL);
        return createPointSQL;
    }
    private static StringBuffer getUpdateStringBuffer(List<String> columns, List<Object> values) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < columns.size(); i++) {
            String tag = columns.get(i);
            Object tagValue = values.get(i);
            stringBuffer.append("`").append(tag).append("`").append(" = ");
            if (tagValue instanceof String) {
                stringBuffer.append("'").append(tagValue).append("'");
            } else if (tagValue instanceof Date) {
                // 根据需要格式化日期值
                // 这里假设使用yyyy-MM-dd HH:mm:ss格式
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                stringBuffer.append("'").append(dateFormat.format((Date) tagValue)).append("'");
            }  else {
                stringBuffer.append(tagValue);
            }
            if (values.size() > 1 && (i + 1) != columns.size()) {
                stringBuffer.append(",");
            }
        }
        return stringBuffer;
    }

    public static String buildUpdateVertexSQL(Object obj) {
        GraphVertexEntity graphVertexEntity = buildGraphVertex(obj);
        List<String> tagList = graphVertexEntity.getTagList();
        List<Object> tagValueList = graphVertexEntity.getTagValueList();
        if (tagList.size() != tagValueList.size()) {
            return "";
        }
        StringBuffer stringBuffer = new StringBuffer();
        if (CollectionUtils.isEmpty(tagList) || CollectionUtils.isEmpty(tagValueList)) {
            throw new NebulaOrmException("update properties cannot be empty!");
        }
        stringBuffer = getUpdateStringBuffer(tagList, tagValueList);
        Object vid = graphVertexEntity.getVid();
        if (vid instanceof String) {
            vid = "'" + StringEscapeUtils.unescapeHtml(vid.toString()) + "'";
        }
        String updateVertexSQL = String.format("USE `%s`; UPDATE VERTEX ON `%s` %s  SET %s;"
                , graphVertexEntity.getSpace(), graphVertexEntity.getTagName(), vid, stringBuffer);
        log.info("更新vertex-gql语句:{}", updateVertexSQL);
        return updateVertexSQL;
    }

    /**
     * @return java.lang.StringBuffer
     * @Description 获取一个拼接好的字符串 "n1", 1
     * @Param [edgeValueList]
     **/
    private static StringBuffer getStringBuffer(List<Object> edgeValueList) {
        StringBuffer stringBuffer = new StringBuffer();
        if (!CollectionUtils.isEmpty(edgeValueList)) {
            //stringBuffer.append("(");
            for (int i = 0; i < edgeValueList.size(); i++) {
                Object value = edgeValueList.get(i);
                if (value instanceof String) {
                    stringBuffer.append("'").append(StringEscapeUtils.unescapeHtml((String) value)).append("'");
                } else if (value instanceof Date) {
                    // 根据需要格式化日期值
                    // 这里假设使用yyyy-MM-dd HH:mm:ss格式
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    stringBuffer.append("'").append(dateFormat.format((Date) value)).append("'");
                } else {
                    stringBuffer.append(value);
                }
                if (edgeValueList.size() > 1 && (i + 1) != edgeValueList.size()) {
                    stringBuffer.append(",");
                }
            }
        }
        return stringBuffer;
    }

    public static StringBuilder fromCondition(String name, List<ColumnEntity> columnEntities) {
        StringBuilder builder = new StringBuilder();
        if (CollectionUtils.isEmpty(columnEntities)) {
            return builder;
        }
        for (ColumnEntity columnEntity : columnEntities) {
            if (builder.length() > 0) {
                builder.append(" AND ");
            }
            OperatorEnum op = columnEntity.getOperatorEnum();
            List<OperatorEnum> operatorEnums = Arrays.asList(OperatorEnum.EQUAL, OperatorEnum.GREATER_THAN, OperatorEnum.GREATER_THAN_OR_EQUAL, OperatorEnum.LESS_THAN, OperatorEnum.LESS_THAN_OR_EQUAL, OperatorEnum.NOT_EQUAL);
            String value = null;
            if (operatorEnums.contains(op)) {
                builder.append(" ").append(name).append(".").append(columnEntity.getColumn()).append(op.getSymbol());
                buildValue(builder, columnEntity.getValue());
            } else if (Arrays.asList(OperatorEnum.IN, OperatorEnum.NOT_IN).contains(op)) {
                if (columnEntity.getValue() instanceof Collection) {
                    throw new RuntimeException(String.format("condition column:%s, op:%s, must be collection", columnEntity.getColumn(), op.getSymbol()));
                }
                //todo
            } else {
                throw new RuntimeException("op" + op.getSymbol() + "cannot be operator");
            }
        }
        return builder;
    }


    private static void buildValue(StringBuilder builder, Object v) {
        if (v instanceof String) {
            builder.append("'").append(v).append("'");
        } else {
            builder.append(v);
        }
    }

}

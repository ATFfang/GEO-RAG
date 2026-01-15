package com.EarthCube.georag_backend.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@MappedTypes({Object.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class MybatisJsonTypeHandler extends BaseTypeHandler<Object> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Class<?> type; // 用于存储当前处理的字段类型

    // 默认构造函数，MyBatis 在处理带 typeHandler 属性的字段时会用到
    public MybatisJsonTypeHandler(Class<?> type) {
        if (log.isTraceEnabled()) {
            log.trace("MybatisJsonTypeHandler initialized for type: {}", type.getSimpleName());
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting Object to JSON string", e);
        }
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private Object parse(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            // 使用构造函数传入的 type 来动态构建 Jackson 类型
            JavaType javaType = objectMapper.getTypeFactory().constructType(type);
            return objectMapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error parsing JSON string to " + type.getSimpleName(), e);
        }
    }
}
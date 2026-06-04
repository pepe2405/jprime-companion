package com.jprime.connect.common;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

public final class SqlSupport {
    private SqlSupport() {}

    public static List<String> names(JdbcTemplate jdbc, String sql, UUID userId) {
        return jdbc.queryForList(sql, String.class, userId);
    }

    public static List<UUID> uuids(List<UUID> values) {
        return values == null ? List.of() : values;
    }

    public static List<String> strings(List<String> values) {
        return values == null ? List.of() : values;
    }

    public static List<String> textArray(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        return Arrays.asList((String[]) array.getArray());
    }

    public static LocalDateTime dateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}

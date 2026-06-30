package com.opendb.service;

import com.opendb.config.ConnectionProperties;
import com.opendb.dialect.SqlDialect;
import com.opendb.exception.OpenDbException;
import com.opendb.model.DatabaseType;
import com.opendb.model.ManagedConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class JdbcHelper {

    private JdbcHelper() {
    }

    static SqlDialect dialect(ManagedConnection managed) {
        return managed.getDialect();
    }

    static Statement createStatement(Connection connection, ConnectionProperties properties) throws SQLException {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(properties.getQueryTimeoutSeconds());
        statement.setMaxRows(properties.getMaxPoolSize() * 1000);
        return statement;
    }

    static Statement createAdminStatement(Connection connection, ConnectionProperties properties) throws SQLException {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(properties.getQueryTimeoutSeconds());
        return statement;
    }

    static void useNamespace(ManagedConnection managed, String namespace) {
        SqlDialect dialect = dialect(managed);
        try {
            Connection connection = managed.getActiveConnection();
            if (isPostgresLike(managed.getConfig().getType())) {
                connection.setCatalog(namespace);
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(dialect.useNamespaceSql(namespace));
            }
        } catch (SQLException e) {
            throw new OpenDbException("Failed to switch namespace: " + e.getMessage(), e);
        }
    }

    static boolean isPostgresLike(DatabaseType type) {
        return type == DatabaseType.POSTGRESQL
                || type == DatabaseType.KINGBASE
                || type == DatabaseType.OPENGAUSS;
    }

    static String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

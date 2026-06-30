package com.opendb.model;

import com.opendb.config.JdbcDriverLoader;
import com.opendb.dialect.DialectRegistry;
import com.opendb.dialect.SqlDialect;
import com.opendb.dto.ConnectionRequest;
import com.opendb.exception.OpenDbException;
import lombok.Getter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Getter
public class ManagedConnection {

    private final String id;
    private final ConnectionRequest config;
    private final String profileId;
    private final SqlDialect dialect;
    private final JdbcDriverLoader driverLoader;
    private Connection connection;

    public ManagedConnection(String id, ConnectionRequest config, SqlDialect dialect, JdbcDriverLoader driverLoader) {
        this(id, config, null, dialect, driverLoader);
    }

    public ManagedConnection(String id, ConnectionRequest config, String profileId, SqlDialect dialect,
                             JdbcDriverLoader driverLoader) {
        this.id = id;
        this.config = config;
        this.profileId = profileId;
        this.dialect = dialect;
        this.driverLoader = driverLoader;
    }

    public static ManagedConnection create(String id, ConnectionRequest config, String profileId,
                                           DialectRegistry registry, JdbcDriverLoader driverLoader) {
        SqlDialect dialect = registry.require(config.getType());
        return new ManagedConnection(id, config, profileId, dialect, driverLoader);
    }

    public void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            driverLoader.ensureDriverLoaded(config.getType());
            connection = DriverManager.getConnection(
                    buildJdbcUrl(),
                    dialect.getConnectionProperties(config.getUsername(), config.getPassword())
            );
        } catch (SQLException e) {
            throw new OpenDbException("Failed to connect: " + e.getMessage(), e);
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new OpenDbException("Failed to disconnect: " + e.getMessage(), e);
            } finally {
                connection = null;
            }
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public Connection getActiveConnection() {
        if (!isConnected()) {
            connect();
        }
        return connection;
    }

    public String buildJdbcUrl() {
        return dialect.buildJdbcUrl(config.getHost(), config.getPort(), config.getDatabase());
    }
}

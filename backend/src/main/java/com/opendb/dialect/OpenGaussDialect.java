package com.opendb.dialect;

import com.opendb.model.DatabaseType;
import org.springframework.stereotype.Component;

@Component
public class OpenGaussDialect extends PostgreSqlDialect {

    @Override
    public DatabaseType getType() {
        return DatabaseType.OPENGAUSS;
    }

    @Override
    public String getAiDialectName() {
        return "openGauss";
    }

    @Override
    public String buildJdbcUrl(String host, int port, String database) {
        String db = database == null || database.isBlank() ? "postgres" : database;
        return "jdbc:opengauss://%s:%d/%s".formatted(host, port, db);
    }
}

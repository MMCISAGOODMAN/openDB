package com.opendb.dialect;

import com.opendb.model.DatabaseType;
import org.springframework.stereotype.Component;

@Component
public class KingbaseDialect extends PostgreSqlDialect {

    @Override
    public DatabaseType getType() {
        return DatabaseType.KINGBASE;
    }

    @Override
    public String getAiDialectName() {
        return "KingbaseES";
    }

    @Override
    public String buildJdbcUrl(String host, int port, String database) {
        String db = database == null || database.isBlank() ? "test" : database;
        return "jdbc:kingbase8://%s:%d/%s".formatted(host, port, db);
    }
}

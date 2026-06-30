package com.opendb.dialect;

import com.opendb.model.DatabaseType;
import org.springframework.stereotype.Component;

@Component
public class DmDialect extends OracleDialect {

    @Override
    public DatabaseType getType() {
        return DatabaseType.DM;
    }

    @Override
    public String getAiDialectName() {
        return "DM / 达梦";
    }

    @Override
    public String buildJdbcUrl(String host, int port, String database) {
        String db = database == null || database.isBlank() ? "DAMENG" : database;
        return "jdbc:dm://%s:%d/%s".formatted(host, port, db);
    }

    @Override
    public String listNamespacesSql() {
        return """
                SELECT username
                FROM all_users
                WHERE username NOT IN ('SYS','SYSTEM','SYSDBA','SYSSSO')
                ORDER BY username
                """;
    }
}

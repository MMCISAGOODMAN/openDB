package com.opendb.model;

public enum DatabaseType {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", 3306, true, false),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", 5432, true, false),
    ORACLE("Oracle", "oracle.jdbc.OracleDriver", 1521, true, false),
    H2("H2", "org.h2.Driver", 9092, true, false),
    DM("达梦 DM8", "dm.jdbc.driver.DmDriver", 5236, true, true),
    KINGBASE("人大金仓 KingbaseES", "com.kingbase8.Driver", 54321, true, true),
    OPENGAUSS("openGauss", "org.opengauss.Driver", 5432, true, false);

    private final String displayName;
    private final String driverClassName;
    private final int defaultPort;
    private final boolean supported;
    private final boolean vendorDriverJar;

    DatabaseType(String displayName, String driverClassName, int defaultPort, boolean supported, boolean vendorDriverJar) {
        this.displayName = displayName;
        this.driverClassName = driverClassName;
        this.defaultPort = defaultPort;
        this.supported = supported;
        this.vendorDriverJar = vendorDriverJar;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public boolean isSupported() {
        return supported;
    }

    public boolean requiresVendorDriverJar() {
        return vendorDriverJar;
    }
}

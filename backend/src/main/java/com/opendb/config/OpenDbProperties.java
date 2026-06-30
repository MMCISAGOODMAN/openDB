package com.opendb.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "opendb")
public class OpenDbProperties {

    /**
     * User data directory for profiles, AI config, and optional vendor JDBC drivers.
     */
    private String dataDir = "./data";

    /**
     * Optional explicit directory for vendor JDBC JAR files (DM, Kingbase).
     */
    private String driversDir = "";

    public Path getDataDir() {
        return Path.of(dataDir);
    }
}

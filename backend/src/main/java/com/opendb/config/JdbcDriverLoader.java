package com.opendb.config;

import com.opendb.exception.OpenDbException;
import com.opendb.model.DatabaseType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcDriverLoader {

    private final OpenDbProperties openDbProperties;
    private final Map<DatabaseType, URLClassLoader> vendorLoaders = new EnumMap<>(DatabaseType.class);

    @PostConstruct
    void init() {
        for (DatabaseType type : DatabaseType.values()) {
            if (!type.requiresVendorDriverJar()) {
                continue;
            }
            try {
                Class.forName(type.getDriverClassName());
                log.info("Vendor driver for {} is available on the classpath", type);
            } catch (ClassNotFoundException ignored) {
                log.info("Vendor driver for {} is not bundled; place JARs in drivers/ before connecting", type);
            }
        }
    }

    public void ensureDriverLoaded(DatabaseType type) {
        try {
            if (vendorLoaders.containsKey(type)) {
                vendorLoaders.get(type).loadClass(type.getDriverClassName());
                return;
            }
            Class.forName(type.getDriverClassName());
        } catch (ClassNotFoundException e) {
            if (type.requiresVendorDriverJar()) {
                loadVendorDriver(type);
                try {
                    vendorLoaders.get(type).loadClass(type.getDriverClassName());
                    return;
                } catch (ClassNotFoundException ex) {
                    throw driverNotFound(type, ex);
                }
            }
            throw driverNotFound(type, e);
        }
    }

    private void loadVendorDriver(DatabaseType type) {
        List<Path> directories = resolveDriverDirectories();
        List<URL> jarUrls = new ArrayList<>();
        for (Path directory : directories) {
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.jar")) {
                for (Path jar : stream) {
                    jarUrls.add(jar.toUri().toURL());
                }
            } catch (Exception e) {
                log.warn("Failed to scan driver directory {}: {}", directory, e.getMessage());
            }
        }
        if (jarUrls.isEmpty()) {
            throw new OpenDbException(
                    "Database driver not found for " + type.getDisplayName()
                            + ". Place the vendor JDBC JAR in backend/drivers/ or "
                            + openDbProperties.getDataDir().resolve("drivers")
                            + ". See backend/drivers/README.md.");
        }
        URLClassLoader loader = new URLClassLoader(jarUrls.toArray(URL[]::new), getClass().getClassLoader());
        vendorLoaders.put(type, loader);
        Thread.currentThread().setContextClassLoader(loader);
        log.info("Loaded {} vendor JDBC JAR(s) for {}", jarUrls.size(), type);
    }

    private List<Path> resolveDriverDirectories() {
        List<Path> directories = new ArrayList<>();
        String explicit = openDbProperties.getDriversDir();
        if (explicit != null && !explicit.isBlank()) {
            directories.add(Path.of(explicit));
        }
        directories.add(openDbProperties.getDataDir().resolve("drivers"));
        directories.add(Path.of("drivers"));
        directories.add(Path.of("backend", "drivers"));
        return directories;
    }

    private OpenDbException driverNotFound(DatabaseType type, Throwable cause) {
        return new OpenDbException(
                "Database driver not found: " + type.getDisplayName()
                        + " (" + type.getDriverClassName() + "). "
                        + "See backend/drivers/README.md for setup instructions.",
                cause);
    }
}

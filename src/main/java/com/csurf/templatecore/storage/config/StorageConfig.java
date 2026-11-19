package com.csurf.templatecore.storage.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for template storage locations.
 */
public class StorageConfig {
    private static final String DEV_STORAGE_DIR = "dev_storage";
    private static final String TEMPLATES_DIR = "templates";
    private static final String PALETTES_DIR = "palettes";
    private static final String AGPREFS_DIR = "agprefs";
    
    public static Path getDevStoragePath() {
        return Paths.get(System.getProperty("user.dir"))
                   .resolve(DEV_STORAGE_DIR);
    }
    
    public static Path getTemplatesPath() {
        return getDevStoragePath().resolve(TEMPLATES_DIR);
    }
    
    public static Path getPalettesPath() {
        return getDevStoragePath().resolve(PALETTES_DIR);
    }

    public static Path getAgPrefsPath() {
        return getDevStoragePath().resolve(AGPREFS_DIR);
    }
}

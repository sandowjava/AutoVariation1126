package com.csurf.templatecore.storage;

import com.csurf.core.ApplicationMediator;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.PaletteWName;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.templatecore.storage.serialization.ColorPtOstDotDeserializer;
import com.csurf.templatecore.storage.serialization.ColorPtOstDotSerializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles saving and loading of PaletteWName instances using JSON serialization.
 * Similar to TemplateStorage but specialized for palettes.
 */
public class PaletteStorage {
    private final ApplicationMediator mediator;
    private final Path storageDir;
    private final ObjectMapper mapper;

    public PaletteStorage(ApplicationMediator mediator) {
        this.mediator = mediator;
        this.storageDir = StorageConfig.getPalettesPath();
        this.mapper = configureMapper();
        initializeStorage();
    }

    private ObjectMapper configureMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Configure to ignore unknown properties
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Register custom serializers/deserializers
        SimpleModule module = new SimpleModule();
        module.addSerializer(ColorPtOstDot.class, new ColorPtOstDotSerializer());
        module.addDeserializer(ColorPtOstDot.class, new ColorPtOstDotDeserializer());
        
        mapper.registerModule(module);
        
        return mapper;
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new TemplateStorageException("Failed to create palette storage directory", e);
        }
    }

    /**
     * Gets the storage directory for palettes.
     */
    public Path getStorageDir() {
        return storageDir;
    }

    /**
     * Saves a PaletteWName instance to storage.
     * 
     * @param palette The PaletteWName to save
     * @param name The name to save the palette under
     * @throws TemplateStorageException if saving fails
     */
    public void savePalette(PaletteWName palette, String name) {
        try {
            // Debug logging
            System.out.println("Palette storage dir: " + storageDir.toAbsolutePath());
            System.out.println("Saving palette with name: " + name);
            
            // Update palette name to match file name
            palette.setPaletteName(name);
            
            Path filePath = storageDir.resolve(name + ".json");
            System.out.println("Full save path: " + filePath.toAbsolutePath());
            
            mapper.writeValue(filePath.toFile(), palette);
            
        } catch (IOException e) {
            throw new TemplateStorageException("Failed to save palette: " + name, e);
        }
    }

    /**
     * Loads a PaletteWName instance from storage.
     * 
     * @param name The name of the palette to load
     * @return The loaded PaletteWName instance
     * @throws TemplateStorageException if loading fails
     */
    public PaletteWName loadPalette(String name) {
        try {
            Path filePath = storageDir.resolve(name + ".json");
            System.out.println("DEBUG PaletteStorage: Loading palette from " + filePath.toAbsolutePath());
            
            if (!Files.exists(filePath)) {
                System.out.println("ERROR PaletteStorage: Palette file does not exist: " + filePath.toAbsolutePath());
                throw new TemplateStorageException("Palette file does not exist: " + name);
            }
            
            System.out.println("DEBUG PaletteStorage: File exists, attempting to read JSON");
            PaletteWName palette = mapper.readValue(filePath.toFile(), PaletteWName.class);
            
            if (palette == null) {
                System.out.println("ERROR PaletteStorage: Mapper returned null palette");
                return null;
            }
            
            System.out.println("DEBUG PaletteStorage: Successfully loaded palette with " + 
                (palette.getColorPtOstdotArray() != null ? palette.getColorPtOstdotArray().length : 0) + " colors");
            
            return palette;
            
        } catch (IOException e) {
            System.out.println("ERROR PaletteStorage: Failed to load palette: " + e.getMessage());
            e.printStackTrace();
            throw new TemplateStorageException("Failed to load palette: " + name, e);
        }
    }

    /**
     * Lists all available palette names.
     * 
     * @return A list of palette names (without file extensions)
     */
    public List<String> listPaletteNames() {
        try {
            List<String> paletteNames = new ArrayList<>();
            
            Files.list(storageDir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    paletteNames.add(fileName.substring(0, fileName.lastIndexOf('.')));
                });
            
            return paletteNames;
            
        } catch (IOException e) {
            throw new TemplateStorageException("Failed to list palettes", e);
        }
    }
}

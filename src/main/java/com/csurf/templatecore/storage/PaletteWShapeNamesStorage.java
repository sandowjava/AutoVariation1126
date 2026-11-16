package com.csurf.templatecore.storage;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.PaletteWShapeNames;
import com.csurf.templatecore.model.ShapeColorPair;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.templatecore.storage.serialization.*;
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
 * Handles saving and loading of PaletteWShapeNames instances using JSON serialization.
 * Stores palettes in dev_storage/palettes/wshapenames/ directory.
 * 
 * This class is separate from PaletteStorage to maintain clear separation of concerns
 * between the old PaletteWName model and the new PaletteWShapeNames model.
 */
public class PaletteWShapeNamesStorage {
    private final Path storageDir;
    private final ObjectMapper mapper;
    private static final String WSHAPENAMES_DIR = "wshapenames";

    public PaletteWShapeNamesStorage() {
        this.storageDir = StorageConfig.getPalettesPath().resolve(WSHAPENAMES_DIR);
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
        module.addSerializer(ShapeColorPair.class, new ShapeColorPairSerializer());
        module.addDeserializer(ShapeColorPair.class, new ShapeColorPairDeserializer());
        module.addSerializer(PaletteWShapeNames.class, new PaletteWShapeNamesSerializer());
        module.addDeserializer(PaletteWShapeNames.class, new PaletteWShapeNamesDeserializer());
        
        mapper.registerModule(module);
        
        return mapper;
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new TemplateStorageException("Failed to create PaletteWShapeNames storage directory", e);
        }
    }

    /**
     * Gets the storage directory for PaletteWShapeNames.
     */
    public Path getStorageDir() {
        return storageDir;
    }

    /**
     * Saves a PaletteWShapeNames instance to storage.
     * 
     * @param palette The PaletteWShapeNames to save
     * @param filename The filename to save under (without .json extension)
     * @throws TemplateStorageException if saving fails
     */
    public void savePaletteWShapeNames(PaletteWShapeNames palette, String filename) {
        try {
            // Validate filename
            if (filename == null || filename.trim().isEmpty()) {
                throw new TemplateStorageException("Filename cannot be null or empty");
            }
            
            // Remove .json extension if present
            String cleanFilename = filename.endsWith(".json") 
                ? filename.substring(0, filename.length() - 5) 
                : filename;
            
            // Set palette name to match filename
            palette.setPaletteName(cleanFilename);
            
            Path filePath = storageDir.resolve(cleanFilename + ".json");
            
            mapper.writeValue(filePath.toFile(), palette);
            
        } catch (IOException e) {
            throw new TemplateStorageException("Failed to save PaletteWShapeNames: " + filename, e);
        }
    }

    /**
     * Loads a PaletteWShapeNames instance from storage.
     * 
     * @param filename The filename to load (without .json extension)
     * @return The loaded PaletteWShapeNames instance
     * @throws TemplateStorageException if loading fails
     */
    public PaletteWShapeNames loadPaletteWShapeNames(String filename) {
        try {
            // Remove .json extension if present
            String cleanFilename = filename.endsWith(".json") 
                ? filename.substring(0, filename.length() - 5) 
                : filename;
            
            Path filePath = storageDir.resolve(cleanFilename + ".json");
            
            if (!Files.exists(filePath)) {
                throw new TemplateStorageException("PaletteWShapeNames file does not exist: " + filename);
            }
            
            PaletteWShapeNames palette = mapper.readValue(filePath.toFile(), PaletteWShapeNames.class);
            
            if (palette == null) {
                throw new TemplateStorageException("Failed to load PaletteWShapeNames: " + filename);
            }
            
            return palette;
            
        } catch (IOException e) {
            throw new TemplateStorageException("Failed to load PaletteWShapeNames: " + filename, e);
        }
    }

    /**
     * Lists all available PaletteWShapeNames files.
     * 
     * @return A list of filenames (without .json extension)
     */
    public List<String> listPaletteWShapeNames() {
        try {
            List<String> filenames = new ArrayList<>();
            
            Files.list(storageDir)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    filenames.add(fileName.substring(0, fileName.lastIndexOf('.')));
                });
            
            return filenames;
            
        } catch (IOException e) {
            throw new TemplateStorageException("Failed to list PaletteWShapeNames files", e);
        }
    }
}

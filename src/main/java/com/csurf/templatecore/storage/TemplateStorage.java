package com.csurf.templatecore.storage;

import com.csurf.core.ApplicationMediator;
import com.csurf.core.Message;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.SelectionGroup;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.model.serialization.SelectionGroupDeserializer;
import com.csurf.templatecore.model.serialization.SelectionGroupSerializer;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.templatecore.storage.serialization.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles saving and loading of Template instances using a hybrid storage approach.
 * Uses JSON for metadata and structure, with custom serializers for complex objects.
 */
public class TemplateStorage {
    private final ApplicationMediator mediator;
    private final Path storageDir;
    private final ObjectMapper mapper;

    public TemplateStorage(ApplicationMediator mediator) {
        this.mediator = mediator;
        this.storageDir = StorageConfig.getTemplatesPath();
        this.mapper = configureMapper();
        initializeStorage();
    }

    private ObjectMapper configureMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Register custom serializers/deserializers
        SimpleModule module = new SimpleModule();
        module.addSerializer(ColorPtOstDot.class, new ColorPtOstDotSerializer());
        module.addDeserializer(ColorPtOstDot.class, new ColorPtOstDotDeserializer());
        module.addSerializer(GeneralPath.class, new GeneralPathSerializer());
        module.addDeserializer(GeneralPath.class, new GeneralPathDeserializer());
        module.addSerializer(Rectangle2D.class, new Rectangle2DSerializer());
        module.addDeserializer(Rectangle2D.class, new Rectangle2DDeserializer());
        module.addSerializer(DrawObj.class, new DrawObjSerializer());
        module.addDeserializer(DrawObj.class, new DrawObjDeserializer());
        module.addSerializer(OstHueColumnStep.class, new OstHueColumnStepSerializer());
        module.addDeserializer(OstHueColumnStep.class, new OstHueColumnStepDeserializer());
        
        // Register SelectionGroup serializer/deserializer
        module.addSerializer(SelectionGroup.class, new SelectionGroupSerializer());
        module.addDeserializer(SelectionGroup.class, new SelectionGroupDeserializer());
        
        mapper.registerModule(module);
        
        return mapper;
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new TemplateStorageException("Failed to create storage directory", e);
        }
    }

    /**
     * Gets the storage directory for templates.
     */
    public Path getStorageDir() {
        return storageDir;
    }

    /**
     * Saves a Template instance to storage.
     * 
     * @param template The Template to save
     * @param name The name to save the template under
     * @throws TemplateStorageException if saving fails
     */
    public void saveTemplate(Template template, String name) {
        try {
            // Debug logging
            System.out.println("Storage dir: " + storageDir.toAbsolutePath());
            System.out.println("Saving template with name: " + name);
            System.out.println("Template before save: " + mapper.writeValueAsString(template));
            
            // Update template name to match file name
            template.setTemplateName(name);
            
            Path filePath = storageDir.resolve(name + ".json");
            System.out.println("Full save path: " + filePath.toAbsolutePath());
            
            mapper.writeValue(filePath.toFile(), template);
            mediator.sendMessage("template", new Message("TEMPLATE_SAVED", template, null));
            
            System.out.println("Save completed successfully");
        } catch (IOException e) {
            System.err.println("Save failed with error: " + e.getMessage());
            e.printStackTrace();
            throw new TemplateStorageException("Failed to save template: " + name, e);
        }
    }

    /**
     * Loads a Template instance from storage.
     * 
     * @param name The name of the template to load
     * @return The loaded Template instance
     * @throws TemplateStorageException if loading fails
     */
    public Template loadTemplate(String name) {
        try {
            // Debug logging
            System.out.println("Storage dir: " + storageDir.toAbsolutePath());
            System.out.println("Loading template with name: " + name);
            
            Path filePath = storageDir.resolve(name + ".json");
            System.out.println("Full load path: " + filePath.toAbsolutePath());
            
            if (!Files.exists(filePath)) {
                System.err.println("Template file not found at: " + filePath.toAbsolutePath());
                throw new TemplateStorageException("Template not found: " + name, null);
            }
            
            Template template = mapper.readValue(filePath.toFile(), Template.class);
            System.out.println("TemplateStorage method -loadTemplate- Template loaded successfully: " + mapper.writeValueAsString(template));
            System.out.println("TemplateStorage method -loadTemplate- uses -mapper- for lengthy String above" );
            System.out.println("  ");
            
            // Initialize template after deserialization to restore transient fields and references
            // This is critical for proper operation of the template, as it:
            // 1. Re-links the DrawObjRoster's templateDrawObjs field to the Template's allDrawObjs list
            // 2. Updates the DrawObjRoster's internal state
            // 3. Resolves SelectionGroup references
            // Without this step, operations that depend on DrawObjRoster's templateDrawObjs
            // (such as palette loading) will fail because the field remains null after deserialization.
            System.out.println("TemplateStorage: Calling initializeAfterLoading for template: " + name);
            template.initializeAfterLoading();
            
            // Resolve SelectionGroup references after loading
            System.out.println("TemplateStorage: Resolving SelectionGroup references for template: " + name);
            template.resolveSelectionGroupReferences();
            
            mediator.sendMessage("template", new Message("TEMPLATE_LOADED", template, null));
            return template;
        } catch (IOException e) {
            System.err.println("Load failed with error: " + e.getMessage());
            e.printStackTrace();
            throw new TemplateStorageException("Failed to load template: " + name, e);
        }
    }
}

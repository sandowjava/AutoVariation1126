package com.csurf.templatecore.svgconverter;

import com.csurf.core.ApplicationMediator;
import com.csurf.core.Message;
import com.csurf.core.SubunitMediator;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.svgconverter.controller.ConvertSVGFileToTemplate;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Mediator for the SVG subunit. Handles communication between the SVG subunit and other parts
 * of the application through the HMVC pattern.
 */
public class SVGPackageMediator implements SubunitMediator {
    private final SVGTopViewManager module;
    private final ApplicationMediator appMediator;
    private DrawObj[] currentDrawObjs;
    
    public SVGPackageMediator(ApplicationMediator appMediator) {
        this.appMediator = appMediator;
        this.module = new SVGTopViewManager();
    }
    
    @Override
    public void initialize() {
        // Nothing to initialize
    }
    
    @Override
    public JComponent getVisualComponent() {
        return module;
    }
    
    @Override
    public Object handleMessage(Message message) {
        switch (message.getType()) {
            case "CONVERT_FILE":
                if (message.getPayload() instanceof File) {
                    try {
                        currentDrawObjs = ConvertSVGFileToTemplate.convertFile((File) message.getPayload());
                        module.setDrawObjs(currentDrawObjs);
                        return currentDrawObjs;
                    } catch (IOException e) {
                        return e;
                    }
                }
                break;
                
            case "GET_CURRENT_OBJECTS":
                return currentDrawObjs;
                
            case "OPEN_SHAPE_EDITOR":
                // Send message to parent mediator to open shape editor
                return appMediator.sendMessage("shape_editor", message);
        }
        return null;
    }
    
    public SVGTopViewManager getModule() {
        return module;
    }
}

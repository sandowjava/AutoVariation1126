package com.csurf.templatecore.svgconverter.controller;

import com.csurf.core.Message;
import com.csurf.core.SubunitMediator;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.svgconverter.view.SVGConverterFrame;

import javax.swing.*;
import java.io.File;

/**
 * Mediator responsible for handling SVG conversion operations.
 * Maintains separation of concerns from the main SmartPicSubunitMediator.
 */
public class SVGConverterSubunitMediator implements SubunitMediator {
    private SVGConverterFrame converterFrame;
    
    public static final String CONVERT_FILE = "CONVERT_FILE";
    public static final String SHOW_CONVERTER = "SHOW_CONVERTER";
    
    public SVGConverterSubunitMediator() {
    } // end constructor



    
    @Override
    public void initialize() {
        // Will be implemented when needed
    } // end method initialize



    
    @Override
    public Object handleMessage(Message message) {
        String type = message.getType();
        
        switch (type) {
            case CONVERT_FILE:
                try {
                    return handleFileConversion(message.getPayload());
                } catch (Exception e) {
                    return e;
                }
            case SHOW_CONVERTER:
                if (converterFrame == null) {
                    // Get ApplicationMediator from the message source
                    SubunitMediator source = message.getSource();
                    if (source instanceof com.csurf.core.ApplicationMediator) {
                        converterFrame = new SVGConverterFrame(
                            (com.csurf.core.ApplicationMediator) source);
                    }
                }
                if (converterFrame != null) {
                    converterFrame.setVisible(true);
                }
                return null;
            default:
                return null;
        }
    } // end method handleMessage



    
    private DrawObj[] handleFileConversion(Object payload) throws Exception {
        if (!(payload instanceof File)) {
            throw new IllegalArgumentException("Expected File payload for CONVERT_FILE message");
        }
        
        File file = (File) payload;
        return ConvertSVGFileToTemplate.convertFile(file);
    } // end method handleFileConversion



    
    @Override
    public JComponent getVisualComponent() {
        return converterFrame != null ? converterFrame.getRootPane() : null;
    }
}

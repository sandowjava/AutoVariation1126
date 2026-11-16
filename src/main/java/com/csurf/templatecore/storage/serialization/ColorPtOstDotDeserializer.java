package com.csurf.templatecore.storage.serialization;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.model.MonotriBook;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom deserializer for ColorPtOstDot objects.
 * Reconstructs the color point using its fill order number.
 * Integrates with FallbackTracker to monitor fallback strategy usage.
 */
public class ColorPtOstDotDeserializer extends JsonDeserializer<ColorPtOstDot> {
    
    // Thread-local storage for current filename being deserialized
    private static final ThreadLocal<String> currentFilename = new ThreadLocal<>();
    private static final ThreadLocal<Integer> colorIndex = new ThreadLocal<>();
    
    /**
     * Sets the current filename for tracking purposes.
     * Should be called before deserializing colors from a file.
     */
    public static void setCurrentFilename(String filename) {
        currentFilename.set(filename);
    }
    
    /**
     * Sets the current color index for tracking purposes.
     */
    public static void setColorIndex(int index) {
        colorIndex.set(index);
    }
    
    /**
     * Clears the thread-local context.
     */
    public static void clearContext() {
        currentFilename.remove();
        colorIndex.remove();
    }
    
    @Override
    public ColorPtOstDot deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        MonotriBook book = MonotriBook.getInstance();
        FallbackTracker tracker = FallbackTracker.getInstance();

        // Extract fill order number and hue number with safety checks
        int fillOrderNum = 1;
        int hueNum = 1;
        int originalFillOrderNum = fillOrderNum;
        int originalHueNum = hueNum;
        
        try {
            if (node.has("fillOrderNum")) {
                fillOrderNum = node.get("fillOrderNum").asInt();
                originalFillOrderNum = fillOrderNum;
            } else {
                System.out.println("WARNING ColorPtOstDotDeserializer: Missing fillOrderNum, using default value 1");
            }
            
            if (node.has("hueNum")) {
                hueNum = node.get("hueNum").asInt();
                originalHueNum = hueNum;
            } else {
                System.out.println("WARNING ColorPtOstDotDeserializer: Missing hueNum, using default value 1");
            }
        } catch (Exception e) {
            System.out.println("WARNING ColorPtOstDotDeserializer: Error reading JSON fields: " + e.getMessage());
            // Continue with default values
        }
        
        System.out.println("DEBUG ColorPtOstDotDeserializer: Deserializing color with hueNum=" + hueNum + ", fillOrderNum=" + fillOrderNum);
        
        // Try multiple fallback strategies to ensure we never return null
        ColorPtOstDot color = null;
        
        try {
            // Strategy 1: Get the ColorPtOstDot from MonotriBook using specified hue and fill order
            try {
                color = book.getMonoTriPage(hueNum).getColorByFillOrder(fillOrderNum);
                if (color != null) {
                    System.out.println("DEBUG ColorPtOstDotDeserializer: Successfully deserialized color for hueNum=" + hueNum + ", fillOrderNum=" + fillOrderNum);
                    return color;
                }
            } catch (Exception e) {
                System.out.println("DEBUG ColorPtOstDotDeserializer: Failed to get color with primary strategy: " + e.getMessage());
            }
            
            // Strategy 2: Try to get the first color in this hue
            try {
                System.out.println("DEBUG ColorPtOstDotDeserializer: Attempting to get first color from hue " + hueNum);
                color = book.getMonoTriPage(hueNum).getColorByFillOrder(1);
                if (color != null) {
                    System.out.println("DEBUG ColorPtOstDotDeserializer: Using first color in hue " + hueNum);
                    recordFallback(tracker, 2, originalHueNum, originalFillOrderNum, hueNum, 1, "First color in hue");
                    return color;
                }
            } catch (Exception e) {
                System.out.println("DEBUG ColorPtOstDotDeserializer: Failed to get first color in hue: " + e.getMessage());
            }
            
            // Strategy 3: Try hue 1 (which should always exist)
            try {
                System.out.println("DEBUG ColorPtOstDotDeserializer: Attempting to get color from default hue 1");
                color = book.getMonoTriPage(1).getColorByFillOrder(1);
                if (color != null) {
                    System.out.println("DEBUG ColorPtOstDotDeserializer: Using color from default hue 1");
                    recordFallback(tracker, 3, originalHueNum, originalFillOrderNum, 1, 1, "Default hue 1");
                    return color;
                }
            } catch (Exception e) {
                System.out.println("DEBUG ColorPtOstDotDeserializer: Failed to get color from default hue: " + e.getMessage());
            }
            
            // Strategy 4: Last resort - brute force search
            System.out.println("WARNING ColorPtOstDotDeserializer: All color lookup strategies failed, attempting brute force search");
            
            // Try to get any color from the book as a last resort
            for (int h = 1; h <= 36; h++) {
                try {
                    color = book.getMonoTriPage(h).getColorByFillOrder(1);
                    if (color != null) {
                        System.out.println("DEBUG ColorPtOstDotDeserializer: Found color in hue " + h);
                        recordFallback(tracker, 4, originalHueNum, originalFillOrderNum, h, 1, "Brute force search");
                        return color;
                    }
                } catch (Exception ignored) {
                    // Just try the next hue
                }
            }
            
            // If we still don't have a color, throw an exception
            throw new IOException("Failed to create a valid ColorPtOstDot after trying all fallback strategies");
            
        } catch (Exception e) {
            System.out.println("ERROR ColorPtOstDotDeserializer: Exception while getting color: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to deserialize ColorPtOstDot", e);
        }
    }
    
    /**
     * Helper method to record a fallback event.
     */
    private void recordFallback(FallbackTracker tracker, int strategy, int originalHueNum,
                               int originalFillOrderNum, int fallbackHueNum, int fallbackFillOrderNum,
                               String reason) {
        String filename = currentFilename.get();
        int index = colorIndex.get() != null ? colorIndex.get() : -1;
        
        if (filename != null) {
            tracker.recordFallback(filename, strategy, originalHueNum, originalFillOrderNum,
                                 fallbackHueNum, fallbackFillOrderNum, reason, index);
        }
    }
}

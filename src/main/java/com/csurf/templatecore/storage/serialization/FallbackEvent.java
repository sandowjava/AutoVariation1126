package com.csurf.templatecore.storage.serialization;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Records a single fallback event during deserialization.
 * Captures all relevant information about when and why a fallback strategy was used.
 */
public class FallbackEvent {
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // Event metadata
    private final String filename;
    private final LocalDateTime timestamp;
    private final int strategy; // 1, 2, 3, or 4
    
    // Original values from JSON
    private final int originalHueNum;
    private final int originalFillOrderNum;
    
    // Fallback values used
    private final int fallbackHueNum;
    private final int fallbackFillOrderNum;
    
    // Reason for fallback
    private final String reason;
    
    // Color index in the file (for palette files)
    private final int colorIndex;
    
    /**
     * Creates a new FallbackEvent.
     * 
     * @param filename Name of the file being deserialized
     * @param strategy Which fallback strategy was used (1-4)
     * @param originalHueNum Original hueNum from JSON
     * @param originalFillOrderNum Original fillOrderNum from JSON
     * @param fallbackHueNum HueNum used as fallback
     * @param fallbackFillOrderNum FillOrderNum used as fallback
     * @param reason Why the fallback was needed
     * @param colorIndex Index of the color in the file
     */
    public FallbackEvent(String filename, int strategy, int originalHueNum, int originalFillOrderNum,
                        int fallbackHueNum, int fallbackFillOrderNum, String reason, int colorIndex) {
        this.filename = filename;
        this.timestamp = LocalDateTime.now();
        this.strategy = strategy;
        this.originalHueNum = originalHueNum;
        this.originalFillOrderNum = originalFillOrderNum;
        this.fallbackHueNum = fallbackHueNum;
        this.fallbackFillOrderNum = fallbackFillOrderNum;
        this.reason = reason;
        this.colorIndex = colorIndex;
    }
    
    // Getters
    public String getFilename() {
        return filename;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public int getStrategy() {
        return strategy;
    }
    
    public int getOriginalHueNum() {
        return originalHueNum;
    }
    
    public int getOriginalFillOrderNum() {
        return originalFillOrderNum;
    }
    
    public int getFallbackHueNum() {
        return fallbackHueNum;
    }
    
    public int getFallbackFillOrderNum() {
        return fallbackFillOrderNum;
    }
    
    public String getReason() {
        return reason;
    }
    
    public int getColorIndex() {
        return colorIndex;
    }
    
    /**
     * Returns a human-readable description of this fallback event.
     */
    @Override
    public String toString() {
        return String.format(
            "[%s] File: %s, Color %d, Strategy %d: " +
            "Original(hue=%d, fill=%d) -> Fallback(hue=%d, fill=%d) [%s]",
            TIMESTAMP_FORMAT.format(timestamp),
            filename,
            colorIndex,
            strategy,
            originalHueNum,
            originalFillOrderNum,
            fallbackHueNum,
            fallbackFillOrderNum,
            reason
        );
    }
    
    /**
     * Returns a CSV-formatted line for this event.
     */
    public String toCsvLine() {
        return String.format(
            "%s,%s,%d,%d,%d,%d,%d,%d,%s",
            TIMESTAMP_FORMAT.format(timestamp),
            filename,
            colorIndex,
            strategy,
            originalHueNum,
            originalFillOrderNum,
            fallbackHueNum,
            fallbackFillOrderNum,
            reason
        );
    }
    
    /**
     * Returns CSV header for FallbackEvent data.
     */
    public static String getCsvHeader() {
        return "Timestamp,Filename,ColorIndex,Strategy,OriginalHueNum,OriginalFillOrderNum,FallbackHueNum,FallbackFillOrderNum,Reason";
    }
}

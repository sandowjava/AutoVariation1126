package com.csurf.templatecore.storage.serialization;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Singleton class that tracks all fallback events during deserialization.
 * Provides methods to record fallback usage and generate reports.
 *
 * <h2>Usage Workflow</h2>
 *
 * <p>The FallbackTracker is used in conjunction with ColorPtOstDotDeserializer to monitor
 * when fallback strategies are used during color deserialization. The typical workflow is:</p>
 *
 * <ol>
 *   <li>Before deserializing a file, call {@link ColorPtOstDotDeserializer#setCurrentFilename(String)}
 *       with the filename being deserialized</li>
 *   <li>Before deserializing each color in a palette, call {@link ColorPtOstDotDeserializer#setColorIndex(int)}
 *       with the 0-based index of that color</li>
 *   <li>When a fallback strategy is used, ColorPtOstDotDeserializer calls {@link #recordFallback(String, int, int, int, int, int, String, int)}
 *       to record the event</li>
 *   <li>After deserialization completes, call {@link ColorPtOstDotDeserializer#clearContext()} to clean up</li>
 *   <li>Query the tracker using methods like {@link #getAllEvents()}, {@link #getEventsForFile(String)},
 *       or {@link #getSummaryForFile(String)} to analyze fallback usage</li>
 * </ol>
 *
 * <h2>Example</h2>
 *
 * <pre>
 * // Before deserialization
 * ColorPtOstDotDeserializer.setCurrentFilename("mypalette.json");
 *
 * // During deserialization of each color (done by PaletteWShapeNamesDeserializer)
 * for (int i = 0; i < colors.size(); i++) {
 *     ColorPtOstDotDeserializer.setColorIndex(i);
 *     // deserialize color i
 * }
 *
 * // After deserialization
 * ColorPtOstDotDeserializer.clearContext();
 *
 * // Query results
 * FallbackTracker tracker = FallbackTracker.getInstance();
 * String summary = tracker.getSummaryForFile("mypalette.json");
 * System.out.println(summary);
 * </pre>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>All public methods are synchronized to ensure thread-safe access. However, the
 * filename and color index context (stored in ThreadLocal) is per-thread, so each
 * thread can independently deserialize files without interference.</p>
 */
public class FallbackTracker {
    
    private static FallbackTracker instance;
    
    // List of all fallback events
    private final List<FallbackEvent> events;
    
    // Statistics
    private int totalFallbacksUsed = 0;
    private int[] strategyUsageCount = new int[5]; // Index 0 unused, 1-4 for strategies
    private Set<String> filesWithFallbacks = new HashSet<>();
    
    // Configuration
    private boolean enabled = true;
    
    /**
     * Private constructor for singleton pattern.
     */
    private FallbackTracker() {
        this.events = new ArrayList<>();
        Arrays.fill(strategyUsageCount, 0);
    }
    
    /**
     * Gets the singleton instance of FallbackTracker.
     */
    public static synchronized FallbackTracker getInstance() {
        if (instance == null) {
            instance = new FallbackTracker();
        }
        return instance;
    }
    
    /**
     * Records a fallback event.
     */
    public synchronized void recordFallback(String filename, int strategy, int originalHueNum,
                                           int originalFillOrderNum, int fallbackHueNum,
                                           int fallbackFillOrderNum, String reason, int colorIndex) {
        if (!enabled) {
            return;
        }
        
        FallbackEvent event = new FallbackEvent(
            filename, strategy, originalHueNum, originalFillOrderNum,
            fallbackHueNum, fallbackFillOrderNum, reason, colorIndex
        );
        
        events.add(event);
        totalFallbacksUsed++;
        strategyUsageCount[strategy]++;
        filesWithFallbacks.add(filename);
        
        System.out.println("FALLBACK TRACKED: " + event);
    }
    
    /**
     * Enables or disables fallback tracking.
     */
    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Returns whether fallback tracking is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Gets all recorded fallback events.
     */
    public synchronized List<FallbackEvent> getAllEvents() {
        return new ArrayList<>(events);
    }
    
    /**
     * Gets fallback events for a specific file.
     */
    public synchronized List<FallbackEvent> getEventsForFile(String filename) {
        List<FallbackEvent> result = new ArrayList<>();
        for (FallbackEvent event : events) {
            if (event.getFilename().equals(filename)) {
                result.add(event);
            }
        }
        return result;
    }
    
    /**
     * Gets fallback events for a specific strategy.
     */
    public synchronized List<FallbackEvent> getEventsForStrategy(int strategy) {
        List<FallbackEvent> result = new ArrayList<>();
        for (FallbackEvent event : events) {
            if (event.getStrategy() == strategy) {
                result.add(event);
            }
        }
        return result;
    }
    
    /**
     * Gets the total number of fallback events recorded.
     */
    public synchronized int getTotalFallbackCount() {
        return totalFallbacksUsed;
    }
    
    /**
     * Gets the number of files that used fallbacks.
     */
    public synchronized int getFilesWithFallbackCount() {
        return filesWithFallbacks.size();
    }
    
    /**
     * Gets the set of files that used fallbacks.
     */
    public synchronized Set<String> getFilesWithFallbacks() {
        return new HashSet<>(filesWithFallbacks);
    }
    
    /**
     * Gets the usage count for a specific strategy.
     */
    public synchronized int getStrategyUsageCount(int strategy) {
        if (strategy < 1 || strategy > 4) {
            return 0;
        }
        return strategyUsageCount[strategy];
    }
    
    /**
     * Prints a summary report to console.
     */
    public synchronized void printSummaryReport() {
        System.out.println("\n========== FALLBACK TRACKING SUMMARY REPORT ==========");
        System.out.println("Total Fallback Events: " + totalFallbacksUsed);
        System.out.println("Files with Fallbacks: " + filesWithFallbacks.size());
        System.out.println("\nStrategy Usage Breakdown:");
        System.out.println("  Strategy 1 (Primary Lookup): " + strategyUsageCount[1]);
        System.out.println("  Strategy 2 (First Color in Hue): " + strategyUsageCount[2]);
        System.out.println("  Strategy 3 (Default Hue): " + strategyUsageCount[3]);
        System.out.println("  Strategy 4 (Brute Force Search): " + strategyUsageCount[4]);
        System.out.println("=====================================================\n");
    }
    
    /**
     * Exports all fallback events to a CSV file.
     */
    public synchronized void exportToCsv(String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.write(FallbackEvent.getCsvHeader() + "\n");
            
            // Write all events
            for (FallbackEvent event : events) {
                writer.write(event.toCsvLine() + "\n");
            }
            
            System.out.println("Fallback events exported to: " + filename);
        }
    }
    
    /**
     * Exports a detailed report to a text file.
     */
    public synchronized void exportDetailedReport(String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("========== FALLBACK TRACKING DETAILED REPORT ==========\n\n");
            
            writer.write("SUMMARY STATISTICS\n");
            writer.write("==================\n");
            writer.write("Total Fallback Events: " + totalFallbacksUsed + "\n");
            writer.write("Files with Fallbacks: " + filesWithFallbacks.size() + "\n");
            writer.write("\nStrategy Usage Breakdown:\n");
            writer.write("  Strategy 1 (Primary Lookup): " + strategyUsageCount[1] + "\n");
            writer.write("  Strategy 2 (First Color in Hue): " + strategyUsageCount[2] + "\n");
            writer.write("  Strategy 3 (Default Hue): " + strategyUsageCount[3] + "\n");
            writer.write("  Strategy 4 (Brute Force Search): " + strategyUsageCount[4] + "\n");
            
            writer.write("\n\nFILES WITH FALLBACKS\n");
            writer.write("====================\n");
            for (String file : filesWithFallbacks) {
                List<FallbackEvent> fileEvents = getEventsForFile(file);
                writer.write(String.format("%s (%d fallbacks)\n", file, fileEvents.size()));
            }
            
            writer.write("\n\nDETAILED EVENTS\n");
            writer.write("===============\n");
            for (FallbackEvent event : events) {
                writer.write(event.toString() + "\n");
            }
            
            writer.write("\n========== END OF REPORT ==========\n");
        }
        
        System.out.println("Detailed report exported to: " + filename);
    }
    
    /**
     * Gets a human-readable summary of fallback events for a specific file.
     *
     * @param filename The filename to get a summary for
     * @return A formatted string containing fallback information for the file, or a message
     *         indicating no fallbacks were recorded for the file
     */
    public synchronized String getSummaryForFile(String filename) {
        List<FallbackEvent> fileEvents = getEventsForFile(filename);

        if (fileEvents.isEmpty()) {
            return String.format("File '%s': No fallbacks recorded", filename);
        }

        // Count fallbacks by strategy
        int[] strategyCount = new int[5];
        for (FallbackEvent event : fileEvents) {
            strategyCount[event.getStrategy()]++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("File '%s': %d total fallbacks\n", filename, fileEvents.size()));
        sb.append("Strategy breakdown:\n");

        if (strategyCount[1] > 0) {
            sb.append(String.format("  Strategy 1 (Primary Lookup): %d\n", strategyCount[1]));
        }
        if (strategyCount[2] > 0) {
            sb.append(String.format("  Strategy 2 (First Color in Hue): %d\n", strategyCount[2]));
        }
        if (strategyCount[3] > 0) {
            sb.append(String.format("  Strategy 3 (Default Hue): %d\n", strategyCount[3]));
        }
        if (strategyCount[4] > 0) {
            sb.append(String.format("  Strategy 4 (Brute Force Search): %d\n", strategyCount[4]));
        }

        sb.append("\nDetailed events:\n");
        for (int i = 0; i < fileEvents.size(); i++) {
            FallbackEvent event = fileEvents.get(i);
            sb.append(String.format("  %d. Color %d: Strategy %d - %s\n",
                i + 1,
                event.getColorIndex(),
                event.getStrategy(),
                event.getReason()));
        }

        return sb.toString();
    }

    /**
     * Clears all recorded events and resets statistics.
     */
    public synchronized void reset() {
        events.clear();
        totalFallbacksUsed = 0;
        Arrays.fill(strategyUsageCount, 0);
        filesWithFallbacks.clear();
        System.out.println("FallbackTracker reset.");
    }
    
    /**
     * Returns a string representation of the current state.
     */
    @Override
    public synchronized String toString() {
        return String.format(
            "FallbackTracker{events=%d, files=%d, strategy1=%d, strategy2=%d, strategy3=%d, strategy4=%d}",
            totalFallbacksUsed,
            filesWithFallbacks.size(),
            strategyUsageCount[1],
            strategyUsageCount[2],
            strategyUsageCount[3],
            strategyUsageCount[4]
        );
    }
}

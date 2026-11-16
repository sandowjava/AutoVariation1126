package com.csurf.templatecore.storage.serialization;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Generates a report comparing scanner results with actual deserialization behavior.
 * Helps identify discrepancies between what the scanner predicts and what actually happens.
 */
public class FileDeserializationReport {
    
    private final FallbackTracker tracker;
    private final Map<String, Integer> fallbackCountByFile;
    private final Map<String, List<String>> fallbackReasonsByFile;
    
    /**
     * Creates a new FileDeserializationReport from the FallbackTracker.
     */
    public FileDeserializationReport(FallbackTracker tracker) {
        this.tracker = tracker;
        this.fallbackCountByFile = new HashMap<>();
        this.fallbackReasonsByFile = new HashMap<>();
        
        // Aggregate fallback data by file
        for (FallbackEvent event : tracker.getAllEvents()) {
            String filename = event.getFilename();
            
            fallbackCountByFile.put(filename, fallbackCountByFile.getOrDefault(filename, 0) + 1);
            
            fallbackReasonsByFile.computeIfAbsent(filename, k -> new ArrayList<>())
                .add(String.format("Strategy %d: %s (color %d)", event.getStrategy(), event.getReason(), event.getColorIndex()));
        }
    }
    
    /**
     * Gets the number of fallback events for a specific file.
     */
    public int getFallbackCountForFile(String filename) {
        return fallbackCountByFile.getOrDefault(filename, 0);
    }
    
    /**
     * Gets all files that had fallback events.
     */
    public Set<String> getFilesWithFallbacks() {
        return new HashSet<>(fallbackCountByFile.keySet());
    }
    
    /**
     * Gets the fallback reasons for a specific file.
     */
    public List<String> getFallbackReasonsForFile(String filename) {
        return fallbackReasonsByFile.getOrDefault(filename, new ArrayList<>());
    }
    
    /**
     * Exports a detailed report to a text file.
     */
    public void exportToFile(String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("========== FILE DESERIALIZATION REPORT ==========\n\n");
            
            writer.write("SUMMARY\n");
            writer.write("=======\n");
            writer.write(String.format("Total Fallback Events: %d\n", tracker.getTotalFallbackCount()));
            writer.write(String.format("Files with Fallbacks: %d\n", fallbackCountByFile.size()));
            writer.write("\nStrategy Usage:\n");
            writer.write(String.format("  Strategy 1 (Primary Lookup): %d\n", tracker.getStrategyUsageCount(1)));
            writer.write(String.format("  Strategy 2 (First Color in Hue): %d\n", tracker.getStrategyUsageCount(2)));
            writer.write(String.format("  Strategy 3 (Default Hue): %d\n", tracker.getStrategyUsageCount(3)));
            writer.write(String.format("  Strategy 4 (Brute Force Search): %d\n", tracker.getStrategyUsageCount(4)));
            
            writer.write("\n\nFILES WITH FALLBACKS\n");
            writer.write("====================\n");
            
            // Sort files by fallback count (descending)
            List<Map.Entry<String, Integer>> sortedFiles = new ArrayList<>(fallbackCountByFile.entrySet());
            sortedFiles.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            
            for (Map.Entry<String, Integer> entry : sortedFiles) {
                String file = entry.getKey();
                int count = entry.getValue();
                writer.write(String.format("\n%s (%d fallbacks)\n", file, count));
                
                List<String> reasons = getFallbackReasonsForFile(file);
                for (String reason : reasons) {
                    writer.write(String.format("  - %s\n", reason));
                }
            }
            
            writer.write("\n\nDETAILED EVENTS\n");
            writer.write("===============\n");
            for (FallbackEvent event : tracker.getAllEvents()) {
                writer.write(event.toString() + "\n");
            }
            
            writer.write("\n========== END OF REPORT ==========\n");
        }
        
        System.out.println("File deserialization report exported to: " + filename);
    }
    
    /**
     * Prints a summary report to console.
     */
    public void printSummary() {
        System.out.println("\n========== FILE DESERIALIZATION SUMMARY ==========");
        System.out.println("Total Fallback Events: " + tracker.getTotalFallbackCount());
        System.out.println("Files with Fallbacks: " + fallbackCountByFile.size());
        System.out.println("\nTop Files by Fallback Count:");
        
        List<Map.Entry<String, Integer>> sortedFiles = new ArrayList<>(fallbackCountByFile.entrySet());
        sortedFiles.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedFiles) {
            if (count >= 10) break;
            System.out.println(String.format("  %s: %d fallbacks", entry.getKey(), entry.getValue()));
            count++;
        }
        
        System.out.println("==================================================\n");
    }
}

package com.csurf.templatecore.storage.validation;

import com.csurf.templatecore.storage.serialization.FallbackTracker;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Compares scanner validation results with actual deserialization fallback behavior.
 * Identifies discrepancies and helps validate scanner accuracy.
 */
public class ValidationComparison {
    
    private final ValidationReport scannerResults;
    private final FallbackTracker fallbackTracker;
    
    // Comparison results
    private final List<String> falsePositives; // Scanner says corrupted, but no fallbacks used
    private final List<String> falseNegatives; // Scanner says clean, but fallbacks were used
    private final List<String> truePositives; // Scanner says corrupted, fallbacks were used
    private final List<String> trueNegatives; // Scanner says clean, no fallbacks used
    
    /**
     * Creates a new ValidationComparison.
     */
    public ValidationComparison(ValidationReport scannerResults, FallbackTracker fallbackTracker) {
        this.scannerResults = scannerResults;
        this.fallbackTracker = fallbackTracker;
        this.falsePositives = new ArrayList<>();
        this.falseNegatives = new ArrayList<>();
        this.truePositives = new ArrayList<>();
        this.trueNegatives = new ArrayList<>();
        
        performComparison();
    }
    
    /**
     * Performs the comparison between scanner results and actual deserialization.
     */
    private void performComparison() {
        Set<String> filesWithFallbacks = fallbackTracker.getFilesWithFallbacks();
        
        for (ValidationResult result : scannerResults.getResults()) {
            String filename = extractFilename(result.getFilePath().toString());
            boolean hasFallbacks = filesWithFallbacks.contains(filename);
            boolean scannerSaysClean = result.getStatus() == ValidationStatus.CLEAN;
            
            if (scannerSaysClean && !hasFallbacks) {
                // True negative: Scanner correct, file is clean
                trueNegatives.add(filename);
            } else if (scannerSaysClean && hasFallbacks) {
                // False negative: Scanner missed corruption
                falseNegatives.add(filename);
            } else if (!scannerSaysClean && hasFallbacks) {
                // True positive: Scanner correct, file has issues
                truePositives.add(filename);
            } else if (!scannerSaysClean && !hasFallbacks) {
                // False positive: Scanner incorrectly flagged file
                falsePositives.add(filename);
            }
        }
    }
    
    /**
     * Extracts filename from a full path.
     */
    private String extractFilename(String path) {
        return new java.io.File(path).getName();
    }
    
    /**
     * Gets false positives (scanner says corrupted, but no fallbacks used).
     */
    public List<String> getFalsePositives() {
        return new ArrayList<>(falsePositives);
    }
    
    /**
     * Gets false negatives (scanner says clean, but fallbacks were used).
     */
    public List<String> getFalseNegatives() {
        return new ArrayList<>(falseNegatives);
    }
    
    /**
     * Gets true positives (scanner says corrupted, fallbacks were used).
     */
    public List<String> getTruePositives() {
        return new ArrayList<>(truePositives);
    }
    
    /**
     * Gets true negatives (scanner says clean, no fallbacks used).
     */
    public List<String> getTrueNegatives() {
        return new ArrayList<>(trueNegatives);
    }
    
    /**
     * Calculates scanner accuracy metrics.
     */
    public double getAccuracy() {
        int total = truePositives.size() + trueNegatives.size() + falsePositives.size() + falseNegatives.size();
        if (total == 0) return 0.0;
        return (double) (truePositives.size() + trueNegatives.size()) / total;
    }
    
    /**
     * Calculates precision (of positive predictions).
     */
    public double getPrecision() {
        int positiveCount = truePositives.size() + falsePositives.size();
        if (positiveCount == 0) return 0.0;
        return (double) truePositives.size() / positiveCount;
    }
    
    /**
     * Calculates recall (of actual positives).
     */
    public double getRecall() {
        int actualPositives = truePositives.size() + falseNegatives.size();
        if (actualPositives == 0) return 0.0;
        return (double) truePositives.size() / actualPositives;
    }
    
    /**
     * Prints a summary report to console.
     */
    public void printSummary() {
        System.out.println("\n========== VALIDATION COMPARISON SUMMARY ==========");
        System.out.println("Total Files Analyzed: " + scannerResults.getTotalFilesScanned());
        System.out.println("Files with Fallbacks: " + fallbackTracker.getFilesWithFallbackCount());
        System.out.println();
        System.out.println("Comparison Results:");
        System.out.println("  True Positives (Correctly Identified as Corrupted): " + truePositives.size());
        System.out.println("  True Negatives (Correctly Identified as Clean): " + trueNegatives.size());
        System.out.println("  False Positives (Incorrectly Flagged as Corrupted): " + falsePositives.size());
        System.out.println("  False Negatives (Missed Corruption): " + falseNegatives.size());
        System.out.println();
        System.out.println("Accuracy Metrics:");
        System.out.println(String.format("  Accuracy: %.2f%%", getAccuracy() * 100));
        System.out.println(String.format("  Precision: %.2f%%", getPrecision() * 100));
        System.out.println(String.format("  Recall: %.2f%%", getRecall() * 100));
        System.out.println("===================================================\n");
    }
    
    /**
     * Exports a detailed comparison report to a file.
     */
    public void exportToFile(String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("========== VALIDATION COMPARISON REPORT ==========\n\n");
            
            writer.write("SUMMARY\n");
            writer.write("=======\n");
            writer.write(String.format("Total Files Analyzed: %d\n", scannerResults.getTotalFilesScanned()));
            writer.write(String.format("Files with Fallbacks: %d\n", fallbackTracker.getFilesWithFallbackCount()));
            writer.write(String.format("Total Fallback Events: %d\n\n", fallbackTracker.getTotalFallbackCount()));
            
            writer.write("COMPARISON RESULTS\n");
            writer.write("==================\n");
            writer.write(String.format("True Positives (Correctly Identified as Corrupted): %d\n", truePositives.size()));
            writer.write(String.format("True Negatives (Correctly Identified as Clean): %d\n", trueNegatives.size()));
            writer.write(String.format("False Positives (Incorrectly Flagged as Corrupted): %d\n", falsePositives.size()));
            writer.write(String.format("False Negatives (Missed Corruption): %d\n\n", falseNegatives.size()));
            
            writer.write("ACCURACY METRICS\n");
            writer.write("================\n");
            writer.write(String.format("Accuracy: %.2f%%\n", getAccuracy() * 100));
            writer.write(String.format("Precision: %.2f%%\n", getPrecision() * 100));
            writer.write(String.format("Recall: %.2f%%\n\n", getRecall() * 100));
            
            if (!falsePositives.isEmpty()) {
                writer.write("FALSE POSITIVES (Scanner says corrupted, but no fallbacks used)\n");
                writer.write("==============================================================\n");
                for (String file : falsePositives) {
                    writer.write(String.format("  - %s\n", file));
                }
                writer.write("\n");
            }
            
            if (!falseNegatives.isEmpty()) {
                writer.write("FALSE NEGATIVES (Scanner says clean, but fallbacks were used)\n");
                writer.write("==============================================================\n");
                for (String file : falseNegatives) {
                    int fallbackCount = fallbackTracker.getEventsForFile(file).size();
                    writer.write(String.format("  - %s (%d fallbacks)\n", file, fallbackCount));
                }
                writer.write("\n");
            }
            
            if (!truePositives.isEmpty()) {
                writer.write("TRUE POSITIVES (Scanner correctly identified as corrupted)\n");
                writer.write("=========================================================\n");
                for (String file : truePositives) {
                    int fallbackCount = fallbackTracker.getEventsForFile(file).size();
                    writer.write(String.format("  - %s (%d fallbacks)\n", file, fallbackCount));
                }
                writer.write("\n");
            }
            
            writer.write("========== END OF REPORT ==========\n");
        }
        
        System.out.println("Validation comparison report exported to: " + filename);
    }
}

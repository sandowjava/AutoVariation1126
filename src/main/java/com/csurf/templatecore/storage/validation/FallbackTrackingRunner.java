package com.csurf.templatecore.storage.validation;

import com.csurf.core.ApplicationMediator;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.storage.TemplateStorage;
import com.csurf.templatecore.storage.serialization.ColorPtOstDotDeserializer;
import com.csurf.templatecore.storage.serialization.FallbackTracker;
import com.csurf.templatecore.storage.serialization.FileDeserializationReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runner for demonstrating fallback tracking and generating comparison reports.
 * 
 * This runner:
 * 1. Loads all template files with fallback tracking enabled
 * 2. Generates a fallback tracking report
 * 3. Compares scanner results with actual deserialization behavior
 * 4. Exports detailed reports
 */
public class FallbackTrackingRunner {
    
    private static final String TEMPLATES_DIR = "dev_storage/templates";
    private static final String PALETTES_DIR = "dev_storage/palettes";
    
    public static void main(String[] args) {
        System.out.println("========== FALLBACK TRACKING RUNNER ==========\n");
        
        FallbackTracker tracker = FallbackTracker.getInstance();
        tracker.setEnabled(true);
        
        try {
            // Step 1: Load scanner results
            System.out.println("Step 1: Loading scanner results...");
            Path paletteDir = Paths.get(PALETTES_DIR);
            Path templateDir = Paths.get(TEMPLATES_DIR);
            FileValidationScanner scanner = new FileValidationScanner(paletteDir, templateDir);
            ValidationReport scannerResults = scanner.scanAllFiles();
            System.out.println("Scanner found: " + scannerResults.getTotalFilesScanned() + " files");
            System.out.println("  - Clean: " + scannerResults.getCleanFiles());
            System.out.println("  - Salvageable: " + scannerResults.getSalvageableFiles());
            System.out.println("  - Not Salvageable: " + scannerResults.getNotSalvageableFiles());
            
            // Step 2: Load template files to trigger deserialization with tracking
            System.out.println("\nStep 2: Loading template files with fallback tracking...");
            loadTemplateFilesWithTracking();
            
            // Step 3: Print fallback summary
            System.out.println("\nStep 3: Fallback tracking summary:");
            tracker.printSummaryReport();
            
            // Step 4: Generate detailed reports
            System.out.println("Step 4: Generating detailed reports...");
            
            // Export fallback events to CSV
            tracker.exportToCsv("fallback_events.csv");
            
            // Export detailed fallback report
            tracker.exportDetailedReport("fallback_tracking_report.txt");
            
            // Generate file deserialization report
            FileDeserializationReport deserializationReport = new FileDeserializationReport(tracker);
            deserializationReport.exportToFile("file_deserialization_report.txt");
            deserializationReport.printSummary();
            
            // Step 5: Compare scanner results with actual deserialization
            System.out.println("Step 5: Comparing scanner results with actual deserialization...");
            ValidationComparison comparison = new ValidationComparison(scannerResults, tracker);
            comparison.printSummary();
            comparison.exportToFile("validation_comparison_report.txt");
            
            System.out.println("\n========== FALLBACK TRACKING COMPLETE ==========");
            System.out.println("Reports generated:");
            System.out.println("  - fallback_events.csv");
            System.out.println("  - fallback_tracking_report.txt");
            System.out.println("  - file_deserialization_report.txt");
            System.out.println("  - validation_comparison_report.txt");
            
        } catch (Exception e) {
            System.err.println("Error during fallback tracking: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads all template files with fallback tracking enabled.
     */
    private static void loadTemplateFilesWithTracking() {
        try {
            Path templatesPath = Paths.get(TEMPLATES_DIR);
            if (!Files.exists(templatesPath)) {
                System.out.println("Templates directory not found: " + templatesPath.toAbsolutePath());
                return;
            }
            
            List<Path> templateFiles = Files.list(templatesPath)
                .filter(p -> p.toString().endsWith(".json"))
                .collect(Collectors.toList());
            
            System.out.println("Found " + templateFiles.size() + " template files");
            
            // Create a minimal ApplicationMediator for storage operations
            ApplicationMediator mediator = new ApplicationMediator();
            TemplateStorage storage = new TemplateStorage(mediator);
            int successCount = 0;
            int failureCount = 0;
            
            for (Path templateFile : templateFiles) {
                try {
                    String filename = templateFile.getFileName().toString();
                    System.out.println("  Loading: " + filename);
                    
                    // Set filename context for tracking
                    ColorPtOstDotDeserializer.setCurrentFilename(filename);
                    
                    // Load the template (remove .json extension for the name parameter)
                    String templateName = filename.endsWith(".json") ? filename.substring(0, filename.length() - 5) : filename;
                    Template template = storage.loadTemplate(templateName);
                    
                    // Clear context
                    ColorPtOstDotDeserializer.clearContext();
                    
                    successCount++;
                } catch (Exception e) {
                    System.out.println("    ERROR: " + e.getMessage());
                    failureCount++;
                }
            }
            
            System.out.println("Template loading complete: " + successCount + " successful, " + failureCount + " failed");
            
        } catch (IOException e) {
            System.err.println("Error reading templates directory: " + e.getMessage());
        }
    }
}

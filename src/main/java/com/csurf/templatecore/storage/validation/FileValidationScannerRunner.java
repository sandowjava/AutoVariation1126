package com.csurf.templatecore.storage.validation;

import com.csurf.templatecore.storage.config.StorageConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileValidationScannerRunner {
    public static void main(String[] args) {
        System.out.println("Starting File Validation Scanner...");
        System.out.println();

        Path paletteDir = StorageConfig.getPalettesPath();
        Path templateDir = StorageConfig.getTemplatesPath();

        System.out.println("Palette Directory: " + paletteDir.toAbsolutePath());
        System.out.println("Template Directory: " + templateDir.toAbsolutePath());
        System.out.println();

        FileValidationScanner scanner = new FileValidationScanner(paletteDir, templateDir);

        ValidationReport report = scanner.scanAllFiles();

        scanner.printSummary(report);

        Path reportFile = Paths.get("validation_report.txt");
        scanner.generateDetailedReport(report, reportFile);
    }
}

package com.csurf.templatecore.storage.validation;

import com.csurf.ostwald.domainmonotri.model.HueNumGUI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FileValidationScanner {
    private final Path paletteDir;
    private final Path templateDir;
    private final ObjectMapper objectMapper;

    public FileValidationScanner(Path paletteDir, Path templateDir) {
        this.paletteDir = paletteDir;
        this.templateDir = templateDir;
        this.objectMapper = new ObjectMapper();
    }

    public ValidationReport scanAllFiles() {
        long startTime = System.currentTimeMillis();
        List<ValidationResult> allResults = new ArrayList<>();

        System.out.println("Scanning palette files...");
        allResults.addAll(scanDirectory(paletteDir, "palette"));

        System.out.println("Scanning template files...");
        allResults.addAll(scanDirectory(templateDir, "template"));

        long duration = System.currentTimeMillis() - startTime;
        return buildReport(allResults, duration);
    }

    public ValidationReport scanPaletteFiles() {
        long startTime = System.currentTimeMillis();
        List<ValidationResult> results = scanDirectory(paletteDir, "palette");
        long duration = System.currentTimeMillis() - startTime;
        return buildReport(results, duration);
    }

    public ValidationReport scanTemplateFiles() {
        long startTime = System.currentTimeMillis();
        List<ValidationResult> results = scanDirectory(templateDir, "template");
        long duration = System.currentTimeMillis() - startTime;
        return buildReport(results, duration);
    }

    private List<ValidationResult> scanDirectory(Path directory, String type) {
        List<ValidationResult> results = new ArrayList<>();

        if (!Files.exists(directory)) {
            System.out.println("WARNING: Directory does not exist: " + directory);
            return results;
        }

        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.toString().endsWith(".json"))
                    .forEach(filePath -> {
                        ValidationResult result = validateFile(filePath);
                        results.add(result);
                        printProgress(filePath, result);
                    });
        } catch (IOException e) {
            System.err.println("ERROR: Failed to scan directory: " + directory);
            e.printStackTrace();
        }

        return results;
    }

    public ValidationResult validateFile(Path filePath) {
        try {
            // Level 1: JSON Parsing
            JsonNode json = objectMapper.readTree(filePath.toFile());

            // Determine file type and validate accordingly
            if (json.has("colorPtOstdotArray")) {
                return validatePaletteFile(filePath, json);
            } else if (json.has("allDrawObjs")) {
                return validateTemplateFile(filePath, json);
            } else {
                return new ValidationResult(
                        filePath,
                        ValidationStatus.NOT_SALVAGEABLE,
                        CorruptionType.UNKNOWN,
                        "Unknown file format: neither palette nor template structure found",
                        "NONE",
                        "File does not match expected palette or template format",
                        0.0
                );
            }

        } catch (IOException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Unknown JSON parsing error";
            if (message.contains("Unexpected end-of-input") || e.getClass().getSimpleName().contains("EOF")) {
                message = "Malformed JSON: Unexpected end-of-input (file may be truncated)";
            } else if (message.contains("Unexpected character")) {
                message = "Malformed JSON: " + message;
            }
            return new ValidationResult(
                    filePath,
                    ValidationStatus.NOT_SALVAGEABLE,
                    CorruptionType.MALFORMED_JSON,
                    message,
                    "NONE",
                    "Cannot parse JSON",
                    0.0
            );
        }
    }

    private ValidationResult validatePaletteFile(Path filePath, JsonNode json) {
        try {
            JsonNode colorArray = json.get("colorPtOstdotArray");
            if (!colorArray.isArray() || colorArray.size() == 0) {
                return new ValidationResult(
                        filePath,
                        ValidationStatus.NOT_SALVAGEABLE,
                        CorruptionType.UNKNOWN,
                        "colorPtOstdotArray is empty or not an array",
                        "NONE",
                        "Invalid palette structure",
                        0.0
                );
            }

            List<String> issues = new ArrayList<>();
            int problemColorCount = 0;

            for (int i = 0; i < colorArray.size(); i++) {
                JsonNode colorObj = colorArray.get(i);
                String colorIssue = validateColorObject(colorObj);
                if (colorIssue != null) {
                    issues.add("Color " + i + ": " + colorIssue);
                    problemColorCount++;
                }
            }

            if (problemColorCount == colorArray.size()) {
                return new ValidationResult(
                        filePath,
                        ValidationStatus.NOT_SALVAGEABLE,
                        CorruptionType.INVALID_FILLORDERNUM,
                        "All colors have invalid hueNum or fillOrderNum",
                        "NONE",
                        "Cannot deserialize any colors in this palette",
                        0.0
                );
            } else if (problemColorCount > 0) {
                return new ValidationResult(
                        filePath,
                        ValidationStatus.SALVAGEABLE,
                        CorruptionType.INCONSISTENT_VALUES,
                        problemColorCount + " out of " + colorArray.size() + " colors have issues: " + 
                            String.join("; ", issues.subList(0, Math.min(3, issues.size()))),
                        "Remove or correct problematic colors",
                        "Some colors are corrupted but most can be deserialized",
                        0.85
                );
            } else {
                return new ValidationResult(
                        filePath,
                        ValidationStatus.CLEAN,
                        CorruptionType.NONE,
                        "All " + colorArray.size() + " colors are valid",
                        "NONE",
                        "Palette is clean and all colors can be deserialized",
                        1.0
                );
            }

        } catch (Exception e) {
            return new ValidationResult(
                    filePath,
                    ValidationStatus.NOT_SALVAGEABLE,
                    CorruptionType.UNKNOWN,
                    "Error validating palette: " + e.getMessage(),
                    "NONE",
                    "Cannot validate palette file",
                    0.0
            );
        }
    }

    private ValidationResult validateTemplateFile(Path filePath, JsonNode json) {
        try {
            JsonNode drawObjArray = json.get("allDrawObjs");
            if (!drawObjArray.isArray() || drawObjArray.size() == 0) {
                return new ValidationResult(
                        filePath,
                        ValidationStatus.NOT_SALVAGEABLE,
                        CorruptionType.UNKNOWN,
                        "allDrawObjs is empty or not an array",
                        "NONE",
                        "Invalid template structure",
                        0.0
                );
            }

            List<String> issues = new ArrayList<>();
            int problemObjCount = 0;

            for (int i = 0; i < drawObjArray.size(); i++) {
                JsonNode drawObj = drawObjArray.get(i);
                String objIssue = validateDrawObject(drawObj);
                if (objIssue != null) {
                    issues.add("Object " + i + ": " + objIssue);
                    problemObjCount++;
                }
            }

            if (problemObjCount == drawObjArray.size()) {
                return new ValidationResult(
                        filePath,
                        ValidationStatus.NOT_SALVAGEABLE,
                        CorruptionType.INVALID_FILLORDERNUM,
                        "All draw objects have invalid hueNum or fillOrderNum",
                        "NONE",
                        "Cannot deserialize any objects in this template",
                        0.0
                );
            } else if (problemObjCount > 0) {
                return new ValidationResult(
                        filePath,
                        ValidationStatus.SALVAGEABLE,
                        CorruptionType.INCONSISTENT_VALUES,
                        problemObjCount + " out of " + drawObjArray.size() + " objects have issues: " + 
                            String.join("; ", issues.subList(0, Math.min(3, issues.size()))),
                        "Remove or correct problematic objects",
                        "Some objects are corrupted but most can be deserialized",
                        0.85
                );
            } else {
                return new ValidationResult(
                        filePath,
                        ValidationStatus.CLEAN,
                        CorruptionType.NONE,
                        "All " + drawObjArray.size() + " objects are valid",
                        "NONE",
                        "Template is clean and all objects can be deserialized",
                        1.0
                );
            }

        } catch (Exception e) {
            return new ValidationResult(
                    filePath,
                    ValidationStatus.NOT_SALVAGEABLE,
                    CorruptionType.UNKNOWN,
                    "Error validating template: " + e.getMessage(),
                    "NONE",
                    "Cannot validate template file",
                    0.0
            );
        }
    }

    private String validateColorObject(JsonNode colorObj) {
        try {
            // Check for hueNum
            if (!colorObj.has("hueNum")) {
                return "Missing hueNum";
            }
            int hueNum = colorObj.get("hueNum").asInt();
            
            // Validate hueNum using HueNumGUI to handle code-facing range (0-35)
            // The JSON contains code-facing hueNum values (0-35), not user-facing (1-36)
            if (!isValidCodeHueNum(hueNum)) {
                return "Invalid hueNum=" + hueNum + " (valid range: 0-35)";
            }

            // Check for fillOrderNum
            if (!colorObj.has("fillOrderNum")) {
                return "Missing fillOrderNum";
            }
            int fillOrderNum = colorObj.get("fillOrderNum").asInt();
            if (fillOrderNum < 1 || fillOrderNum > 78) {
                return "Invalid fillOrderNum=" + fillOrderNum + " (valid range: 1-78)";
            }

            // If both are valid, deserialization will succeed
            // Check for consistency of other fields (optional, for corruption detection)
            return null;

        } catch (Exception e) {
            return "Error parsing color object: " + e.getMessage();
        }
    }

    private String validateDrawObject(JsonNode drawObj) {
        try {
            // Check for hueNum
            if (!drawObj.has("hueNum")) {
                return "Missing hueNum";
            }
            int hueNum = drawObj.get("hueNum").asInt();
            
            // Validate hueNum using HueNumGUI to handle code-facing range (0-35)
            // The JSON contains code-facing hueNum values (0-35), not user-facing (1-36)
            if (!isValidCodeHueNum(hueNum)) {
                return "Invalid hueNum=" + hueNum + " (valid range: 0-35)";
            }

            // Check for fillOrderNum
            if (!drawObj.has("fillOrderNum")) {
                return "Missing fillOrderNum";
            }
            int fillOrderNum = drawObj.get("fillOrderNum").asInt();
            if (fillOrderNum < 1 || fillOrderNum > 78) {
                return "Invalid fillOrderNum=" + fillOrderNum + " (valid range: 1-78)";
            }

            // If both are valid, deserialization will succeed
            return null;

        } catch (Exception e) {
            return "Error parsing draw object: " + e.getMessage();
        }
    }

    /**
     * Validates that a hueNum is in the valid code-facing range (0-35).
     * Uses HueNumGUI to explicitly flag the conversion and avoid off-by-one errors.
     * 
     * The JSON contains code-facing hueNum values (0-35) from the serializer,
     * which calls getStdCodeHueNum() that returns the internal code-facing value.
     * 
     * @param hueNum The hue number to validate (should be 0-35)
     * @return true if hueNum is valid (0-35), false otherwise
     */
    private boolean isValidCodeHueNum(int hueNum) {
        // First check: is it in the valid code-facing range?
        if (hueNum < 0 || hueNum > 35) {
            return false;
        }
        
        // Second check: verify using HueNumGUI that this is a valid code-facing value
        // If we add 1 to get user-facing, it should be in range 1-36
        try {
            HueNumGUI hueNumGUI = HueNumGUI.getHueNumGUIInstance(hueNum + 1);
            // If this succeeds without throwing an exception, hueNum is valid
            return true;
        } catch (IllegalArgumentException e) {
            // If HueNumGUI rejects it, it's not a valid code-facing value
            return false;
        }
    }

    private ValidationReport buildReport(List<ValidationResult> results, long duration) {
        int totalFiles = results.size();
        int cleanCount = 0;
        int salvageableCount = 0;
        int notSalvageableCount = 0;
        Map<CorruptionType, Integer> corruptionCounts = new HashMap<>();

        for (ValidationResult result : results) {
            switch (result.getStatus()) {
                case CLEAN:
                    cleanCount++;
                    break;
                case SALVAGEABLE:
                    salvageableCount++;
                    break;
                case NOT_SALVAGEABLE:
                    notSalvageableCount++;
                    break;
            }

            CorruptionType type = result.getCorruptionType();
            corruptionCounts.put(type, corruptionCounts.getOrDefault(type, 0) + 1);
        }

        return new ValidationReport(
                results,
                totalFiles,
                cleanCount,
                salvageableCount,
                notSalvageableCount,
                corruptionCounts,
                duration
        );
    }

    public void printSummary(ValidationReport report) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("FILE VALIDATION REPORT SUMMARY");
        System.out.println("=".repeat(70));
        System.out.println();

        System.out.println("OVERALL STATISTICS:");
        System.out.println("  Total Files Scanned: " + report.getTotalFilesScanned());
        System.out.println("  Clean Files: " + report.getCleanFiles() + 
                " (" + String.format("%.1f%%", report.getCleanPercentage()) + ")");
        System.out.println("  Salvageable Corruptions: " + report.getSalvageableFiles() + 
                " (" + String.format("%.1f%%", report.getSalvageablePercentage()) + ")");
        System.out.println("  Not Salvageable: " + report.getNotSalvageableFiles() + 
                " (" + String.format("%.1f%%", report.getNotSalvageablePercentage()) + ")");
        System.out.println("  Scan Duration: " + report.getScanDurationMs() + " ms");
        System.out.println();

        System.out.println("CORRUPTION TYPES FOUND:");
        Map<CorruptionType, Integer> counts = report.getCorruptionTypeCounts();
        if (counts.isEmpty() || (counts.size() == 1 && counts.containsKey(CorruptionType.NONE))) {
            System.out.println("  None - all files are clean");
        } else {
            counts.entrySet().stream()
                    .filter(e -> e.getKey() != CorruptionType.NONE)
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue() + " files"));
        }
        System.out.println();

        System.out.println("SALVAGEABLE CORRUPTIONS:");
        List<ValidationResult> salvageable = report.getResults().stream()
                .filter(r -> r.getStatus() == ValidationStatus.SALVAGEABLE)
                .toList();
        if (salvageable.isEmpty()) {
            System.out.println("  None");
        } else {
            for (ValidationResult result : salvageable) {
                System.out.println("  " + result.getFilePath().getFileName());
                System.out.println("    Issue: " + result.getIssue());
                System.out.println("    Repair: " + result.getRepairMethod());
            }
        }
        System.out.println();

        System.out.println("NOT SALVAGEABLE CORRUPTIONS:");
        List<ValidationResult> notSalvageable = report.getResults().stream()
                .filter(r -> r.getStatus() == ValidationStatus.NOT_SALVAGEABLE)
                .toList();
        if (notSalvageable.isEmpty()) {
            System.out.println("  None");
        } else {
            for (ValidationResult result : notSalvageable) {
                System.out.println("  " + result.getFilePath().getFileName());
                System.out.println("    Issue: " + result.getIssue());
                System.out.println("    Reason: " + result.getReason());
            }
        }
        System.out.println();

        System.out.println("=".repeat(70));
    }

    public void generateDetailedReport(ValidationReport report, Path outputFile) {
        try {
            StringBuilder sb = new StringBuilder();

            sb.append("FILE VALIDATION REPORT\n");
            sb.append("=".repeat(80)).append("\n\n");

            sb.append("SUMMARY STATISTICS\n");
            sb.append("-".repeat(80)).append("\n");
            sb.append("Total Files Scanned: ").append(report.getTotalFilesScanned()).append("\n");
            sb.append("Clean Files: ").append(report.getCleanFiles())
                    .append(" (").append(String.format("%.1f%%", report.getCleanPercentage())).append(")\n");
            sb.append("Salvageable Corruptions: ").append(report.getSalvageableFiles())
                    .append(" (").append(String.format("%.1f%%", report.getSalvageablePercentage())).append(")\n");
            sb.append("Not Salvageable: ").append(report.getNotSalvageableFiles())
                    .append(" (").append(String.format("%.1f%%", report.getNotSalvageablePercentage())).append(")\n");
            sb.append("Scan Duration: ").append(report.getScanDurationMs()).append(" ms\n\n");

            sb.append("CORRUPTION TYPES FOUND\n");
            sb.append("-".repeat(80)).append("\n");
            Map<CorruptionType, Integer> counts = report.getCorruptionTypeCounts();
            if (counts.isEmpty() || (counts.size() == 1 && counts.containsKey(CorruptionType.NONE))) {
                sb.append("None - all files are clean\n\n");
            } else {
                counts.entrySet().stream()
                        .filter(e -> e.getKey() != CorruptionType.NONE)
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append(" files\n"));
                sb.append("\n");
            }

            sb.append("CLEAN FILES\n");
            sb.append("-".repeat(80)).append("\n");
            List<ValidationResult> clean = report.getResults().stream()
                    .filter(r -> r.getStatus() == ValidationStatus.CLEAN)
                    .toList();
            if (clean.isEmpty()) {
                sb.append("None\n\n");
            } else {
                for (ValidationResult result : clean) {
                    sb.append(result.getFilePath()).append("\n");
                }
                sb.append("\n");
            }

            sb.append("SALVAGEABLE CORRUPTIONS\n");
            sb.append("-".repeat(80)).append("\n");
            List<ValidationResult> salvageable = report.getResults().stream()
                    .filter(r -> r.getStatus() == ValidationStatus.SALVAGEABLE)
                    .toList();
            if (salvageable.isEmpty()) {
                sb.append("None\n\n");
            } else {
                for (ValidationResult result : salvageable) {
                    sb.append(result.getFilePath()).append("\n");
                    sb.append("  Issue: ").append(result.getIssue()).append("\n");
                    sb.append("  Repair: ").append(result.getRepairMethod()).append("\n");
                    sb.append("  Reason: ").append(result.getReason()).append("\n");
                    sb.append("  Confidence: ").append(String.format("%.0f%%", result.getConfidence() * 100)).append("\n\n");
                }
            }

            sb.append("NOT SALVAGEABLE CORRUPTIONS\n");
            sb.append("-".repeat(80)).append("\n");
            List<ValidationResult> notSalvageable = report.getResults().stream()
                    .filter(r -> r.getStatus() == ValidationStatus.NOT_SALVAGEABLE)
                    .toList();
            if (notSalvageable.isEmpty()) {
                sb.append("None\n\n");
            } else {
                for (ValidationResult result : notSalvageable) {
                    sb.append(result.getFilePath()).append("\n");
                    sb.append("  Issue: ").append(result.getIssue()).append("\n");
                    sb.append("  Reason: ").append(result.getReason()).append("\n\n");
                }
            }

            Files.write(outputFile, sb.toString().getBytes());
            System.out.println("Detailed report written to: " + outputFile);

        } catch (IOException e) {
            System.err.println("ERROR: Failed to write report file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printProgress(Path filePath, ValidationResult result) {
        String status = result.getStatus().toString();
        String fileName = filePath.getFileName().toString();
        System.out.println("  [" + status + "] " + fileName);
    }
}

package com.csurf.templatecore.storage.validation;

import java.nio.file.Path;

public class ValidationResult {
    private final Path filePath;
    private final ValidationStatus status;
    private final CorruptionType corruptionType;
    private final String issue;
    private final String repairMethod;
    private final String reason;
    private final double confidence;

    public ValidationResult(
            Path filePath,
            ValidationStatus status,
            CorruptionType corruptionType,
            String issue,
            String repairMethod,
            String reason,
            double confidence) {
        this.filePath = filePath;
        this.status = status;
        this.corruptionType = corruptionType;
        this.issue = issue;
        this.repairMethod = repairMethod;
        this.reason = reason;
        this.confidence = confidence;
    }

    public Path getFilePath() {
        return filePath;
    }

    public ValidationStatus getStatus() {
        return status;
    }

    public CorruptionType getCorruptionType() {
        return corruptionType;
    }

    public String getIssue() {
        return issue;
    }

    public String getRepairMethod() {
        return repairMethod;
    }

    public String getReason() {
        return reason;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "filePath=" + filePath +
                ", status=" + status +
                ", corruptionType=" + corruptionType +
                ", issue='" + issue + '\'' +
                ", repairMethod='" + repairMethod + '\'' +
                ", reason='" + reason + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}

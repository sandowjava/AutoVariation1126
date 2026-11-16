package com.csurf.templatecore.storage.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationReport {
    private final List<ValidationResult> results;
    private final int totalFilesScanned;
    private final int cleanFiles;
    private final int salvageableFiles;
    private final int notSalvageableFiles;
    private final Map<CorruptionType, Integer> corruptionTypeCounts;
    private final long scanDurationMs;

    public ValidationReport(
            List<ValidationResult> results,
            int totalFilesScanned,
            int cleanFiles,
            int salvageableFiles,
            int notSalvageableFiles,
            Map<CorruptionType, Integer> corruptionTypeCounts,
            long scanDurationMs) {
        this.results = new ArrayList<>(results);
        this.totalFilesScanned = totalFilesScanned;
        this.cleanFiles = cleanFiles;
        this.salvageableFiles = salvageableFiles;
        this.notSalvageableFiles = notSalvageableFiles;
        this.corruptionTypeCounts = new HashMap<>(corruptionTypeCounts);
        this.scanDurationMs = scanDurationMs;
    }

    public List<ValidationResult> getResults() {
        return new ArrayList<>(results);
    }

    public int getTotalFilesScanned() {
        return totalFilesScanned;
    }

    public int getCleanFiles() {
        return cleanFiles;
    }

    public int getSalvageableFiles() {
        return salvageableFiles;
    }

    public int getNotSalvageableFiles() {
        return notSalvageableFiles;
    }

    public Map<CorruptionType, Integer> getCorruptionTypeCounts() {
        return new HashMap<>(corruptionTypeCounts);
    }

    public long getScanDurationMs() {
        return scanDurationMs;
    }

    public double getCleanPercentage() {
        if (totalFilesScanned == 0) return 0.0;
        return (cleanFiles * 100.0) / totalFilesScanned;
    }

    public double getSalvageablePercentage() {
        if (totalFilesScanned == 0) return 0.0;
        return (salvageableFiles * 100.0) / totalFilesScanned;
    }

    public double getNotSalvageablePercentage() {
        if (totalFilesScanned == 0) return 0.0;
        return (notSalvageableFiles * 100.0) / totalFilesScanned;
    }

    @Override
    public String toString() {
        return "ValidationReport{" +
                "totalFilesScanned=" + totalFilesScanned +
                ", cleanFiles=" + cleanFiles +
                ", salvageableFiles=" + salvageableFiles +
                ", notSalvageableFiles=" + notSalvageableFiles +
                ", scanDurationMs=" + scanDurationMs +
                '}';
    }
}

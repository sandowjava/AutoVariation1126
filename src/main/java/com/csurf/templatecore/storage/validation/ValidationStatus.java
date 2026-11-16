package com.csurf.templatecore.storage.validation;

public enum ValidationStatus {
    CLEAN,              // No issues found
    SALVAGEABLE,        // Can be automatically repaired
    NOT_SALVAGEABLE     // Cannot be automatically repaired
}

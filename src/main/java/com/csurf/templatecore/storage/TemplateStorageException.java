package com.csurf.templatecore.storage;

/**
 * Exception thrown when template storage operations fail.
 */
public class TemplateStorageException extends RuntimeException {
    public TemplateStorageException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public TemplateStorageException(String message) {
        super(message);
    }
}

package com.csurf.variationmodule.model;

/**
 * Thrown when a SmartPic cannot be built: an unreadable or invalid AGprefs
 * defaults file, or a mismatch between the file's shape rows and the chosen
 * Template's draw objects.
 */
public class SmartPicFactoryException extends RuntimeException {

    public SmartPicFactoryException(String message) {
        super(message);
    }

    public SmartPicFactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}

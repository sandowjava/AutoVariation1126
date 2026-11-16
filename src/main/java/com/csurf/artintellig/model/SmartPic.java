package com.csurf.artintellig.model;


import com.csurf.artintellig.domainanchorpattern.model.IQAnchorPattern;
import com.csurf.templatecore.model.Template;

/**
 * Immutable class representing the ideal state of a SmartPic,
 * containing both the Template and its associated AnchorPattern.
 * This class manages the relationship between geometric shapes,
 * their colors, and the color manipulation parameters.
 */
public final class SmartPic {
    private final Template template;
    private final IQAnchorPattern anchorPattern;

    /**
     * Creates a new SmartPic with the specified template and anchor pattern.
     * @param template The Template containing draw objects and their colors
     * @param anchorPattern The AnchorPattern containing dial angles for color manipulation
     * @throws IllegalArgumentException if either parameter is null
     */
    public SmartPic(Template template, IQAnchorPattern anchorPattern) {
        if (template == null || anchorPattern == null) {
            throw new IllegalArgumentException("Template and AnchorPattern must not be null");
        }
        this.template = template;
        this.anchorPattern = anchorPattern;
    }

    /**
     * Gets the Template instance containing draw objects and their colors.
     * @return the Template instance
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * Gets the AnchorPattern instance containing dial angles.
     * @return the AnchorPattern instance
     */
    public IQAnchorPattern getAnchorPattern() {
        return anchorPattern;
    }

    // TODO: Add methods for color manipulation based on anchor pattern
    // TODO: Add methods for accessing and managing the color collection/palette
}

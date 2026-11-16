package com.csurf.artintellig.model;

import com.csurf.artintellig.domainanchorpattern.model.IQAnchorPattern;
import com.csurf.templatecore.model.SmartpicPalette;
import com.csurf.templatecore.model.Template;

import java.util.Collection;

/**
 * Interface for components that need to respond to SmartPic state changes.
 * Implements Observer pattern for state change notifications.
 */
public interface ISmartPicStateListener {
    /**
     * Called when colors are changed in PipeOneColor objects.
     *
     * @param shapes Collection of shapes whose colors have changed
     */
    void onColorChanged(Collection<PipeOneColor> shapes);
    
    /**
     * Called when a new palette is generated.
     *
     * @param newSmartpicPalette The newly generated palette
     */
    void onPaletteGenerated(SmartpicPalette newSmartpicPalette);
    
    /**
     * Called when the artistic goal strategy changes.
     *
     * @param newGoal The new artistic goal
     */
    void onArtisticGoalChanged(ArtisticGoal newGoal);
    
    /**
     * Called when the anchor pattern changes.
     *
     * @param newPattern The new anchor pattern
     */
    void onAnchorPatternChanged(IQAnchorPattern newPattern);
    
    /**
     * Called when a new template is created.
     *
     * @param template The newly created template
     */
    default void onTemplateCreated(Template template) {
        // Default empty implementation
    }
}

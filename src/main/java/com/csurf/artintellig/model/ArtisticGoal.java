package com.csurf.artintellig.model;

import com.csurf.artintellig.domainanchorpattern.model.IQAnchorPattern;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

/**
 * Base class for artistic coloring goals. Implements the strategy interface
 * and provides common functionality for all artistic goal implementations.
 */
public abstract class ArtisticGoal implements ArtisticGoalStrategy {
    private final String name;
    private final String description;

    protected ArtisticGoal(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Gets the name of this artistic goal.
     *
     * @return The name of this artistic goal
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the description of this artistic goal.
     *
     * @return The description of this artistic goal
     */
    public String getDescription() {
        return description;
    }

    @Override
    public abstract ColorPtOstDot generateColor(IQOnePipe smarts, IQAnchorPattern anchorPattern);
}

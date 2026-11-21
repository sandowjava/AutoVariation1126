package com.csurf.artintellig.model;

import com.csurf.artintellig.domainanchorpattern.model.IQAnchorPattern;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.SmartpicPalette;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class responsible for generating new palettes based on RelationMatrix data
 * and artistic goals. Uses the Strategy pattern via ArtisticGoal implementations
 * to determine specific color generation algorithms.
 */
public class PaletteGenerator {
    private final RelationMatrixPrelim relationMatrixPrelim;
    private ArtisticGoalStrategy strategy;
    private IQAnchorPattern anchorPattern;

    /**
     * Creates a new PaletteGenerator with default settings.
     * Uses a default artistic goal and anchor pattern.
     */
    public PaletteGenerator() {
        this(null, null, null);
    }

    /**
     * Creates a new PaletteGenerator with just the RelationMatrix.
     * Uses default strategy and anchor pattern.
     *
     * @param relationMatrixPrelim The RelationMatrix containing shape relationships
     */
    public PaletteGenerator(RelationMatrixPrelim relationMatrixPrelim) {
        this(relationMatrixPrelim, null, null);
    }

    /**
     * Creates a new PaletteGenerator with the specified components.
     *
     * @param relationMatrixPrelim The RelationMatrix containing shape relationships
     * @param strategy The artistic goal strategy to use for color generation
     * @param anchorPattern The anchor pattern guiding color selection
     */
    public PaletteGenerator(RelationMatrixPrelim relationMatrixPrelim,
                            ArtisticGoalStrategy strategy,
                            IQAnchorPattern anchorPattern) {
        this.relationMatrixPrelim = relationMatrixPrelim;
        this.strategy = strategy; // Strategy must be provided, no default implementation
        this.anchorPattern = anchorPattern != null ? anchorPattern : new IQAnchorPattern();
    }




    /**
     * Sets the artistic goal strategy.
     * 
     * @param strategy The strategy to use for color generation
     * @throws IllegalArgumentException if strategy is null
     */
    public void setStrategy(ArtisticGoalStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }
        this.strategy = strategy;
    }

    /**
     * Sets the anchor pattern.
     *
     * @param pattern The anchor pattern to use
     */
    public void setAnchorPattern(IQAnchorPattern pattern) {
        this.anchorPattern = pattern != null ? pattern : new IQAnchorPattern();
    }

    /**
     * Gets the current artistic goal strategy.
     *
     * @return The current strategy
     */
    public ArtisticGoalStrategy getStrategy() {
        return strategy;
    }

    /**
     * Gets the current anchor pattern.
     *
     * @return The current anchor pattern
     */
    public IQAnchorPattern getAnchorPattern() {
        return anchorPattern;
    }

    /**
     * Gets the current RelationMatrix.
     *
     * @return The current RelationMatrix
     */
    public RelationMatrixPrelim getRelationMatrix() {
        return relationMatrixPrelim;
    }

    /**
     * Generates a new palette based on the current strategy and RelationMatrix data.
     *
     * @return A new Palette instance containing generated colors
     * @throws IllegalStateException if RelationMatrix is not set
     */
    public SmartpicPalette generatePalette() {
        if (relationMatrixPrelim == null) {
            throw new IllegalStateException("RelationMatrix must be set before generating palette");
        }

        IQAllPipes smarts = relationMatrixPrelim.getCollectiveSmarts();
        List<ColorPtOstDot> colors = new ArrayList<>();
        Map<String, ColorPtOstDot> colorMap = new HashMap<>();

        // Generate colors for each shape based on its IQOnePipe
        for (IQOnePipe objSmarts : smarts.getAllSmarts()) {
            ColorPtOstDot color = strategy.generateColor(objSmarts, anchorPattern);
            colors.add(color);
            colorMap.put(objSmarts.getId(), color);
        }

        // Create new Palette with generated colors and mappings
        SmartpicPalette smartpicPalette = new SmartpicPalette(colors, colorMap);
        return smartpicPalette;
    }
}

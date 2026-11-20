package com.csurf.variationmodule.model;

import com.csurf.templatecore.model.Template;

import java.util.Collections;
import java.util.List;

/**
 * The 12 Template instances produced by a variation process — the domain
 * result the user selects from for further work (e.g. opening one variation
 * in a TemplateWorkFrame).
 *
 * <p>Initially just an immutable holder for the Template list, in recipe
 * order: index 0 is the bootstrap/base variation, indices 1–11 the
 * processed runs. Will evolve to carry per-slot provenance (recipe row,
 * palette, frozen shapes); the slot index is the stable key for that
 * future data.
 *
 * <p>Domain object: no dependency on views or controllers. Both variation
 * flows populate it — the SmartPic cycle (distinct palettes per slot) and
 * the palette-variations V1 flow (identical loads per slot).
 */
public final class TemplateVariationsResult {

    private final List<Template> templates;

    /**
     * @param templates the variation Templates in slot order (typically 12)
     */
    public TemplateVariationsResult(List<Template> templates) {
        if (templates == null) {
            throw new IllegalArgumentException("templates must not be null");
        }
        this.templates = Collections.unmodifiableList(templates);
    }

    /** All variation Templates in slot order (unmodifiable). */
    public List<Template> templates() {
        return templates;
    }

    /** The Template at 0-based {@code slot} (0 = bootstrap/base). */
    public Template templateAt(int slot) {
        return templates.get(slot);
    }

    /** Number of variation slots. */
    public int numVariations() {
        return templates.size();
    }
} // end class TemplateVariationsResult

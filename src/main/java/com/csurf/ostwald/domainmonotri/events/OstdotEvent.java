package com.csurf.ostwald.domainmonotri.events;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.ostwald.domainmonotri.view.OstdotPanel;
import java.util.EventObject;

/**
 * Base class for events related to OstdotPanel interactions.
 */
public class OstdotEvent extends EventObject {
    private final OstdotPanel source;
    private final ColorPtOstDot color;
    private final OstdotEventType type;

    public OstdotEvent(OstdotPanel source, ColorPtOstDot color, OstdotEventType type) {
        super(source);
        this.source = source;
        this.color = color;
        this.type = type;
    }

    @Override
    public OstdotPanel getSource() {
        return source;
    }

    public ColorPtOstDot getColor() {
        return color;
    }

    public OstdotEventType getType() {
        return type;
    }

    public enum OstdotEventType {
        HOVER_ENTER,
        HOVER_EXIT,
        SELECTED,
        DESELECTED
    }
}

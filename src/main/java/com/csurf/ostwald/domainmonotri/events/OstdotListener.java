package com.csurf.ostwald.domainmonotri.events;

import java.util.EventListener;

/**
 * Listener interface for OstdotPanel events.
 */
public interface OstdotListener extends EventListener {
    /**
     * Called when an OstdotEvent occurs.
     * 
     * @param event The event that occurred
     */
    void ostdotEventOccurred(OstdotEvent event);
}

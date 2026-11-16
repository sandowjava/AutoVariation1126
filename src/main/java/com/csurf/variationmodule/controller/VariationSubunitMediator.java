package com.csurf.variationmodule.controller;

import com.csurf.core.ApplicationMediator;
import com.csurf.core.Message;
import com.csurf.core.SubunitMediator;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Module-level subunit mediator for the variationmodule package.
 * Thin shell for now: registered later with the ApplicationMediator
 * (e.g., appMediator.registerSubunit("variationmodule", ...)).
 *
 * Intended role: demultiplex inbound messages to the correct
 * VariationDisplayController instance by window ID, since the
 * ApplicationMediator maps one name to one subunit while multiple
 * VariationDisplayFrame windows may be open simultaneously.
 */
public class VariationSubunitMediator implements SubunitMediator {
    private final ApplicationMediator appMediator;
    // Registry of open variation windows: windowId -> controller (for future routing)
    private final Map<UUID, VariationDisplayController> controllers = new HashMap<>();

    public VariationSubunitMediator(ApplicationMediator appMediator) {
        this.appMediator = appMediator;
    }

    @Override
    public void initialize() {
        // No initialization needed yet
    }

    @Override
    public JComponent getVisualComponent() {
        return null;
    }

    @Override
    public Object handleMessage(Message message) {
        // TODO: route inbound messages (e.g., GENERATE_VARIATIONS, APPLY_PALETTE)
        // to the appropriate VariationDisplayController via controllers map.
        return null;
    }

    /**
     * Registers a VariationDisplayController under its window ID.
     * Intended for future multi-window message routing.
     */
    public void registerController(UUID windowId, VariationDisplayController controller) {
        if (windowId != null && controller != null) {
            controllers.put(windowId, controller);
        }
    }

    /**
     * Removes a controller when its VariationDisplayFrame is disposed.
     */
    public void unregisterController(UUID windowId) {
        controllers.remove(windowId);
    }

    public ApplicationMediator getAppMediator() {
        return appMediator;
    }
} // end class VariationSubunitMediator

package com.csurf.templatecore.controller;

import com.csurf.app.ApplicFrame;
import com.csurf.artintellig.model.*;
import com.csurf.artintellig.view.AnchorPatternView;
import com.csurf.core.Message;
import com.csurf.core.SubunitMediator;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Mediator class that coordinates Template core operations. Refactored to support
 * the GUI cluster architecture where each Template instance manages its own
 * display components.
 */
public class TemplateSubunitMediator implements SubunitMediator {
    private final List<ISmartPicStateListener> listeners = new ArrayList<>();
    private ApplicFrame mainFrame;




    /**
     * Creates a new SmartPicSubunitMediator with minimal initialization.
     */
    public TemplateSubunitMediator(ApplicFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public void initialize() {
    }

    @Override
    public JComponent getVisualComponent() {
        return null;
    }




    @Override
    public Object handleMessage(Message message) {
        String type = message.getType();

        if (type.equals("SHOW_ANCHOR_PATTERN")) {
            showAnchorPattern();
        }

        return null;
    } // end method handleMessage




    // TODO 110325 this is OLD stuff - no clear pattern here, why only this one flow? - concept unclear
    private void showAnchorPattern() {
        // Create view with a consumer to handle pattern updates
        AnchorPatternView view = new AnchorPatternView(pattern -> {
            // Notify listeners when pattern is updated
            for (ISmartPicStateListener listener : listeners) {
                listener.onAnchorPatternChanged(pattern);
            }
        });

        // Register with ApplicFrame
        mainFrame.registerFrame(view);
        view.setVisible(true);
    } // end method showAnchorPattern




    /**
     * Adds a listener for SmartPic state changes.
     * @param listener The listener to add
     */
    public void addStateListener(ISmartPicStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    } // end method addStateListener



    /**
     * Removes a state change listener.
     * @param listener The listener to remove
     */
    public void removeStateListener(ISmartPicStateListener listener) {
        listeners.remove(listener);
    } // end method removeStateListener



} // end class TemplateSubunitMediator

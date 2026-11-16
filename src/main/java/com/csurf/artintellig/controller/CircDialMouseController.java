package com.csurf.artintellig.controller;

import com.csurf.artintellig.model.CircDialTryoutMouser;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Controller class that handles mouse interactions with the CircDialTryoutMouser.
 * Processes clicks and drags to find the closest hit markers and sectors.
 */
public class CircDialMouseController extends MouseAdapter {
    private final CircDialTryoutMouser dialComponent;
    
    public CircDialMouseController(CircDialTryoutMouser dialComponent) {
        this.dialComponent = dialComponent;
        System.out.println("CircDialMouseController created and attached to component");
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("MouseClick detected at: " + e.getPoint());
        Point clickPoint = e.getPoint();
        dialComponent.handleMouseClick(clickPoint);
    } // end method mouseClicked


    
    @Override
    public void mousePressed(MouseEvent e) {
        System.out.println("MousePressed detected at: " + e.getPoint());
        Point clickPoint = e.getPoint();
        dialComponent.handleMouseClick(clickPoint);
    } // end method mousePressed



} // end class

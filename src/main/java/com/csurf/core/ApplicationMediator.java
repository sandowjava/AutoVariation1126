package com.csurf.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Top-level mediator that coordinates communication between subunits.
 */
public class ApplicationMediator {
    private final Map<String, SubunitMediator> subunits;
    
    public ApplicationMediator() {
        this.subunits = new HashMap<>();
    }
    
    public void registerSubunit(String name, SubunitMediator subunit) {
        subunits.put(name, subunit);
        subunit.initialize();
    }
    
    public Object sendMessage(String targetSubunit, Message message) {
        SubunitMediator target = subunits.get(targetSubunit);
        if (target != null) {
            return target.handleMessage(message);
        }
        return null;
    }
    
    public SubunitMediator getSubunit(String name) {
        return subunits.get(name);
    }
}

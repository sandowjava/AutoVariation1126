package com.csurf.core;

/**
 * Represents a message passed between mediators in the HMVC architecture.
 */
public class Message {
    private final String type;
    private final Object payload;
    private final SubunitMediator source;
    
    public Message(String type, Object payload, SubunitMediator source) {
        this.type = type;
        this.payload = payload;
        this.source = source;
    }
    
    public String getType() {
        return type;
    }
    
    public Object getPayload() {
        return payload;
    }
    
    public SubunitMediator getSource() {
        return source;
    }
}

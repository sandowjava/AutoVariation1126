package com.csurf.templatecore.storage.serialization;

import com.csurf.ostwald.domainmonotri.model.ColumnStepWhiteBlackParams;
import com.csurf.ostwald.domainmonotri.model.OstHueColumnStep;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class OstHueColumnStepDeserializer extends JsonDeserializer<OstHueColumnStep> {
    
    @Override
    public OstHueColumnStep deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        // Get the hue number from stdHueNum
        JsonNode stdHueNumNode = node.get("stdHueNum");
        if (stdHueNumNode == null) {
            throw new IOException("Missing required field 'stdHueNum' in OstHueColumnStep JSON");
        }
        
        // Get the nested ostPosition object
        JsonNode ostPositionNode = node.get("ostPosition");
        if (ostPositionNode == null) {
            // If ostPosition is not present, try to get values from root level
            // (for backward compatibility)
            JsonNode hueNumNode = node.get("hueNum");
            JsonNode columnNode = node.get("column");
            JsonNode stepNode = node.get("step");
            JsonNode fillOrderNumNode = node.get("fillOrderNum");
            JsonNode seriesWNode = node.get("seriesW");
            JsonNode seriesBNode = node.get("seriesB");
            JsonNode drawOrderNumNode = node.get("drawOrderNum");
            
            // Debug print each value
            System.out.println("DEBUG: hueNum = " + (hueNumNode != null ? hueNumNode.asInt() : "null"));
            System.out.println("DEBUG: column = " + (columnNode != null ? columnNode.asInt() : "null"));
            System.out.println("DEBUG: step = " + (stepNode != null ? stepNode.asInt() : "null"));
            System.out.println("DEBUG: fillOrderNum = " + (fillOrderNumNode != null ? fillOrderNumNode.asInt() : "null"));
            System.out.println("DEBUG: seriesW = " + (seriesWNode != null ? seriesWNode.asInt() : "null"));
            System.out.println("DEBUG: seriesB = " + (seriesBNode != null ? seriesBNode.asInt() : "null"));
            System.out.println("DEBUG: drawOrderNum = " + (drawOrderNumNode != null ? drawOrderNumNode.asInt() : "null"));
            
            // Check if any required field is missing
            if (hueNumNode == null || columnNode == null || stepNode == null || 
                fillOrderNumNode == null || seriesWNode == null || seriesBNode == null || drawOrderNumNode == null) {
                throw new IOException("Missing required fields in OstHueColumnStep JSON: " + node.toString());
            }
            
            // Create parameter bundle from the deserialized values
            ColumnStepWhiteBlackParams paramBundle = 
                new ColumnStepWhiteBlackParams(
                    columnNode.asInt(),
                    stepNode.asInt(),
                    seriesWNode.asInt(),
                    seriesBNode.asInt(),
                    drawOrderNumNode.asInt()
                );
            
            // Create new OstHueColumnStep instance
            return new OstHueColumnStep(hueNumNode.asInt(), paramBundle, fillOrderNumNode.asInt());
        }
        
        // Extract values from ostPosition
        int columnNum = ostPositionNode.get("columnNum").asInt();
        int stepNum = ostPositionNode.get("stepNum").asInt();
        int blackSeriesNum = ostPositionNode.get("blackSeriesNum").asInt();
        int whiteSeriesNum = ostPositionNode.get("whiteSeriesNum").asInt();
        int drawOrderNum = ostPositionNode.get("drawOrderNum").asInt();
        
        // Extract positionNum from ostPosition - this IS the fillOrderNum needed by constructor
        int positionNum = ostPositionNode.get("positionNum").asInt();
        
        // Create parameter bundle from the position values
        // Note: Constructor expects (column, step, whiteSeries, blackSeries, drawOrderNum)
        ColumnStepWhiteBlackParams paramBundle = 
            new ColumnStepWhiteBlackParams(
                columnNum,
                stepNum,
                whiteSeriesNum,
                blackSeriesNum,
                drawOrderNum
            );
        
        // Create new OstHueColumnStep instance using the standard hue number and position info
        // Pass positionNum (which IS the fillOrderNum) to constructor, not drawOrderNum
        return new OstHueColumnStep(stdHueNumNode.asInt(), paramBundle, positionNum);
    }
}

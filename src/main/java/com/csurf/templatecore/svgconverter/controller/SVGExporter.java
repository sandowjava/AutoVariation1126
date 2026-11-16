package com.csurf.templatecore.svgconverter.controller;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;

import java.awt.*;
import java.awt.geom.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * SVGExporter is responsible for exporting Template instances to SVG files.
 * It preserves original SVG data when available for better round-trip conversion.
 */
public class SVGExporter {

    /**
     * Exports a Template to an SVG file.
     * 
     * @param template the Template to export
     * @param file the file to write the SVG to
     * @throws IOException if an I/O error occurs
     */
    public static void exportToSVG(Template template, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            exportToSVG(template, writer);
        }
    }

    /**
     * Exports a Template to an SVG document using the provided Writer.
     * 
     * @param template the Template to export
     * @param writer the Writer to write the SVG to
     * @throws IOException if an I/O error occurs
     */
    public static void exportToSVG(Template template, Writer writer) throws IOException {
        // Start SVG document
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        writer.write("<svg xmlns=\"http://www.w3.org/2000/svg\"");
        
        // TEMPORARY HACK: Hard-coded ViewBox of 800x800 for testing
        // This bypasses both the stored ViewBox and calculated bounds
        writer.write(" viewBox=\"0 0 800 800\"");
        
        /* Original ViewBox code - commented out for testing
        // Add viewBox if available
        String viewBox = template.getSvgViewBox();
        if (viewBox != null && !viewBox.isEmpty()) {
            writer.write(" viewBox=\"" + viewBox + "\"");
        } else {
            // Calculate viewBox from DrawObj bounds if not available
            Rectangle2D bounds = calculateBounds(template.getAllDrawObjs());
            writer.write(String.format(" viewBox=\"%.2f %.2f %.2f %.2f\"", 
                bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()));
        }
        */
        
        // Add width and height if available
        double width = template.getSvgWidth();
        double height = template.getSvgHeight();
        
        // If width/height not specified in template, derive from viewBox or bounds
        if (width <= 0) {
            width = 800; // Match hard-coded ViewBox width
        }
        if (height <= 0) {
            height = 800; // Match hard-coded ViewBox height
        }
        
        // Always write width and height attributes
        writer.write(String.format(" width=\"%.2f\"", width));
        writer.write(String.format(" height=\"%.2f\"", height));
        
        // Add other document attributes
        for (Map.Entry<String, String> entry : template.getSvgDocumentAttributes().entrySet()) {
            writer.write(" " + entry.getKey() + "=\"" + entry.getValue() + "\"");
        }
        
        writer.write(">\n");
        
        // Write each DrawObj as an SVG element
        for (DrawObj drawObj : template.getAllDrawObjs()) {
            writeDrawObj(writer, drawObj);
        }
        
        // End SVG document
        writer.write("</svg>\n");
    }
    
    /**
     * Writes a DrawObj as an SVG element.
     * 
     * @param writer the Writer to write to
     * @param drawObj the DrawObj to write
     * @throws IOException if an I/O error occurs
     */
    private static void writeDrawObj(Writer writer, DrawObj drawObj) throws IOException {
        String elementType = drawObj.getSvgElementType();
        if (elementType == null) {
            elementType = "path"; // Default to path if not specified
        }
        
        writer.write("  <" + elementType);
        
        // Add id attribute if name is available
        String name = drawObj.getName();
        if (name != null && !name.isEmpty()) {
            writer.write(" id=\"" + name + "\"");
        }
        
        // Add fill color
        ColorPtOstDot fillColor = drawObj.getColorPtOstDotFillColor();
        if (fillColor != null) {
            Color awtColor = fillColor.toAWTColor();
            String hexColor = String.format("#%02x%02x%02x", 
                awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
            writer.write(" fill=\"" + hexColor + "\"");
        } else {
            // If ColorPtOstDot is null, try to use the AWT Color directly
            Color strokeColor = drawObj.getStrokeColor();
            if (strokeColor != null) {
                String hexColor = String.format("#%02x%02x%02x", 
                    strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue());
                writer.write(" fill=\"" + hexColor + "\"");
            }
        }
        
        // Add other attributes from original SVG if available
        for (Map.Entry<String, String> entry : drawObj.getSvgAttributes().entrySet()) {
            String key = entry.getKey();
            // Skip attributes we've already handled or will handle separately
            if (!key.equals("id") && !key.equals("fill") && !key.equals("d") && 
                !key.equals("points") && !key.equals("cx") && !key.equals("cy") && 
                !key.equals("r") && !key.equals("rx") && !key.equals("ry") && 
                !key.equals("x") && !key.equals("y") && !key.equals("width") && 
                !key.equals("height") && !key.equals("x1") && !key.equals("y1") && 
                !key.equals("x2") && !key.equals("y2")) {
                writer.write(" " + key + "=\"" + entry.getValue() + "\"");
            }
        }
        
        // Add element-specific attributes
        switch (elementType) {
            case "path":
                // Use original path data if available, otherwise generate from GeneralPath
                String pathData = drawObj.getSvgOriginalPathData();
                if (pathData == null || pathData.isEmpty()) {
                    pathData = convertPathToSVGPathData(drawObj.getPath());
                }
                writer.write(" d=\"" + pathData + "\"");
                break;
                
            case "circle":
                Rectangle2D bounds = drawObj.getBounds();
                double cx = bounds.getCenterX();
                double cy = bounds.getCenterY();
                double r = Math.min(bounds.getWidth(), bounds.getHeight()) / 2;
                writer.write(String.format(" cx=\"%.2f\" cy=\"%.2f\" r=\"%.2f\"", cx, cy, r));
                break;
                
            case "ellipse":
                bounds = drawObj.getBounds();
                cx = bounds.getCenterX();
                cy = bounds.getCenterY();
                double rx = bounds.getWidth() / 2;
                double ry = bounds.getHeight() / 2;
                writer.write(String.format(" cx=\"%.2f\" cy=\"%.2f\" rx=\"%.2f\" ry=\"%.2f\"", cx, cy, rx, ry));
                break;
                
            case "rect":
                bounds = drawObj.getBounds();
                writer.write(String.format(" x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\"", 
                    bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()));
                break;
                
            case "line":
                // For line, we need to extract the endpoints from the path
                Point2D[] points = extractLinePoints(drawObj.getPath());
                writer.write(String.format(" x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\"", 
                    points[0].getX(), points[0].getY(), points[1].getX(), points[1].getY()));
                break;
                
            case "polygon":
            case "polyline":
                String pointsStr = convertPathToPointsString(drawObj.getPath());
                writer.write(" points=\"" + pointsStr + "\"");
                break;
        }
        
        writer.write(" />\n");
    }
    
    /**
     * Calculates the bounding box of a list of DrawObjs.
     * 
     * @param drawObjs the list of DrawObjs
     * @return the bounding box
     */
    private static Rectangle2D calculateBounds(List<DrawObj> drawObjs) {
        if (drawObjs == null || drawObjs.isEmpty()) {
            return new Rectangle2D.Double(0, 0, 800, 600); // More reasonable default size
        }
        
        Rectangle2D bounds = null;
        for (DrawObj drawObj : drawObjs) {
            Rectangle2D objBounds = drawObj.getBounds();
            if (bounds == null) {
                bounds = objBounds;
            } else {
                bounds = bounds.createUnion(objBounds);
            }
        }
        
        // Add padding (10% on each side) to ensure all elements are visible
        if (bounds != null) {
            double paddingX = Math.max(bounds.getWidth() * 0.1, 10);
            double paddingY = Math.max(bounds.getHeight() * 0.1, 10);
            bounds = new Rectangle2D.Double(
                bounds.getX() - paddingX,
                bounds.getY() - paddingY,
                bounds.getWidth() + (paddingX * 2),
                bounds.getHeight() + (paddingY * 2)
            );
        }
        
        return bounds != null ? bounds : new Rectangle2D.Double(0, 0, 800, 600);
    }
    
    /**
     * Converts a GeneralPath to SVG path data.
     * 
     * @param path the GeneralPath to convert
     * @return the SVG path data
     */
    private static String convertPathToSVGPathData(GeneralPath path) {
        StringBuilder sb = new StringBuilder();
        PathIterator pi = path.getPathIterator(new AffineTransform());
        float[] coords = new float[6];
        
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    sb.append(String.format("M%.2f,%.2f ", coords[0], coords[1]));
                    break;
                case PathIterator.SEG_LINETO:
                    sb.append(String.format("L%.2f,%.2f ", coords[0], coords[1]));
                    break;
                case PathIterator.SEG_QUADTO:
                    sb.append(String.format("Q%.2f,%.2f %.2f,%.2f ", 
                        coords[0], coords[1], coords[2], coords[3]));
                    break;
                case PathIterator.SEG_CUBICTO:
                    sb.append(String.format("C%.2f,%.2f %.2f,%.2f %.2f,%.2f ", 
                        coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]));
                    break;
                case PathIterator.SEG_CLOSE:
                    sb.append("Z ");
                    break;
            }
            pi.next();
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Extracts the endpoints of a line from a GeneralPath.
     * 
     * @param path the GeneralPath representing a line
     * @return an array of two points representing the line endpoints
     */
    private static Point2D[] extractLinePoints(GeneralPath path) {
        Point2D[] points = new Point2D[2];
        PathIterator pi = path.getPathIterator(new AffineTransform());
        float[] coords = new float[6];
        
        int index = 0;
        while (!pi.isDone() && index < 2) {
            int type = pi.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                points[index++] = new Point2D.Float(coords[0], coords[1]);
            }
            pi.next();
        }
        
        // If we didn't find two points, use the bounding box
        if (index < 2) {
            Rectangle2D bounds = path.getBounds2D();
            points[0] = new Point2D.Double(bounds.getX(), bounds.getY());
            points[1] = new Point2D.Double(bounds.getX() + bounds.getWidth(), 
                                         bounds.getY() + bounds.getHeight());
        }
        
        return points;
    }
    
    /**
     * Converts a GeneralPath to an SVG points string for polygon/polyline.
     * 
     * @param path the GeneralPath to convert
     * @return the SVG points string
     */
    private static String convertPathToPointsString(GeneralPath path) {
        StringBuilder sb = new StringBuilder();
        PathIterator pi = path.getPathIterator(new AffineTransform());
        float[] coords = new float[6];
        
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                sb.append(String.format("%.2f,%.2f ", coords[0], coords[1]));
            }
            pi.next();
        }
        
        return sb.toString().trim();
    }
}

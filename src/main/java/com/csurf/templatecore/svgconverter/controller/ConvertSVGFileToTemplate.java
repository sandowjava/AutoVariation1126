package com.csurf.templatecore.svgconverter.controller;

import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConvertSVGFileToTemplate { // 062425 renamed class, was "ConvertSVGPathToDrawObj". Does more than convert individual SVG paths
    private static Template lastTemplateCreated;




    /**
     * Gets the last Template that was created by this converter.
     * @return the last Template created, or null if none created yet
     */
    public static Template getLastTemplateCreated() {
        return lastTemplateCreated;
    }




    /**
     * Creates a Template from an array of DrawObjs. // TODO 062425 why is static method createTemplate in this class? Examine method that calls it.
     * 062425 kicked off by SVGConverterFrame browse menuItem - goes through ApplicationMediator, then SVGConverterSubunitMediator
     * @param drawObjs array of DrawObjs to create Template from
     * @return the created Template
     */
    public static Template createTemplate(DrawObj[] drawObjs) {
        Template template = new Template(drawObjs);
        lastTemplateCreated = template;
        return template;
    } // end method createTemplate




    public static DrawObj[] convertFile(File file) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            Element root = document.getDocumentElement();
            
            // Create Template instance to store metadata
            Template template = new Template();
            template.setOriginalSvgFilePath(file.getAbsolutePath());
            
            // Get viewBox if present
            String viewBox = root.getAttribute("viewBox");
            if (!viewBox.isEmpty()) {
                System.out.println("ViewBox: " + viewBox);
                template.setSvgViewBox(viewBox);
            }
            
            // Get width and height
            String widthStr = root.getAttribute("width");
            String heightStr = root.getAttribute("height");
            double width = parseSVGDimension(widthStr);
            double height = parseSVGDimension(heightStr);
            template.setSvgWidth(width);
            template.setSvgHeight(height);
            
            // Store other document attributes
            for (int i = 0; i < root.getAttributes().getLength(); i++) {
                String name = root.getAttributes().item(i).getNodeName();
                String value = root.getAttributes().item(i).getNodeValue();
                if (!name.equals("width") && !name.equals("height") && !name.equals("viewBox")) {
                    template.setSvgDocumentAttribute(name, value);
                }
            }
            
            List<DrawObj> drawObjs = new ArrayList<>();
            processElement(root, drawObjs);
            
            // Create Template from DrawObj array
            Template createdTemplate = createTemplate(drawObjs.toArray(new DrawObj[0]));
            
            // Copy metadata from our temporary template to the created one
            createdTemplate.setSvgViewBox(template.getSvgViewBox());
            createdTemplate.setSvgWidth(template.getSvgWidth());
            createdTemplate.setSvgHeight(template.getSvgHeight());
            createdTemplate.setOriginalSvgFilePath(template.getOriginalSvgFilePath());
            for (java.util.Map.Entry<String, String> entry : template.getSvgDocumentAttributes().entrySet()) {
                createdTemplate.setSvgDocumentAttribute(entry.getKey(), entry.getValue());
            }
            
            return drawObjs.toArray(new DrawObj[0]);
            
        } catch (ParserConfigurationException e) {
            throw new IOException("Error configuring XML parser: " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new IOException("Error parsing SVG file: " + e.getMessage(), e);
        }
    } // end method convertFile




    public static Rectangle2D getSVGBounds(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);
        
        Element svg = document.getDocumentElement();
        String widthStr = svg.getAttribute("width");
        String heightStr = svg.getAttribute("height");
        
        // Try to parse width and height
        double width = parseSVGDimension(widthStr);
        double height = parseSVGDimension(heightStr);
        
        // If width/height not specified, try viewBox
        if (width == 0 || height == 0) {
            String viewBox = svg.getAttribute("viewBox");
            if (viewBox != null && !viewBox.isEmpty()) {
                String[] parts = viewBox.trim().split("\\s+");
                if (parts.length >= 4) {
                    width = Double.parseDouble(parts[2]);
                    height = Double.parseDouble(parts[3]);
                }
            }
        }
        
        return new Rectangle2D.Double(0, 0, width, height);
    } // end method getSVGBounds



    private static double parseSVGDimension(String dim) {
        if (dim == null || dim.isEmpty()) return 0;
        
        // Remove any units (px, pt, etc) and parse
        dim = dim.replaceAll("[^0-9.]", "");
        try {
            return Double.parseDouble(dim);
        } catch (NumberFormatException e) {
            return 0;
        }
    } // end method parseSVGDimension


    /**
     * Processes an SVG element and converts it into a DrawObj instance. This method:
     * - Identifies the element using id, class, or generated name
     * - Processes child elements first to maintain stacking order
     * - Extracts and parses SVG path data
     * - Processes style attributes for fill colors
     * - Creates a DrawObj with the parsed path and styling
     * - Adds the created DrawObj to the provided list
     *
     * @param element the SVG element to process
     * @param drawObjs the list to which the created DrawObj will be added
     */
    private static void processElement(Element element, List<DrawObj> drawObjs) {
        String elementType = element.getTagName();
        String name = element.getAttribute("id");
        if (name.isEmpty()) {
            name = element.getAttribute("class");
        }
        if (name.isEmpty()) {
            name = elementType + drawObjs.size();
        }
        System.out.println("\nProcessing element: " + elementType + " (" + name + ")");
        
        // Process any child elements first (to maintain correct stacking order)
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                processElement((Element) children.item(i), drawObjs);
            }
        } // end FOR
        
        // Get style attribute which may contain fill color
        String style = element.getAttribute("style");
        String fillColor = element.getAttribute("fill");
        
        // Parse style attribute if present
        if (!style.isEmpty()) {
            System.out.println("  Found style attribute: " + style);
            String[] styles = style.split(";");
            for (String s : styles) {
                String[] parts = s.trim().split(":");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if ("fill".equals(key)) {
                        fillColor = value;
                        System.out.println("  Found fill in style: " + fillColor);
                    }
                }
            }
        } // end IF
        
        // Handle group element
        if ("g".equals(elementType)) {
            return; // Skip groups, we'll process their children directly
        }

        // Handle shape elements
        GeneralPath path = null;
        String originalPathData = null;
        try {
            switch (elementType) {
                case "path":
                    String pathData = element.getAttribute("d");
                    originalPathData = pathData;
                    if (pathData != null && !pathData.isEmpty()) {
                        path = parseSVGPath(pathData);
                    }
                    break;
                    
                case "circle":
                    double cx = parseDouble(element.getAttribute("cx"));
                    double cy = parseDouble(element.getAttribute("cy"));
                    double r = parseDouble(element.getAttribute("r"));
                    path = createCirclePath(cx, cy, r);
                    break;
                    
                case "ellipse":
                    cx = parseDouble(element.getAttribute("cx"));
                    cy = parseDouble(element.getAttribute("cy"));
                    double rx = parseDouble(element.getAttribute("rx"));
                    double ry = parseDouble(element.getAttribute("ry"));
                    path = createEllipsePath(cx, cy, rx, ry);
                    break;
                    
                case "rect":
                    double x = parseDouble(element.getAttribute("x"));
                    double y = parseDouble(element.getAttribute("y"));
                    double width = parseDouble(element.getAttribute("width"));
                    double height = parseDouble(element.getAttribute("height"));
                    path = createRectPath(x, y, width, height);
                    break;
                    
                case "line":
                    double x1 = parseDouble(element.getAttribute("x1"));
                    double y1 = parseDouble(element.getAttribute("y1"));
                    double x2 = parseDouble(element.getAttribute("x2"));
                    double y2 = parseDouble(element.getAttribute("y2"));
                    path = createLinePath(x1, y1, x2, y2);
                    break;
                    
                case "polygon":
                case "polyline":
                    String points = element.getAttribute("points");
                    path = createPolyPath(points, "polygon".equals(elementType));
                    break;
            }
            
            if (path != null) {
                Color fill = parseColor(fillColor);
                if (fill == null) {
                    fill = Color.BLACK; // Default fill color
                }
                
                System.out.println("Creating DrawObj:");
                System.out.println("  Name: " + name);
                System.out.println("  Fill color: " + fill);
                
                DrawObj drawObj = new DrawObj(name, path, fill);
                
                // Store original SVG element information
                drawObj.setSvgElementType(elementType);
                if (originalPathData != null) {
                    drawObj.setSvgOriginalPathData(originalPathData);
                }
                
                // Store all attributes
                for (int i = 0; i < element.getAttributes().getLength(); i++) {
                    String attrName = element.getAttributes().item(i).getNodeName();
                    String attrValue = element.getAttributes().item(i).getNodeValue();
                    drawObj.setSvgAttribute(attrName, attrValue);
                }
                
                // Store style as a separate attribute if present
                if (!style.isEmpty()) {
                    drawObj.setSvgAttribute("style", style);
                }
                
                drawObjs.add(drawObj);
            }
            
        } catch (Exception e) {
            System.err.println("Error processing element: " + elementType);
            e.printStackTrace();
        } // end TRY-CATCH

    } // end method processElement




    private static Color parseColor(String colorStr) {
        System.out.println("Parsing color string: '" + colorStr + "'");
        
        if (colorStr == null || colorStr.isEmpty() || 
            "none".equals(colorStr) || "transparent".equals(colorStr)) {
            return null;
        }
        
        // Handle named colors
        String lowerColor = colorStr.toLowerCase();
        switch (lowerColor) {
            case "black": return Color.BLACK;
            case "white": return Color.WHITE;
            case "red": return Color.RED;
            case "green": return Color.GREEN;
            case "blue": return Color.BLUE;
            case "yellow": return Color.YELLOW;
            case "gray": return Color.GRAY;
            case "purple": return Color.MAGENTA;
            case "cyan": return Color.CYAN;
            case "currentcolor": return Color.BLACK;
        }
        
        try {
            // Handle hex colors with # prefix
            if (colorStr.startsWith("#")) {
                String hex = colorStr.substring(1);
                // Handle short form hex (#RGB)
                if (hex.length() == 3) {
                    hex = String.format("%c%c%c%c%c%c", 
                                      hex.charAt(0), hex.charAt(0),
                                      hex.charAt(1), hex.charAt(1),
                                      hex.charAt(2), hex.charAt(2));
                }
                return Color.decode("#" + hex);
            }
            // Handle rgb() format
            else if (colorStr.startsWith("rgb")) {
                String[] rgb = colorStr.replaceAll("[^0-9,]", "").split(",");
                if (rgb.length == 3) {
                    return new Color(
                        Integer.parseInt(rgb[0].trim()),
                        Integer.parseInt(rgb[1].trim()),
                        Integer.parseInt(rgb[2].trim())
                    );
                }
            }
            // Try to parse as hex without # prefix
            return Color.decode("#" + colorStr);
            
        } catch (NumberFormatException e) {
            System.out.println("  -> Error parsing color: " + e.getMessage());
            return null;
        }
    } // end method parseColor




    private static double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    } // end method parseDouble


    private static GeneralPath createCirclePath(double cx, double cy, double r) {
        GeneralPath path = new GeneralPath();
        path.append(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2), false);
        return path;
    } // end method createCirclePath


    private static GeneralPath createEllipsePath(double cx, double cy, double rx, double ry) {
        GeneralPath path = new GeneralPath();
        path.append(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2), false);
        return path;
    } // end method createEllipsePath


    private static GeneralPath createRectPath(double x, double y, double width, double height) {
        GeneralPath path = new GeneralPath();
        path.append(new Rectangle2D.Double(x, y, width, height), false);
        return path;
    } // end method createRectPath


    private static GeneralPath createLinePath(double x1, double y1, double x2, double y2) {
        GeneralPath path = new GeneralPath();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        return path;
    } // end method createLinePath


    private static GeneralPath createPolyPath(String points, boolean closePath) {
        GeneralPath path = new GeneralPath();
        String[] coords = points.trim().split("\\s+");
        boolean first = true;
        
        for (String coord : coords) {
            String[] xy = coord.split(",");
            if (xy.length == 2) {
                double x = Double.parseDouble(xy[0].trim());
                double y = Double.parseDouble(xy[1].trim());
                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }
        }
        
        if (closePath) {
            path.closePath();
        }
        return path;
    } // end method createPolyPath




    private static GeneralPath parseSVGPath(String pathData) {
        GeneralPath path = new GeneralPath();
        String[] commands = pathData.split("(?=[MmLlHhVvCcSsQqTtAaZz])");
        double lastX = 0, lastY = 0;
        double lastControlX = 0, lastControlY = 0;  // For smooth curves
        char lastCommand = ' ';
        
        for (String cmd : commands) {
            if (cmd.isEmpty()) continue;
            
            char command = cmd.charAt(0);
            String coords = cmd.substring(1).trim();
            String[] parts = coords.split("[\\s,]+");
            int idx = 0;
            
            switch (command) {
                case 'M':
                case 'm':
                    while (idx < parts.length - 1) {
                        double x = parseDouble(parts[idx]);
                        double y = parseDouble(parts[idx + 1]);
                        if (command == 'm') {
                            x += lastX;
                            y += lastY;
                        }
                        if (idx == 0) {
                            path.moveTo(x, y);
                        } else {
                            path.lineTo(x, y);
                        }
                        lastX = x;
                        lastY = y;
                        idx += 2;
                    }
                    break;
                    
                case 'L':
                case 'l':
                    while (idx < parts.length - 1) {
                        double x = parseDouble(parts[idx]);
                        double y = parseDouble(parts[idx + 1]);
                        if (command == 'l') {
                            x += lastX;
                            y += lastY;
                        }
                        path.lineTo(x, y);
                        lastX = x;
                        lastY = y;
                        idx += 2;
                    }
                    break;
                    
                case 'H':
                case 'h':
                    while (idx < parts.length) {
                        double x = parseDouble(parts[idx]);
                        if (command == 'h') {
                            x += lastX;
                        }
                        path.lineTo(x, lastY);
                        lastX = x;
                        idx++;
                    }
                    break;
                    
                case 'V':
                case 'v':
                    while (idx < parts.length) {
                        double y = parseDouble(parts[idx]);
                        if (command == 'v') {
                            y += lastY;
                        }
                        path.lineTo(lastX, y);
                        lastY = y;
                        idx++;
                    }
                    break;

                case 'C':
                case 'c':
                    while (idx < parts.length - 5) {
                        double x1 = parseDouble(parts[idx]);
                        double y1 = parseDouble(parts[idx + 1]);
                        double x2 = parseDouble(parts[idx + 2]);
                        double y2 = parseDouble(parts[idx + 3]);
                        double x = parseDouble(parts[idx + 4]);
                        double y = parseDouble(parts[idx + 5]);
                        
                        if (command == 'c') {
                            x1 += lastX;
                            y1 += lastY;
                            x2 += lastX;
                            y2 += lastY;
                            x += lastX;
                            y += lastY;
                        }
                        
                        path.curveTo(x1, y1, x2, y2, x, y);
                        lastControlX = x2;
                        lastControlY = y2;
                        lastX = x;
                        lastY = y;
                        idx += 6;
                    }
                    break;

                case 'S':
                case 's':
                    while (idx < parts.length - 3) {
                        // Reflect previous control point
                        double x1 = lastX + (lastX - lastControlX);
                        double y1 = lastY + (lastY - lastControlY);
                        
                        if (lastCommand != 'C' && lastCommand != 'c' && 
                            lastCommand != 'S' && lastCommand != 's') {
                            x1 = lastX;
                            y1 = lastY;
                        }
                        
                        double x2 = parseDouble(parts[idx]);
                        double y2 = parseDouble(parts[idx + 1]);
                        double x = parseDouble(parts[idx + 2]);
                        double y = parseDouble(parts[idx + 3]);
                        
                        if (command == 's') {
                            x2 += lastX;
                            y2 += lastY;
                            x += lastX;
                            y += lastY;
                        }
                        
                        path.curveTo(x1, y1, x2, y2, x, y);
                        lastControlX = x2;
                        lastControlY = y2;
                        lastX = x;
                        lastY = y;
                        idx += 4;
                    }
                    break;

                case 'Q':
                case 'q':
                    while (idx < parts.length - 3) {
                        double x1 = parseDouble(parts[idx]);
                        double y1 = parseDouble(parts[idx + 1]);
                        double x = parseDouble(parts[idx + 2]);
                        double y = parseDouble(parts[idx + 3]);
                        
                        if (command == 'q') {
                            x1 += lastX;
                            y1 += lastY;
                            x += lastX;
                            y += lastY;
                        }
                        
                        // Convert quadratic to cubic Bezier
                        double cx1 = lastX + 2.0/3.0 * (x1 - lastX);
                        double cy1 = lastY + 2.0/3.0 * (y1 - lastY);
                        double cx2 = x + 2.0/3.0 * (x1 - x);
                        double cy2 = y + 2.0/3.0 * (y1 - y);
                        
                        path.curveTo(cx1, cy1, cx2, cy2, x, y);
                        lastControlX = x1;
                        lastControlY = y1;
                        lastX = x;
                        lastY = y;
                        idx += 4;
                    }
                    break;

                case 'T':
                case 't':
                    while (idx < parts.length - 1) {
                        // Reflect previous control point
                        double x1 = lastX + (lastX - lastControlX);
                        double y1 = lastY + (lastY - lastControlY);
                        
                        if (lastCommand != 'Q' && lastCommand != 'q' && 
                            lastCommand != 'T' && lastCommand != 't') {
                            x1 = lastX;
                            y1 = lastY;
                        }
                        
                        double x = parseDouble(parts[idx]);
                        double y = parseDouble(parts[idx + 1]);
                        
                        if (command == 't') {
                            x += lastX;
                            y += lastY;
                        }
                        
                        // Convert quadratic to cubic Bezier
                        double cx1 = lastX + 2.0/3.0 * (x1 - lastX);
                        double cy1 = lastY + 2.0/3.0 * (y1 - lastY);
                        double cx2 = x + 2.0/3.0 * (x1 - x);
                        double cy2 = y + 2.0/3.0 * (y1 - y);
                        
                        path.curveTo(cx1, cy1, cx2, cy2, x, y);
                        lastControlX = x1;
                        lastControlY = y1;
                        lastX = x;
                        lastY = y;
                        idx += 2;
                    }
                    break;
                    
                case 'Z':
                case 'z':
                    path.closePath();
                    break;
            }
            
            lastCommand = command;
        }
        
        return path;
    } // end method parseSVGPath


} // end class ConvertSVGFileToTemplate

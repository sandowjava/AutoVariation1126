package com.csurf.app;

import com.csurf.core.ApplicationMediator;
// import com.csurf.mosaicfullmodule.controller.MosaicSubunitMediator;
import com.csurf.templatecore.controller.TemplateSubunitMediator;
import com.csurf.templatecore.svgconverter.SVGPackageMediator;
import com.csurf.templatecore.svgconverter.controller.SVGConverterSubunitMediator;

import javax.swing.*;

public class ApplicClass {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create the application mediator
            ApplicationMediator appMediator = new ApplicationMediator();

            // Initialize the main application frame
            ApplicFrame mainFrame = new ApplicFrame(appMediator);

            // Register the SVG subunit
            SVGPackageMediator svgMediator = new SVGPackageMediator(appMediator);
            appMediator.registerSubunit("svg", svgMediator);

            // Register the SVG converter subunit
            SVGConverterSubunitMediator svgConverterMediator = new SVGConverterSubunitMediator();
            appMediator.registerSubunit("svgconverter", svgConverterMediator);

            // Register the SmartPic subunit (minimal version)
            TemplateSubunitMediator smartPicMediator = new TemplateSubunitMediator(mainFrame);
            appMediator.registerSubunit("smartpic", smartPicMediator);

            /*
            // Register the Mosaic subunit
            MosaicSubunitMediator mosaicMediator = new MosaicSubunitMediator(appMediator);
            appMediator.registerSubunit("mosaic", mosaicMediator);

             */

            // Show the main frame
            mainFrame.setVisible(true);
        });
    } // end MAIN method

} // end ApplicClass

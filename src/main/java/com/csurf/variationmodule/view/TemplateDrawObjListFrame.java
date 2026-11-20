package com.csurf.variationmodule.view;

import com.csurf.templatecore.model.DrawObj;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Read-only diagnostic window listing the names of a Template's drawObjs,
 * numbered by their positional index (position i ↔ shape i+1 — the same
 * positional contract used by palettes and AGprefs rows).
 *
 * <p>View only — the controller supplies the drawObjs at open time, so the
 * list always reflects the last selected Template.
 */
public class TemplateDrawObjListFrame extends JFrame {

    /**
     * @param templateName display name of the Template (used in the title)
     * @param drawObjs     the Template's ordered drawObj list
     */
    public TemplateDrawObjListFrame(String templateName, List<DrawObj> drawObjs) {
        super("DrawObjs: " + templateName);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < drawObjs.size(); i++) {
            text.append(String.format("%2d  %s%n", i + 1, drawObjs.get(i).getName()));
        } // end FOR

        JTextArea area = new JTextArea(text.toString());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBorder(new EmptyBorder(8, 8, 8, 8));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);

        setPreferredSize(new Dimension(320, 480));
        pack();
    }
} // end class TemplateDrawObjListFrame

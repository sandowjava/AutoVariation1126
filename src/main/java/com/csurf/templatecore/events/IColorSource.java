package com.csurf.templatecore.events;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;

public interface IColorSource {
    void addColorSelectionListener(IColorSelectionListener listener);
    void removeColorSelectionListener(IColorSelectionListener listener);
    ColorPtOstDot getCurrentColor();
}


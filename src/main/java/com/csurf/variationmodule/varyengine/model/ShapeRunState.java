package csurf.cleandesign.variationmodule.varyengine.model;

import csurf.cleandesign.ostwald.domainmonotri.model.ColorPtOstDot;
import csurf.cleandesign.ostwald.domainmonotri.model.HueNumGUI;

import java.util.Optional;


/**
 * One shape's run state (parent §4.2, "the sheet"), made of two layers:
 *
 * <ul>
 *   <li><b>Decided layer</b> — {@code assignedAG}, {@code assignedHue},
 *       {@code chosenOstDot}; each may be {@link Undecided#UNDECIDED}.</li>
 *   <li><b>Override layer</b> — the {@code frozen} flag (decided
 *       representation). The actual override values come from the base
 *       at read time — see {@code effective*} on {@link VariationRunState}.</li>
 * </ul>
 *
 * Slots hold immutable values ({@link HueNumGUI}, {@link ColorPtOstDot}),
 * so copies are shallow by design (isolation invariant §4.3/4). Mutable;
 * written by Stratgs, read for dependencies; discarded at end of run.
 */
public class ShapeRunState {

    private Object assignedAG;      // Integer or Undecided.UNDECIDED — AG id 1–4
    private Object assignedHue;     // HueNumGUI or Undecided.UNDECIDED
    private Object chosenOstDot;    // ColorPtOstDot or Undecided.UNDECIDED
    private boolean frozen;

    public ShapeRunState() {
        this.assignedAG = Undecided.UNDECIDED;
        this.assignedHue = Undecided.UNDECIDED;
        this.chosenOstDot = Undecided.UNDECIDED;
        this.frozen = false;
    } // end constructor

    // ---------- decided-layer accessors: Optional read, raw write ----------

    public Optional<Integer> getAssignedAG() {
        return assignedAG == Undecided.UNDECIDED
                ? Optional.empty()
                : Optional.of((Integer) assignedAG);
    } // end method getAssignedAG

    public void setAssignedAG(int ag) {
        this.assignedAG = ag;
    } // end method setAssignedAG

    public Optional<HueNumGUI> getAssignedHue() {
        return assignedHue == Undecided.UNDECIDED
                ? Optional.empty()
                : Optional.of((HueNumGUI) assignedHue);
    } // end method getAssignedHue

    public void setAssignedHue(HueNumGUI hue) {
        this.assignedHue = hue;
    } // end method setAssignedHue

    public Optional<ColorPtOstDot> getChosenOstDot() {
        return chosenOstDot == Undecided.UNDECIDED
                ? Optional.empty()
                : Optional.of((ColorPtOstDot) chosenOstDot);
    } // end method getChosenOstDot

    public void setChosenOstDot(ColorPtOstDot dot) {
        this.chosenOstDot = dot;
    } // end method setChosenOstDot

    // ---------- override layer ----------

    public boolean isFrozen() {
        return frozen;
    } // end method isFrozen

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    } // end method setFrozen

} // end class ShapeRunState

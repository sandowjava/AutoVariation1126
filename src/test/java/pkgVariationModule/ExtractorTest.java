package pkgVariationModule;

import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.PaletteWName;
import com.csurf.variationmodule.varyengine.controller.PalettesThe12Variations;
import com.csurf.variationmodule.varyengine.controller.VariationPaletteExtractor;
import com.csurf.variationmodule.varyengine.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


/**
 * End-to-end seam test added 111926 during the frozenSet no-op
 * investigation: exercises the exact controller path —
 * {@code runCleanCycle} + {@code extract12VariationPalettes} — and
 * asserts the freeze overlay survives palette projection for all six
 * frozen sets, and that the frozenSet choice changes the palettes.
 */
class ExtractorTest {

    private static final Facts FACTS = AuthoredFactsTable.instance();
    private static final String HUE_SET = "05-03-06-08-legacyTableRowNum13";

    @Test
    void palettesReinstateFrozenShapes_fromSlot1_forAllSets() {
        for (int frozenSet = 0; frozenSet < FrozenSets.FROZEN_SET_COUNT; frozenSet++) {
            PalettesThe12Variations palettes = extract(frozenSet);

            ColorPtOstDot[] slot1 = palettes.getPaletteAt(0).getColorPtOstdotArray();
            Set<Integer> frozen = FrozenSets.get(frozenSet);
            for (int v = 1; v < PalettesThe12Variations.NUM_VARIATIONS; v++) {
                ColorPtOstDot[] dots = palettes.getPaletteAt(v).getColorPtOstdotArray();
                for (int shape : frozen) {
                    Assertions.assertSame(slot1[shape - 1], dots[shape - 1],
                            "frozenSet " + frozenSet + " slot " + (v + 1) + " shape " + shape);
                } // end FOR shape
            } // end FOR slot
        } // end FOR frozenSet
    }

    @Test
    void frozenSetChoice_changesPalettes() {
        PalettesThe12Variations a = extract(0);
        PalettesThe12Variations b = extract(1);

        boolean differs = false;
        for (int v = 1; v < PalettesThe12Variations.NUM_VARIATIONS && !differs; v++) {
            PaletteWName pa = a.getPaletteAt(v);
            PaletteWName pb = b.getPaletteAt(v);
            for (int i = 0; i < pa.getSize(); i++) {
                if (pa.getColorAt(i) != pb.getColorAt(i)) {
                    differs = true;
                    break;
                } // end IF
            } // end FOR color
        } // end FOR slot
        Assertions.assertTrue(differs, "frozenSet 0 vs 1 produced identical palettes");
    }

    /** The exact controller seam: cook+run, then project results to palettes. */
    private static PalettesThe12Variations extract(int frozenSet) {
        List<VariationResult> results =
                VariationPaletteExtractor.runCleanCycle(HUE_SET, frozenSet, FACTS);
        return new VariationPaletteExtractor().extract12VariationPalettes(results);
    }

} // end class ExtractorTest

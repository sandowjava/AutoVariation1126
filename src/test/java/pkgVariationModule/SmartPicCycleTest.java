package pkgVariationModule;

import com.csurf.core.ApplicationMediator;
import com.csurf.ostwald.domainmonotri.model.ColorPtOstDot;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.PaletteWName;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.storage.TemplateStorage;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.variationmodule.model.SmartPic;
import com.csurf.variationmodule.model.SmartPicFactory;
import com.csurf.variationmodule.varyengine.controller.PalettesThe12Variations;
import com.csurf.variationmodule.varyengine.controller.VariationPaletteExtractor;
import com.csurf.variationmodule.varyengine.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


/**
 * 111926 real-data diagnostic written during the frozenSet no-op
 * investigation. Reproduces the exact {@code CreateSmartPicController}
 * seam with real files — a saved Template from {@code dev_storage},
 * {@code SmartPicFactory} + {@code authored-defaults-v1.json},
 * {@code SmartPicFacts} → {@code runCleanCycle} →
 * {@code extract12VariationPalettes} → positional palette apply onto
 * fresh Template copies (mirroring {@code loadPaletteColoredTemplates}).
 * Asserts the freeze overlay is visible on the applied DrawObjs.
 */
class SmartPicCycleTest {

    private static final String TEMPLATE_NAME = "heroEye111726purplsFewBrwns-v1wAGs";
    private static final String AGPREFS_NAME = "authored-defaults-v1.json";
    private static final String HUE_SET = "04-05-02-07-legacyTableRowNum1";

    @Test
    void realTemplateCycle_frozenDrawObjsReinstateSlot1() {
        for (int frozenSet = 0; frozenSet < FrozenSets.FROZEN_SET_COUNT; frozenSet++) {
            List<Template> slots = buildVariationTemplates(frozenSet);
            ColorPtOstDot slot1AtFrozen;
            Set<Integer> frozen = FrozenSets.get(frozenSet);
            for (int v = 1; v < slots.size(); v++) {
                List<DrawObj> drawObjs = slots.get(v).getAllDrawObjs();
                for (int shape : frozen) {
                    slot1AtFrozen = slots.get(0).getAllDrawObjs().get(shape - 1)
                            .getColorPtOstDotFillColor();
                    Assertions.assertEquals(
                            loc(slot1AtFrozen),
                            loc(drawObjs.get(shape - 1).getColorPtOstDotFillColor()),
                            "frozenSet " + frozenSet + " slot " + (v + 1) + " shape " + shape);
                } // end FOR shape
            } // end FOR slot
        } // end FOR frozenSet
    }

    @Test
    void realTemplateCycle_frozenSetChoiceChangesPalettes() {
        List<Template> a = buildVariationTemplates(0);
        List<Template> b = buildVariationTemplates(5);   // disjoint-ish set {15,17}

        boolean differs = false;
        for (int v = 1; v < a.size() && !differs; v++) {
            List<DrawObj> da = a.get(v).getAllDrawObjs();
            List<DrawObj> db = b.get(v).getAllDrawObjs();
            for (int i = 0; i < da.size(); i++) {
                if (!loc(da.get(i).getColorPtOstDotFillColor())
                        .equals(loc(db.get(i).getColorPtOstDotFillColor()))) {
                    differs = true;
                    break;
                } // end IF
            } // end FOR drawObj
        } // end FOR slot
        Assertions.assertTrue(differs,
                "frozenSet 0 vs 5 produced identical displayed templates");
    }

    /** Diagnostic dump: which shapes visibly differ between slot 1 and slot 2 under frozenSet 0. */
    @Test
    void realTemplateCycle_dumpFreezeEvidence() {
        int frozenSet = 0;
        List<Template> slots = buildVariationTemplates(frozenSet);
        Set<Integer> frozen = FrozenSets.get(frozenSet);
        System.out.println("=== frozenSet " + frozenSet + " = " + frozen + " ===");
        List<DrawObj> s1 = slots.get(0).getAllDrawObjs();
        List<DrawObj> s2 = slots.get(1).getAllDrawObjs();
        for (int shape = 1; shape <= s1.size(); shape++) {
            String a = loc(s1.get(shape - 1).getColorPtOstDotFillColor());
            String b = loc(s2.get(shape - 1).getColorPtOstDotFillColor());
            System.out.printf("shape %2d %-28s slot1=%-7s slot2=%-7s %s%n",
                    shape, s1.get(shape - 1).getName(), a, b,
                    frozen.contains(shape) ? (a.equals(b) ? "FROZEN(same)" : "FROZEN-MISMATCH!")
                            : (a.equals(b) ? "same-by-coincidence" : "varied"));
        }
    }

    // ---------- the exact controller seam ----------

    private static List<Template> buildVariationTemplates(int frozenSet) {
        TemplateStorage storage = new TemplateStorage(new ApplicationMediator());
        Template template = storage.loadTemplate(TEMPLATE_NAME);
        SmartPic smartPic = new SmartPicFactory().createSmartPic(
                template, StorageConfig.getAgPrefsPath().resolve(AGPREFS_NAME));

        Facts facts = new SmartPicFacts(smartPic);
        List<VariationResult> results =
                VariationPaletteExtractor.runCleanCycle(HUE_SET, frozenSet, facts);
        PalettesThe12Variations palettes =
                new VariationPaletteExtractor().extract12VariationPalettes(results);

        // mirrors CreateSmartPicController.loadPaletteColoredTemplates
        List<Template> copies = new java.util.ArrayList<>();
        for (int v = 0; v < PalettesThe12Variations.NUM_VARIATIONS; v++) {
            Template copy = storage.loadTemplate(TEMPLATE_NAME);
            PaletteWName palette = palettes.getPaletteAt(v);
            List<DrawObj> drawObjs = copy.getAllDrawObjs();
            Assertions.assertEquals(palette.getSize(), drawObjs.size());
            ColorPtOstDot[] dots = palette.getColorPtOstdotArray();
            for (int i = 0; i < drawObjs.size(); i++) {
                drawObjs.get(i).setFillColorOstdot(dots[i]);
            } // end FOR
            copies.add(copy);
        } // end FOR slot
        return copies;
    }

    /** Ostwald location: user-facing hue + fill order — the "same color" contract. */
    private static String loc(ColorPtOstDot dot) {
        return dot.getHueNumGUI().getUserFacingHueNum() + ":" + dot.getFillOrderNum();
    }

} // end class SmartPicCycleTest

package pkgVariationModule;

import com.csurf.variationmodule.varyengine.model.AGHuePattern;
import com.csurf.variationmodule.varyengine.model.AGHuePatterns;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * Verification of the name-keyed {@link AGHuePatterns} catalog that
 * replaced the {@code AnchorHueSets} index table: canonical naming,
 * deduped legacy rows, sorted access, and loud failure on unknown names.
 */
class AGHuePatternsTest {

    // ---------- canonical naming ----------

    @Test
    void canonicalPrefix_isZeroPaddedAndAgOrdered() {
        Assertions.assertEquals("06-09-04-02", AGHuePatterns.canonicalPrefix(6, 9, 4, 2));
        Assertions.assertEquals("05-03-06-08", AGHuePatterns.canonicalPrefix(5, 3, 6, 8));
        // zero-padding makes lexicographic order equal numeric order
        Assertions.assertTrue("06-09-04-02".compareTo("12-13-10-15") < 0);
        Assertions.assertFalse("6-9-4-2".compareTo("12-13-10-15") < 0); // unpadded sorts wrong
    }

    @Test
    void canonicalName_suffixOptional_prefixAloneIsLegal() {
        Assertions.assertEquals("06-09-04-02", AGHuePatterns.canonicalName(6, 9, 4, 2, null));
        Assertions.assertEquals("06-09-04-02", AGHuePatterns.canonicalName(6, 9, 4, 2, "  "));
        Assertions.assertEquals("06-09-04-02-heroEye-v2",
                AGHuePatterns.canonicalName(6, 9, 4, 2, " heroEye-v2 "));
    }

    @Test
    void canonicalize_trimsCollapsesAndStripsAroundHyphens() {
        Assertions.assertEquals("06-09-04-02 spring",
                AGHuePatterns.canonicalize("  06 - 09-04 -02  spring  "));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> AGHuePatterns.canonicalize(null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> AGHuePatterns.canonicalize("   "));
    }

    // ---------- the legacy table ----------

    @Test
    void catalogHolds23DedupedLegacyRows() {
        // 25 legacy rows minus rows 19 (≡0) and 23 (≡1)
        Assertions.assertEquals(23, AGHuePatterns.count());
        Assertions.assertEquals(23, AGHuePatterns.all().size());
        Assertions.assertEquals(23, AGHuePatterns.allNames().size());
    }

    @Test
    void legacyNamesCarryRowProvenance() {
        AGHuePattern row0 = AGHuePatterns.get("06-09-04-02-legacyTableRowNum0");
        Assertions.assertEquals(6, row0.hueNumForAG(1));
        Assertions.assertEquals(9, row0.hueNumForAG(2));
        Assertions.assertEquals(4, row0.hueNumForAG(3));
        Assertions.assertEquals(2, row0.hueNumForAG(4));

        AGHuePattern row18 = AGHuePatterns.get("13-10-06-08-legacyTableRowNum18");
        Assertions.assertEquals(13, row18.hueNumForAG(1));
        Assertions.assertEquals(8, row18.hueNumForAG(4));
    }

    @Test
    void lookupIsCaseInsensitiveButPrefixIsNotAKey() {
        // case-insensitive canonical key
        Assertions.assertSame(
                AGHuePatterns.get("06-09-04-02-legacyTableRowNum0"),
                AGHuePatterns.get("  06-09-04-02-LEGACYTABLEROWNUM0 "));

        // the hue prefix alone is descriptive, not a key
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> AGHuePatterns.get("06-09-04-02"));
        Assertions.assertFalse(AGHuePatterns.exists("06-09-04-02"));

        // unknown name → loud error naming the key
        IllegalArgumentException thrown = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> AGHuePatterns.get("no-such-pattern"));
        Assertions.assertTrue(thrown.getMessage().contains("no-such-pattern"));
    }

    @Test
    void allReturnsSortedView() {
        List<String> names = new ArrayList<>(AGHuePatterns.allNames());
        List<String> sorted = new ArrayList<>(names);
        java.util.Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        Assertions.assertEquals(sorted, names);
    }

} // end class AGHuePatternsTest

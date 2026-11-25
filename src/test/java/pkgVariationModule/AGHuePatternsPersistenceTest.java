package pkgVariationModule;

import com.csurf.variationmodule.varyengine.model.AGHuePattern;
import com.csurf.variationmodule.varyengine.model.AGHuePatterns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Phase 2 persistence verification for {@link AGHuePatterns}: JSON
 * round-trip through a {@code @TempDir} stand-in for
 * {@code dev_storage/aghuepatterns}, plus the decided collision policy —
 * existing entries win on load with a loud skip — and warn-and-skip
 * tolerance for malformed/drifted files.
 *
 * <p>All pattern names use {@code Zz…} suffixes so entries registered in
 * the JVM-shared catalog can never collide with built-ins or other
 * tests; {@link AGHuePatterns#clearUserPatternsForTests()} restores a
 * clean catalog around each test.
 */
class AGHuePatternsPersistenceTest {

    private static final String BUILTIN_NAME = "13-10-06-08-legacyTableRowNum18";

    @TempDir
    Path dir;

    @BeforeEach
    void cleanCatalog() {
        AGHuePatterns.clearUserPatternsForTests();
    }

    @AfterEach
    void cleanCatalogAfter() {
        AGHuePatterns.clearUserPatternsForTests();
    }

    private void writeJson(String fileName, String json) throws IOException {
        Files.writeString(dir.resolve(fileName), json);
    }

    private static String docJson(String name, String hues) {
        return "{ \"formatVersion\": 1, \"name\": \"" + name + "\", "
                + "\"description\": \"test\", \"anchorHues\": " + hues + " }";
    }

    // ---------- round-trip ----------

    @Test
    void saveTo_writesFileAndRegistersThenLoadFromRestores() throws IOException {
        AGHuePattern pattern = new AGHuePattern(
                "11-22-33-04-ZzRoundTrip", 11, 22, 33, 4);
        AGHuePatterns.saveTo(dir, pattern, "round-trip test");

        Path file = dir.resolve("11-22-33-04-ZzRoundTrip.json");
        Assertions.assertTrue(Files.exists(file));
        Assertions.assertTrue(Files.readString(file).contains("\"anchorHues\""));
        Assertions.assertEquals(AGHuePatterns.BUILTIN_COUNT + 1, AGHuePatterns.count());

        // simulate a fresh launch: drop in-memory user entries, reload from disk
        AGHuePatterns.clearUserPatternsForTests();
        Assertions.assertEquals(1, AGHuePatterns.loadFrom(dir));
        AGHuePattern restored = AGHuePatterns.get("11-22-33-04-ZzRoundTrip");
        Assertions.assertEquals(pattern, restored);
        Assertions.assertEquals(22, restored.hueNumForAG(2));
        Assertions.assertEquals(4, restored.hueNumForAG(4));
    }

    // ---------- load collision policy: existing wins ----------

    @Test
    void loadSkipsFileCollidingWithBuiltIn() throws IOException {
        writeJson("builtin-dup.json", docJson(BUILTIN_NAME, "[13, 10, 6, 8]"));
        Assertions.assertEquals(0, AGHuePatterns.loadFrom(dir));
        Assertions.assertEquals(AGHuePatterns.BUILTIN_COUNT, AGHuePatterns.count());
        // the built-in still resolves
        Assertions.assertEquals(BUILTIN_NAME,
                AGHuePatterns.get(BUILTIN_NAME).getName());
    }

    @Test
    void loadTwoFilesSameKey_firstSortedFileWins() throws IOException {
        // same canonicalKey, different case → different file names, sorted a<z
        writeJson("a-clash.json", docJson("11-22-33-04-ZzClash", "[11, 22, 33, 4]"));
        writeJson("z-clash.json", docJson("11-22-33-04-zzclash", "[11, 22, 33, 4]"));
        Assertions.assertEquals(1, AGHuePatterns.loadFrom(dir));
        Assertions.assertEquals("11-22-33-04-ZzClash",
                AGHuePatterns.get("11-22-33-04-ZZCLASH").getName());
    }

    // ---------- validation: warn-and-skip ----------

    @Test
    void loadSkipsNameWhosePrefixDoesNotMatchHues() throws IOException {
        // file drift: name prefix says 11-22-33-04 but anchorHues say 1,2,3,4
        writeJson("drift.json", docJson("11-22-33-04-ZzDrift", "[1, 2, 3, 4]"));
        Assertions.assertEquals(0, AGHuePatterns.loadFrom(dir));
        Assertions.assertFalse(AGHuePatterns.exists("11-22-33-04-ZzDrift"));
    }

    @Test
    void loadSkipsWrongHueCountAndUnsupportedVersion() throws IOException {
        writeJson("short.json", docJson("11-22-33-04-ZzShort", "[11, 22, 33]"));
        writeJson("v99.json", "{ \"formatVersion\": 99, \"name\": "
                + "\"11-22-33-04-ZzV99\", \"anchorHues\": [11, 22, 33, 4] }");
        Assertions.assertEquals(0, AGHuePatterns.loadFrom(dir));
        Assertions.assertFalse(AGHuePatterns.exists("11-22-33-04-ZzShort"));
        Assertions.assertFalse(AGHuePatterns.exists("11-22-33-04-ZzV99"));
    }

    @Test
    void malformedFileDoesNotBlockValidFile() throws IOException {
        writeJson("aa-good.json", docJson("11-22-33-04-ZzGood", "[11, 22, 33, 4]"));
        writeJson("zz-bad.json", "{ not valid json !!!");
        Assertions.assertEquals(1, AGHuePatterns.loadFrom(dir));
        Assertions.assertTrue(AGHuePatterns.exists("11-22-33-04-ZzGood"));
    }

    @Test
    void loadFromMissingDirReturnsZero() {
        Assertions.assertEquals(0, AGHuePatterns.loadFrom(dir.resolve("nope")));
        Assertions.assertEquals(0, AGHuePatterns.loadFrom(null));
    }

    // ---------- save-side overwrite ----------

    @Test
    void saveOverwritesSameKeyUserEntry() throws IOException {
        AGHuePatterns.saveTo(dir,
                new AGHuePattern("11-22-33-04-ZzOver", 11, 22, 33, 4), "v1");
        AGHuePatterns.saveTo(dir,
                new AGHuePattern("11-22-33-04-ZzOver", 11, 22, 33, 4), "v2");
        Assertions.assertEquals(AGHuePatterns.BUILTIN_COUNT + 1, AGHuePatterns.count());
        Assertions.assertTrue(
                Files.readString(dir.resolve("11-22-33-04-ZzOver.json")).contains("v2"));
    }

} // end class AGHuePatternsPersistenceTest

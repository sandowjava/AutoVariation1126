package pkgVariationModule;

import com.csurf.core.ApplicationMediator;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.Template;
import com.csurf.templatecore.storage.TemplateStorage;
import com.csurf.templatecore.storage.config.StorageConfig;
import com.csurf.variationmodule.model.RelationMatrix;
import com.csurf.variationmodule.model.RelationMatrixDraft;
import com.csurf.variationmodule.model.ShapeColorPrefs;
import com.csurf.variationmodule.model.SmartDrawObj;
import com.csurf.variationmodule.model.SmartPic;
import com.csurf.variationmodule.model.SmartPicFactory;
import com.csurf.variationmodule.model.SmartPicFactory.AgPrefsDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

/**
 * 112126 v0G round-trip regression for the RelationMatrix work: loads the
 * v1 positional defaults file, binds a draft to a real saved Template,
 * serializes to a formatVersion 2 (UUID-keyed) file, reloads it through
 * the factory's v2 path, and asserts the resulting SmartPic carries the
 * same prefs — proving the v2 write/read contract end-to-end.
 */
class RelationMatrixRoundTripTest {

    private static final String TEMPLATE_NAME = "heroEye111726purplsFewBrwns-v1wAGs";
    private static final String AGPREFS_NAME = "authored-defaults-v1.json";

    @TempDir
    Path tempDir;

    @Test
    void v1FileDraftSaveV2Reload_producesIdenticalPrefs() {
        Template template = loadTemplate();
        SmartPicFactory factory = new SmartPicFactory();
        Path v1File = StorageConfig.getAgPrefsPath().resolve(AGPREFS_NAME);

        // draft from the v1 doc binds positionally — all rows bound
        AgPrefsDocument v1Doc = factory.loadAgPrefs(v1File);
        RelationMatrixDraft draft = RelationMatrixDraft.fromDocument(v1Doc);
        draft.bindToTemplate(template);
        Assertions.assertTrue(draft.isFullyBound(),
                "v1 draft should bind positionally: " + draft.unboundCount()
                        + " unbound, " + draft.uncoveredCount() + " uncovered");
        Assertions.assertEquals(template.getAllDrawObjs().size(), draft.rows().size());

        // save as v2 and reload through the factory's UUID path
        Path v2File = tempDir.resolve("roundtrip-rmat2.json");
        SmartPicFactory.writeAgPrefs(v2File, draft.toDocument());
        AgPrefsDocument v2Doc = factory.loadAgPrefs(v2File);
        Assertions.assertEquals(AgPrefsDocument.FORMAT_UUID, v2Doc.formatVersion());
        for (SmartPicFactory.ShapeRow row : v2Doc.shapes) {
            Assertions.assertNotNull(row.drawObjId,
                    "v2 row missing drawObjId: shape " + row.shape);
        }

        // the v2 SmartPic must carry the same prefs as the v1 SmartPic
        SmartPic v1Pic = factory.createSmartPic(template, v1File);
        SmartPic v2Pic = factory.createSmartPic(template, v2File);
        Assertions.assertEquals(v1Pic.numShapes(), v2Pic.numShapes());
        for (int shape = 1; shape <= v1Pic.numShapes(); shape++) {
            ShapeColorPrefs a = v1Pic.smartDrawObj(shape).colorPrefs();
            ShapeColorPrefs b = v2Pic.smartDrawObj(shape).colorPrefs();
            Assertions.assertEquals(a.valueTarget(), b.valueTarget(), "shape " + shape);
            Assertions.assertEquals(a.defaultAG(), b.defaultAG(), "shape " + shape);
            Assertions.assertArrayEquals(a.agPrefs(), b.agPrefs(),
                    "shape " + shape + " agPrefs");
        }
    }

    @Test
    void relationMatrixFacade_viewsAndDocumentMatchSmartPic() {
        Template template = loadTemplate();
        SmartPicFactory factory = new SmartPicFactory();
        Path v1File = StorageConfig.getAgPrefsPath().resolve(AGPREFS_NAME);
        SmartPic pic = factory.createSmartPic(template, v1File);

        RelationMatrix matrix = new RelationMatrix(pic);
        Assertions.assertEquals(pic.numShapes(), matrix.numShapes());
        Assertions.assertEquals(pic.numShapes(), matrix.shapes().size());

        // row view: prefs by UUID match prefs by shapeNum
        List<DrawObj> drawObjs = template.getAllDrawObjs();
        for (int i = 0; i < drawObjs.size(); i++) {
            SmartDrawObj sdo = pic.smartDrawObj(i + 1);
            Assertions.assertEquals(sdo.drawObjUuid(), drawObjs.get(i).getUniqueId(),
                    "SmartDrawObj should carry the DrawObj's UUID, shape " + (i + 1));
            Assertions.assertSame(sdo.colorPrefs(),
                    matrix.prefsFor(drawObjs.get(i).getUniqueId()));
        }

        // column view: defaultAgMembers partitions all shapes
        int total = 0;
        for (int ag = 1; ag <= ShapeColorPrefs.NUM_AGS; ag++) {
            total += matrix.defaultAgMembers(ag).size();
        }
        Assertions.assertEquals(pic.numShapes(), total);

        // toDocument emits v2 rows in draw order keyed by UUID
        AgPrefsDocument doc = matrix.toDocument("test", "round trip");
        Assertions.assertEquals(AgPrefsDocument.FORMAT_UUID, doc.formatVersion());
        Assertions.assertEquals(pic.numShapes(), doc.shapes.size());
        for (int i = 0; i < doc.shapes.size(); i++) {
            Assertions.assertEquals(i + 1, doc.shapes.get(i).shape);
            Assertions.assertEquals(drawObjs.get(i).getUniqueId().toString(),
                    doc.shapes.get(i).drawObjId);
        }
    }

    private Template loadTemplate() {
        TemplateStorage storage = new TemplateStorage(new ApplicationMediator());
        return storage.loadTemplate(TEMPLATE_NAME);
    }

} // end class RelationMatrixRoundTripTest

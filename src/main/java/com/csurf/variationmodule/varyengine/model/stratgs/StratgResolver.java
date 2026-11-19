package com.csurf.variationmodule.varyengine.model.stratgs;

import com.csurf.variationmodule.varyengine.model.Stratg;
import com.csurf.variationmodule.varyengine.model.StratgType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;


/**
 * {@link StratgType} → executable instance (step 5b). Reused by the
 * engine in step 7 to run flattened spec lists, and by
 * {@link DefaultNestedRunner} for inner specs. Executables are
 * stateless, so the resolver hands out shared singletons.
 */
public final class StratgResolver {

    private static final Map<StratgType, Stratg> EXECUTABLES = new EnumMap<>(StratgType.class);

    static {
        EXECUTABLES.put(StratgType.ASSIGN_AGS, new AssignAGsStratg());
        EXECUTABLES.put(StratgType.ALTERNATE_AG_POOLS, new AlternateAGPoolsStratg());
        EXECUTABLES.put(StratgType.ASSIGN_ANCHOR_HUES, new AssignAnchorHuesStratg());
        EXECUTABLES.put(StratgType.ASSIGN_OST_DOTS, new AssignOstDotsStratg());
        EXECUTABLES.put(StratgType.FREEZE_SHAPES, new FreezeShapesStratg());
    } // end static init

    private StratgResolver() {
    } // end private constructor

    /** The shared executable for {@code type}; throws when unregistered. */
    public static Stratg resolve(StratgType type) {
        Stratg stratg = EXECUTABLES.get(Objects.requireNonNull(type, "type"));
        if (stratg == null) {
            throw new IllegalArgumentException("no executable registered for " + type);
        } // end IF
        return stratg;
    } // end method resolve

} // end class StratgResolver

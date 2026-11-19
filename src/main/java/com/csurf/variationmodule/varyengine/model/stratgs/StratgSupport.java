package csurf.cleandesign.variationmodule.varyengine.model.stratgs;

import csurf.cleandesign.variationmodule.varyengine.model.StratgSpec;
import csurf.cleandesign.variationmodule.varyengine.model.StratgType;


/** Shared helpers for the stratg executables. Package-private. */
final class StratgSupport {

    private StratgSupport() {
    } // end private constructor

    /** Fails loudly when the spec's type is not the executable's — catches miswired calls early. */
    static void requireType(StratgSpec params, StratgType expected) {
        if (params.getStratgType() != expected) {
            throw new IllegalArgumentException(
                    "spec type " + params.getStratgType() + " passed to " + expected + " executable");
        } // end IF
    } // end method requireType

} // end class StratgSupport

package csurf.cleandesign.variationmodule.varyengine.model;

import java.util.Objects;


/**
 * The concrete, immutable {@link StratgContext} (Context-A, §7.2).
 * Built per execution: the engine binds facts + live run state +
 * prevForFrozen + nestedStratgToRun; nestedStratgToRun rebinds the same
 * shape to a scratch for a nested call — so a nested Stratg's
 * {@code nested()} is bound to the scratch, not the live state.
 */
public final class BasicStratgContext implements StratgContext {

    private final Facts facts;
    private final RunState runState;
    private final Inputs prevForFrozen;
    private final NestedRunner nestedStratgToRun;   // may be null — non-nesting Stratgs never call it

    private BasicStratgContext(Facts facts, RunState runState, Inputs inputs, NestedRunner nestedRunner) {
        this.facts = Objects.requireNonNull(facts, "facts");
        this.runState = Objects.requireNonNull(runState, "runState");
        this.prevForFrozen = Objects.requireNonNull(inputs, "prevForFrozen");
        this.nestedStratgToRun = nestedRunner;
    } // end private constructor

    public static BasicStratgContext of(Facts facts, RunState runState, Inputs inputs, NestedRunner nestedRunner) {
        return new BasicStratgContext(facts, runState, inputs, nestedRunner);
    } // end method of

    /** Context for a Stratg that cannot nest (5a executables, tests). */
    public static BasicStratgContext withoutRunner(Facts facts, RunState runState, Inputs inputs) {
        return new BasicStratgContext(facts, runState, inputs, null);
    } // end method withoutRunner

    @Override
    public Facts facts() {
        return facts;
    } // end method facts

    @Override
    public RunState runState() {
        return runState;
    } // end method runState

    @Override
    public Inputs inputs() {
        return prevForFrozen;
    } // end method inputs

    @Override
    public NestedRunner nested() {
        if (nestedStratgToRun == null) {
            throw new IllegalStateException(
                    "no nested runner bound to this context — nested Stratgs require ctx.nested()");
        } // end IF
        return nestedStratgToRun;
    } // end method nested

} // end class BasicStratgContext

package csurf.cleandesign.variationmodule.varyengine.model;


/**
 * The optional fast-path core (D5): a Stratg whose result depends only
 * on facts, params, and immutable global tables may expose its kernel
 * as {@code compute(facts, params) → Proposal}.
 *
 * <p>Eligibility is <b>structural</b>: the signature takes no run state,
 * no base, no flags, and no runner — a core cannot read decided state
 * because it cannot reach it. A Stratg that gains a state dependency
 * must drop this interface (a compile error in its shell), which keeps
 * the classification honest.
 *
 * <p>Slice-1 eligibility: 921 yes (its own {@code execute} delegates —
 * shell delegation invariant); 555, 255, 031 no. 920 is an outer Stratg.
 *
 * <p>Determinism is required by the contract; Java cannot forbid side
 * effects inside a core, so the equivalence harness covers that gap.
 */
public interface PureCore {

    Proposal compute(Facts facts, StratgSpec params);

} // end interface PureCore

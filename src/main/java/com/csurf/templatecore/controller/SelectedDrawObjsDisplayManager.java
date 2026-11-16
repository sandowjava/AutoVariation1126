package com.csurf.templatecore.controller;

import com.csurf.templatecore.events.ISelectionEventListener;
import com.csurf.templatecore.events.SelectionEvent;
import com.csurf.templatecore.model.DrawObj;
import com.csurf.templatecore.model.DrawObjRoster;
import com.csurf.templatecore.model.SelectionGroupListFacade;
import com.csurf.templatecore.model.SortOrder;
import com.csurf.templatecore.sorting.DrawObjSortStrategy;
import com.csurf.templatecore.sorting.strategies.AlphabeticalAZStrategy;
import com.csurf.templatecore.sorting.strategies.AlphabeticalZAStrategy;
import com.csurf.templatecore.sorting.strategies.OriginalOrderStrategy;
import com.csurf.templatecore.view.SelectedShapesListPanel;

import java.util.List;

/**
 * Manages the display of selected DrawObjs in the listSelectDrawObjsPanel.
 *
 * This class is the single point of responsibility for keeping listSelectDrawObjsPanel
 * in sync with the roster's current selection state. It:
 *   - Registers directly as an ISelectionEventListener on the DrawObjRoster
 *   - OWNS the sort strategy for the selection list (sortStrategy field)
 *   - Passes the strategy explicitly to the facade (no shared mutable state)
 *   - Calls listSelectDrawObjsPanel.setDrawObjs() as the only path to update that panel
 *
 * DESIGN: This manager owns its sortStrategy field completely. When refreshDisplay() is called,
 * it passes the strategy explicitly to the facade via getSelectedDrawObjsSorted(sortStrategy).
 * This eliminates shared mutable state and ensures the manager's strategy is always used,
 * regardless of what other parts of the system are doing.
 *
 * It does NOT maintain its own copy of the selection state. Every refresh
 * queries the roster on-demand via the facade.
 * 
 * Supports both legacy SortOrder enum (for backward compatibility) and new
 * DrawObjSortStrategy interface (for extensibility).
 */
public class SelectedDrawObjsDisplayManager implements ISelectionEventListener {

    private DrawObjRoster roster;
    private SelectionGroupListFacade facade;
    private final SelectedShapesListPanel listSelectDrawObjsPanel;
    private SortOrder sortOrder;
    private DrawObjSortStrategy sortStrategy;

    /**
     * Creates a new SelectedDrawObjsDisplayManager.
     *
     * The manager starts with no roster attached. Call attachToRoster() before use.
     *
     * @param listSelectDrawObjsPanel The panel this manager drives. Never null.
     */
    public SelectedDrawObjsDisplayManager(SelectedShapesListPanel listSelectDrawObjsPanel) {
        if (listSelectDrawObjsPanel == null) {
            throw new IllegalArgumentException("listSelectDrawObjsPanel must not be null");
        }
        this.listSelectDrawObjsPanel = listSelectDrawObjsPanel;
        this.sortOrder = SortOrder.ORIGINAL_ORDER;
        this.sortStrategy = new OriginalOrderStrategy();  // Default strategy
    }

    /**
     * Attaches this manager to a new roster and facade, detaching from any previous roster.
     *
     * Called by TemplateWorkFrame.updateUIWithTemplate() after the facade is created.
     * Safe to call with null to detach (e.g., when no template is loaded).
     *
     * @param roster The DrawObjRoster to listen to, or null to detach
     * @param facade The SelectionGroupListFacade to query for sorted selection, or null to detach
     */
    public void attachToRoster(DrawObjRoster roster, SelectionGroupListFacade facade) {
        if (this.roster != null) {
            this.roster.removeSelectionListener(this);
        }
        this.roster = roster;
        this.facade = facade;
        if (this.roster != null) {
            this.roster.addSelectionListener(this);
        }
        refreshDisplay();
    }

    /**
     * Sets the sort order for the selection list and immediately refreshes the display.
     *
     * Called by TemplateWorkFrame.setSortOrderSelectedDrawObjs().
     * Converts the SortOrder enum to a DrawObjSortStrategy.
     *
     * @param sortOrder The new sort order to apply
     */
    public void setSortOrder(SortOrder sortOrder) {
        if (sortOrder != null && sortOrder != this.sortOrder) {
            this.sortOrder = sortOrder;
            // Convert SortOrder enum to strategy
            this.sortStrategy = convertSortOrderToStrategy(sortOrder);
            refreshDisplay();
        }
    }

    /**
     * Sets the sort strategy for the selection list and immediately refreshes the display.
     *
     * This is the new strategy-based API. The manager owns the strategy and passes it
     * explicitly to the facade, eliminating shared mutable state.
     *
     * @param strategy the DrawObjSortStrategy to apply
     */
    public void setSortStrategy(DrawObjSortStrategy strategy) {
        if (strategy != null && !strategy.equals(this.sortStrategy)) {
            this.sortStrategy = strategy;
            refreshDisplay();
        }
    }

    /**
     * Gets the current sorting strategy.
     *
     * @return the currently active DrawObjSortStrategy
     */
    public DrawObjSortStrategy getCurrentStrategy() {
        return sortStrategy;
    }

    /**
     * Converts a legacy SortOrder enum to a DrawObjSortStrategy.
     *
     * @param sortOrder The SortOrder enum value
     * @return The corresponding DrawObjSortStrategy
     */
    private DrawObjSortStrategy convertSortOrderToStrategy(SortOrder sortOrder) {
        switch (sortOrder) {
            case ALPHABETICAL_AZ:
                return new AlphabeticalAZStrategy();
            case ALPHABETICAL_ZA:
                return new AlphabeticalZAStrategy();
            case ORIGINAL_ORDER:
            default:
                return new OriginalOrderStrategy();
        }
    }

    /**
     * Refreshes the display of listSelectDrawObjsPanel from the current roster state.
     *
     * This is the single path through which listSelectDrawObjsPanel is updated.
     * Queries the facade for the sorted selection using the manager's own strategy,
     * eliminating dependency on shared facade state.
     */
    public void refreshDisplay() {
        if (facade == null) {
            listSelectDrawObjsPanel.setDrawObjs(new DrawObj[0]);
            return;
        }
        List<DrawObj> sorted = facade.getSelectedDrawObjsSorted(sortStrategy);
        listSelectDrawObjsPanel.setDrawObjs(sorted.toArray(new DrawObj[0]));
    }

    /**
     * Handles a SelectionEvent from the roster by refreshing the display.
     *
     * This is called directly by the roster when selection state changes.
     */
    @Override
    public void handleSelectionEvent(SelectionEvent event) {
        refreshDisplay();
    }
}

package com.csurf.templatecore.model;

import com.csurf.templatecore.sorting.DrawObjSortStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Facade for adapting SelectionGroup and Template data for display purposes.
 * 
 * This class provides display-oriented methods for accessing DrawObj collections
 * with support for different sort strategies. It acts as an adapter between the
 * model (SelectionGroup, Template) and display components (list panels).
 * 
 * The facade queries the underlying DrawObjRoster on-demand to get fresh
 * SelectionGroup instances, ensuring it always reflects the current selection state.
 * 
 * DESIGN: This facade is a STATELESS ADAPTER. Clients pass sort strategies explicitly
 * as method parameters. This eliminates shared mutable state and enables independent
 * management of sort orders for different display lists.
 * 
 * API:
 * - getSelectedDrawObjsSorted(DrawObjSortStrategy strategy): Returns selected DrawObjs sorted by the given strategy
 * - getAllDrawObjsSorted(DrawObjSortStrategy strategy): Returns all DrawObjs sorted by the given strategy
 * - getSelectedDrawObjsSorted(SortOrder sortOrder): Legacy method using SortOrder enum (for backward compatibility)
 * - getAllDrawObjsSorted(SortOrder sortOrder): Legacy method using SortOrder enum (for backward compatibility)
 * 
 * Supports both legacy SortOrder enum (for backward compatibility) and new
 * DrawObjSortStrategy interface (for extensibility).
 */
public class SelectionGroupListFacade {
    private final DrawObjRoster roster;
    private final Template template;

    /**
     * Creates a new SelectionGroupListFacade.
     * 
     * @param roster The DrawObjRoster containing the current selection state
     * @param template The Template containing all DrawObjs
     */
    public SelectionGroupListFacade(DrawObjRoster roster, Template template) {
        this.roster = roster;
        this.template = template;
    }

    /**
     * Gets the selected DrawObjs sorted according to the specified strategy.
     * 
     * This is the new stateless API. The strategy is passed as an explicit parameter,
     * eliminating shared mutable state.
     * 
     * @param strategy The DrawObjSortStrategy to use for sorting
     * @return List of selected DrawObjs in sorted order
     */
    public List<DrawObj> getSelectedDrawObjsSorted(DrawObjSortStrategy strategy) {
        if (roster == null) {
            return new ArrayList<>();
        }

        SelectionGroup selectionGroup = roster.getSelectionGroup();
        if (selectionGroup == null) {
            return new ArrayList<>();
        }

        Set<DrawObj> selectedSet = selectionGroup.getSelectedDrawObjsSet();
        List<DrawObj> selectedList = new ArrayList<>(selectedSet);
        if (strategy != null) {
            selectedList.sort((obj1, obj2) -> strategy.compare(obj1, obj2));
        }
        return selectedList;
    }

    /**
     * Gets all DrawObjs in the Template sorted according to the specified strategy.
     * 
     * This is the new stateless API. The strategy is passed as an explicit parameter,
     * eliminating shared mutable state.
     * 
     * @param strategy The DrawObjSortStrategy to use for sorting
     * @return List of all DrawObjs in sorted order
     */
    public List<DrawObj> getAllDrawObjsSorted(DrawObjSortStrategy strategy) {
        if (template == null) {
            return new ArrayList<>();
        }

        List<DrawObj> allDrawObjs = new ArrayList<>(template.getAllDrawObjs());
        if (strategy != null) {
            allDrawObjs.sort((obj1, obj2) -> strategy.compare(obj1, obj2));
        }
        return allDrawObjs;
    }


    /**
     * Gets the selected DrawObjs in the specified sort order.
     * 
     * Always queries the current SelectionGroup state from the roster, then applies sorting.
     * 
     * @param sortOrder The desired sort order
     * @return List of selected DrawObjs in the specified order
     */
    public List<DrawObj> getSelectedDrawObjsSorted(SortOrder sortOrder) {
        if (roster == null) {
            System.out.println("021926 SELECTION SORT ORDER - SelectionGroupListFacade.getSelectedDrawObjsSorted() - roster is null");
            return new ArrayList<>();
        }

        SelectionGroup selectionGroup = roster.getSelectionGroup();
        if (selectionGroup == null) {
            System.out.println("021926 SELECTION SORT ORDER - SelectionGroupListFacade.getSelectedDrawObjsSorted() - selectionGroup is null");
            return new ArrayList<>();
        }

        Set<DrawObj> selectedSet = selectionGroup.getSelectedDrawObjsSet();
        System.out.println("021926 SELECTION SORT ORDER - SelectionGroupListFacade.getSelectedDrawObjsSorted() - Found " + selectedSet.size() + 
            " selected objects before sorting");
        List<DrawObj> selectedList = new ArrayList<>(selectedSet);
        
        List<DrawObj> sorted = sortDrawObjs(selectedList, sortOrder);
        System.out.println("021926 SELECTION SORT ORDER - SelectionGroupListFacade.getSelectedDrawObjsSorted() - Returning " + sorted.size() + 
            " sorted objects with order: " + sortOrder);
        return sorted;
    }

    /**
     * Gets all DrawObjs in the Template in the specified sort order.
     * 
     * Always queries the current Template state, then applies sorting.
     * 
     * @param sortOrder The desired sort order
     * @return List of all DrawObjs in the specified order
     */
    public List<DrawObj> getAllDrawObjsSorted(SortOrder sortOrder) {
        if (template == null) {
            return new ArrayList<>();
        }

        List<DrawObj> allDrawObjs = new ArrayList<>(template.getAllDrawObjs());
        
        return sortDrawObjs(allDrawObjs, sortOrder);
    }

    /**
     * Sorts a list of DrawObjs according to the specified sort order.
     * 
     * @param drawObjs The list to sort
     * @param sortOrder The desired sort order
     * @return The sorted list (original order for ORIGINAL_ORDER)
     */
    private List<DrawObj> sortDrawObjs(List<DrawObj> drawObjs, SortOrder sortOrder) {
        if (drawObjs == null || drawObjs.isEmpty()) {
            return drawObjs;
        }

        switch (sortOrder) {
            case ALPHABETICAL_AZ:
                drawObjs.sort((obj1, obj2) -> obj1.getName().compareToIgnoreCase(obj2.getName()));
                break;
            case ALPHABETICAL_ZA:
                drawObjs.sort((obj1, obj2) -> obj2.getName().compareToIgnoreCase(obj1.getName()));
                break;
            case ORIGINAL_ORDER:
                // No sorting needed—keep original order
                break;
        }

        return drawObjs;
    }

    /**
     * Gets the number of selected DrawObjs.
     * 
     * @return The count of selected objects
     */
    public int getSelectedCount() {
        if (roster == null) {
            return 0;
        }
        SelectionGroup selectionGroup = roster.getSelectionGroup();
        if (selectionGroup == null) {
            return 0;
        }
        return selectionGroup.getSelectedDrawObjsSet().size();
    }

    /**
     * Checks if a specific DrawObj is selected.
     * 
     * @param drawObj The DrawObj to check
     * @return true if the DrawObj is in the current selection, false otherwise
     */
    public boolean isDrawObjSelected(DrawObj drawObj) {
        if (roster == null || drawObj == null) {
            return false;
        }
        SelectionGroup selectionGroup = roster.getSelectionGroup();
        if (selectionGroup == null) {
            return false;
        }
        return selectionGroup.getSelectedDrawObjsSet().contains(drawObj);
    }

    /**
     * Gets the underlying SelectionGroup's selected DrawObjs set directly.
     * 
     * Use this only when you need the unordered set. For display purposes,
     * prefer getSelectedDrawObjsSorted().
     * 
     * @return The unordered set of selected DrawObjs
     */
    public Set<DrawObj> getSelectedDrawObjsSet() {
        if (roster == null) {
            return Set.of();
        }
        SelectionGroup selectionGroup = roster.getSelectionGroup();
        if (selectionGroup == null) {
            return Set.of();
        }
        return selectionGroup.getSelectedDrawObjsSet();
    }

    /**
     * Gets the underlying SelectionGroup instance from the roster.
     * 
     * Returns a fresh instance from the roster on each call.
     * 
     * @return The SelectionGroup instance from the roster
     */
    public SelectionGroup getSelectionGroup() {
        if (roster == null) {
            return new SelectionGroup();
        }
        return roster.getSelectionGroup();
    }
}

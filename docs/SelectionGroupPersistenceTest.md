# Manual Test Instructions for SelectionGroup Persistence

This document provides step-by-step instructions for manually testing the persistence of named SelectionGroups in templates.

## Prerequisites
- Ensure the application is built and running correctly
- Understand how to create and manipulate SelectionGroups in the UI

## Test Procedure

### Test 1: Creating and Saving Named SelectionGroups

1. Launch the application
2. Create a new template or open an existing one
3. Add several DrawObj instances to the template
4. Select 2-3 DrawObj instances
5. Open the SelectionGroupChoiceFrame (via the menu option)
6. Save the current selection as a named group (e.g., "TestGroup1")
7. Create another selection of different DrawObj instances
8. Save this as another named group (e.g., "TestGroup2")
9. Verify both groups appear in the SelectionGroupChoiceFrame list
10. Save the template to a file (note the file path)

### Test 2: Loading Template with Named SelectionGroups

1. Close the application (or just close the current template)
2. Relaunch the application (if needed)
3. Load the previously saved template
4. Open the SelectionGroupChoiceFrame
5. Verify that both named groups ("TestGroup1" and "TestGroup2") appear in the list
6. Select each group and verify that the correct DrawObj instances are selected in the template

### Test 3: Modifying Named SelectionGroups After Loading

1. With the loaded template, select one of the named groups
2. Modify the selection (add or remove DrawObj instances)
3. Save the modified selection with the same name
4. Save the template to a file (can use the same file or a new one)
5. Close and reopen the template
6. Verify that the modified group contains the correct DrawObj instances

### Test 4: Edge Cases

1. **Empty Group Test**: Create and save an empty selection group
2. **Large Group Test**: Create and save a group with many DrawObj instances (10+)
3. **Special Characters Test**: Create groups with special characters in names
4. Save the template, close, and reopen
5. Verify all groups load correctly

## Expected Results

- Named SelectionGroups should persist when templates are saved and loaded
- The SelectionGroupChoiceFrame should display all named groups after loading a template
- Selecting a named group should correctly select all DrawObj instances that were part of that group
- The reference resolution should work correctly, maintaining the integrity of DrawObj references

## Debugging Tips

- Check the console output for debug messages related to SelectionGroup loading and reference resolution
- Look for messages like:
  - "Template[name]: resolveSelectionGroupReferences called for X named selection groups"
  - "Template[name]: Resolving references for group 'GroupName' with X references"
  - "Template[name]: Resolved reference for DrawObj with ID [id]"
- If references fail to resolve, you'll see error messages like:
  - "Template[name]: Failed to resolve reference for DrawObj with ID [id]"

## Reporting Issues

If any issues are found during testing, document:
1. The exact steps to reproduce
2. The expected behavior
3. The actual behavior
4. Any error messages or logs
5. Screenshots if applicable

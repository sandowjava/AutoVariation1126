# COMMIT_MESSAGE_version 0K-prelim1 (112526): CreateSmartPicFrame menu rework and help-doc item

Small UI pass on the Create-SmartPic window's menu bar: the three
workflow menus are renamed with step numbers so the intended order of
operations reads left-to-right across the menu bar, the standalone
Patterns menu is folded into Controls-info, and a new menu item opens
the (stub) "SmartPic creation Help Manual" in the app's standard
HelpTextWindow.

## Menu changes (CreateSmartPicFrame)

- **Numbered workflow menus.** `Template` → `1)Template`,
  `Shape-AGprefs` → `2)RMatrix`, `SmartPic` → `3)SmartPic` — the
  numbering mirrors the create flow: pick a Template, pick the RMatrix
  (AGprefs) file, then create the SmartPic. The AGprefs chooser item is
  renamed `Choose AGprefs defaults file...` →
  `Choose AGprefs file (RMatrix)` to match the new menu name.
- **Patterns menu folded into Controls-info.** The standalone
  `Patterns` menu is removed; its item now lives in `Controls-info` as
  `Save AGHuePattern...` (was `Save new AGHuePattern...`), between
  `Template drawObj list` and `Hue anchors/ frozen`.
- **New "Help -open help doc" item.** Added at the bottom of
  `Controls-info` after a separator; wired to the new
  `Actions.showHelpDoc()` callback.

## Modified classes

- **`CreateSmartPicFrame`** — the menu changes above plus
  `Actions.showHelpDoc()`.
- **`CreateSmartPicController`** — `showHelpDoc()` opens a
  `com.csurf.app.HelpTextWindow` titled "SmartPic creation Help
  Manual", loading the classpath resource
  `help/smartpic_creation_help_manual.rtf` (new
  `HELP_DOC_RESOURCE` constant), centered on the frame.

## New files

- **`src/main/resources/help/smartpic_creation_help_manual.rtf`** —
  stub RTF for the SmartPic creation Help Manual, stored alongside the
  other help docs so `HelpTextWindow` finds it on the classpath.
  Content to be written.
- **`dev_storage/aghuepatterns/11-24-09-22-splitComplementary.json`** —
  user-authored AGHuePattern.
- **`dev_storage/agprefs/`** — `slamDunk112326orngGr-Rmatrix-v2`,
  `slamDunk112526orngRedBlu-Rmtrx-v1`, `slamDunk112526purplR-Rmtrx-v1`.
- **`dev_storage/palettes/`** — `slamDunk112526orngRedBlu-4hues-v1`,
  `slamDunk112526purplRed-4hues-v1`.
- **`dev_storage/templates/`** — `slamDunk112526orngRedBlu-4hues-v1groups`,
  `slamDunk112526purplRed-4hues-v1groups`.

## Notes

- The help manual is a stub; writing the actual content is deferred.
- No behavior changes — menu labels/order and the new help hook only.

Refactor and reorganize this JavaFX Course Player FXML layout to create a cleaner, more professional arrangement of all existing components while preserving ALL functionality, fx:id values, controller bindings, event handlers, and style classes.

==================================================
STRICT RULES
==================================================
- Do NOT change:
  - fx:controller
  - Any fx:id values
  - onAction handlers
  - Labels and button texts
  - styleClass values
  - stylesheets references
- Do NOT remove any controls.
- Do NOT change business logic.
- You may ONLY rearrange the layout structure and improve spacing.
- Return the full updated FXML only.

==================================================
NEW LAYOUT STRUCTURE
==================================================

Use a premium studio layout with the following arrangement:

┌─────────────────────────────────────────────────────────────┐
│ LEFT SIDEBAR (320px)                                        │
│ - Back button                                               │
│ - Course title + meta                                       │
│ - Scrollable sections list                                  │
│ - Sticky Add Section button                                 │
├─────────────────────────────────────────────────────────────┤
│ RIGHT MAIN CONTENT                                          │
│                                                             │
│ [Header Card]                                               │
│ - Breadcrumb                                                │
│ - Lesson Title                                              │
│ - Last Updated + Preview Badge                              │
│                                                             │
│ [Action Buttons Row]                                        │
│ - Edit | Replace | Delete | Save                            │
│                                                             │
│ [Large Video Player Card]                                   │
│                                                             │
│ [Tabs Pill Bar]                                             │
│                                                             │
│ [Scrollable Tab Content Card]                               │
└─────────────────────────────────────────────────────────────┘

==================================================
LAYOUT IMPROVEMENTS
==================================================

1. OUTER WORKSPACE
- Add consistent padding around main content:
  24px top, right, bottom, left.
- Use spacing of 20–24px between major sections.

2. HEADER CARD
- Put breadcrumb and title inside a dedicated card.
- Ensure lesson title wraps properly.
- Keep metadata directly under title.

3. ACTION BUTTONS
- Place in their own row below the header.
- Add spacing 10px.
- Save button aligned to the far right.

4. VIDEO PLAYER
- Make it the dominant section.
- Give it VBox.vgrow="ALWAYS".
- Minimum height around 420–480px.

5. TAB BAR
- Separate from video area with top margin.

6. TAB CONTENT
- Place inside a dedicated card below the tabs.
- Use ScrollPane with fitToWidth="true".
- Padding 24px.

7. SIDEBAR
- Fixed width around 320px.
- ScrollPane grows to fill available height.
- Add Section button always remains at bottom.

==================================================
SPACING RULES
==================================================
- Major sections spacing: 24px
- Internal card spacing: 8–12px
- Buttons spacing: 10px
- Tab spacing: 6px

==================================================
VISUAL HIERARCHY
==================================================
- Header card at top.
- Action row directly below.
- Large video player centered.
- Tabs below video.
- Content card below tabs.

==================================================
RESPONSIVE BEHAVIOR
==================================================
- Sidebar width fixed.
- Main content fills remaining width.
- Video player expands vertically.
- Tab content scrolls if needed.

==================================================
GOAL
==================================================
Rearrange all existing elements into a polished instructor studio layout with:
- Better spacing
- Clear visual hierarchy
- More balanced proportions
- Cleaner professional composition
- Improved usability

Do not redesign styles; only improve structure and positioning.
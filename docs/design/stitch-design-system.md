---
name: Kinetic Obsidian
colors:
  surface: '#111317'
  surface-dim: '#111317'
  surface-bright: '#37393d'
  surface-container-lowest: '#0c0e11'
  surface-container-low: '#1a1c1f'
  surface-container: '#1e2023'
  surface-container-high: '#282a2d'
  surface-container-highest: '#333538'
  on-surface: '#e2e2e6'
  on-surface-variant: '#c4c9ae'
  inverse-surface: '#e2e2e6'
  inverse-on-surface: '#2f3034'
  outline: '#8e937a'
  outline-variant: '#444934'
  surface-tint: '#aad600'
  primary: '#ffffff'
  on-primary: '#283500'
  primary-container: '#c5f331'
  on-primary-container: '#556d00'
  inverse-primary: '#506600'
  secondary: '#c0c7d0'
  on-secondary: '#2a3138'
  secondary-container: '#40474f'
  on-secondary-container: '#afb6bf'
  tertiary: '#ffffff'
  on-tertiary: '#442b00'
  tertiary-container: '#ffddb1'
  on-tertiary-container: '#895b00'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#c5f331'
  primary-fixed-dim: '#aad600'
  on-primary-fixed: '#161f00'
  on-primary-fixed-variant: '#3b4d00'
  secondary-fixed: '#dce3ed'
  secondary-fixed-dim: '#c0c7d0'
  on-secondary-fixed: '#151c23'
  on-secondary-fixed-variant: '#40474f'
  tertiary-fixed: '#ffddb1'
  tertiary-fixed-dim: '#ffba4b'
  on-tertiary-fixed: '#291800'
  on-tertiary-fixed-variant: '#624000'
  background: '#111317'
  on-background: '#e2e2e6'
  surface-variant: '#333538'
typography:
  metric-xl:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 52px
    letterSpacing: -0.03em
  metric-xl-mobile:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  metric-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: 0em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0em
  label-caps:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.08em
  label-md:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 1rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system delivers a high-performance, dark-mode-first aesthetic engineered for modern boutique gyms, cross-training boxes, and premium fitness studios. The visual tone is technical, disciplined, and uncompromisingly modern. It rejects decorative skeuomorphism, noisy gradients, and generic lifestyle photography in favor of structural clarity, precise geometry, and functional contrast.

The emotional core is performance and control. For gym operators, the interface communicates operational command, real-time analytics, and operational efficiency through dense, clean data presentation. For members using the mobile app, the experience feels elite, motivational, and seamless—akin to a telemetry dashboard for their physical progression.

Key visual attributes:
- **True Dark Architecture**: Built on deep obsidian and graphite foundations to eliminate eye fatigue in low-light gym floors and staff back-offices.
- **Electric Accentuation**: Monochromatic restraint punctuated deliberately with high-energy electric lime to direct intent, flag state changes, and celebrate numerical performance.
- **Micro-Metric Precision**: Data-dense layouts leveraging high-contrast typographic scale, tabular numerals, and clean SVG line work over non-essential imagery.
- **Spanish-First Cadence**: Typographic hierarchies and label structures are tuned for Spanish-language interfaces, accounting for longer average string lengths while maintaining strict geometric rhythm.

## Colors

The palette is engineered exclusively for dark mode, relying on precise tonal steps rather than arbitrary elevation drops or blurry drop shadows.

### Core Tonal Palette
- **Canvas Base (`#0B0D10`)**: The foundational canvas for all views; a cold near-black with deep slate undertones that prevents pixel burnout and guarantees deep contrast.
- **Surface Elevation 1 (`#14181D`)**: The primary container for cards, panels, list row items, and navigation drawers.
- **Surface Elevation 2 (`#1B2128`)**: Interactive hover layers, inner-well inputs, and secondary nested groupings.
- **Structural Border (`#232A31`)**: Rigid 1px separator line applied to cards, tables, headers, and contextual groupings. Provides architectural structure without visual clutter.
- **Subtle Divider (`#1A2027`)**: Low-contrast internal partition lines within dense lists.

### Accent & Feedback
- **Electric Lime (`#C6F432`)**: High-visibility signature token. Reserved exclusively for primary actions, active navigational states, metric callouts, progress tracks, and confirmation tags. Never use for static decorative backgrounds.
- **Risk / Terminal (`#FF5C5C`)**: Highlights over-capacity alerts, churn risk indicators, failed payments, and cancel actions.
- **Warning / Pending (`#FFB020`)**: Indicates pending bookings, waitlists, unverified waivers, and warning thresholds.
- **Neutral White (`#F2F4F5`)**: High-contrast off-white applied to primary metrics, active headers, and essential readouts.
- **Muted Steel (`#8B949E`)**: Used for metadata, inactive tabs, secondary descriptions, and micro-labels.
- **Dimmed Text (`#525A65`)**: Used for placeholder inputs, disabled states, and auxiliary table column titles.

## Typography

The typography is built entirely on `Inter` to provide geometric clarity, high legibility across dense data interfaces, and natural rendering of Spanish diacritics (`á`, `é`, `í`, `ó`, `ú`, `ñ`).

### Usage Directives
- **Metric Displays (`metric-xl`, `metric-lg`)**: Strictly paired with `font-feature-settings: "tnum" 1` (tabular numerals) to ensure that counters, financial tallies, and member ratios do not shift horizontally during updates.
- **Micro-Labels (`label-caps`)**: All section titles, table header labels, badge descriptions, and category dividers use uppercase casing with `0.08em` tracking. This anchors components and prevents visual dominance over raw numbers.
- **Spanish-Language Accommodations**: UI components with text labels (such as tabs, secondary navigation, and pill filters) must account for ~20-30% expansion in character count compared to English counterparts (e.g., "Members" vs. "Miembros", "Schedule" vs. "Horarios"). Maintain compact line heights (`1.2` to `1.4`) to prevent multi-line overflow.

## Layout & Spacing

The layout operates on an uncompromising 8-pixel base unit. All paddings, row heights, and component envelopes scale predictably via multiples of 8px (with 4px used strictly as an auxiliary half-step for micro-tags and icon-to-label gaps).

### Screen Architectures
- **Desktop Dashboard (Gym Operator)**:
  - Fixed persistent vertical navigation bar (240px wide).
  - Main operational workspace utilizing a 12-column fluid grid.
  - Page margin of `2rem` (32px) and gutters of `1.5rem` (24px).
  - Dense analytic rows: cards span 3, 4, 6, or 12 columns with standard `1.5rem` internal padding.
- **Mobile Member App (Native Viewport)**:
  - Single-column linear layout with `1rem` (16px) outer edge margins.
  - Bottom sticky navigation bar (64px safe-area anchored).
  - Card modules span 100% of the interior grid with `1rem` internal padding to maximize data density.

### Spacing Principles
- White space is utilitarian, not decorative. Use space to group related transactional controls rather than creating vast blank voids.
- Metric cards must consistently stack the label, the primary counter, and the delta comparison using `space-xs` (4px) and `space-sm` (8px) gaps.

## Elevation & Depth

This design system avoids soft, blurred drop shadows. In a dark, high-performance interface, diffuse shadows muddy the contrast boundaries and detract from clean data consumption. Depth is achieved purely through **tonal stratification and structural borders**.

### Layer Stratification
1. **Level 0 (Canvas Base - `#0B0D10`)**: The bottom plane. Contains global layout frameworks and desktop navigation sidebars.
2. **Level 1 (Card & Module Surface - `#14181D`)**: Standard elevated layer for cards, analytics widgets, schedules, and data tables. Bound by a crisp `1px solid #232A31` outline.
3. **Level 2 (Active & Secondary Wells - `#1B2128`)**: Used for embedded form controls, inner stat segments, list hover states, and inactive segment tabs.
4. **Level 3 (Overlays & Dialogs - `#181D23`)**: Modals, slide-out member sheets, and dropdown menus. Wrapped with `1px solid #2E3742` and a hard scrim backdrop blur (`backdrop-filter: blur(8px); background-color: rgba(11, 13, 16, 0.8)`).

### Border & Highlight Rules
- **No diffuse drop shadows**: All elevation distinction is produced by the brightness delta between background tones.
- **Active Focus Ring**: Interactive elements in focus state use a razor-sharp, zero-blur outline: `2px solid #C6F432` with an offset of `2px` over `#0B0D10`.

## Shapes

The geometry uses a deliberate, dual-tier corner structure:
- **Macro Structural Shells (12px)**: Cards, modals, bottom sheets, and main dashboard widgets utilize an explicit `12px` border radius (`rounded-md`). This softens the overall framework without feeling toy-like.
- **Micro Interactive Controls (8px)**: Buttons, text inputs, dropdown triggers, and list row items snap to an `8px` radius (`rounded-sm`). This provides an ergonomic, clickable silhouette.
- **Status & Metric Pills (9999px)**: Tags, capacity badges, live indicator dots, and category filter chips use fully circular pill contours.

Sharp 0px corners are reserved strictly for technical charts, partition rules, and split data tables.

## Components

### Buttons
- **Primary Action**: Solid `#C6F432` background with `#0B0D10` heavy text (`label-md`, 600 weight). No border. Border-radius 8px. Hover shifts background to `#D5F758`. Active state triggers scale down to `0.98`.
- **Secondary Action**: Background `#14181D`, border `1px solid #232A31`, text `#F2F4F5`. Hover shifts background to `#1B2128` and border to `#2E3742`.
- **Destructive Action**: Background `transparent`, border `1px solid rgba(255, 92, 92, 0.3)`, text `#FF5C5C`. Hover fills with `rgba(255, 92, 92, 0.1)`.

### Chips & Badges
- **Status Pills**: Height 24px, radius 9999px, padding 2px 10px, typography `label-sm`.
  - *Activo / Óptimo*: Text `#C6F432`, background `rgba(198, 244, 50, 0.12)`.
  - *Capacidad Llena / Riesgo*: Text `#FF5C5C`, background `rgba(255, 92, 92, 0.12)`.
  - *Pendiente / Espera*: Text `#FFB020`, background `rgba(255, 176, 32, 0.12)`.

### Cards & Data Panels
- Background `#14181D`, border `1px solid #232A31`, radius 12px.
- Padding: `24px` on desktop, `16px` on mobile.
- Section Header: Uppercase micro-label (`label-caps`) in `#8B949E` with a metric value beneath it in `#F2F4F5` using tabular numerals.

### Input Fields
- Height 44px, radius 8px, background `#14181D`, border `1px solid #232A31`.
- Text `#F2F4F5`, placeholder `#525A65`, padding horizontal 14px.
- Focus: Border color `#C6F432`, zero outline glow.

### Checkboxes & Radios
- Size 18x18px, background `#14181D`, border `1.5px solid #232A31`, radius 4px (checkbox) or 9999px (radio).
- Checked: Background `#C6F432`, border `#C6F432`, checkmark icon in `#0B0D10`.

### Data Tables & Member Lists
- Header: `#0B0D10` background, uppercase micro-labels (`label-caps`), text `#8B949E`, border-bottom `1px solid #232A31`.
- Rows: Background `#14181D`, hover background `#1B2128`, border-bottom `1px solid #1A2027`.
- Density: 48px row height for compact view, 64px for standard desktop tables.

### Minimalist Line Charts (Sparklines & Velocity)
- Vector strokes strictly 2px width in `#C6F432`.
- Grid reference lines: 1px dashed `#232A31`.
- Underfill: Transparent or low-opacity vertical linear cut (`rgba(198, 244, 50, 0.05)` to transparent). No complex glowing gradients.
- Data points: 4px solid circle `#C6F432` on hover only.
# DESIGN.md - Rolling Icons Live Wallpaper

This document defines the exact visual design system, UI components, and screen layouts for the **Rolling Icons Live Wallpaper** Android application. It serves as the single source of truth for UI/UX developers and generative design engines.

---

## 1. Design Philosophy & Theme
*   **Aesthetic Style**: **Retro Neo-Brutalism** / **Paper Craft Boardgame**.
*   **Core Characteristics**: Playful, high-tactile feel, flat vector illustrations, bold outlines, warm color palette, clean grid boundaries, and card-based modular sections.
*   **Key Attributes**:
    *   Thick dark outlines on all elements (cards, buttons, icons).
    *   No soft gradients for shadows; instead, use flat offset solid blocks.
    *   Warm, earthy, organic paper texture look through solid warm colors.

---

## 2. Design Tokens & Styling System

### 2.1 Color Palette
*   **Canvas Background (`#F9EED2`)**: Soft, warm cream paper background. Used for full-page screens.
*   **Card Background (`#F5D8B3`)**: Warm light tan/wheat kraft paper color.
*   **Borders / Outlines / Heavy Text (`#2E1C0C`)**: Deep dark espresso coffee/charcoal. Used for all primary text, borders, and dark graphic elements.
*   **Accent Color (`#C85C32`)**: Terracotta orange / warm burnt copper. Used for primary call-to-actions, selection highlights, and active states.
*   **Secondary Text (`#4E3629`)**: Muted cocoa brown. Used for helper text, subtitles, and descriptions.
*   **Pure Highlight (`#FFFFFF`)**: Clean white. Used inside input fields, icons, and text on primary buttons.
*   **Transparent Badge Tint (`#202E1C0C`)**: Semi-transparent dark brown (12% opacity) for badges and tag backings.

### 2.2 Typography
*   **Font Family**: Custom friendly, geometric Sans-Serif (e.g., *Outfit*, *Inter*, *Lexend*, or a geometric system font).
*   **Headers**: Bold, uppercase, with a small letter-spacing of `0.01` to `0.05` to feel structured.
*   **Hierarchies**:
    *   `H1` (Screen Titles): `22sp`, Bold, `#2E1C0C`
    *   `H2` (Card Titles): `18sp`, Bold, `#2E1C0C`
    *   `Sub-Header` (Section Labels): `12sp`, Bold, Uppercase, `#C85C32`
    *   `Body Text`: `12sp` or `14sp`, Regular, `#4E3629`
    *   `Badge Text`: `9sp`, Bold, Uppercase, `#2E1C0C`

### 2.3 Borders & Shadows
*   **Standard Border Stroke**: `2dp` (or `2px` equivalent in web/design tools), Solid, color `#2E1C0C`.
*   **Thick Border Stroke**: `3dp` (used to denote active selected states), Solid, color `#C85C32` or `#2E1C0C`.
*   **Corner Radii**:
    *   Main cards: `20dp` (Smooth rounded corners)
    *   Secondary items/buttons: `16dp` (Cartoonish rounded style)
    *   Badges & Tags: `12dp`
    *   Primary buttons: `100dp` (Pill shape)
*   **Shadows**: Flat solid offset, `4dp` offset X, `4dp` offset Y, Color `#2E1C0C` (no blur/spread).

---

## 3. UI Components

### 3.1 Buttons
*   **Primary Button**:
    *   Pill-shaped (Corner Radius `100dp`).
    *   Terracotta orange (`#C85C32`) background.
    *   Dark brown (`#2E1C0C`) border, `2dp` thickness.
    *   White (`#FFFFFF`) bold text.
*   **Secondary Button**:
    *   Rounded box (Corner Radius `16dp`).
    *   White (`#FFFFFF`) background.
    *   Dark brown (`#2E1C0C`) border, `2.5dp` thickness.
    *   Dark brown (`#2E1C0C`) bold text.
*   **Floating Action Button (FAB)**:
    *   Perfect circle.
    *   Terracotta orange (`#C85C32`) background with a dark brown (`#2E1C0C`) plus symbol (`+`).
    *   Thick dark brown border.

### 3.2 Cards
*   **Normal Card**:
    *   Background `#F5D8B3` (warm tan).
    *   Border `2dp` `#2E1C0C`.
    *   Corner Radius `20dp`.
*   **Selected Card**:
    *   Background `#F5D8B3` (warm tan).
    *   Thick border `3dp` `#C85C32` (terracotta orange).
    *   Corner Radius `20dp`.

### 3.3 Badges (Tags)
*   *Styling*: Background `#202E1C0C` (semi-transparent brown), no border, Corner Radius `12dp`, padding `8dp` horizontal, `3dp` vertical. Text color `#2E1C0C` in `9sp` bold uppercase.

### 3.4 Controls & Inputs
*   **Search Bar**: Rounded container (Corner Radius `16dp`), white background, dark brown border, search magnifying glass icon on the left.
*   **Sliders (Gravity, Size, Friction)**: Flat thick track in `#2E1C0C` (or `#202E1C0C`), with a terracotta circle knob.
*   **Switches**: Playful retro toggle switch. The track is off-white when OFF, terracotta orange when ON, with a dark brown knob.

---

## 4. Page Layouts & Screens

### Screen 1: Dashboard (Main Screen)
*   **Header Bar**: Height `64dp`. Contains a bold screen title `3D Custom Icons` on the left and a settings icon button (`ic_settings_white` tinted dark brown) on the right.
*   **Subtitle Banner**: Small uppercase text reading `3D INTERACTIVE PHYSICS SIMULATION` in `#C85C32` centered.
*   **Scrollable Feed**: A vertical list of feature cards with a `16dp` padding.
    *   **Card 1: Rolling Icon**: Layout divided into columns (left: preview image showing app icons stacked on a screen; right: title "Rolling Icon", badges for "GRAVITY: 3D" and "CHAOTIC", description, and primary button "Start").
    *   **Card 2: Spinning Icon**: Mirror layout (left: text details for "Spinning Icon", badges for "ROTATION: 2D" and "SPIRAL", description, and "Start" button; right: circular swirl icon preview image).
    *   **2x2 Grid of Small Cards** (placed below Card 1 and Card 2):
        *   **Grid Card 1: Shape Path**: Small card with a top-left circular badge containing a terracotta star icon, bold title "Shape Path", and a short description.
        *   **Grid Card 2: Emoji Icon**: Small card with a top-left circular badge containing an emoji smiley face icon, bold title "Emoji Icon", and short description "Manage active emojis".
        *   **Grid Card 3: Photo Icon**: Small card with a top-left circular badge containing a custom photo icon, bold title "Photo Icon", and short description "Manage active photos".
        *   **Grid Card 4: Wallpaper**: Small card with a top-left circular badge containing a phone/wallpaper screen icon, bold title "Wallpaper", and short description "Set static / live wallpaper".

### Screen 2: Customize Settings
*   **Top Bar**: Title `CUSTOMIZE SETTINGS` in bold uppercase, centered. Back arrow on the left.
*   **Section 1: Select Display Mode**: Label `SELECT DISPLAY MODE` in small bold terracotta orange text.
    *   Contains two clickable cards stacked vertically: `Live Wallpaper Mode` (thick terracotta outline when selected) and `Home Launcher Mode` (standard dark brown outline).
*   **Section 2: Configuration Options**: Label `CONFIGURATION OPTIONS`.
    *   A container card with settings controls:
        *   **Icon Size**: Three-step segmented pill selector: `small`, `middle`, `large`.
        *   **Friction, Gravity, FPS Sliders**: Graphical sliders with numerical indicators.
        *   **Toggles**: Switches for `Enable Gravity (Sensor)` and `Float Button`.
*   **Bottom Bar**: Primary terracotta button taking full width to save and apply the wallpaper.

### Screen 3: Icon Content Picker
*   **Top Header**: Back arrow, title `Configure Icons`, and active count `Selected 5 of 30` in a badge.
*   **Search Field**: Prominent search bar below the header.
*   **Tab Layout**: Three tabs: `Apps` (selected tab with bold text and filled background), `Emoji`, and `Photos` (unselected tabs with transparent backgrounds and outlines).
*   **Grid Content**:
    *   Under *Apps*: Grid of circular app icons (2D icons inside circles with thick outlines).
    *   Under *Emoji*: Grid of various emojis on light tan circular backdrops.
    *   Under *Photos*: Grid of user-uploaded images, plus a dashed box with a "+" sign to add new images.
    *   *Selected state indicator*: A small green or terracotta badge containing a checkmark in the upper-right corner of each selected grid cell.
*   **Actions Bar**: Floats at the bottom with a white 'Cancel' button on the left and terracotta 'Continue' button on the right.

### Screen 4: Shape Path Selector
*   **Top Header**: Title `Select Shape Path`.
*   **Grid layout**: A 3x2 grid of path shape option cards:
    *   *Shapes*: Heart, Infinity, Star, Flower, Four-leaf Clover, Butterfly.
    *   *Inside each card*: A clear dotted/dashed vector outline representation of the movement path, with small icons floating along the path lines.
    *   *Selected state*: Active shape card gets an accent border in `#C85C32` and an active background shade.

### Screen 5: Active Simulation Preview Canvas
*   **Main Display**: A full screen rendered using LibGDX. 
*   **Visual Assets**:
    *   Background wallpaper image (selected by the user from a gallery or default list).
    *   Physics icons (2D circular sprites matching the selected Apps, Emojis, or Photos) tumbling freely under physics rules.
*   **Interactive Details**: Icons can be touched, dragged, and flung across the screen with simulated gravity and elastic collisions.
*   **HUD Controls**:
    *   Top right: A floating card containing a level score or launcher status.
    *   Bottom right: A round floating action button to add more items.

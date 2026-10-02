---
name: SIGEPES
description: Sistema de Gestión de Pesca Sostenible. Frosted glass panes laid over a sunlit nautical chart.
colors:
  navy-abyss: "#071E30"
  navy-deep: "#0B3B5C"
  navy: "#0F4C75"
  navy-light: "#3D6E92"
  navy-tint: "#E7EEF3"
  teal: "#0E7C86"
  teal-dark: "#0A5F67"
  teal-light: "#4FA8AE"
  teal-tint: "#E3F2F1"
  sea-shallow: "#EEF7F7"
  sea-lagoon: "#D3EBEE"
  sea-reef: "#B5DCE1"
  ink: "#0F2433"
  ink-soft: "#33495A"
  ink-muted: "#4E6474"
  rule-200: "#D3DFE5"
  utility-bar: "#06192A"
  glass-pane: "rgba(255,255,255,0.64)"
  glass-pane-strong: "rgba(255,255,255,0.82)"
  glass-smoked: "rgba(7,30,48,0.74)"
  error: "#B3261E"
  warning: "#A85A00"
  success: "#1B6B3F"
  info: "#0B6E86"
typography:
  display:
    fontFamily: "Public Sans, Segoe UI, Roboto, system-ui, sans-serif"
    fontSize: "3.25rem"
    fontWeight: 800
    lineHeight: 1.05
    letterSpacing: "-0.025em"
  headline:
    fontFamily: "Public Sans, Segoe UI, Roboto, system-ui, sans-serif"
    fontSize: "2.375rem"
    fontWeight: 800
    lineHeight: 1.2
    letterSpacing: "-0.02em"
  title-page:
    fontFamily: "Public Sans, Segoe UI, Roboto, system-ui, sans-serif"
    fontSize: "2.125rem"
    fontWeight: 800
    lineHeight: 1.235
    letterSpacing: "-0.015em"
  title:
    fontFamily: "Public Sans, Segoe UI, Roboto, system-ui, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 800
    lineHeight: 1.6
  stat:
    fontFamily: "Public Sans, Segoe UI, Roboto, system-ui, sans-serif"
    fontSize: "2.25rem"
    fontWeight: 800
    lineHeight: 1
    letterSpacing: "-0.02em"
    fontFeature: "\"tnum\""
  body:
    fontFamily: "Public Sans, Segoe UI, Roboto, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
    fontFeature: "\"tnum\""
  lede:
    fontFamily: "Public Sans, Segoe UI, Roboto, system-ui, sans-serif"
    fontSize: "1.0625rem"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "Public Sans, Segoe UI, Roboto, system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 650
    letterSpacing: "0.1px"
  label-caps:
    fontFamily: "Public Sans, Segoe UI, Roboto, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 700
    letterSpacing: "0.4px"
rounded:
  ruler: "6px"
  tooltip: "8px"
  field: "10px"
  menu: "12px"
  card: "14px"
  row: "16px"
  dialog: "18px"
  panel: "20px"
  section: "24px"
  hero: "28px"
  pill: "999px"
spacing:
  unit: "8px"
  gutter: "16px"
  page-x-mobile: "16px"
  page-x-desktop: "32px"
  card-pad: "20px"
  section-y-mobile: "56px"
  section-y-desktop: "88px"
components:
  button-primary:
    backgroundColor: "{colors.navy-deep}"
    textColor: "#FFFFFF"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "0 22px"
    height: "44px"
  button-primary-hover:
    backgroundColor: "{colors.navy-abyss}"
  button-action:
    backgroundColor: "{colors.teal}"
    textColor: "#FFFFFF"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "0 28px"
    height: "52px"
  button-action-hover:
    backgroundColor: "{colors.teal-dark}"
  button-outlined:
    backgroundColor: "rgba(255,255,255,0.45)"
    textColor: "{colors.navy-deep}"
    rounded: "{rounded.pill}"
    padding: "0 22px"
    height: "44px"
  input:
    backgroundColor: "rgba(255,255,255,0.78)"
    textColor: "{colors.ink}"
    rounded: "{rounded.field}"
  input-focus:
    backgroundColor: "#FFFFFF"
  card:
    backgroundColor: "{colors.glass-pane}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "20px"
  nav-item:
    textColor: "{colors.ink-soft}"
    rounded: "{rounded.pill}"
    height: "44px"
  nav-item-selected:
    backgroundColor: "{colors.navy-deep}"
    textColor: "#FFFFFF"
  app-bar:
    backgroundColor: "{colors.glass-smoked}"
    textColor: "#FFFFFF"
    height: "64px"
  chip:
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
---

# Design System: SIGEPES

## Overview

**Creative North Star: "Glass over the Nautical Chart"**

Every surface in SIGEPES is a frosted pane laid over a bathymetric chart. The page itself is pale, sunlit shallow water: a fixed gradient from shallow aqua to lagoon, with a layer of navy isobath contour lines (`/images/isobaths.svg`, procedurally generated, not survey data) and a slow drifting caustic of light. Permits, vedas, catches and audits are read on glass, over the water those rules govern. The glass is functional, not ornamental: it keeps the chart and imagery present behind the work while the near-white fill holds text contrast in coastal daylight.

Two glass temperatures carry all hierarchy. Clear frosted white panes hold work and reading. Smoked navy glass marks authority and chrome: the top bar, the mission band, the footer, the auth highlights. Navy is the ink of the system and teal is its only accent hue. Typography is a single family, Public Sans, set heavy (800) for headings with tight tracking, and tabular numerals everywhere because the product counts centimetres, permit numbers and dates.

Density is calm and generous on the public portal (sections 88px apart, panes with 24-28px corners) and practical in the app (floating sidebar, 14px cards, tables with tabular figures). The system rejects the flat white-card enterprise dashboard and the solid navy-banner government portal; glass over sea is the replacement for both.

**Key Characteristics:**
- Sea-field page background with isobath contours and a 38s caustic drift, frozen under reduced motion.
- Three glass recipes only: pane, pane-strong, smoked.
- Navy ink plus one teal accent; semantic colours only for status.
- Public Sans 800 headings, tabular numerals, Spanish institutional copy.
- Pill-shaped controls and selections; rounded panes that grow with their scale (14px card to 28px hero).

## Colors

A cool, sunlit maritime palette: deep navy ink, a single teal accent, and three shallow-water tints that form the ground.

### Primary
- **Abyss Navy** (navy-abyss): the darkest water. Smoked glass tint, hero photo scrims, hero H1 colour, tooltip fill, reduced-transparency app bar.
- **Deep Chart Navy** (navy-deep): the system's ink for action. Default contained button fill, selected sidebar pill, isobath stroke colour, divider base (12% alpha), outline/scrollbar tints.
- **Harbor Navy** (navy) and **Slate Swell** (navy-light): hover fill for selected nav, input border on hover.
- **Navy Mist** (navy-tint): logo shield fill on light grounds.

### Secondary
- **Signal Teal** (teal): the one accent. Public conversion CTAs ("Solicitar acceso", "Registrarse", "Ir a mi panel"), focus-visible outline (3px), focused input border and 4px halo, tab indicator, text selection, caret, list markers, the minimum-size mark on the species ruler, the active roadmap ring.
- **Deep Teal** (teal-dark): icon colour inside tinted icon tiles, role caption, legend/value text that must pass AA on glass.
- **Lagoon Teal** (teal-light): accent on dark smoked glass only (stat numerals in the mission band, selected nav icon, auth highlight icons, logo wave).
- **Teal Wash** (teal-tint): soft accent ground.

### Neutral
- **Shallow Water** (sea-shallow), **Lagoon** (sea-lagoon), **Reef** (sea-reef): the sea field. Body gradient runs `#EEF7F7 -> #DCEFF1 -> #C4E3E8 -> #B3D9DF` at 170deg under a pale top-left glare.
- **Chart Ink** (ink): body text. **Soft Ink** (ink-soft): nav item text, table header text. **Muted Ink** (ink-muted): secondary text, inactive icons, list subheaders.
- **Rule** (rule-200): light hairlines; most dividers are navy-deep at 9-14% alpha instead.
- **Utility Night** (utility-bar): the thin institutional utility strip above the header.
- **Glass fills** (glass-pane 64%, glass-pane-strong 82%, glass-smoked 74%): see Elevation & Depth.

### Semantic
- **error**, **warning**, **success**, **info**: status only (chips, alerts, legality ticks, connection dot). Alerts use these as 25-28% borders over 82-88% tinted frosted fills.

### Named Rules
**The One Accent Rule.** Teal is the only accent hue. It marks the conversion action, focus, selection and the legal threshold; nothing decorative is teal. Navy is ink, not accent; status colours never decorate.

**The Dark-Glass Teal Rule.** On smoked navy surfaces use teal-light; on white glass use teal or teal-dark. Never put teal-light on white glass (fails contrast).

## Typography

**Display Font:** Public Sans (with Segoe UI, Roboto, system-ui)
**Body Font:** Public Sans
**Label/Mono Font:** Public Sans with tabular numerals (`font-variant-numeric: tabular-nums` on body and table cells)

**Character:** One sturdy civic grotesque, loaded from Google Fonts in weights 400-800. Headings are heavy (800) with negative tracking so they hold on frosted glass; body stays regular and plain, as befits a legal register.

### Hierarchy
- **Display** (800, 2.125rem mobile to 3.25rem desktop, 1.05, -0.025em): the hero H1 on the frosted hero pane only. Balanced wrap.
- **Headline** (800, 1.75rem to 2.375rem, -0.02em): public section headings (h2), the auth statement.
- **Page Title** (800, 1.75rem to 2.125rem, -0.015em): the app page H1 in `PageHeader` / `PageContainer`.
- **Title** (800, 1.25rem): card, ledger and roadmap headings (h3); dialog titles.
- **Stat** (800, 2.25rem, line-height 1, tabular): StatCard values; 3rem in the smoked mission band.
- **Lede** (400, 1.0625rem, ~1.6): section ledes, max 680px.
- **Body** (400, 1rem, 1.5): running text; mission copy 1.7 line-height on smoked glass.
- **Label** (650, 0.875rem): buttons, tabs, chips, nav items (600; 650 when selected).
- **Label Caps** (700, 0.72-0.75rem, 0.4-1px tracking, uppercase): only table column headers and sidebar group subheaders.

### Named Rules
**The No-Kicker Rule.** No eyebrow or kicker label above a heading. Module context is carried by breadcrumbs; `PageHeader` deliberately does not render its legacy `eyebrow` prop. Uppercase small caps exist only as table headers and nav group labels.

**The Spanish Register Rule.** All interface copy is Spanish, formal and institutional, addressing the citizen as "tú" in CTAs and ledes as built, with legal citations written in full (Ley 13 de 1990, Ley 1851 de 2017, AUNAP, ODS 14 meta 14.4).

## Layout

- **App shell:** sticky smoked app bar (64px) under a thin utility strip (desktop only). On desktop (md+) a floating frosted sidebar (264px) sits inset 16px from the edge and bar, sticky, full remaining height, 20px corners. Content max width 1440px, padding 16px mobile / 32px desktop; bottom padding 104px on mobile to clear the bottom nav.
- **Mobile:** sidebar becomes a left drawer (20px outer corners); a frosted bottom navigation (64px, top corners 18px, safe-area padded) carries the first five modules.
- **Public portal:** content shell max 1280px with 16/24/32px side padding. Sections are separated by 56px (mobile) / 88px (desktop) of open chart, not by bands of colour. Anchored sections offset scroll by header height + 16px.
- **Hero:** full-bleed photo with navy scrims, the frosted hero pane bottom-left (max 600px) and the legality example pane (340px) bottom-right, rising in with a 0.9s ease-out.
- **Auth:** full-bleed catch photo, institutional message left on desktop, frosted form pane (max 460px) floating right.
- **Grouped content is one pane with internal hairlines**, not a grid of separate cards: quick-access list (2 columns), legal ledger (1/2/4 columns), contact channels (1/3 columns).
- **Rhythm:** MUI 8px unit; card content padding 20px; pane padding 20-56px scaled with pane size.

## Elevation & Depth

Depth is glass, not paper. Every elevated surface is a translucent fill plus backdrop blur and saturation over the sea field or a photograph, a 1px light rim, an inner top highlight, and a soft navy-offset shadow. There are exactly three recipes, exported from the theme as `glass`:

### Shadow Vocabulary
- **Pane** (`background rgba(255,255,255,0.64); backdrop-filter: blur(20px) saturate(170%); border: 1px solid rgba(255,255,255,0.75); box-shadow: inset 0 1px 0 rgba(255,255,255,0.9), 0 1px 2px rgba(11,59,92,0.06), 0 12px 32px -8px rgba(11,59,92,0.18)`): cards, outlined papers, grouped section panes.
- **Pane Strong** (`rgba(255,255,255,0.82); blur(24px) saturate(180%); rim 0.85; box-shadow: inset 0 1px 0 #fff, 0 2px 4px rgba(11,59,92,0.06), 0 24px 48px -12px rgba(7,30,48,0.28)`): sidebar, drawer, dialog, menus/popovers, bottom nav, auth form, hero pane (fill raised to 0.78), current roadmap card.
- **Smoked** (`rgba(7,30,48,0.74); blur(18px) saturate(160%); border-bottom 1px rgba(255,255,255,0.12); box-shadow: inset 0 -1px 0 rgba(255,255,255,0.04), 0 8px 24px -12px rgba(7,30,48,0.5)`): app bar, public header, footer (0.86), mission band (0.84), auth highlights.
- **Action lift** (`inset 0 1px 0 rgba(255,255,255,0.18-0.22), 0 6px 16px -6px` navy-deep or teal-dark at 55-60%): contained buttons and the selected nav pill.
- **Modal backdrop**: navy-abyss at 35% with 4px blur.

### Named Rules
**The Glass-Over-Something Rule.** Glass only sits over imagery or the chart field. A frosted pane over a flat white surface is just a grey box; if there is nothing behind it, it is not glass.

**The Opaque Fallback Rule.** Without `backdrop-filter`, cards and outlined papers go to 94% white. Under `prefers-reduced-transparency`, panes, drawers and dialogs go to 97% white with no blur, and the app bar goes solid navy-abyss. Under `prefers-reduced-motion`, the caustic drift and pane-rise entrances stop.

## Shapes

Soft, water-worn geometry. Corners scale with the size of the pane: 6px ruler, 8px tooltip, 10px fields, 12px menus/alerts/icon tiles, 14px cards and tables, 16px list rows, 18px dialogs, 20px sidebar and roadmap cards, 24px section panes and auth form, 28px hero pane and mission band. Every control that a finger presses (buttons, chips, nav items, header links, connection status, scrollbar thumb) is a full pill (999px). Tinted icon tiles are rounded squares (40-48px, 12-14px radius) with a 1px inset ring of their own colour. Circles are reserved for avatars, status dots, legality ticks and roadmap waypoints. Borders are 1px hairlines; the only dashed lines are the roadmap route and the empty-state outline.

## Components

### Buttons
Pill-shaped, confident, lightly lifted.
- **Shape:** full pill (999px), min height 44px (34px small, 52px large), 22px side padding (14/28).
- **Primary (in-app):** navy-deep fill, white label 650, action-lift shadow. Default for page actions.
- **Action (conversion):** teal fill for the public CTA and registration; large size in the hero with a trailing arrow.
- **Outlined:** 1.5px border over 45% white with 8px blur; 70% white on hover. On dark smoked chrome: white text, 40% white border, 6% white fill.
- **Text:** navy-deep 6% hover wash.
- **States:** transitions .2s; press nudges down 1px with `cubic-bezier(.16,1,.3,1)`; focus is the global 3px teal outline, 2px offset.

### Chips
- **Style:** pill, label 650; outlined chips 1.5px border on 50% white. Status chips use semantic colours (Legal, Disponible, En planeación).

### Cards / Containers
- **Corner Style:** 14px (card), 20-28px for section panes.
- **Background:** Pane recipe; tables inside 14px containers with a 5% navy head row.
- **Shadow Strategy:** Pane / Pane Strong (see Elevation & Depth).
- **Border:** 1px light rim; internal dividers navy-deep at 9-14%.
- **Internal Padding:** 20px for StatCard; 24-28px for ledger cells.
- **StatCard:** tinted icon tile + muted label, then an 800-weight tabular value.
- **Empty State:** dashed navy 22% outline on 42% white, teal-light inbox icon.

### Inputs / Fields
- **Style:** 10px radius, 78% white fill, navy 22% outline; navy-light outline on hover; labels 600.
- **Focus:** white fill, 2px teal border, 4px teal halo at 14%.
- **Disabled:** 40% white.

### Navigation
- **App bar:** smoked glass, 64px, light logotype, pill connection indicator (green or amber dot with a 3px halo).
- **Sidebar:** Pane Strong, 20px corners, floating 16px from the edges; uppercase group subheaders; items are 44px pills in soft ink with muted icons; selected = navy-deep pill, white 650 label, teal-light icon, action-lift shadow; hover 6% navy wash. User identity block at the foot.
- **Public header:** smoked glass, 68px, sticky over the hero photo; pill links at 86% white, brighten on hover.
- **Bottom nav (mobile):** Pane Strong with 18px top corners; selected item in navy-deep.
- **Tabs:** 3px teal rounded indicator, labels 650 sentence case.

### Legality Check (signature)
A small frosted pane showing a catch verified against the law: species and size in 800 navy, italic scientific name, a success chip, then three rows each led by a 22px green tick circle. Always labelled as an illustrative example when it uses demo data.

### Species Ruler (signature)
Fallback visual for species without a photo: the isobath chart at 260% under a teal-to-navy wash, a fish silhouette positioned at the legal size, and a translucent measuring ruler with 10% and 2% ticks, the filled length tinted teal and the minimum marked by a 2px teal line with "mín. N cm".

### Isobath Field (signature)
The page ground: 31 navy contour paths (7 index contours at 1.4px / 22% opacity, 24 intermediate at 0.8px / 12%) over the sea gradient, plus a radial caustic that drifts over 38s. Reused as texture inside the species ruler.

## Do's and Don'ts

### Do:
- **Do** put every elevated surface on one of the three glass recipes (pane, pane-strong, smoked) and keep it over the chart field or a photograph.
- **Do** use teal (#0E7C86) as the single action accent: conversion CTA, focus, selection, the legal threshold. Use teal-light only on smoked navy.
- **Do** group related items inside one pane separated by hairlines (navy-deep at 9-14%) rather than a grid of separate cards.
- **Do** set headings in Public Sans 800 with negative tracking and keep numerals tabular.
- **Do** write all copy in Spanish, institutional and legally precise, with full legal citations.
- **Do** honour the opaque fallbacks for no-blur, reduced transparency and reduced motion.
- **Do** make pressable controls pills with a minimum 44px target.

### Don't:
- **Don't** place kicker or eyebrow labels above headings; breadcrumbs carry module context.
- **Don't** use coloured border-left accents on cards, alerts or list items. Hairline column dividers inside a pane are structure, not accent.
- **Don't** put frosted glass over a flat white or empty ground.
- **Don't** introduce a second accent hue or use semantic status colours decoratively.
- **Don't** use hard offset or heavy black shadows; depth is soft navy-offset glass shadow only.
- **Don't** replace the sea field with a flat background or a solid navy banner.

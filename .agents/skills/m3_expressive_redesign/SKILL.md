---
name: m3-expressive-redesign
description: Redesign Jetpack Compose screens with Material 3 Expressive principles, contrasting shapes, motion physics, strong hierarchy, adaptive surfaces, and morphing RoundedPolygons.
---

# Material 3 Expressive Redesign Skill

Use this skill to redesign Android Jetpack Compose screens with Material 3 Expressive, not just baseline Material 3.

## Goal
Redesign existing UI into a more tactile, modern, expressive interface using Material 3 Expressive principles: contrasting shapes, motion physics, stronger hierarchy, adaptive surfaces, and deliberate emphasis.

## Primary references
- Material 3 docs/spec: https://m3.material.io
- Official Figma kit: https://www.figma.com/community/file/1035203688168086460/material-3-design-kit
- Motion theming: https://m3.material.io/blog/m3-expressive-motion-theming
- Graphics shapes API: https://developer.android.com/reference/kotlin/androidx/graphics/shapes/package-summary
- Compose shapes guide: https://developer.android.com/develop/ui/compose/graphics/draw/shapes

## Hard requirements
1. Use Jetpack Compose and Material 3 APIs.
2. Prefer token-driven redesign over one-off visual tweaks.
3. Preserve accessibility, contrast, semantics, and touch targets.
4. Do not add random gradients, glassmorphism, neon glows, or generic dribbble-style decoration.
5. Keep the UI product-grade: expressive, but still functional.

## Redesign workflow
1. Audit the current screen:
   - Identify primary action, secondary actions, hierarchy issues, dense zones, and weak affordances.
   - Mark which containers should stay quiet and which deserve expressive emphasis.
2. Rebuild theme foundations:
   - Adopt Material 3 theme roles for color, typography, and shape.
   - If available in project dependencies, use expressive APIs and motion theming.
   - If not available, emulate expressive behavior with shape tokens, spring animation, and stronger container hierarchy.
3. Redesign layout:
   - Increase clarity before adding decoration.
   - Use larger visual separation between primary and secondary actions.
   - Use surface container levels instead of heavy borders.
4. Add expressive shape behavior:
   - Replace repetitive rounded rectangles with intentional shape variety.
   - Use pills, larger-radius hero containers, and morphing interactive shapes where state changes matter.
5. Add motion:
   - Use spring-based motion for expansion, selection, emphasis, and container transitions.
   - Motion must communicate state, not just look fancy.
6. Validate:
   - Check dark mode, accessibility, responsiveness, and consistency with Material conventions.

## Shape system guidance
Use shapes as hierarchy, not decoration.

### When to use standard corner tokens
- Dense lists, forms, settings rows: keep shapes restrained.
- Inputs and dense cards: small to medium radius.
- Primary cards and feature containers: large to extra large radius.
- FABs, chips, segmented controls: pill/full shapes.

### When to use expressive shapes
Use expressive or morphic shapes only when they improve attention, affordance, or state clarity:
- FAB expansion
- selected/unselected segmented buttons
- media cards
- onboarding hero panels
- loading/progress states
- bottom sheets and floating toolbars

Avoid shape morphing on every card in a feed.

## Morphing shapes
For true morphing shapes, use `androidx.graphics.shapes` with `RoundedPolygon`, `Morph`, and Compose drawing APIs.

### Preferred approach
- Define two polygon states, for example `MaterialShapes.Circle` to `MaterialShapes.Cookie4Sided` or a custom rounded polygon.
- Animate a `progress` float from 0f to 1f.
- Draw or clip the shape using the morphed path.
- Apply morphing only to meaningful state transitions.

### Good uses
- Pressed button state
- Expanded FAB menu trigger
- Selected chip or segmented button thumb
- Play/pause media container state
- Pull-to-refresh or progress indicator

### Bad uses
- Constant idle morphing everywhere
- Morphing all list items at once
- Decorative shape changes with no UX meaning

## Motion rules
Prefer spring or expressive motion tokens over arbitrary tween durations.

### Motion principles
- Fast for microinteractions.
- Medium for container resize and selection.
- Slow only for large transitions.
- Fade and shape changes should support spatial motion, not replace it.

### Good patterns
- Corner radius changes on press.
- FAB expands into labeled action.
- Segmented control thumb morphs shape on selection.
- Card expands with emphasized spring and tonal elevation shift.

### Avoid
- Over-bouncy navigation transitions.
- Long animation chains for routine actions.
- Fading everything without spatial relationship.

## Component replacement guidance
When redesigning, prefer these swaps where they improve UX:
- Standard button row -> expressive button group or pill buttons
- Flat card list -> tonal cards with stronger shape hierarchy
- Basic FAB -> extended or expanding FAB
- Generic top app bar -> flexible top bar where screen importance warrants it
- Plain progress indicator -> expressive indicator only if it matches product tone
- Dense tabs -> segmented controls when choices are mode-like and few in number

## Color and surface rules
- Use Material color roles, not raw hardcoded colors scattered across files.
- Use surfaceContainerLow, surfaceContainer, surfaceContainerHigh, and surfaceContainerHighest to build depth.
- Keep expressive emphasis focused on action zones and hero content.
- Avoid relying on saturated color where shape and spacing can do the job.

## Typography rules
- Strengthen hierarchy using display/headline/title roles, not random font sizes.
- Use emphasized styles sparingly for primary headings, totals, and key call-to-action labels.
- Do not make every title bold and oversized.

## Coding instructions for the agent
When applying this skill, follow this implementation order:

1. Theme
   - Centralize colors, typography, shapes, and motion.
   - Remove duplicated hardcoded corner radii, padding, and colors.

2. Screen structure
   - Refactor composables into clear sections: hero/header, content groups, action area, supporting info.
   - Improve whitespace and grouping before animation.

3. Shape upgrade
   - Map containers to shape tiers.
   - Upgrade high-emphasis elements first.
   - Add pill or morph shapes only where state/value justifies it.

4. Motion integration
   - Add animated shape, elevation, size, and content transitions.
   - Use consistent motion specs across similar interactions.

5. Review
   - Verify no expressive element feels random.
   - Ensure reduced-motion fallback remains usable.

## Prompt template for Antigravity
Use this exact prompt as a task starter:

"Redesign this Android Jetpack Compose screen using Material 3 Expressive principles. Keep it production-grade and accessible. First audit the current hierarchy and interaction model, then refactor the theme to use Material color roles, expressive shape tiers, and spring-based motion. Replace repetitive rounded rectangles with intentional shape hierarchy. Add true morphing shapes only for meaningful state changes using androidx.graphics.shapes where appropriate. Prefer surface container depth over heavy borders. Keep code modular, remove hardcoded UI values, and explain every major redesign choice briefly in comments or commit notes."

## Dependency hints
Use or check for these depending on project state:

```gradle
implementation("androidx.compose.material3:material3:<version>")
implementation("androidx.graphics:graphics-shapes:<version>")
```

If expressive APIs are experimental in the target project:
- opt in explicitly where needed
- keep fallbacks for stable Material 3 behavior

## Review checklist
- Is the primary action visually dominant?
- Are shape changes meaningful?
- Is there less visual noise than before?
- Are surface levels doing real hierarchy work?
- Does motion improve comprehension?
- Does the screen still feel fast and usable?
- Does the redesign look like a real Android product instead of a concept shot?

## Deliverable expectations
When using this skill, the coding agent should output:
- updated theme/tokens
- redesigned composables
- any custom morph shape wrappers
- concise notes explaining shape, motion, and hierarchy decisions

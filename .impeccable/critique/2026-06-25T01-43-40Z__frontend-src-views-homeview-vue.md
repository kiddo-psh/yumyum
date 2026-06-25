---
target: frontend/src/views/HomeView.vue
total_score: 19
p0_count: 2
p1_count: 2
timestamp: 2026-06-25T01-43-40Z
slug: frontend-src-views-homeview-vue
---
## Design Health Score
Total: 19/40 — Acceptable (significant improvements needed before users are happy)

Nielsen scores: H1=3, H2=2, H3=2, H4=2, H5=2, H6=3, H7=1, H8=2, H9=1, H10=1

## Anti-Patterns Verdict
Three absolute bans active: uppercase eyebrow (오늘의 칼로리), hero-metric template (achievement card), English AI labels ("Progress", "Goal:"). Detector: bounce-easing ×2 (line 98 cubic-bezier elastic, line 420 plan-dot bounce).

## Priority Issues
P0: No mobile layout (grid-cols-12, no sm:/md: breakpoints)
P0: SVG progress circle has no ARIA semantics
P1: English labels "Progress" and "Goal:" in core Korean UI
P1: Nyam character double-instanced, dilutes coach brand signal
P2: 0% empty state presents absence, not invitation

## Persona Red Flags
Jordan: "New!" badge unresponsive, BMR/TDEE jargon in loading, no first-action callout.
Casey: Mobile layout untested, text-[10px] illegible in motion.
Sam: SVG void for screen reader, no aria-label on icon buttons, no label on weight input.
박지수: Macro bars show fill with no target context, English labels break Korean-first feel.

## Minor Observations
text-[10px] at 2 locations. pollForProgram recursive leak risk. Only recommendations[0] used. border-r seam artifact. weightSubmitting "..." not accessible.

## Questions
1. Why does the plan-loading state get more emotional care than the daily 0% empty state?
2. Two visualizations of the same ratio — cut one so the other speaks loudly?
3. What is the rule for when Nyam appears?

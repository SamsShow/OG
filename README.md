# OG

An offline Android gym and physique tracker built around one goal: a wider back, a fuller
upper chest, a V-taper, and less belly fat. Kotlin, Jetpack Compose, Room. No accounts, no
network, no third-party UI libraries.

## What it does

**Train** — 70 exercises across six muscle groups, each declaring the muscles it actually
hits. Log sets, reps and weight; the app compares every lift against its previous session
and tells you whether you beat it.

**Body** — an interactive anatomy diagram, front and back. Muscles light up from what you
logged and fade over the week. Tap one to see when you last trained it and the best
exercises for it. Abs render as eight separate blocks, quads as three heads, biceps and
triceps as two each, so you can see the shape of what a lift hits.

**Fuel** — a fixed four-meal plan hitting ~137 g protein on ~1,820 kcal, with editable
portions, cheaper swaps for tight weeks, and ad-hoc logging for anything eaten off-plan.
The optional second whey scoop only appears when the day genuinely cannot reach the target.

**Stats** — consistency, weekly volume, progressive overload, muscle coverage, and
measurement trends.

## The score

A 0–100 composite: consistency 25, progressive overload 20, protein 20, diet adherence 15,
muscle coverage 10, physique trend 10.

Components without data are **dropped from the denominator** rather than scored as zero, so
a new install reads "not yet graded" instead of a red 0.

## Projections

Weeks-to-goal comes from a least-squares fit on your own measurements — never a formula.
Every path that cannot produce an honest number returns no number plus the reason: fewer
than three entries, less than two weeks of span, a flat trend, or moving away from the
target.

Creatine pulls water into muscle and adds 1–2 kg that is neither fat nor muscle. Readings
inside the four weeks after your start date are drawn as hollow points and **excluded from
the weight trend fit**, because leaving them in makes every downstream estimate read a gain
that did not happen.

## Design

Light theme: lime and deep forest on a pale mint canvas, Plus Jakarta Sans, a floating
glass navigation bar with real backdrop blur on Android 12+.

The palette is validated, not eyeballed. The muscle-heat ramp is one hue, monotone in
lightness, with its lightest step clearing 2:1 on the white card it draws on. Accent and
critical clear 3:1 and separate under simulated colour-vision deficiency (ΔE 8.6 deutan).

Two rules the palette depends on:

- **Lime is a fill, never a data mark.** At 1.29:1 on white it is invisible as a line or
  dot, so it only appears behind dark ink (13.8:1) or on forest (11.0:1).
- **There is no amber warning.** Under deutan, gold and yellow-green converge no matter how
  they are stepped, so the middle state is neutral grey carrying an icon and a word.

## Build

Requires JDK 17 and Android SDK platform 37.

```
./gradlew :app:assembleDebug      # APK at app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest  # 20 unit tests over the analytics
```

CI builds the APK on every push and uploads it as a workflow artifact.

## Tests

The unit tests cover the parts that would fail silently rather than loudly: the
least-squares fit, every refusal path in the projection, score-weight redistribution, the
creatine hold-out, streak counting across rest days, and ad-hoc intake.

## Licence

The bundled typeface, Plus Jakarta Sans, is under the SIL Open Font License 1.1 —
see [`LICENSES/PlusJakartaSans-OFL.txt`](LICENSES/PlusJakartaSans-OFL.txt).

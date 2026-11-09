# 이미지 제작 프롬프트

제작 방식: 내장 이미지 생성 도구. 아래는 에셋별 실제 사용 프롬프트다. 배경은 한 장의 비반복 이미지이며, 시트는 투명 PNG다. 수치·여백 지시는 생성 목표이며 실측 추출 좌표는 atlas.json을 우선한다.

## cave_01_background

```text
Use case: stylized-concept. Production BACKGROUND ONLY for HOOK & RECOIL, side-view 2D grapple/recoil cowboy game. Premium crisp pixel-art cavern, calm low-saturation charcoal/slate-blue, muted teal only in sparse tiny distant mineral accents. Low contrast quiet central 65 percent, no bright bloom, no dramatic shafts, no neon. Natural large continuous geological forms, modest background detail, no texture noise. NO player NO enemies NO UI NO text NO interactive rings NO playable foreground platforms NO visible collectible gems. Orthographic side elevation, NOT isometric. Background rocks must be distinctly distant and lower contrast than gameplay terrain. One continuous nonrepeating backdrop. Stage 1 WAKING CAVERN. Wide 16:9 composition, welcoming broad sheltered chamber opening into greater depth toward upper-right. Rounded layered blue-gray cave arches, faint water reflected on distant lower wall, sparse stalactites, subdued cool fog. Broad negative space in middle, faint warm gray rock lip accents at distant edges. No foreground obstacles.
```

## cave_02_background

```text
Use case: stylized-concept. Production BACKGROUND ONLY for HOOK & RECOIL, side-view 2D grapple/recoil cowboy game. Premium crisp pixel-art cavern, calm low-saturation charcoal/slate-blue, muted teal only in sparse tiny distant mineral accents. Low contrast quiet central 65 percent, no bright bloom, no dramatic shafts, no neon. Natural large continuous geological forms, modest background detail, no texture noise. NO player NO enemies NO UI NO text NO interactive rings NO playable foreground platforms NO visible collectible gems. Orthographic side elevation, NOT isometric. Background rocks must be distinctly distant and lower contrast than gameplay terrain. One continuous nonrepeating backdrop. Stage 2 ECHO GORGE. Wide 16:9 composition. Tall layered cavern ravine with overlapping distant vertical walls and arch bridges, deeper central abyss, subtle receding cool mist and distant thin mineral streaks. Emphasize depth and long gaps, with empty quiet upper middle for grapple chains. No actual ropes or gameplay geometry.
```

## cave_03_background

```text
Use case: stylized-concept. Production BACKGROUND ONLY for HOOK & RECOIL, side-view 2D grapple/recoil cowboy game. Premium crisp pixel-art cavern, calm low-saturation charcoal/slate-blue, muted teal only in sparse tiny distant mineral accents. Low contrast quiet central 65 percent, no bright bloom, no dramatic shafts, no neon. Natural large continuous geological forms, modest background detail, no texture noise. NO player NO enemies NO UI NO text NO interactive rings NO playable foreground platforms NO visible collectible gems. Orthographic side elevation, NOT isometric. Background rocks must be distinctly distant and lower contrast than gameplay terrain. One continuous nonrepeating backdrop. Stage 3 THORN PASSAGE. Wide 16:9 composition. Low compressed cavern tunnel with distant tapered rock teeth along the upper and lower far background; central horizontal corridor kept quiet open and readable. Dark desaturated navy stone, muted gray planes, subtle distant mist. Background pointed forms must remain faint and non-interactive, no bright dangerous spikes in foreground.
```

## cave_04_background

```text
Use case: stylized-concept. Production background-only for Stage4 FRACTURING LEDGES in a calm 2D side view grapple cowboy pixel-art game. Wide 16:9 continuous cavern vista, large dark slate blue-gray strata with occasional broken shelves and distant collapsed arches, dust suspended subtly in empty chamber, thin rock fissures large and few, crumbling geology suggested in FAR BACKGROUND only. Quiet central 65 percent. Low contrast low saturation, sparse tiny muted teal mineral traces and very subtle gray rock edge accents. Moderate background detail, no grain/no busy noise, no bright glow. No characters no UI no text no playable foreground ledges no rings no actual collapsing game platforms. Orthographic side elevation, not isometric. One nonrepeating background image, no atlas.
```

## cave_05_background

```text
Use case: stylized-concept. Production background-only for Stage5 VERTICAL RIFT, the cave chapter final exam of a 2D side view grapple cowboy game. Tall PORTRAIT 9:16 image suitable for a vertically ascending level background, a monumental narrow rift rises through layered dark slate-blue rock walls into faint cool mist at top. Large continuous geological arches and distant fractured pillars recede across depth; keep central climbing corridor quiet, low contrast and free of foreground obstructions. Low saturation charcoal desaturated blue-gray, sparse tiny muted teal mineral seams, no neon, no strong shafts or bloom. Premium crisp pixel-art cavern, restrained background detail, quiet lonely atmosphere. NO player NO UI NO text NO collectible NO rings NO playable platforms, no diagram, not isometric. One continuous portrait backdrop, no panel split.
```

## cave_terrain_kit

```text
Use case: stylized-concept. Actual transparent modular TERRAIN SPRITE KIT for 5 cave stages of a precision 2D SIDE VIEW platformer. 3 columns x 3 rows, 9 separate big shapes. Every entire shape inside central 55 percent of its cell, 22 percent alpha-zero empty margin all four sides, absolutely no touching. Simple premium PIXEL ART with large clean angular rock planes, three desaturated charcoal/slate-blue shades, very tiny sparse gray edge highlights at exposed chips ONLY. No realistic grain/no fine texture/no bloom/no shine. Row1 long solid flat-top rock ledge, medium solid flat-top rock ledge, small narrow landing rock ledge. Row2 tall left-face cave wall, tall right-face cave wall, thick overhead rock slab with clean flat underside. Row3 connected inside L corner lower-left, connected inside L corner lower-right, quiet rectangular solid rock fill slab. Collision surface tops deliberately level and readable. All rock masses continuous no visible square tile dividers or seams inside shapes. Exact orthographic side elevation no perspective top surface, no isometric. Genuine transparent alpha outside rock only. No background no cast shadows no text no grid no decorative crystals. Natural simple contour and coherent stone palette, useful as modular chunks; don't pretend they form seamless autotiles.
```

## cave_stage_devices

```text
Use case: stylized-concept. Production GAMEPLAY SPRITE sheet on true transparent alpha for calm pixel-art 2D cave grapple game. EXACT 4 columns x 3 rows, twelve isolated sprites, each whole object fits central 50 percent cell leaving gigantic 25 percent empty alpha-zero margin every side. 3 flat muted shades per material, simple readable silhouettes, dark slate stone/aged bronze muted teal indicator, tiny POINT highlights only, no grain no busy detail no bloom. Row1: bronze fixed grapple anchor with round ring on small mount; same anchor connected with one tiny ivory indicator; unlit checkpoint lantern on squat stone base; same checkpoint lit flat pale ocher no halo. Row2: floor upward spike cluster pointed silhouette; ceiling downward spike cluster; left-wall horizontal spikes pointing right; right-wall spikes pointing left. Spikes dark gray with thin muted warm ivory tips, distinguish dangerous spikes from decoration. Row3: identical width flat rock BREAKABLE PLATFORM undamaged; same platform with a few large visible cracks warning state; same platform splitting downward into three large fragments collapse; same platform debris fragments below the original top plane gone state. Four collapse sprites same original top line, size and center. Side-view orthographic NO isometric NO background NO labels NO UI NO gem NO ropes baked. Uniform crisp pixel density, no fragments escape safe central cell area.
```

## stage_select_ui_kit

```text
Use case: ui-mockup. Actual reusable GAME UI COMPONENT SPRITE KIT for restrained dark cave cowboy pixel-art platformer. Transparent alpha outside objects, NO text NO numerals NO complete UI screenshot. 3 columns x 3 rows of isolated components, generous 20 percent fully empty alpha margin EVERY side, never touching. Minimal matte charcoal rectangular panels, thin desaturated slate border, tiny worn bronze corner marks at most, muted teal only for selection. Flat sober modern game UI with crisp pixel steps, no ornate fantasy metal frames no gold trim no bloom no gradients no glass. Row1 three IDENTICALLY SIZED HORIZONTAL rectangular stage card plates: normal dark slate, focused subtle pale teal thin outline, locked dim low contrast slate. Blank interiors for runtime thumbnails and text, simple straight repeatable edges for nine-slicing. Row2 three IDENTICALLY SIZED compact horizontal button plates: normal, focused subtle teal border, disabled dim; no letters. Row3: large plain rectangular MAIN WINDOW panel with quiet charcoal center and thin slate border, simple thin inner frame suitable nine-slicing; a small isolated closed padlock ICON; a small isolated checkmark ICON. Every full component fits its own cell with wide clear padding; no backdrop no shadows no icon halos, no lettering. Components are separate practical runtime assets, not a baked menu.
```\n
# 광산 이미지 제작 프롬프트

내장 이미지 생성 도구로 제작한 실제 프롬프트다. 세로 배경은 16:9 플레이 화면에서 상하 스크롤하는 원경이다. 패딩·모양 지시는 생성 목표이며 실제 추출은 atlas.json 좌표를 따른다.

## mine_01

```text
Use case: stylized-concept. Actual BACKGROUND ONLY for HOOK & RECOIL stages6-10, abandoned industrial MINE theme, different from blue natural cave chapter. Tall PORTRAIT 9:16 continuous nonrepeating backdrop for vertical scrolling in a landscape 2D side-view game. Premium crisp pixel art, subdued low saturation, dark umber iron charcoal dusty olive gray and sparse dim amber work lamps. Large readable forms, quiet low-contrast CENTRAL 60 percent for gameplay overlays, moderate background detail but no noisy texture, NO strong bloom NO neon NO bright lava. Structures visibly distant, no playable foreground platforms or hazards. Orthographic SIDE ELEVATION, no isometric floor. NO player NO UI NO text NO collectible gems NO usable grapple rings NO actual game obstacles. Stage6 MINING LIFT SHAFT: deep vertical timber-and-iron reinforced shaft, distant elevator rails on walls, huge dark pulley housings far above, old support braces, dust suspended in narrow amber lamp pools. Depth recedes into dark olive umber, empty middle corridor rises through the image. Mechanically plausible mining supports. No elevator in foreground.
```

## mine_02

```text
Use case: stylized-concept. Actual BACKGROUND ONLY for HOOK & RECOIL stages6-10, abandoned industrial MINE theme, different from blue natural cave chapter. Tall PORTRAIT 9:16 continuous nonrepeating backdrop for vertical scrolling in a landscape 2D side-view game. Premium crisp pixel art, subdued low saturation, dark umber iron charcoal dusty olive gray and sparse dim amber work lamps. Large readable forms, quiet low-contrast CENTRAL 60 percent for gameplay overlays, moderate background detail but no noisy texture, NO strong bloom NO neon NO bright lava. Structures visibly distant, no playable foreground platforms or hazards. Orthographic SIDE ELEVATION, no isometric floor. NO player NO UI NO text NO collectible gems NO usable grapple rings NO actual game obstacles. Stage7 MINECART JUNCTION: tall complex multi-level mine shaft, distant intersecting curved ore rail bridges at several heights, dark steel trestles and timber braces, a few far abandoned cart silhouettes clearly scenery. Muted smoky brown and aged olive steel, dim sparse amber signals, different distinct composition from plain lift shaft. Broad quiet middle for climb-drop-reclimb routing.
```

## mine_03

```text
Use case: stylized-concept. Actual BACKGROUND ONLY for HOOK & RECOIL stages6-10, abandoned industrial MINE theme, different from blue natural cave chapter. Tall PORTRAIT 9:16 continuous nonrepeating backdrop for vertical scrolling in a landscape 2D side-view game. Premium crisp pixel art, subdued low saturation, dark umber iron charcoal dusty olive gray and sparse dim amber work lamps. Large readable forms, quiet low-contrast CENTRAL 60 percent for gameplay overlays, moderate background detail but no noisy texture, NO strong bloom NO neon NO bright lava. Structures visibly distant, no playable foreground platforms or hazards. Orthographic SIDE ELEVATION, no isometric floor. NO player NO UI NO text NO collectible gems NO usable grapple rings NO actual game obstacles. Stage8 CRUSHING PLANT: vertical abandoned heavy ore processing building within mine, massive distant iron crusher housings, thick pipes, piston cylinders, dusty copper-rust and charcoal machinery, several horizontal maintenance catwalks far behind gameplay plane. Warm gray/brown muted machinery and sparse dull warning lamps, no active foreground crushing teeth. Quiet central vertical gap, industrial claustrophobia without visual noise.
```

## mine_04

```text
Use case: stylized-concept. Stage9 OVERHEATED CONVEYORS background-only for tall vertical 2D side-view grapple mine game. PORTRAIT 9:16 continuous nonrepeating industrial backdrop. Large distant inclined conveyor galleries zigzag upward behind playfield, dark rusted steel gantries, abandoned ore hoppers and thick pipes, faint copper red heat reflected only behind distant grilles, no lava no bright fire. Smoky muted burnt umber and desaturated copper against charcoal, a few DIM amber work lamps. Different from blue cave, subtly hotter atmosphere than lift stages while low saturation calm readability stays. Quiet central climbing channel, all structures distant low contrast. Premium crisp pixel art, medium background detail but no noisy grain. NO foreground hazards NO player NO UI NO text NO usable rings NO collectible. Background not an actual playable map. No bloom or bright neon, no isometric ground.
```

## mine_05

```text
Use case: stylized-concept. Stage10 ABANDONED MINE ESCAPE chapter finale BACKGROUND ONLY. Tall PORTRAIT 9:16 for side-view 2D precision grapple game with vertical camera. Monumental broken freight shaft rising from warm dark umber iron at bottom to muted cool gray mist at top, distant damaged rail bridges at several elevations, enormous quiet iron supports and old cables lining sides, sealed mining galleries, a hint of distant upper maintenance opening not daylight surface. Low saturation charcoal aged bronze muted olive steel, tiny amber work lights, no bright shafts no glow. Premium crisp pixel art, restrained geological/industrial detail, broad quiet central vertical route for gameplay overlays. Not natural blue cave theme. No player no foreground moving platform no spikes no UI no labels no gems no usable rings. One continuous nonrepeating background, no split panels, orthographic side elevation.
```

## mine_terrain

```text
Use case: stylized-concept. Production GAME TERRAIN sprite kit on genuine transparent alpha for abandoned industrial mine 2D side-view platformer. EXACT 3 columns x 3 rows, NINE distinct isolated terrain chunks. Each item within central 55 percent of its equal cell, huge alpha-zero empty margins minimum20 percent EVERY side. Simple attractive crisp PIXEL ART, low detail, large matte charcoal iron panels and desaturated brown rock planes, 3 shades per material, tiny sparse warm-gray highlights only on exposed metal edges, no glossy reflections no grain no busy bolts no bloom. Row1 long flat steel maintenance platform with rock underside, medium flat steel landing platform, short narrow landing platform. Row2 tall vertical steel-reinforced wall facing left, matching wall facing right, thick overhead metal beam with flat underside collision plane. Row3 inside lower-left corner continuous rock/steel L, matching lower-right L, plain rectangular rock/iron solid fill slab. Metal trim modest and readable, no yellow hazard stripe clutter. All assets side elevation no perspective top planes, no isometric no shadows outside shapes no labels no grid no background. No separate spikes or machinery baked into terrain. Each whole piece one continuous silhouette and no internal square tile borders.
```

## mine_devices

```text
Use case: stylized-concept. Actual GAMEPLAY MECHANISM SPRITE KIT for 2D side-view abandoned mine grapple/recoil platformer. TRUE TRANSPARENT alpha. EXACT 4 columns x 4 rows, 16 separate sprites, huge transparent gutters. Each whole item including blade tips stays within CENTRAL 50 percent of its cell with25 percent empty alpha-zero margin every side. Simple crisp pixel art, LOW DETAIL large matte shapes, charcoal iron desaturated bronze brown, tiny isolated edge highlights, no grain no shiny textures no bloom. Row1: flat small MINING LIFT platform with small rear winch mount; compact minecart side view with FLAT WALKABLE top lip and small wheels; moving grapple round ring on a compact carriage block; airborne AMMO RECHARGE ring clearly distinct double-ring shape with two small subdued teal inset marks, no letters. Row2: upper ceiling-mounted piston CRUSHER retracted; SAME upper crusher extended down with clear flat toothed plate; lower floor-mounted crusher retracted; SAME lower crusher extended up, consistent plate size and shaft. Row3: circular metal SAW blade phase0 with safe transparent border beyond teeth; SAME saw blade rotated slightly phase1 identical center/size; straight short vertical guide RAIL segment; ground upward mine SPIKES on small base. Row4: ceiling downward spike cluster; leftwall spikes pointing right; rightwall spikes pointing left; small WARNING signal lamp with flat muted amber center, no glow. No big support scenery no full ropes no character no UI no text no frame outlines no background. Every object fully inside its central safe area, no overlaps. Components will have separate code-defined collisions and movement paths.
```\n
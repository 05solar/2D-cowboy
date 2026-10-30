"""파일: BuildCityStages.py
역할: 지하도시 V2의 긴 횡단·상승 구간과 기존 고정 ID 카탈로그를 생성한다.
주요 책임: 얇은 발판, 명시 콜라이더, 카메라 배경 구역, 장치·CP·보석 배치.
협력 대상: assets/city_chapter/stages_patch.json, assets/city_chapter/v2_layout_blueprints.json,
          assets/stages/city_01.json~city_05.json, StageDefinition.
관련 사양: docs/CITY_CHAPTER_V2.md. 이 스크립트는 플레이 통과를 보증하지 않는다.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATCH = json.loads((ROOT / 'assets/city_chapter/stages_patch.json').read_text(encoding='utf-8'))
BLUEPRINTS = json.loads((ROOT / 'assets/city_chapter/v2_layout_blueprints.json').read_text(encoding='utf-8'))
CATALOG_PATH = ROOT / 'assets/cave_chapter/stages.json'
catalog = json.loads(CATALOG_PATH.read_text(encoding='utf-8'))
MODULE_COUNTS = {10 + int(x['stageId'][-2:]): len(x['modules']) for x in BLUEPRINTS['stages']}
LENGTH_TARGETS = {10 + int(x['stageId'][-2:]): x['routeLengthTargetH'] for x in BLUEPRINTS['stages']}
ACTION_TARGETS = {11: 8, 12: 9, 13: 10, 14: 11, 15: 12}
THICKNESS_H = 0.16
ROW_X = [3.3, 8.2, 13.1, 18.0, 22.9]
ROW_Y_OFFSETS = [0.0, 0.32, -0.18, 0.42, 0.05]


def point(x, y):
    return {'x': round(x, 3), 'y': round(y, 3)}


def platform(center_x, top_y, width, sprite, one_way=None, rope_stops=True, crumble=False):
    if one_way is None:
        one_way = sprite in {'one_way', 'moving_horizontal', 'moving_vertical'}
    return {
        'x': round(center_x - width / 2, 3), 'y': round(top_y - THICKNESS_H, 3),
        'width': round(width, 3), 'height': THICKNESS_H, 'sprite': sprite,
        'oneWay': one_way, 'ropeStops': rope_stops, 'crumble': crumble,
    }


def ceiling(anchor_x, anchor_y, index):
    return {
        'x': round(anchor_x - 0.72, 3), 'y': round(anchor_y + 0.15, 3),
        'width': 1.44, 'height': 0.18, 'oneWay': False, 'ropeStops': True, 'collision': False,
        'sprite': 'long_truss',
    }


def add_anchor(x, y, index, ceilings, anchors):
    anchors.append(point(x, y))
    ceilings.append(ceiling(x, y, index))


def stage_background_regions(number, module_count, height, width):
    city_index = number - 10
    horizontal = f'assets/backgrounds/city_{city_index:02d}_horizontal.png'
    vertical = f'assets/backgrounds/city_{city_index:02d}.png'
    result = []
    for module in range(module_count):
        lane_y = 2.0 + module * 4.65
        result.append({
            'id': f'city_{number:02d}.bg.horizontal.{module + 1:02d}', 'variant': 'horizontal',
            'path': horizontal, 'minX': 0.0, 'maxX': width,
            'minY': lane_y - 2.2, 'maxY': lane_y + 2.5, 'priority': 0,
        })
        if module + 1 < module_count:
            right_to_left = module % 2 == 0
            # The background follows the camera center, which clamps one viewport half-width inside the map edge.
            transition_x = (19.9 if right_to_left else 7.1) + module * 0.06
            result.append({
                'id': f'city_{number:02d}.bg.vertical.{module + 1:02d}', 'variant': 'vertical',
                'path': vertical, 'minX': transition_x - 2.4, 'maxX': transition_x + 2.4,
                'minY': lane_y + 1.7, 'maxY': lane_y + 7.5, 'priority': 10,
            })
    return result


def build(meta):
    number = meta['number']
    stage_id = meta['id']
    module_count = MODULE_COUNTS[number]
    width = 27.0
    platforms = []
    anchors = []
    ceilings = []
    checkpoints = []
    hazards = []
    movers = []
    wind = []
    barriers = []
    structures = []
    recharge = []
    saws = []
    crushers = []
    lane_platform_indices = []
    anchor_index = 0

    for module in range(module_count):
        lane_top = 2.0 + module * 4.65
        drift = module * 0.06
        centers = [x + drift for x in (ROW_X if module % 2 == 0 else list(reversed(ROW_X)))]
        narrow_width = [1.5, 1.4, 1.3, 1.2, 1.1][number - 11]
        widths = [3.0, 1.8, narrow_width if module == module_count - 3 else 1.8, 1.8, 3.0]
        sprites = []
        for j in range(5):
            if j in (0, 4):
                options = [('concrete_long', 3.0), ('one_way', 3.0), ('grating_long', 2.7)]
                sprite, widths[j] = options[(module + j) % len(options)]
            elif j == 2:
                sprite = 'concrete_medium'
            elif (module + j) % 2:
                sprite, widths[j] = 'concrete_medium', 1.8
            else:
                sprite, widths[j] = 'grating_medium', 1.6
            sprites.append(sprite)
        row_indices = []
        tops = [lane_top + offset for offset in ROW_Y_OFFSETS]
        for j, (center_x, top_y) in enumerate(zip(centers, tops)):
            sprite = sprites[j]
            if j == 2 and number >= 14:
                sprite = 'breakable_cracked'
            row_indices.append(len(platforms))
            solid_landing = (module == 0 and j == 0) or (module == module_count - 1 and j == 4)
            platforms.append(platform(center_x, top_y, widths[j], sprite, one_way=not solid_landing,
                                      crumble=(j == 2 and number >= 14 and module == module_count - 2)))
        lane_platform_indices.append(row_indices)

        direction = 1 if module % 2 == 0 else -1
        for j in range(4):
            x = (centers[j] + centers[j + 1]) * 0.5
            y = max(tops[j], tops[j + 1]) + 2.35
            add_anchor(x, y, anchor_index, ceilings, anchors)
            anchor_index += 1
        if module + 1 < module_count:
            x = centers[-1]
            y = lane_top + 3.4
            add_anchor(x - (0.5 if direction > 0 else -0.5), y, anchor_index, ceilings, anchors)
            anchor_index += 1

        if module in (2, 5) and number >= 13:
            # A visible perimeter hazard adds route pressure without sealing the vertical lane.
            hazard_x = 0.8
            hazard_top = tops[2]
            hazards.append({'x': round(hazard_x, 3), 'y': round(hazard_top - 0.48, 3),
                            'width': 1.36, 'height': 0.48, 'direction': 'floor'})
        if number >= 14 and module == module_count - 2:
            index = row_indices[2]
            platforms[index]['crumble'] = True

    for checkpoint_module in ([1, 4] if number < 15 else [4]):
        row = lane_platform_indices[checkpoint_module]
        index = row[-1]
        block = platforms[index]
        checkpoints.append(point(block['x'] + block['width'] * 0.5, block['y'] + block['height']))

    # Moving surfaces use thin platform artwork as their only walkable body.
    moving_modules = [1] if number == 11 else [1, 4] if number < 14 else [2, module_count - 2]
    for mover_number, module in enumerate(moving_modules):
        index = lane_platform_indices[module][2]
        block = platforms[index]
        moving_sprite = 'moving_horizontal' if mover_number % 2 == 0 else 'moving_vertical'
        center_x = block['x'] + block['width'] * .5
        moving_width = 1.85 if mover_number % 2 == 0 else 1.7
        block['sprite'] = moving_sprite
        block['width'] = moving_width
        block['x'] = round(center_x - moving_width * .5, 3)
        block['crumble'] = False
        mover = {
            'id': f'{stage_id}.mover.{mover_number + 1:02d}', 'kind': 'city_platform', 'platform': index,
            'fromX': block['x'], 'fromY': block['y'], 'toX': round(block['x'] + (1.15 if module % 2 == 0 else -1.15), 3),
            'toY': block['y'] if mover_number % 2 == 0 else round(block['y'] + 1.0, 3),
            'period': round(4.8 + number * 0.06, 2), 'phase': round(mover_number * 0.37, 2),
        }
        if number >= 14 and mover_number == 0:
            x, y = mover['fromX'], mover['fromY']
            mover['toX'], mover['toY'] = round(x + 1.2, 3), round(y + 1.2, 3)
            mover['waypoints'] = [[x, y], [round(x + 1.2, 3), y],
                                  [round(x + 1.2, 3), round(y + 1.2, 3)], [x, round(y + 1.2, 3)]]
            mover['travelSeconds'], mover['holdSeconds'] = 1.8, 0.35
        movers.append(mover)

    if number >= 12:
        for module in range(1, module_count, 3):
            row = lane_platform_indices[module]
            first = platforms[row[0]]
            wind.append({'id': f'{stage_id}.wind.{module:02d}', 'x': 0.8, 'y': first['y'] + 0.2,
                         'width': 8.0, 'height': 4.5, 'ax': 3.0 if module % 2 == 0 else 0, 'ay': 8.0 if module % 2 else 0})
    if number >= 13:
        module = min(2, module_count - 1)
        a, b = platforms[lane_platform_indices[module][1]], platforms[lane_platform_indices[module][2]]
        barriers.append({'id': f'{stage_id}.barrier.01', 'x': round((a['x'] + a['width'] + b['x']) * 0.5, 3),
                         'y': round(a['y'] + 0.25, 3), 'width': 0.14, 'height': 3.0,
                         'offSeconds': 1.8, 'warningSeconds': 0.5, 'onSeconds': 1.5, 'phase': 0.9})
        recharge.append({'id': f'{stage_id}.recharge.01', 'x': 13.0, 'y': 2.0 + (module + 0.6) * 4.65,
                         'halfWidth': 0.35, 'halfHeight': 0.4})
    if number >= 14:
        module = min(3, module_count - 1)
        row = lane_platform_indices[module]
        block = platforms[row[2]]
        crushers.append({'id': f'{stage_id}.crusher.01', 'direction': 'down', 'x': block['x'] + block['width'] + 0.5,
                         'topY': block['y'] + 4.2, 'width': 1.0, 'stroke': 2.0, 'period': 3.6,
                         'phase': 0.28 if number == 15 else 0.05})
        if number == 15:
            saw_y = 2.0 + (module + 1.05) * 4.65
            saws.append({'id': f'{stage_id}.saw.01', 'fromX': 7.0, 'fromY': saw_y,
                         'toX': 19.0, 'toY': saw_y, 'radius': 0.48, 'period': 4.2, 'phase': 0.36})

    for module in range(0, module_count, 2):
        row = lane_platform_indices[module]
        structures.append({
            'id': f'{stage_id}.frame.{module + 1:02d}', 'sprite': ['overhead_arch', 'u_frame', 'window_frame', 'z_step_frame'][module % 4],
            'x': 8.8, 'y': platforms[row[1]]['y'] + 0.35, 'width': 7.0, 'height': 2.5,
            'collisionMode': 'none', 'colliders': [],
        })

    first = platforms[lane_platform_indices[0][0]]
    final = platforms[lane_platform_indices[-1][-1]]
    gem_top = final['y'] + final['height']
    route_target = LENGTH_TARGETS[number]
    stage_data = {
        'id': stage_id, 'name': meta['name'], 'width': width,
        'height': round(2.0 + (module_count - 1) * 4.65 + 8.0, 2), 'deathPlaneY': 0.75,
        'spawnX': round(first['x'] + first['width'] * 0.5, 3), 'spawnY': round(first['y'] + first['height'], 3),
        'exitX': round(final['x'] + final['width'] * 0.5, 3), 'platforms': platforms,
        'ceilings': ceilings, 'anchors': anchors, 'checkpoints': checkpoints, 'optionalGems': [],
        'clearGem': {'id': f'{stage_id}.clear_gem', 'type': 'clear_gem',
                     'x': round(final['x'] + final['width'] * 0.5, 3), 'y': round(gem_top + 0.45, 3),
                     'halfWidth': 0.18, 'halfHeight': 0.22},
        'hazards': hazards, 'mineMovers': movers, 'mineCrushers': crushers, 'mineSaws': saws,
        'rechargeRings': recharge, 'cityWindZones': wind, 'cityBarriers': barriers,
        'cityStructures': structures,
        'cityBackgroundRegions': stage_background_regions(number, module_count, 2.0 + module_count * 4.65, width),
        'routeDesign': {'moduleCount': module_count, 'routeLengthTargetH': route_target,
                        'lengthMeasured': False, 'targetCoreChainActions': ACTION_TARGETS[number]},
        'zones': [{'x': 0, 'title': meta['name'], 'hint': '', 'detail': ''}],
    }
    (ROOT / 'assets/stages' / f'{stage_id}.json').write_text(
        json.dumps(stage_data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


for meta in PATCH['stages']:
    entry = next((e for e in catalog['stages'] if e['id'] == meta['id']), None)
    if entry is None:
        raise ValueError(f'Missing fixed stage ID: {meta["id"]}')
    entry.update(meta)
    entry['available'] = True  # User requested all implemented/test stages remain unlocked for test play.
    number = meta['number']
    entry['background'] = f'assets/backgrounds/city_{number - 10:02d}.png'
    entry['backgroundRepeat'] = False
    entry['backgroundVariants'] = {
        'horizontal': f'assets/backgrounds/city_{number - 10:02d}_horizontal.png',
        'vertical': f'assets/backgrounds/city_{number - 10:02d}.png',
        'repeat': False,
    }
    entry['routeLengthTargetH'] = LENGTH_TARGETS[number]
    entry['targetModules'] = MODULE_COUNTS[number]
    entry['routeLengthMeasured'] = False
    build(meta)

CATALOG_PATH.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Built V2 city stage layouts with thin one-way surfaces; route targets remain unmeasured and unverified.')

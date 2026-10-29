"""파일: BuildCityStages.py
역할: 지하도시 11~15의 초기 정적 맵 데이터와 ID 카탈로그 병합을 생성한다.
주요 책임: 스폰·발판·천장 링·체크포인트·보석 배치를 JSON에 기록하고 원본 20개 카탈로그를 보존한다.
협력 대상: assets/city_chapter/stages_patch.json, assets/stages, StageDefinition.
관련 사양: docs/assets/city_chapter/STAGES_11_15.md. 초기 배치는 클리어 검증이 아니다.
"""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PATCH=json.loads((ROOT/'assets/city_chapter/stages_patch.json').read_text(encoding='utf-8'))
CATALOG_PATH=ROOT/'assets/cave_chapter/stages.json'
catalog=json.loads(CATALOG_PATH.read_text(encoding='utf-8'))
BLOCKS=json.loads((ROOT/'assets/city_chapter/blocks.json').read_text(encoding='utf-8'))['colliders']

def platform(center,top,width,sprite):
    return dict(x=round(center-width/2,3),y=round(top-.55,3),width=width,height=.55,
                sprite=sprite,crumble=False)

def point(x,y):return dict(x=round(x,3),y=round(y,3))

def structure(sprite,x,y,width,height):
    return dict(id=sprite+'_'+str(round(x,2)),sprite=sprite,x=x,y=y,width=width,height=height,
                colliders=BLOCKS[sprite])

def build(meta):
    number=meta['number']
    meta_id=meta['id']
    # Authored alternating ledges provide an inspectable vertical prototype route;
    # later playtests must replace this scaffold with section-by-section tuning.
    count=11 if number<15 else 13
    tops=[2.0+i*(2.0+(number-11)*.055) for i in range(count)]
    # Each ascent drifts sideways so no repeated ledge forms a vertical jump shaft.
    centers=[3.4+(2.7 if i%2 else 0.0)+1.6*(i//2) for i in range(count)]
    centers[-1]=round(centers[-2]+2.2,3)
    widths=[3.3]+[max(1.7,2.25-(number-11)*.08)]*(count-2)+[4.5]
    sprites=['platform_long' if w>3.5 else 'platform_medium' if w>2.3 else 'platform_small' for w in widths]
    blocks=[platform(x,y,w,s) for x,y,w,s in zip(centers,tops,widths,sprites)]
    checkpoints=[3,7] if number<15 else [6]
    cps=[point(centers[i],tops[i]) for i in checkpoints]
    ceilings=[];anchors=[]
    for i in range(1,count-1):
        # Every authored anchor sits below an explicit overhead beam for grapple validation.
        # Keep the low beam opposite the next ledge so it teaches line blocking
        # without sealing the authored upward route from the current perch.
        nextMovesRight=centers[i+1]>centers[i]
        x=centers[i]+(-1.8 if nextMovesRight else 1.8); y=tops[i]+1.9
        ceilings.append(dict(x=round(x-.75,3),y=round(y+.28,3),width=1.5,height=.35,crumble=False))
        anchors.append(point(x,y))
    movers=[];wind=[];barriers=[];structures=[];rings=[]
    def mover(platform_index,target_x,target_y,period,phase=0,kind='city_platform'):
        block=blocks[platform_index]
        movers.append(dict(id=f'{meta_id}.mover.{len(movers)+1:02d}',kind=kind,platform=platform_index,
            fromX=block['x'],fromY=block['y'],toX=round(target_x,3),toY=round(target_y,3),period=period,phase=phase))
    if number==11:
        mover(5,blocks[5]['x']+1.55,blocks[5]['y'],4.6,.12)
        structures.append(structure('u_block',17.5,7.5,2.2,2.1))
    elif number==12:
        mover(5,blocks[5]['x'],blocks[5]['y']+1.0,5.2,.18)
        mover(8,blocks[8]['x']-.6,blocks[8]['y'],4.9,.43)
        wind.append(dict(id=meta_id+'.wind.updraft',x=3.4,y=6.1,width=6.2,height=7.2,ax=0,ay=8))
        structures.append(structure('window',17.5,11.0,2.5,2.0))
    elif number==13:
        mover(5,blocks[5]['x']+1.35,blocks[5]['y'],4.8,.12)
        mover(8,blocks[8]['x'],blocks[8]['y']+2.2,5.4,.38)
        wind.append(dict(id=meta_id+'.wind.cross',x=2.2,y=11.7,width=5.2,height=2.4,ax=3,ay=0))
        barriers.append(dict(id=meta_id+'.barrier.01',x=round((centers[3]+centers[4])*.5,3),y=9.2,width=.16,height=3.8,offSeconds=1.8,warningSeconds=.5,onSeconds=1.5,phase=.9))
        structures.append(structure('arch',17.5,9.8,2.6,2.0))
        rings.append(dict(id=meta_id+'.recharge.01',x=centers[7],y=tops[7]+1.2,halfWidth=.35,halfHeight=.4))
    elif number==14:
        blocks[2]['crumble']=True
        mover(5,blocks[5]['x']+1.5,blocks[5]['y'],5.1,.21)
        moving=structure('l_right',17.5,10.0,2.6,2.4);structures.append(moving)
        movers.append(dict(id=meta_id+'.mover.l_block',kind='city_structure',structure=0,
            fromX=moving['x'],fromY=moving['y'],toX=moving['x']+1.8,toY=moving['y']+1.6,
            period=7.2,phase=.2,waypoints=[[moving['x'],moving['y']],[moving['x']+1.8,moving['y']],
            [moving['x']+1.8,moving['y']+1.6],[moving['x'],moving['y']+1.6]],travelSeconds=1.8,holdSeconds=.35))
        structures.append(structure('t_block',17.5,17.0,2.6,2.4))
        barriers.append(dict(id=meta_id+'.barrier.01',x=round((centers[6]+centers[7])*.5,3),y=16.4,width=.16,height=3.8,offSeconds=1.8,warningSeconds=.5,onSeconds=1.5,phase=1.6))
    else:
        blocks[3]['crumble']=True
        mover(4,blocks[4]['x'],blocks[4]['y']+2.6,5.4,.18)
        mover(8,blocks[8]['x']+1.7,blocks[8]['y'],5.0,.53)
        structures.append(structure('l_left',17.5,19.5,2.8,2.5))
        wind.extend([dict(id=meta_id+'.wind.updraft',x=3.4,y=6.0,width=6.2,height=7.2,ax=0,ay=8),
                     dict(id=meta_id+'.wind.cross',x=7.0,y=18.0,width=5.5,height=2.3,ax=3,ay=0)])
        barriers.extend([dict(id=meta_id+'.barrier.01',x=round((centers[3]+centers[4])*.5,3),y=10.0,width=.16,height=3.8,offSeconds=1.8,warningSeconds=.5,onSeconds=1.5,phase=2.0),
                         dict(id=meta_id+'.barrier.02',x=round((centers[8]+centers[9])*.5,3),y=20.1,width=.16,height=3.8,offSeconds=1.8,warningSeconds=.5,onSeconds=1.5,phase=2.3)])
        rings.append(dict(id=meta_id+'.recharge.01',x=centers[8],y=tops[8]+1.2,halfWidth=.35,halfHeight=.4))
    data=dict(id=meta_id,name=meta['name'],width=22.0,height=round(tops[-1]+7,2),deathPlaneY=0.75,
        spawnX=centers[0],spawnY=tops[0],exitX=centers[-1],platforms=blocks,ceilings=ceilings,
        anchors=anchors,checkpoints=cps,optionalGems=[],
        clearGem=dict(id=meta_id+'.clear_gem',type='clear_gem',x=centers[-1],y=tops[-1]+.48,halfWidth=.12,halfHeight=.19),
        hazards=[],mineMovers=movers,mineCrushers=[],mineSaws=[],rechargeRings=rings,
        cityWindZones=wind,cityBarriers=barriers,cityStructures=structures,
        zones=[dict(x=0,title=meta['name'],hint='',detail='')])
    (ROOT/'assets/stages'/f'{meta_id}.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

for meta in PATCH['stages']:
    entry=next((e for e in catalog['stages'] if e['id']==meta['id']),None)
    if entry is None:raise ValueError(f"Missing fixed stage ID: {meta['id']}")
    entry.update(meta)
    entry['available']=True
    build(meta)
CATALOG_PATH.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Built city_01..city_05 playable data; route diagnostics remain in verifyCity.')

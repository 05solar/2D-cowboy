"""파일: BuildMineStages.py
역할: 광산 6~10 정적 배치와 카탈로그 병합을 생성한다.
주요 책임: 실제 충돌 좌표·장치 경로·ID 선행 조건을 JSON으로 기록한다.
협력 대상: mine_chapter/stages_patch.json, StageDefinition, MineDeviceRuntime.
관련 사양: docs/assets/mine_chapter/STAGES_06_10.md.
"""
import argparse
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
patch=json.loads((ROOT/'assets/mine_chapter/stages_patch.json').read_text(encoding='utf-8'))
catalog=json.loads((ROOT/'assets/cave_chapter/stages.json').read_text(encoding='utf-8'))
terrain=json.loads((ROOT/'assets/mine_chapter/atlas.json').read_text(encoding='utf-8'))['sheets']['mine_terrain']['sprites']
device_sprites=json.loads((ROOT/'assets/mine_chapter/atlas.json').read_text(encoding='utf-8'))['sheets']['mine_devices']['sprites']

def platform(center,top,width,sprite=None):
    if sprite is None:sprite='platform_long' if width>=3.1 else 'platform_medium' if width>=2.4 else 'platform_small'
    rect=terrain[sprite]['rect'];profile_px=terrain[sprite]['surface_profile_px']
    profile=[-width*px/rect[2] for px in profile_px]
    return dict(x=round(center-width/2,3),y=round(top-.8,3),width=width,height=.8,
        surfaceProfile=[round(v,5) for v in profile],sprite=sprite,oneWay=True,ropeStops=True,crumble=False)

def center_surface_offset(block):
    profile=block['surfaceProfile'];mid=len(profile)//2
    return (profile[mid-1]+profile[mid])*.5 if len(profile)>1 else profile[0]

def ceiling(x,y,width):return dict(x=x,y=y,width=width,height=.5,crumble=False)
def point(x,y):return dict(x=x,y=y)
def spike(x,y,w,h,direction):return dict(x=x,y=y,width=w,height=h,direction=direction)

def build(number,tops,centers,widths,kind,height,checkpoints,movers=None,crushers=None,saws=None,rings=None,hazards=None,ceilings=None,anchors=None,checkpoint_xs=None,extensions=None,exclude_indices=None):
    meta=next(x for x in patch['stages'] if x['number']==number)
    blocks=[platform(x,y,w) for i,(x,y,w) in enumerate(zip(centers,tops,widths)) if i not in (exclude_indices or set())]
    for center,top,width in extensions or []:
        blocks.append(platform(center,top,width))
    for m in movers or []:
        if m.get('platform',-1)>=0:
            b=blocks[m['platform']]
            terrain_surface_offset=center_surface_offset(b)
            device=device_sprites['lift' if m['kind']=='lift' else 'minecart']
            sprite_width=device['rect'][2]
            device_profile=[-b['width']*px/sprite_width for px in device['surface_profile_px']]
            device_surface_offset=center_surface_offset({'surfaceProfile':device_profile})
            baseline_shift=terrain_surface_offset-device_surface_offset
            m['fromY']=round(m['fromY']+baseline_shift,3);m['toY']=round(m['toY']+baseline_shift,3)
            b['x']=m['fromX'];b['y']=m['fromY']
            b['surfaceProfile']=[round(v,5) for v in device_profile]
    checkpoint_xs=checkpoint_xs or {}
    cpoints=[]
    for i in checkpoints:
        block=blocks[i]
        cx=checkpoint_xs.get(i,block['x']+block['width']*.5)
        cpoints.append(point(cx,block['y']+block['height']+center_surface_offset(block)))
    goal_x=(extensions[-1][0] if extensions else centers[-1])
    goal_y=blocks[-1]['y']+blocks[-1]['height']+center_surface_offset(blocks[-1])+.48
    stage_width=max(18,max(b['x']+b['width'] for b in blocks)+1)
    barriers=[]
    if number>=8:
        # 경로 안에서 사용할 수 있는 천장 링을 각 장거리 구간 위에 둔다.
        anchors=[];ceilings=[]
        for i in range(2,len(blocks)-2,3):
            b=blocks[i];cx=b['x']+b['width']*.5;cy=b['y']+b['height']+4.5
            ceilings.append(ceiling(cx-.7,cy,1.4));anchors.append(point(cx,cy-.4))
    data=dict(id=meta['id'],name=meta['name'],width=stage_width,height=height,deathPlaneY=1,
        spawnX=centers[0],spawnY=tops[0]+center_surface_offset(blocks[0]),exitX=goal_x,platforms=blocks,ceilings=ceilings or [],
        anchors=anchors or [],checkpoints=cpoints,clearGem=dict(id=meta['id']+'.clear_gem',type='clear_gem',
        x=goal_x,y=goal_y,halfWidth=.0716,halfHeight=.15),optionalGems=[],hazards=hazards or [],
        mineMovers=movers or [],mineCrushers=crushers or [],mineSaws=saws or [],cityBarriers=barriers,rechargeRings=rings or [],
        zones=[dict(x=0,title=f"{meta['number']:02d}  {meta['name']}",hint=meta['objective'],
        detail='A/D 이동 · SPACE 점프 · 우클릭 천장 링 · 좌클릭 반동 · M 스테이지 선택')])
    data.update(json.loads((ROOT/'assets/stages/mine_obstacles.json').read_text(encoding='utf-8'))[meta['id']])
    (ROOT/'assets/stages'/f"{meta['id']}.json").write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def add_anchor(x,y,rail_x,rail_y,rail_w,mover=None):
    return [ceiling(rail_x,rail_y,rail_w)],[point(x,y)],[mover] if mover else []

# Each mine stage now has a distinct silhouette and a deliberately larger obstacle set.
# Stage 6 teaches the lift on a broad zigzag; 7 makes carts cross a wider S route.
ce,an,mv=add_anchor(13.0,12.0,12.0,12.4,3.0,dict(id='lift_ring',kind='anchor',anchor=0,fromX=13.0,fromY=12.0,toX=16.0,toY=13.2,period=5.4))
build(6,[2.0,2.0,3.0,4.1,5.0,6.1,7.2,8.2],[3.0,6.0,9.4,13.0,15.0,18.0,21.5,25.0],[5.2,2.5,2.8,2.2,3.0,3.0,2.6,4.5],
      'lift',22,[3,5],[dict(id='lift_01',kind='lift',platform=1,fromX=4.75,fromY=1.2,toX=4.75,toY=3.7,period=7.2)]+
      mv,ceilings=ce,anchors=an,checkpoint_xs={5:18.0},extensions=[(28.5,8.7,3.0),(32.5,9.3,4.5)])

# Stage 7 crosses the shaft twice with a moving minecart; hazards come from mine_obstacles.json.
ce,an,mv=add_anchor(15.0,13.0,14.0,13.4,3.2,dict(id='cart_ring',kind='anchor',anchor=0,fromX=15.0,fromY=13.0,toX=17.0,toY=14.0,period=4.8))
build(7,[2.0,2.0,3.0,4.2,5.2,6.2,7.2,7.2,7.2],[3.0,6.0,10.0,13.0,15.0,18.5,19.0,16.7,14.0],[5.2,2.6,2.3,3.2,3.2,3.0,2.4,3.4,4.2],
      'cart',22,[4,7],[dict(id='cart_01',kind='cart',platform=1,fromX=4.7,fromY=1.2,toX=5.5,toY=1.2,period=4.8)]+mv,
      ceilings=ce,anchors=an,checkpoint_xs={4:15.0,7:14.0},
      # 6번보다 클리어 시간이 짧아지는 문제를 막기 위해 보석 앞 안전 구간을 2칸 더 늘렸다(위험물 없음).
      extensions=[(17.5,7.2,3.0),(21.5,7.6,4.5),(25.3,8.0,3.0),(29.1,8.4,4.5),(35.3,8.9,5.8)])

# Horizontal crusher gallery, broken saw islands, and a tall switchback finale.
build(8,[2,2.7,3.0,4,3.5,4.5,4.7,5.5,6.5],
      [3,6.5,10.5,15,19,24,28.5,33,37],
      [5.2,3.2,2.3,3.8,2.6,3.2,2.4,3.4,4.5],'crusher',13,[3,6])
build(9,[2,3,4,5,6.2,7.4,9,10.2,11.3,12.3],
      [3,6,9,13,17,21,18,14,18,23],
      [5.2,3.1,2.3,3.4,2.4,3.2,2.7,3.4,2.5,4.5],'saw',18,[3,7],
      # 8번보다 클리어 시간이 짧아지는 문제를 막기 위해 보석 앞 안전 구간을 4칸 늘렸다(위험물 없음).
      extensions=[(27.0,13.6,3.2),(31.0,14.7,2.6),(35.5,15.8,3.2),(40.0,17.0,2.6),(44.2,17.0,3.4)])
build(10,[2,3.2,4.4,5.6,6.8,8,9.2,10.4,11.6,12.8,14,15.2,16.4,17.6],
      [3,6.5,10,13,16,19,22,25,28,31,34,37,40,43],
      [5.2,3.2,2.3,3.4,2.5,3.3,2.4,3.2,2.6,3.3,2.4,3.2,2.5,4.5],'escape',26,[7])

routes={
 'mine_01':[dict(platform=1,waitTop=4.4),dict(platform=2),dict(platform=3),dict(platform=4),dict(platform=5),dict(platform=6),dict(platform=7),dict(platform=8),dict(platform=9)],
 'mine_02':[dict(platform=1,waitX=5.4),dict(platform=2),dict(platform=3),dict(platform=4),dict(platform=5),dict(platform=6,jump=True,waitX=9.8),dict(platform=7),dict(platform=8),dict(platform=9),dict(platform=10),dict(platform=11),dict(platform=12),dict(platform=13)],
 'mine_03':[dict(platform=i,approachEdge=True) for i in [1,2,3,4,5,6,7,8]],
 'mine_04':[dict(platform=i,approachEdge=True) for i in range(1,15)],
 'mine_05':[dict(platform=i,approachEdge=True) for i in range(1,14)]
}
for step in routes['mine_01']:step['approachEdge']=True
for stage_id,targets in [('mine_04',{2,4,5}),('mine_05',{3,6,9})]:
    for step in routes[stage_id]:
        if step['platform'] in targets:step['takeoffInset']=1.9 if stage_id=='mine_05' else 1.0
(ROOT/'assets/stages/mine_routes.json').write_text(json.dumps(routes,indent=2)+'\n',encoding='utf-8')

parser=argparse.ArgumentParser()
parser.add_argument('--activate',action='store_true',help='Make verified mine maps playable in the stage selector')
args=parser.parse_args()
for m in patch['stages']:
    entry=next(e for e in catalog['stages'] if e['id']==m['id'])
    entry.update(m)
    if args.activate:entry['available']=True
    entry.pop('artAvailable',None)
(ROOT/'assets/cave_chapter/stages.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f"Built mine_01..mine_05; selectable={args.activate}")

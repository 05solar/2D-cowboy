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
        surfaceProfile=[round(v,5) for v in profile],sprite=sprite,crumble=False)

def center_surface_offset(block):
    profile=block['surfaceProfile'];mid=len(profile)//2
    return (profile[mid-1]+profile[mid])*.5 if len(profile)>1 else profile[0]

def ceiling(x,y,width):return dict(x=x,y=y,width=width,height=.5,crumble=False)
def point(x,y):return dict(x=x,y=y)
def spike(x,y,w,h,direction):return dict(x=x,y=y,width=w,height=h,direction=direction)

def build(number,tops,centers,widths,kind,height,checkpoints,movers=None,crushers=None,saws=None,rings=None,hazards=None,ceilings=None,anchors=None):
    meta=next(x for x in patch['stages'] if x['number']==number)
    blocks=[platform(x,y,w) for x,y,w in zip(centers,tops,widths)]
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
    cpoints=[point(centers[i],tops[i]+center_surface_offset(blocks[i])) for i in checkpoints]
    goal_x=centers[-1]
    stage_width=max(18,max(x+w*.5 for x,w in zip(centers,widths))+1)
    data=dict(id=meta['id'],name=meta['name'],width=stage_width,height=height,deathPlaneY=1,
        spawnX=centers[0],spawnY=tops[0]+center_surface_offset(blocks[0]),exitX=goal_x,platforms=blocks,ceilings=ceilings or [],
        anchors=anchors or [],checkpoints=cpoints,clearGem=dict(id=meta['id']+'.clear_gem',type='clear_gem',
        x=goal_x,y=tops[-1]+center_surface_offset(blocks[-1])+.48,halfWidth=.0716,halfHeight=.15),optionalGems=[],hazards=hazards or [],
        mineMovers=movers or [],mineCrushers=crushers or [],mineSaws=saws or [],rechargeRings=rings or [],
        zones=[dict(x=0,title=f"{meta['number']:02d}  {meta['name']}",hint=meta['objective'],
        detail='A/D 이동 · SPACE 점프 · 우클릭 천장 링 · 좌클릭 반동 · M 스테이지 선택')])
    (ROOT/'assets/stages'/f"{meta['id']}.json").write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def add_anchor(x,y,rail_x,rail_y,rail_w,mover=None):
    return [ceiling(rail_x,rail_y,rail_w)],[point(x,y)],[mover] if mover else []

# Each mine stage now has a distinct silhouette and a deliberately larger obstacle set.
# Stage 6 teaches the lift on a broad zigzag; 7 makes carts cross a wider S route.
ce,an,mv=add_anchor(11.2,11.8,10.2,12.2,3.0,dict(id='lift_ring',kind='anchor',anchor=0,fromX=11.2,fromY=11.8,toX=14.0,toY=13.4,period=5.4))
build(6,[2.0,2.0,4.2,6.0,7.8,9.6,12.0,14.7],[3.0,6.0,9.0,9.0,9.0,9.0,11.0,14.0],[5.2,2.5,3.2,2.1,2.2,2.0,2.4,4.5],
      'lift',22,[3,6],[dict(id='lift_01',kind='lift',platform=1,fromX=4.75,fromY=1.2,toX=4.75,toY=3.7,period=7.2)]+
      mv,ceilings=ce,anchors=an,hazards=[spike(10.85,4.15,.55,.42,'floor')])

# Stage 7 crosses the shaft twice with moving minecarts and adds three readable spike groups.
ce,an,mv=add_anchor(12.6,13.0,11.0,13.4,3.2,dict(id='cart_ring',kind='anchor',anchor=0,fromX=12.6,fromY=13.0,toX=15.2,toY=14.1,period=4.8))
build(7,[2.0,2.0,4.7,7.2,9.4,11.6,11.6,14.2,17.0],[3.0,6.0,10.0,14.0,11.0,7.0,9.0,12.0,15.0],[5.2,2.5,2.4,2.0,2.2,2.0,2.4,2.0,4.2],
      'cart',22,[4,7],[dict(id='cart_01',kind='cart',platform=1,fromX=4.75,fromY=1.2,toX=7.25,toY=1.2,period=4.8),
      dict(id='cart_02',kind='cart',platform=6,fromX=7.8,fromY=10.8,toX=10.2,toY=10.8,period=4.4,phase=.35)]+mv,
      ceilings=ce,anchors=an,hazards=[spike(10.9,4.65,.55,.42,'floor'),spike(7.8,12.75,.6,.48,'ceiling'),spike(16.5,7.0,.55,1.0,'left')])

# Stage 8 is a crusher shaft: short side shelves alternate around two timed pistons.
ce,an,mv=add_anchor(11.6,14.1,10.0,14.5,3.2)
build(8,[2.0,4.4,6.7,9.0,11.3,13.6,15.9,18.2],[3.0,7.0,12.0,15.0,11.0,6.0,3.0,9.0],[5.2,2.5,2.2,2.0,2.1,1.9,2.0,4.2],
      'crusher',24,[3,6],crushers=[dict(id='press_upper',direction='down',x=8.5,topY=8.8,width=1.25,stroke=1.55,period=3.6,phase=.15),
      dict(id='press_lower',direction='up',x=4.0,topY=17.0,width=1.25,stroke=1.45,period=3.6,phase=.62)],
      ceilings=ce,anchors=an,hazards=[spike(12.45,6.65,.55,.42,'floor'),spike(14.0,10.15,.65,.5,'ceiling'),
      spike(.3,13.1,.55,1.0,'right'),spike(5.0,13.55,.55,.42,'floor')])

# Stage 9 is a saw gallery with a long horizontal transfer between alternating ledges.
ce,an,mv=add_anchor(12.4,14.2,10.8,14.6,3.2,dict(id='saw_route_anchor',kind='anchor',anchor=0,fromX=12.4,fromY=14.2,toX=15.2,toY=15.0,period=5.1))
build(9,[2.0,4.5,7.0,9.5,12.0,14.5,17.0,19.5],[3.0,7.0,12.0,16.0,12.0,7.0,3.0,9.0],[5.2,2.4,2.0,1.8,2.0,1.8,2.0,4.2],
      'saw',24,[3,6],movers=mv,saws=[dict(id='saw_01',fromX=9.5,fromY=8.6,toX=14.5,toY=8.6,radius=.52,period=3.6,phase=.2),
      dict(id='saw_02',fromX=4.2,fromY=13.0,toX=8.5,toY=13.0,radius=.52,period=4.2,phase=.55),
      dict(id='saw_03',fromX=7.4,fromY=18.0,toX=12.0,toY=18.0,radius=.5,period=4.6,phase=.15)],
      rings=[dict(id='mine_04.recharge.01',x=8.0,y=8.1,halfWidth=.35,halfHeight=.4)],ceilings=ce,anchors=an,
      hazards=[spike(12.65,6.95,.55,.42,'floor'),spike(17.1,9.1,.5,1.0,'left'),spike(11.0,11.2,.65,.5,'ceiling'),
      spike(6.0,14.45,.6,.42,'floor'),spike(.4,16.5,.55,1.0,'right')])

# Stage 10 combines a lift, cart, moving ring, two crushers, two saw paths and six spike groups.
ce,an,mv=add_anchor(12.5,16.2,10.9,16.6,3.2,dict(id='escape_ring',kind='anchor',anchor=0,fromX=12.5,fromY=16.2,toX=15.5,toY=17.6,period=5.2))
build(10,[2.0,4.3,6.6,8.9,11.2,13.5,15.8,18.1,20.4],[3.0,6.0,10.0,15.0,12.0,8.0,3.0,10.0,15.0],[5.2,2.3,2.0,1.9,1.8,1.8,1.8,2.6,4.8],
      'escape',26,[4],movers=[dict(id='escape_lift',kind='lift',platform=2,fromX=9.0,fromY=5.8,toX=9.0,toY=7.5,period=6.2),
      dict(id='escape_cart',kind='cart',platform=5,fromX=7.1,fromY=12.7,toX=9.1,toY=12.7,period=4.6)]+mv,
      crushers=[dict(id='escape_press_upper',direction='down',x=13.4,topY=11.5,width=1.2,stroke=1.4,period=3.6,phase=.18),
      dict(id='escape_press_lower',direction='up',x=5.0,topY=17.6,width=1.2,stroke=1.3,period=3.6,phase=.58)],
      saws=[dict(id='escape_saw_01',fromX=10.0,fromY=12.7,toX=14.3,toY=12.7,radius=.5,period=3.8,phase=.15),
      dict(id='escape_saw_02',fromX=4.2,fromY=18.0,toX=8.2,toY=18.0,radius=.5,period=4.1,phase=.42)],
      rings=[dict(id='mine_05.recharge.01',x=8.0,y=15.0,halfWidth=.35,halfHeight=.4)],ceilings=ce,anchors=an,
      hazards=[spike(10.55,6.55,.45,.42,'floor'),spike(10.35,6.05,.65,.5,'ceiling'),spike(17.0,8.7,.5,1.0,'left'),
      spike(11.2,13.0,.65,.48,'ceiling'),spike(3.55,15.75,.55,.42,'floor'),spike(.4,19.1,.55,1.0,'right')])

routes={
 'mine_01':[dict(platform=1,waitTop=4.4),dict(platform=2),dict(platform=3),dict(platform=4),dict(platform=5),dict(platform=6),dict(platform=7)],
 'mine_02':[dict(platform=1,waitX=7.3),dict(platform=2),dict(platform=3),dict(platform=4),dict(platform=5),dict(platform=6,jump=True,waitX=9.8),dict(platform=7),dict(platform=8)],
 'mine_03':[dict(platform=i) for i in range(1,8)],
 'mine_04':[dict(platform=i) for i in range(1,8)],
 'mine_05':[dict(platform=1),dict(platform=2,waitTop=8.5),dict(platform=3),dict(platform=4),dict(platform=5,waitX=6.8),dict(platform=6),dict(platform=7),dict(platform=8)]
}
(ROOT/'assets/stages/mine_routes.json').write_text(json.dumps(routes,indent=2)+'\n',encoding='utf-8')

parser=argparse.ArgumentParser()
parser.add_argument('--activate',action='store_true',help='Make verified mine maps playable in the stage selector')
args=parser.parse_args()
for m in patch['stages']:
    entry=next(e for e in catalog['stages'] if e['id']==m['id'])
    entry.update(m)
    entry['available']=bool(args.activate)
    entry.pop('artAvailable',None)
(ROOT/'assets/cave_chapter/stages.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f"Built mine_01..mine_05; selectable={args.activate}")

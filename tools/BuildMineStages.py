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
    # Give short authored ledges enough control room for air braking; the hazards and device timing carry difficulty.
    widths=[max(w,3.2) if i not in (0,len(widths)-1) else w for i,w in enumerate(widths)]
    blocks=[platform(x,y,w) for x,y,w in zip(centers,tops,widths)]
    for m in movers or []:
        if m.get('platform',-1)>=0:
            b=blocks[m['platform']]
            b['x']=m['fromX'];b['y']=m['fromY']
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

# 6 / 7 use their authored moving platforms on the lower route. Ring paths are fixed ping-pong.
ce,an,mv=add_anchor(10.5,11.5,9.8,11.9,3.2,dict(id='lift_ring',kind='anchor',anchor=0,fromX=10.5,fromY=11.5,toX=13.5,toY=13.1,period=5.4))
build(6,[2,2,3.5,5.3,7.1,8.9,10.7,12.5],[3,6,9,9,9,9,9,9],[5.2,2.4,2.2,2.1,2.0,2.0,2.0,4.2],
      'lift',22,[3,6],[dict(id='lift_01',kind='lift',platform=1,fromX=4.8,fromY=1.2,toX=4.8,toY=3.7,period=7.2)]+
      mv,ceilings=ce,anchors=an,hazards=[spike(11.8,15.6,1.1,.65,'ceiling')])

ce,an,mv=add_anchor(12,12.3,10.4,12.7,3.2,dict(id='cart_ring',kind='anchor',anchor=0,fromX=12,fromY=12.3,toX=15, toY=13.3,period=4.8))
build(7,[2,2,4.5,7,9.5,12,12,14.5,17],[3,6,9,6,3,6,9,9,6],[5.2,2.4,2.2,2.0,2.0,2.0,2.4,2.0,4.0],
      'cart',22,[4,7],[dict(id='cart_01',kind='cart',platform=1,fromX=4.8,fromY=1.2,toX=7.6,toY=1.2,period=4.8),
      dict(id='cart_02',kind='cart',platform=6,fromX=7.8,fromY=11.2,toX=10.2,toY=11.2,period=4.4,phase=.35)]+mv,
      ceilings=ce,anchors=an,hazards=[spike(12,10.2,.8,.55,'ceiling'),spike(1.0,5.8,.55,1.1,'right')])

ce,an,mv=add_anchor(11.8,14.1,10.2,14.5,3.2)
build(8,[2,4.5,7,9.5,12,14.5,17,19.5],[3,6,9,6,3,6,9,6],[5.2,2.2,2.0,2.0,2.0,2.0,2.0,4.0],
      'crusher',24,[3,6],crushers=[dict(id='press_upper',direction='down',x=7.4,topY=12.4,width=1.25,stroke=1.8,period=3.6,phase=.15),
      dict(id='press_lower',direction='up',x=2.5,topY=15.5,width=1.25,stroke=1.55,period=3.6,phase=.62)],
      ceilings=ce,anchors=an,hazards=[spike(11.5,17.3,1,.65,'ceiling'),spike(8.2,8.2,1.0,.55,'floor')])

ce,an,mv=add_anchor(11.8,13.5,10.2,13.9,3.2,dict(id='saw_route_anchor',kind='anchor',anchor=0,fromX=11.8,fromY=13.5,toX=14.2,toY=14.4,period=5.1))
build(9,[2,4.5,7,9.5,12,14.5,17,19.5],[3,6,9,6,3,6,9,6],[5.2,2.2,2.0,2.0,2.0,2.0,2.0,4.0],
      'saw',24,[3,6],movers=mv,saws=[dict(id='saw_01',fromX=10.8,fromY=10.8,toX=13.5,toY=10.8,radius=.55,period=3.6,phase=.2),
      dict(id='saw_02',fromX=3.2,fromY=16.3,toX=5.2,toY=16.3,radius=.52,period=4.2,phase=.55)],
      rings=[dict(id='mine_04.recharge.01',x=7.6,y=8.2,halfWidth=.35,halfHeight=.4)],ceilings=ce,anchors=an,
      hazards=[spike(11.7,12.3,.8,.6,'floor'),spike(2.3,18.0,.9,.6,'ceiling')])

ce,an,mv=add_anchor(12,16.0,10.4,16.4,3.2,dict(id='escape_ring',kind='anchor',anchor=0,fromX=12,fromY=16,toX=15, toY=17.4,period=5.2))
build(10,[2,4.5,7,9.5,12,14.5,17,19.5,22],[3,6,9,6,3,6,9,6,3],[5.2,2.2,2.0,2.0,2.0,2.0,2.0,2.0,4.0],
      'escape',27,[4],movers=[dict(id='escape_lift',kind='lift',platform=2,fromX=7.9,fromY=6.2,toX=7.9,toY=8.0,period=6.2),
      dict(id='escape_cart',kind='cart',platform=5,fromX=5,fromY=13.7,toX=7, toY=13.7,period=4.6)]+mv,
      crushers=[dict(id='escape_press',direction='down',x=7.4,topY=17.2,width=1.25,stroke=1.8,period=3.6,phase=.18)],
      saws=[dict(id='escape_saw',fromX=10.4,fromY=20.2,toX=13.2,toY=20.2,radius=.55,period=3.8,phase=.15)],
      rings=[dict(id='mine_05.recharge.01',x=7.5,y=15.1,halfWidth=.35,halfHeight=.4)],ceilings=ce,anchors=an,
      hazards=[spike(11.7,18.6,.9,.6,'ceiling'),spike(1.5,7.5,1,.55,'floor'),spike(10.4,11.0,.8,.6,'floor')])

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

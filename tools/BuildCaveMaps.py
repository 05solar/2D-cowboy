"""파일: BuildCaveMaps.py
역할: 수작업 설계한 동굴 좌표 직렬화. 책임: 맵과 자동 조작 검증 경로 생성.
협력: StageDefinition, CaveVerification. 관련 사양: docs/12_CAVE_CHAPTER.md.
배경/이미지 크기로 충돌을 만들지 않으며 실행 시 출력 JSON만 갱신한다.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
catalog = json.loads((ROOT / 'assets/cave_chapter/stages.json').read_text(encoding='utf-8'))
routes = {}

def block(x, right, top=2, crumble=False, bottom=None):
    if bottom is None: bottom = top - .7 if top > 2 or crumble else 0
    return dict(x=x, y=bottom, width=right-x, height=top-bottom, crumble=crumble)

def point(x,y): return dict(x=x,y=y)

def hook(x, top=2, offset=0):
    return ([block(x, x+4, top+6.5, bottom=top+5), block(x+4,x+9,top+6.5+offset,bottom=top+5+offset)],
            [point(x+2,top+4.6),point(x+6.5,top+4.6+offset)])

def route(kind, launch, target, direction=1, anchor=0):
    return dict(kind=kind,launch=launch,target=target,direction=direction,anchor=anchor)

def write(number, platforms, cp, goal, width, connections, steps, hazards=None, height=12):
    e=catalog['stages'][number-1]; id=e['id']; ceilings=[]; anchors=[]
    for args in connections:
        ce,an=hook(*args);ceilings+=ce;anchors+=an
    # Cave 1 keeps its authored low recovery pad; collapsing-floor pits remain lethal.
    death_plane={1:-1.25}.get(number,1.0)
    data=dict(id=id,name=e['name'],width=width,height=height,spawnX=2,spawnY=2,deathPlaneY=death_plane,exitX=goal[0],
        platforms=platforms,ceilings=ceilings,anchors=anchors,checkpoints=[point(*p) for p in cp],
        clearGem=dict(id=id+'.clear_gem',type='clear_gem',x=goal[0],y=goal[1]+.48,halfWidth=.0716,halfHeight=.15),
        optionalGems=[dict(id=id+'.optional.01',type='optional',x=4.5,y=3.05,halfWidth=.09,halfHeight=.12)],
        hazards=hazards or [],zones=[dict(x=0,title=f'{number:02d}  {e["name"]}',hint=e['objective'],
            detail='A/D 이동 · SPACE 점프 · 우클릭 천장 링 · 좌클릭 반동 · M 스테이지 선택')])
    (ROOT/f'assets/stages/{id}.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    routes[id]=steps

write(1,[block(0,8),block(17,23),block(28,34),block(39,46),block(9,16,-1,bottom=-1.7)],
    [(20,2),(31,2)],(43,2),46,[(8,)],
    [route('hook',7,1),route('recoil',22.5,2),route('recoil',33.5,3)])
write(2,[block(0,8),block(17,23,2.5),block(32,38),block(43,50)],
    [(20,2.5),(35,2)],(47,2),50,[(8,2,.5),(23,2.5,-.4)],
    [route('hook',7,1),route('hook',22,2,anchor=2),route('recoil',37.5,3)])
hazards=[dict(x=9,y=-.3,width=6,height=.24,direction='floor'),
         dict(x=11.7,y=5.65,width=.9,height=.2,direction='ceiling'),
         dict(x=24,y=-.3,width=.2,height=.7,direction='left'),
         dict(x=35,y=-.3,width=.2,height=.7,direction='right')]
write(3,[block(0,8),block(17,23),block(28,34),block(39,41),block(43,50)],
    [(20,2),(31,2)],(47,2),50,[(8,)],
    [route('hook',7,1),route('recoil',22.5,2),route('brake',33.5,3),route('basic',40.5,4)],hazards)
write(4,[block(0,3),block(3,6,crumble=True),block(6,8),block(10,13,crumble=True),
    block(15,18,crumble=True),block(20,26),block(35,38,crumble=True),block(40,46),block(51,58)],
    [(23,2),(43,2)],(55,2),58,[(26,)],
    [route('basic',7.5,3),route('basic',12.5,4),route('basic',17.5,5),route('hook',25,6),route('basic',37.5,7),route('recoil',45.5,8)])
write(5,[block(0,6),block(8,12,4),block(14,18,6,True),block(20,26,8),block(35,41,8),
    block(43,47,10),block(49,53,12),block(43,47,14),block(37,41,16),block(31,35,18,True),block(23,29,20)],
    [(23,8)],(26,20),55,[(26,8)],
    [route('up',5.5,1),route('up',11.5,2),route('up',17.5,3),route('hook',25,4),
     route('up',40.5,5),route('up',46.5,6),route('up',49.5,7,-1),route('up',43.5,8,-1),route('up',37.5,9,-1),route('up',31.5,10,-1)],height=28)
(ROOT/'assets/stages/cave_routes.json').write_text(json.dumps(routes,indent=2)+'\n',encoding='utf-8')

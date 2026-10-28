"""Run with Python 3; tests reference math only, not engine integration."""
import json
import math
from pathlib import Path
from motion_reference import sample_motion,cycle_duration,barrier_state

ROOT=Path(__file__).resolve().parents[1]
data=json.loads((ROOT/'motion_presets.json').read_text())
for name,p in data['motions'].items():
    period=cycle_duration(p)
    # Position and velocity recur at the same phase, including after many loops.
    for t in [0,.1,.6,1.17,2.91,period-.001]:
        a=sample_motion(p,t);b=sample_motion(p,t+period*50)
        for key in ['positionH','velocityHPerSecond']:
            assert all(math.isclose(x,y,abs_tol=1e-9) for x,y in zip(a[key],b[key])),name
    assert sample_motion(p,0)['velocityHPerSecond']==[0.0,0.0]
    assert sample_motion(p,p['waypoints'][0]['holdSeconds']/2)['holding']
    # Analytic derivative agrees with centered finite difference during travel.
    t=p['waypoints'][0]['holdSeconds']+p['travelSeconds']*.37;eps=1e-5
    a=sample_motion(p,t-eps);b=sample_motion(p,t+eps);mid=sample_motion(p,t)
    derivative=[(b['positionH'][j]-a['positionH'][j])/(2*eps) for j in range(2)]
    assert all(math.isclose(x,y,abs_tol=1e-7) for x,y in zip(derivative,mid['velocityHPerSecond'])),name
    # Every configured segment is axis-aligned, including closing segment.
    points=[x['positionH'] for x in p['waypoints']]
    for a,b in zip(points,points[1:]+points[:1]):assert a[0]==b[0] or a[1]==b[1]
    reset=sample_motion(p,0,.23)
    for _ in range(100):assert sample_motion(p,0,.23)==reset

p=data['barriers']['standard'];off=p['offSeconds'];warn=p['warningSeconds'];period=sum(p.values())
assert barrier_state(p,off-1e-6)['state']=='off'
assert barrier_state(p,off)['state']=='warning'
assert not barrier_state(p,off+warn-1e-6)['lethal']
assert barrier_state(p,off+warn)['lethal']
assert barrier_state(p,off+warn)['blocksGrapple']
assert barrier_state(p,period)['state']=='off'

blocks=json.loads((ROOT/'blocks.json').read_text())['colliders']
for name,rects in blocks.items():
    for x,y,w,h in rects:assert x>=0 and y>=0 and w>0 and h>0 and x+w<=1.000001 and y+h<=1.000001
    for i,(x,y,w,h) in enumerate(rects):
        for X,Y,W,H in rects[i+1:]:
            assert min(x+w,X+W)<=max(x,X)+1e-9 or min(y+h,Y+H)<=max(y,Y)+1e-9,name
def contains(rects,x,y):return any(X<x<X+w and Y<y<Y+h for X,Y,w,h in rects)
assert not contains(blocks['window'],.5,.5)
assert not contains(blocks['u_block'],.5,.3)
assert not contains(blocks['arch'],.5,.6)
print('PASS: 4 motion cycles, holds, derivatives, axis paths, repeat reset, barrier boundaries, compound openings')

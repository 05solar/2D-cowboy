"""Engine-independent examples. Units H, Y up. No collision or rider solver."""
from math import isfinite


def _validate_motion(preset):
    points=preset['waypoints']
    travel=preset['travelSeconds']
    if len(points)<2 or not isfinite(travel) or travel<=0:
        raise ValueError('Need >=2 points and finite positive travelSeconds')
    for point in points:
        xy=point['positionH'];hold=point.get('holdSeconds',0)
        if len(xy)!=2 or not all(isfinite(v) for v in xy):
            raise ValueError('Invalid waypoint')
        if not isfinite(hold) or hold<0:
            raise ValueError('Invalid hold duration')
    return points,travel


def cycle_duration(preset):
    points,travel=_validate_motion(preset)
    return len(points)*travel+sum(p.get('holdSeconds',0) for p in points)


def sample_motion(preset,physics_time,phase_seconds=0):
    """Return position/analytic velocity from absolute checkpoint simulation time."""
    if not isfinite(physics_time) or physics_time<0 or not isfinite(phase_seconds):
        raise ValueError('Invalid simulation time')
    points,travel=_validate_motion(preset)
    t=(physics_time+phase_seconds)%cycle_duration(preset)
    for i,point in enumerate(points):
        a=point['positionH'];b=points[(i+1)%len(points)]['positionH']
        hold=point.get('holdSeconds',0)
        if t<hold:
            return {'positionH':list(a),'velocityHPerSecond':[0.0,0.0],'holding':True}
        t-=hold
        if t<travel:
            u=t/travel;s=u*u*(3-2*u);ds=6*u*(1-u)/travel
            return {'positionH':[a[j]+(b[j]-a[j])*s for j in range(2)],
                    'velocityHPerSecond':[(b[j]-a[j])*ds for j in range(2)],'holding':False}
        t-=travel
    # Floating-point roundoff at the period boundary only.
    return {'positionH':list(points[0]['positionH']),'velocityHPerSecond':[0.0,0.0],'holding':True}


def barrier_state(preset,physics_time,phase_seconds=0):
    """Only on is lethal/blocking; warning is a visible nonlethal advance cue."""
    if not isfinite(physics_time) or physics_time<0 or not isfinite(phase_seconds):
        raise ValueError('Invalid simulation time')
    lengths=[preset[k] for k in ['offSeconds','warningSeconds','onSeconds']]
    if not all(isfinite(v) and v>0 for v in lengths):
        raise ValueError('Durations must be finite and positive')
    t=(physics_time+phase_seconds)%sum(lengths)
    state='off' if t<lengths[0] else 'warning' if t<sum(lengths[:2]) else 'on'
    return {'state':state,'lethal':state=='on','blocksGrapple':state=='on'}

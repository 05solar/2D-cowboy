"""파일: BuildCrumbleSound.py
역할: 붕괴 전 균열 알림 음원 생성. 책임: 짧은 두 번의 저음 잡음 타격을 WAV로 저장.
협력: GameplayScreen. 수명: 개발 시 일회 실행. 관련 사양: docs/12_CAVE_CHAPTER.md.
"""
from pathlib import Path
import math, random, struct, wave
random.seed(7)
path=Path(__file__).resolve().parents[1]/'assets/audio/crumble_warning.wav'
path.parent.mkdir(exist_ok=True)
sr=22050
raw=[]
for i in range(11025):
    t=i/sr
    age=t if t<.17 else t-.17
    envelope=math.exp(-age*24)
    value=(random.uniform(-1,1)*.55+math.sin(2*math.pi*70*t)*.5)*envelope*.55
    raw.append(value)
# 저역통과로 잡음의 날카로운 고역을 깎아 더 낮고 묵직하게 들리게 한다.
samples=[]
prev=0
for v in raw:
    prev=prev+(v-prev)*.5
    value=max(-1,min(1,prev*1.15))
    samples.append(struct.pack('<h',int(value*32767)))
with wave.open(str(path),'wb') as out:
    out.setnchannels(1);out.setsampwidth(2);out.setframerate(sr);out.writeframes(b''.join(samples))

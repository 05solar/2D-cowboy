"""파일: BuildCrumbleSound.py
역할: 붕괴 전 균열 알림 음원 생성. 책임: 짧은 두 번의 잡음 타격을 WAV로 저장.
협력: GameplayScreen. 수명: 개발 시 일회 실행. 관련 사양: docs/12_CAVE_CHAPTER.md.
"""
from pathlib import Path
import math, random, struct, wave
random.seed(7)
path=Path(__file__).resolve().parents[1]/'assets/audio/crumble_warning.wav'
path.parent.mkdir(exist_ok=True)
samples=[]
for i in range(11025):
    t=i/22050
    age=t if t<.17 else t-.17
    envelope=math.exp(-age*28)
    value=(random.uniform(-1,1)*.6+math.sin(2*math.pi*140*t)*.4)*envelope*.55
    samples.append(struct.pack('<h',int(value*32767)))
with wave.open(str(path),'wb') as out:
    out.setnchannels(1);out.setsampwidth(2);out.setframerate(22050);out.writeframes(b''.join(samples))

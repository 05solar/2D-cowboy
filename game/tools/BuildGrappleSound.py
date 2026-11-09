"""파일: BuildGrappleSound.py
역할: 갈고리 발사 효과음 생성. 책임: 하강하는 사인 스윕과 잡음을 섞어 '휙' 소리를 WAV로 저장.
협력: GameplayScreen. 수명: 개발 시 일회 실행. 관련 사양: 없음(신규 효과음).
"""
from pathlib import Path
import math, random, struct, wave
random.seed(11)
path=Path(__file__).resolve().parents[1]/'assets/audio/grapple_whoosh.wav'
path.parent.mkdir(exist_ok=True)
sr=22050
duration=.22
samples=[]
n=int(sr*duration)
for i in range(n):
    t=i/sr
    frac=i/n
    hump=math.sin(math.pi*frac)
    freq=900-650*frac
    tone=math.sin(2*math.pi*freq*t)
    value=(random.uniform(-1,1)*.45+tone*.55)*hump*.6
    value=max(-1,min(1,value))
    samples.append(struct.pack('<h',int(value*32767)))
with wave.open(str(path),'wb') as out:
    out.setnchannels(1);out.setsampwidth(2);out.setframerate(sr);out.writeframes(b''.join(samples))

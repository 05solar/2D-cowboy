"""파일: BuildPrologueFont.py
역할: DungGeunMo.ttf와 실제 게임 소스·스테이지 문자를 읽어 64px BMFont(prologue.fnt/png)를 생성한다.
주요 책임: 프로젝트 전역에서 쓰이는 문자 수집, 글자별 타이트 바운딩 박스 계산, BMFont 텍스트 출력.
협력 대상: HudView(소비), DungGeunMo.ttf, assets/stages 및 core 소스의 실제 문자열.
관련 사양: docs/10_PROLOGUE.md.

이전 tools/BuildPrologueFont.java(AWT Font/GlyphVector)는 DungGeunMo.ttf 자체는 정상임에도
Java2D의 한글 레이아웃 경로에서 전/체/모/정 등 일부 음절을 진/제/보/성 등 다른(그러나 올바르게
그려지는) 음절의 글리프로 바꿔 그리는 버그가 있었다. fontTools로 cmap을 확인해 두 음절이 서로
다른 glyph name(uniXXXX)으로 정확히 분리되어 있음을 확인했고, Pillow(FreeType) 렌더링은 같은
문자들을 전부 올바르게 그려 AWT 쪽 레이아웃 엔진 버그로 좁혔다. 이 스크립트는 FreeType 기반
Pillow로 교체해 같은 버그를 피한다. 셰이핑(GSUB/문맥 치환)을 적용하지 않는 Pillow 기본
layout_engine만 사용하고, 글자 하나씩 독립된 작은 캔버스에 그려 셰이핑 가능성을 원천 차단한다.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
FONT_PATH = ROOT / "assets/ui/fonts/DungGeunMo.ttf"
DEST_DIR = ROOT / "assets/ui/fonts"
PAD = 2
CELL = 80
COLUMNS = 40
SIZE = 64


def collect_chars():
    chars = {chr(c) for c in range(32, 127)}
    paths = sorted((ROOT / "core/src/main/java").rglob("*.java"))
    paths += sorted((ROOT / "assets/stages").rglob("*.json"))
    paths.append(ROOT / "assets/cave_chapter/stages.json")
    for path in paths:
        text = path.read_text(encoding="utf-8")
        for c in text:
            if ord(c) >= 32 and c != "﻿":
                chars.add(c)
    return sorted(chars, key=ord)


def glyph_bbox(font, char):
    canvas = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    ascent, _ = font.getmetrics()
    baseline_local = ascent + PAD
    draw.text((PAD, baseline_local), char, font=font, fill=(255, 255, 255, 255), anchor="ls")
    bbox = canvas.getchannel("A").getbbox()
    if bbox is None:
        bbox = (PAD, baseline_local, PAD, baseline_local)
    bx0, by0, bx1, by1 = bbox
    if (bx1 - bx0) + PAD * 2 > CELL or (by1 - by0) + PAD * 2 > CELL:
        raise ValueError(f"Glyph does not fit atlas cell: U+{ord(char):04X} {bbox}")
    return canvas, bx0, by0, bx1, by1


def main():
    chars = collect_chars()
    font = ImageFont.truetype(str(FONT_PATH), SIZE)
    for c in chars:
        if font.getmask(c).size == (0, 0) and c != " ":
            raise ValueError(f"DungGeunMo has no glyph for U+{ord(c):04X}")
    ascent, descent = font.getmetrics()
    rows = (len(chars) + COLUMNS - 1) // COLUMNS
    atlas = Image.new("RGBA", (COLUMNS * CELL, rows * CELL), (0, 0, 0, 0))
    lines = [
        'info face="DungGeunMo" size=64 bold=0 italic=0 charset="" unicode=1 stretchH=100 '
        "smooth=0 aa=0 padding=0,0,0,0 spacing=0,0",
        f"common lineHeight={ascent + descent} base={ascent} scaleW={atlas.width} "
        f"scaleH={atlas.height} pages=1 packed=0",
        'page id=0 file="prologue.png"',
        f"chars count={len(chars)}",
    ]
    for index, c in enumerate(chars):
        cell_x, cell_y = (index % COLUMNS) * CELL, (index // COLUMNS) * CELL
        glyph, bx0, by0, bx1, by1 = glyph_bbox(font, c)
        atlas.alpha_composite(glyph, (cell_x, cell_y))
        advance = round(font.getlength(c))
        lines.append(
            f"char id={ord(c)} x={cell_x + bx0} y={cell_y + by0} width={bx1 - bx0} "
            f"height={by1 - by0} xoffset={bx0 - PAD} yoffset={by0 - PAD} "
            f"xadvance={advance + 1} page=0 chnl=15"
        )
    DEST_DIR.mkdir(parents=True, exist_ok=True)
    atlas.save(DEST_DIR / "prologue.png")
    (DEST_DIR / "prologue.fnt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Font generated: {len(chars)} glyphs, {font.getname()}")


if __name__ == "__main__":
    main()

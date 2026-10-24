"""由 modicon.jpg 生成 Fabric mod 图标 assets/ylctiertagger/icon.png。

源图是米白宣纸底的黑墨书法。先按墨色强度把纸底抠成透明，再把黑色笔画放到一块
浅色圆角底板上——直接用透明底黑字的话，ModMenu 的深色面板上根本看不见。

用法：python tools/make-icon.py
"""

import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "modicon.jpg")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "ylctiertagger", "icon.png")

SIZE = 128          # Fabric mod 图标推荐尺寸
PAPER = (253, 249, 237)   # 源图角落的纸色
INK_LUM = 28.0            # 墨色亮度参考值
BOARD = (250, 245, 233)   # 底板颜色（宣纸色）
GLYPH_H = 0.74            # 笔画高度占画布的比例
CORNER = 18               # 底板圆角半径


def luminance(r, g, b):
    return 0.299 * r + 0.587 * g + 0.114 * b


def build_alpha_and_ink():
    im = Image.open(SRC).convert("RGB")
    w, h = im.size
    px = im.load()

    paper_lum = luminance(*PAPER)
    span = paper_lum - INK_LUM

    alpha_img = Image.new("L", (w, h), 0)
    ink_img = Image.new("RGB", (w, h), (0, 0, 0))
    a_px = alpha_img.load()
    i_px = ink_img.load()

    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            lum = luminance(r, g, b)
            a = (paper_lum - lum) / span
            if a <= 0.02:
                continue
            if a > 1.0:
                a = 1.0
            # un-premultiply：还原被纸色稀释后的墨色
            ink = []
            for comp, paper in zip((r, g, b), PAPER):
                v = (comp - paper * (1.0 - a)) / a
                ink.append(max(0, min(255, int(round(v)))))
            a_px[x, y] = int(round(a * 255))
            i_px[x, y] = tuple(ink)

    return alpha_img, ink_img


def main():
    alpha, ink = build_alpha_and_ink()
    rgba = ink.convert("RGBA")
    rgba.putalpha(alpha)

    bbox = alpha.getbbox()
    if bbox:
        rgba = rgba.crop(bbox)

    # 笔画按高度缩放，宽度等比
    target_h = int(SIZE * GLYPH_H)
    scale = target_h / rgba.size[1]
    target_w = max(1, int(rgba.size[0] * scale))
    glyph = rgba.resize((target_w, target_h), Image.LANCZOS)

    # 浅色底板，保证在深色面板上也能看清
    canvas = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle([0, 0, SIZE - 1, SIZE - 1], radius=CORNER,
                           fill=BOARD + (255,))

    # 底板描一圈淡边，压住纸纹噪点
    draw.rounded_rectangle([0, 0, SIZE - 1, SIZE - 1], radius=CORNER,
                           outline=(214, 206, 188, 255), width=2)

    canvas.alpha_composite(glyph, ((SIZE - target_w) // 2, (SIZE - target_h) // 2))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    canvas.save(OUT, "PNG")
    print("已生成", OUT, canvas.size)


if __name__ == "__main__":
    main()

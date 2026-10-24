"""由 modicon.jpg 生成 Fabric mod 图标 assets/ylctiertagger/icon.png。

源图是米白宣纸底的黑墨书法，Fabric 的 mod 列表背景偏深，直接用会是一块米白色方块。
这里把纸底抠成透明，按墨色强度生成 alpha，再做 un-premultiply 还原笔画本身的颜色。

用法：python tools/make-icon.py
"""

import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "modicon.jpg")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "ylctiertagger", "icon.png")

SIZE = 128          # Fabric mod 图标推荐尺寸
PAPER = (253, 249, 237)   # 源图角落的纸色
INK_LUM = 28.0            # 墨色亮度参考值
PAD_RATIO = 0.06          # 裁剪后四周留白比例


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

    w, h = rgba.size
    side = max(w, h)
    pad = int(side * PAD_RATIO)
    side += pad * 2

    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(rgba, ((side - w) // 2, (side - h) // 2), rgba)

    canvas = canvas.resize((SIZE, SIZE), Image.LANCZOS)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    canvas.save(OUT, "PNG")
    print("已生成", OUT, canvas.size)


if __name__ == "__main__":
    main()

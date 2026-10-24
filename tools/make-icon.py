"""由 modicon.jpg 生成 Fabric mod 图标 assets/ylctiertagger/icon.png。

只做格式转换：等比缩放到目标尺寸后转成 PNG，**不抠图、不透明化、不改动任何像素内容**，
原图的背景与文字完整保留。

用法：python tools/make-icon.py [尺寸]
"""

import os
import sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "modicon.jpg")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "ylctiertagger", "icon.png")

DEFAULT_SIZE = 128      # Fabric mod 图标惯例尺寸


def main():
    size = int(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SIZE

    im = Image.open(SRC)
    if im.mode != "RGB":
        im = im.convert("RGB")

    # 等比缩放到能盖住目标画布的尺寸，居中放置，不裁剪
    scale = size / max(im.size)
    resized = im.resize(
        (max(1, round(im.size[0] * scale)), max(1, round(im.size[1] * scale))),
        Image.LANCZOS,
    )

    canvas = Image.new("RGB", (size, size), (255, 255, 255))
    canvas.paste(resized, ((size - resized.size[0]) // 2, (size - resized.size[1]) // 2))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    canvas.save(OUT, "PNG")
    print("已生成", OUT, canvas.size, "（源图", im.size, "，仅格式转换，未改动内容）")


if __name__ == "__main__":
    main()

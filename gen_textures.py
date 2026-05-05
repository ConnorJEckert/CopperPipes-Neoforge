#!/usr/bin/env python3
"""Generate 16x16 copper pipe textures with proper shading and color."""
from PIL import Image
import math
import os

OUT_DIR = os.path.join(os.path.dirname(__file__),
    "src", "main", "resources", "assets", "copperpipes", "textures", "block")

def clamp(v): return max(0, min(255, int(v)))

# ---------------------------------------------------------------------------
# Pipe body  (cylindrical horizontal shading)
# ---------------------------------------------------------------------------

def make_body(col_palette):
    """16x16 side texture using per-column colors with subtle row variation."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b = col_palette[x]
            # faint horizontal band every 8 rows for surface interest
            row_dim = 0.90 if (y % 8) == 7 else 1.0
            px[x, y] = (clamp(r * row_dim), clamp(g * row_dim), clamp(b * row_dim), 255)
    return img


def _copper_base():
    """Warm copper columns: highlight at center, deep shadow at edges."""
    return [
        ( 86,  40, 13),  # 0  deep shadow
        (108,  52, 19),  # 1
        (130,  65, 26),  # 2  shadow
        (150,  78, 32),  # 3
        (164,  90, 38),  # 4  mid-shadow
        (178, 104, 44),  # 5
        (192, 116, 50),  # 6  mid-highlight
        (208, 130, 58),  # 7  highlight
        (214, 136, 61),  # 8  bright highlight
        (202, 122, 54),  # 9
        (184, 108, 46),  # 10 mid-highlight
        (164,  90, 38),  # 11 mid-shadow
        (148,  76, 30),  # 12
        (126,  62, 24),  # 13 shadow
        (104,  50, 18),  # 14
        ( 84,  38, 12),  # 15 deep shadow
    ]


def _blend(base_rgb, tint_rgb, t):
    return tuple(clamp(base_rgb[i] * (1 - t) + tint_rgb[i] * t) for i in range(3))


def copper_palette():    return _copper_base()

def exposed_palette():
    base = _copper_base()
    verdigris = (150, 152, 84)
    # Patchy verdigris on a few columns
    patched = {3, 4, 9, 10}
    return [(_blend(c, verdigris, 0.36) if i in patched else c) for i, c in enumerate(base)]

def weathered_palette():
    base = _copper_base()
    verdigris = (76, 136, 100)
    # Mostly verdigris; only center 2 columns keep some copper
    return [(_blend(c, verdigris, 0.22 if i in (7, 8) else 0.80)) for i, c in enumerate(base)]

def oxidized_palette():
    """Rich teal-green verdigris with cylindrical shading."""
    return [
        ( 40,  96, 74),  # 0  deep shadow
        ( 48, 108, 84),  # 1
        ( 55, 118, 92),  # 2
        ( 60, 126, 98),  # 3
        ( 64, 132,102),  # 4
        ( 67, 136,106),  # 5
        ( 70, 140,109),  # 6
        ( 76, 146,114),  # 7  highlight
        ( 79, 150,117),  # 8  bright highlight
        ( 73, 142,111),  # 9
        ( 67, 136,106),  # 10
        ( 62, 128,100),  # 11
        ( 56, 120, 94),  # 12
        ( 48, 108, 84),  # 13
        ( 43, 100, 78),  # 14
        ( 38,  92, 70),  # 15
    ]


# ---------------------------------------------------------------------------
# End cap  (annular ring — pipe opening viewed head-on)
# ---------------------------------------------------------------------------

def make_cap():
    """16x16 end cap: dark center hole, warm copper ring with angle shading."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    cx, cy = 7.5, 7.5

    for y in range(16):
        for x in range(16):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)

            if d <= 3.0:
                # Dark interior — very slight warm tint for depth
                t = max(0.0, 1.0 - d / 3.0)
                r = clamp(10 + t * 12)
                g = clamp( 5 + t *  5)
                b = clamp( 2 + t *  3)
                px[x, y] = (r, g, b, 255)

            elif d <= 6.0:
                # Copper ring: angular shading (upper-left lighter, lower-right darker)
                angle = math.atan2(y - cy, x - cx)
                light_angle = math.atan2(-1, -1)   # upper-left
                diff = abs(((angle - light_angle + math.pi) % (2 * math.pi)) - math.pi)
                light = 1.0 - diff / math.pi        # 0..1

                ring_t = (d - 3.0) / 3.0            # 0 inner edge, 1 outer edge
                # Brighter at mid-ring, darker at inner and outer edges
                rim_boost = math.sin(ring_t * math.pi) * 0.2

                combined = light * 0.8 + rim_boost + 0.1   # 0.1..1.1 → clamp later

                r = clamp(88 + combined * (222 - 88))
                g = clamp(42 + combined * (140 - 42))
                b = clamp(14 + combined * ( 62 - 14))
                px[x, y] = (r, g, b, 255)

            else:
                # Outside the cap — match pipe body edge (very dark copper)
                fade = max(0.0, 1.0 - (d - 6.0) / 2.5)
                r = clamp(88 * fade)
                g = clamp(42 * fade)
                b = clamp(14 * fade)
                px[x, y] = (r, g, b, 255)

    return img


# ---------------------------------------------------------------------------
# Inner pipe opening  (very dark, slight warm glow at center)
# ---------------------------------------------------------------------------

def make_inner():
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            glow = max(0.0, 0.45 - d / 14.0)
            r = clamp(14 + glow * 28)
            g = clamp( 7 + glow * 10)
            b = clamp( 3 + glow *  6)
            px[x, y] = (r, g, b, 255)
    return img


# ---------------------------------------------------------------------------
# Brace / fitting  (polished copper — warmer, higher contrast than body)
# ---------------------------------------------------------------------------

def make_brace():
    """16x16 copper fitting ring: warm, saturated, machined-metal look."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()

    # Richer, slightly more orange copper for polished fittings
    cols = [
        ( 78,  34, 10),  # 0  deep shadow
        (102,  48, 16),  # 1
        (128,  62, 22),  # 2  shadow
        (150,  76, 28),  # 3
        (168,  92, 36),  # 4  mid-shadow
        (184, 108, 44),  # 5
        (200, 122, 52),  # 6  mid-highlight
        (220, 142, 62),  # 7  highlight
        (228, 150, 66),  # 8  bright highlight
        (212, 136, 58),  # 9
        (192, 116, 48),  # 10
        (166,  90, 36),  # 11 mid-shadow
        (146,  72, 26),  # 12
        (120,  56, 18),  # 13 shadow
        ( 96,  42, 14),  # 14
        ( 76,  32,  9),  # 15 deep shadow
    ]

    for y in range(16):
        for x in range(16):
            r, g, b = cols[x]
            # Subtle machined bands top/bottom
            if y in (0, 15):
                factor = 0.72
            elif y in (4, 11):
                factor = 0.88
            else:
                factor = 1.0
            px[x, y] = (clamp(r * factor), clamp(g * factor), clamp(b * factor), 255)

    return img


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

if __name__ == '__main__':
    os.makedirs(OUT_DIR, exist_ok=True)

    tasks = [
        ('copper_pipe_body.png',           make_body(copper_palette())),
        ('exposed_copper_pipe_body.png',   make_body(exposed_palette())),
        ('weathered_copper_pipe_body.png', make_body(weathered_palette())),
        ('oxidized_copper_pipe_body.png',  make_body(oxidized_palette())),
        ('copper_pipe_cap.png',            make_cap()),
        ('copper_pipe_inner.png',          make_inner()),
        ('copper_pipe_brace.png',          make_brace()),
    ]

    for name, img in tasks:
        path = os.path.join(OUT_DIR, name)
        img.save(path)
        print(f'  saved  {name}')

    print('Done.')

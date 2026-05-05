#!/usr/bin/env python3
"""Generate arrow texture and inject arrow rules into all 8 pipe blockstate files."""
import os, json
from PIL import Image

ROOT = os.path.dirname(__file__)
TEX_DIR  = os.path.join(ROOT, "src","main","resources","assets","copperpipes","textures","block")
BS_DIR   = os.path.join(ROOT, "src","main","resources","assets","copperpipes","blockstates")
MOD_DIR  = os.path.join(ROOT, "src","main","resources","assets","copperpipes","models","block")

# ─── Arrow texture ────────────────────────────────────────────────────────────

def make_arrow():
    """16x16 downward arrow (shaft rows 1-8, arrowhead rows 9-12).
    'Down' in texture = south = toward the pipe output."""
    img = Image.new('RGBA', (16, 16))
    px  = img.load()

    bg      = (165, 90, 38)      # mid-shadow copper – blends with pipe body
    ar_col  = (245, 225, 165)    # warm cream/gold arrow
    outline = (70,  32, 10)      # very dark copper outline

    for y in range(16):
        for x in range(16):
            px[x, y] = (*bg, 255)

    # Arrow pixel positions: (row, x_start, x_end_exclusive)
    rows = [
        (1,  7,  9),   # shaft
        (2,  7,  9),
        (3,  7,  9),
        (4,  7,  9),
        (5,  7,  9),
        (6,  7,  9),
        (7,  7,  9),
        (8,  7,  9),
        (9,  4, 12),   # arrowhead – widest
        (10, 5, 11),
        (11, 6, 10),
        (12, 7,  9),   # tip
    ]

    arrow_set = {(x, y) for (y, x0, x1) in rows for x in range(x0, x1)}

    # 1-pixel dark outline
    for (ax, ay) in arrow_set:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                nx, ny = ax + dx, ay + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in arrow_set:
                    px[nx, ny] = (*outline, 255)

    # Arrow fill
    for (ax, ay) in arrow_set:
        px[ax, ay] = (*ar_col, 255)

    return img


img = make_arrow()
img.save(os.path.join(TEX_DIR, "copper_pipe_arrow.png"))
print("  saved  copper_pipe_arrow.png")

# ─── Arrow model ─────────────────────────────────────────────────────────────

arrow_model = {
    "textures": {
        "arrow": "copperpipes:block/copper_pipe_arrow"
    },
    "elements": [
        {
            "from": [5.5, 10.5, 10.5],
            "to":   [10.5, 10.52, 14.0],
            "faces": {
                "up": { "uv": [0, 0, 16, 16], "texture": "#arrow" }
            }
        }
    ]
}

model_path = os.path.join(MOD_DIR, "pipe_arrow_south.json")
with open(model_path, "w") as f:
    json.dump(arrow_model, f, indent=2)
print("  saved  pipe_arrow_south.json")

# ─── Blockstate injection ─────────────────────────────────────────────────────

ARROW_RULES = [
    { "when": { "facing": "south" }, "apply": { "model": "copperpipes:block/pipe_arrow_south" } },
    { "when": { "facing": "north" }, "apply": { "model": "copperpipes:block/pipe_arrow_south", "y": 180, "uvlock": True } },
    { "when": { "facing": "east"  }, "apply": { "model": "copperpipes:block/pipe_arrow_south", "y": 270, "uvlock": True } },
    { "when": { "facing": "west"  }, "apply": { "model": "copperpipes:block/pipe_arrow_south", "y": 90,  "uvlock": True } },
    { "when": { "facing": "up"    }, "apply": { "model": "copperpipes:block/pipe_arrow_south", "x": 90,  "uvlock": True } },
    { "when": { "facing": "down"  }, "apply": { "model": "copperpipes:block/pipe_arrow_south", "x": 270, "uvlock": True } },
]

BLOCKSTATES = [
    "copper_pipe.json",
    "exposed_copper_pipe.json",
    "weathered_copper_pipe.json",
    "oxidized_copper_pipe.json",
    "waxed_copper_pipe.json",
    "waxed_exposed_copper_pipe.json",
    "waxed_weathered_copper_pipe.json",
    "waxed_oxidized_copper_pipe.json",
]

for fname in BLOCKSTATES:
    path = os.path.join(BS_DIR, fname)
    with open(path, "r", encoding="utf-8-sig") as f:
        data = json.load(f)

    # Skip if already patched
    existing = data.get("multipart", [])
    already = any(
        r.get("when", {}).get("facing") == "south"
        and r.get("apply", {}).get("model", "").endswith("pipe_arrow_south")
        for r in existing
    )
    if already:
        print(f"  skip   {fname}  (already patched)")
        continue

    data["multipart"].extend(ARROW_RULES)

    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"  patched {fname}")

print("Done.")

#!/usr/bin/env python3
"""
Redesign fittings and arrows for all 8 pipe blockstate files.

1. Generate 3 new oxidation-matched arrow textures (exposed, weathered, oxidized)
2. Update pipe_arrow_south.json: add east + west side panels for multi-angle visibility
3. Create variant arrow models (exposed/weathered/oxidized) via parent override
4. Create pipe_brace_center.json: single ring centered on core, replaces directional braces
5. Update all 8 blockstate files:
   - Remove all pipe_brace_south rules, add single locked=true -> pipe_brace_center rule
   - Remove all *pipe_arrow_south rules, add oxidation-matched variant rules
"""

import os, json
from PIL import Image

ROOT    = os.path.dirname(__file__)
TEX_DIR = os.path.join(ROOT, "src","main","resources","assets","copperpipes","textures","block")
BS_DIR  = os.path.join(ROOT, "src","main","resources","assets","copperpipes","blockstates")
MOD_DIR = os.path.join(ROOT, "src","main","resources","assets","copperpipes","models","block")

def clamp(v): return max(0, min(255, int(v)))

# ── Arrow textures ─────────────────────────────────────────────────────────────

def make_arrow(bg):
    """16x16 subtle copper-toned arrow.
    Arrow fill is a warm highlight ~46 units brighter than bg (same hue family).
    Outline uses a shadow tone slightly darker than bg — no near-black.
    """
    img = Image.new('RGBA', (16, 16))
    px  = img.load()
    r, g, b = bg
    ar_col  = (min(255, r+46), min(255, g+38), min(255, b+16))
    outline = (max(30,  r-52), max(20,  g-40), max(10,  b-14))

    for y in range(16):
        for x in range(16):
            px[x, y] = (*bg, 255)

    rows = [
        (1,7,9),(2,7,9),(3,7,9),(4,7,9),(5,7,9),(6,7,9),(7,7,9),(8,7,9),
        (9,4,12),(10,5,11),(11,6,10),(12,7,9),
    ]
    arrow_set = {(x,y) for (y,x0,x1) in rows for x in range(x0,x1)}

    for (ax,ay) in arrow_set:
        for dx in (-1,0,1):
            for dy in (-1,0,1):
                nx,ny = ax+dx, ay+dy
                if 0<=nx<16 and 0<=ny<16 and (nx,ny) not in arrow_set:
                    px[nx,ny] = (*outline, 255)
    for (ax,ay) in arrow_set:
        px[ax,ay] = (*ar_col, 255)
    return img

# Mid-shadow column color sampled from each oxidation body palette
ARROW_VARIANTS = [
    ("copper_pipe_arrow.png",           (164,  90,  38)),
    ("exposed_copper_pipe_arrow.png",   (159, 112,  55)),
    ("weathered_copper_pipe_arrow.png", ( 94, 127,  88)),
    ("oxidized_copper_pipe_arrow.png",  ( 67, 136, 106)),
]

for fname, bg in ARROW_VARIANTS:
    make_arrow(bg).save(os.path.join(TEX_DIR, fname))
    print(f"  saved  {fname}")

# ── Arrow models ───────────────────────────────────────────────────────────────

# Base model: top + east + west panels so the arrow is visible from 3 angles.
# East face rotation=90  -> arrow points south on east face.
# West face rotation=270 -> arrow points south on west face.
arrow_model_base = {
    "textures": {
        "arrow": "copperpipes:block/copper_pipe_arrow"
    },
    "elements": [
        {
            "from": [5.5, 10.5, 10.5],
            "to":   [10.5, 10.52, 14.0],
            "faces": {
                "up": { "uv": [0,0,16,16], "texture": "#arrow" }
            }
        },
        {
            "from": [10.5, 5.5, 10.5],
            "to":   [10.52, 10.5, 14.0],
            "faces": {
                "east": { "uv": [0,0,16,16], "texture": "#arrow", "rotation": 90 }
            }
        },
        {
            "from": [5.48, 5.5, 10.5],
            "to":   [5.5, 10.5, 14.0],
            "faces": {
                "west": { "uv": [0,0,16,16], "texture": "#arrow", "rotation": 270 }
            }
        }
    ]
}

with open(os.path.join(MOD_DIR, "pipe_arrow_south.json"), "w") as f:
    json.dump(arrow_model_base, f, indent=2)
print("  saved  pipe_arrow_south.json")

for prefix in ("exposed", "weathered", "oxidized"):
    variant_model = {
        "parent": "copperpipes:block/pipe_arrow_south",
        "textures": {
            "arrow": f"copperpipes:block/{prefix}_copper_pipe_arrow"
        }
    }
    fname = f"{prefix}_pipe_arrow_south.json"
    with open(os.path.join(MOD_DIR, fname), "w") as f:
        json.dump(variant_model, f, indent=2)
    print(f"  saved  {fname}")

# ── Center brace model ─────────────────────────────────────────────────────────
# Single ring centered on the core (core: z=5.5 to 10.5, midpoint z=8.0).
# Ring width 5.0-11.0 (0.5 wider than core on each side to avoid z-fighting).
# Bolts on all 4 perpendicular sides.

brace_center = {
    "textures": {
        "brace":    "copperpipes:block/copper_pipe_brace",
        "particle": "copperpipes:block/copper_pipe_brace"
    },
    "elements": [
        {
            "from": [5.0, 5.0, 7.5],
            "to":   [11.0, 11.0, 8.5],
            "faces": {
                "north": { "uv": [5,5,11,11], "texture": "#brace" },
                "south": { "uv": [5,5,11,11], "texture": "#brace" },
                "east":  { "uv": [0,5,1,11],  "texture": "#brace" },
                "west":  { "uv": [0,5,1,11],  "texture": "#brace" },
                "up":    { "uv": [5,0,11,1],  "texture": "#brace" },
                "down":  { "uv": [5,0,11,1],  "texture": "#brace" }
            }
        },
        {
            "from": [11.0, 7.0, 7.5],
            "to":   [11.5,  9.0, 8.5],
            "faces": {
                "east":  { "uv": [0,6,1,8],    "texture": "#brace" },
                "north": { "uv": [11,7,12,9],  "texture": "#brace" },
                "south": { "uv": [11,7,12,9],  "texture": "#brace" }
            }
        },
        {
            "from": [4.5, 7.0, 7.5],
            "to":   [5.0,  9.0, 8.5],
            "faces": {
                "west":  { "uv": [0,6,1,8],    "texture": "#brace" },
                "north": { "uv": [4,7,5,9],    "texture": "#brace" },
                "south": { "uv": [4,7,5,9],    "texture": "#brace" }
            }
        },
        {
            "from": [7.0, 11.0, 7.5],
            "to":   [9.0, 11.5, 8.5],
            "faces": {
                "up":    { "uv": [7,0,9,1],    "texture": "#brace" },
                "north": { "uv": [7,11,9,12],  "texture": "#brace" },
                "south": { "uv": [7,11,9,12],  "texture": "#brace" }
            }
        },
        {
            "from": [7.0, 4.5, 7.5],
            "to":   [9.0, 5.0,  8.5],
            "faces": {
                "down":  { "uv": [7,0,9,1],    "texture": "#brace" },
                "north": { "uv": [7,11,9,12],  "texture": "#brace" },
                "south": { "uv": [7,11,9,12],  "texture": "#brace" }
            }
        }
    ]
}

with open(os.path.join(MOD_DIR, "pipe_brace_center.json"), "w") as f:
    json.dump(brace_center, f, indent=2)
print("  saved  pipe_brace_center.json")

# ── Blockstate updates ─────────────────────────────────────────────────────────

BRACE_CENTER_RULE = {
    "when":  { "locked": "true" },
    "apply": { "model": "copperpipes:block/pipe_brace_center" }
}

def arrow_rules(model_name):
    return [
        { "when": {"facing":"south"}, "apply": {"model": f"copperpipes:block/{model_name}"} },
        { "when": {"facing":"north"}, "apply": {"model": f"copperpipes:block/{model_name}", "y": 180} },
        { "when": {"facing":"east"},  "apply": {"model": f"copperpipes:block/{model_name}", "y": 270} },
        { "when": {"facing":"west"},  "apply": {"model": f"copperpipes:block/{model_name}", "y": 90} },
        { "when": {"facing":"up"},    "apply": {"model": f"copperpipes:block/{model_name}", "x": 90} },
        { "when": {"facing":"down"},  "apply": {"model": f"copperpipes:block/{model_name}", "x": 270} },
    ]

FILE_ARROW_MODEL = {
    "copper_pipe.json":                 "pipe_arrow_south",
    "exposed_copper_pipe.json":         "exposed_pipe_arrow_south",
    "weathered_copper_pipe.json":       "weathered_pipe_arrow_south",
    "oxidized_copper_pipe.json":        "oxidized_pipe_arrow_south",
    "waxed_copper_pipe.json":           "pipe_arrow_south",
    "waxed_exposed_copper_pipe.json":   "exposed_pipe_arrow_south",
    "waxed_weathered_copper_pipe.json": "weathered_pipe_arrow_south",
    "waxed_oxidized_copper_pipe.json":  "oxidized_pipe_arrow_south",
}

for fname, arrow_model in FILE_ARROW_MODEL.items():
    path = os.path.join(BS_DIR, fname)
    with open(path, "r", encoding="utf-8-sig") as f:
        data = json.load(f)

    kept = [
        r for r in data.get("multipart", [])
        if not r.get("apply", {}).get("model", "").endswith("pipe_brace_south")
        and not r.get("apply", {}).get("model", "").endswith("pipe_arrow_south")
    ]
    kept.append(BRACE_CENTER_RULE)
    kept.extend(arrow_rules(arrow_model))
    data["multipart"] = kept

    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"  updated {fname}")

print("Done.")

#!/usr/bin/env python3
"""Remove uvlock from arrow rules in all 8 pipe blockstate files."""
import os, json

ROOT   = os.path.dirname(__file__)
BS_DIR = os.path.join(ROOT, "src","main","resources","assets","copperpipes","blockstates")

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

    changed = False
    for rule in data.get("multipart", []):
        apply_block = rule.get("apply", {})
        if apply_block.get("model", "").endswith("pipe_arrow_south"):
            if "uvlock" in apply_block:
                del apply_block["uvlock"]
                changed = True

    if changed:
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
        print(f"  fixed  {fname}")
    else:
        print(f"  skip   {fname}  (no uvlock found)")

print("Done.")

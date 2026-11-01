"""
The four fishing shards (Deep Bite, Treasure Hunter, Twin Catch, Mythic Lure), made the way every other shard is: the
crystal of telekinesis.bbmodel recoloured, and a pixel emblem on its back — a hook, a little chest, two fish, a glowing
lure. Writes <id>.bbmodel here, and the pack's model and texture.

    python3 fishing_shards.py
"""
import base64
import colorsys
import copy
import io
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, "..", "..", "src", "main", "resources", "de", "raindancer", "modules", "enchants", "pack",
                    "assets", "rainsenchants")
TEMPLATE = "telekinesis"

# crystal colour; emblem swatches a (main), b (highlight), c (accent), d (dark); the emblem, top row first
SHARDS = {
    "deep_bite": ("#2f6fd6", {"a": "#c8ccd6", "b": "#ffffff", "c": "#8a90a0", "d": "#4a4e5a"},
                  ["....a.",
                   "....a.",
                   "....a.",
                   "a...a.",
                   "ab.ba.",
                   ".aaa.."]),
    "treasure_hunter": ("#e8b830", {"a": "#f8d050", "b": "#fff0a0", "c": "#8a5a2a", "d": "#4a2e14"},
                        [".cccc.",
                         "cccccc",
                         "aaaaaa",
                         "ccbacc",
                         "cccccc",
                         ".dddd."]),
    "twin_catch": ("#f0849a", {"a": "#ff8a6a", "b": "#ffd0b0", "c": "#d05a40", "d": "#2a1a14"},
                   [".aa.a.",
                    "adaaa.",
                    ".aa.a.",
                    "..bb.b",
                    ".bdbbb",
                    "..bb.b"]),
    "mythic_lure": ("#8a5af0", {"a": "#ffffff", "b": "#7ff6ff", "c": "#3dd8e0", "d": "#3a3346"},
                    ["..d...",
                     "...d..",
                     "...d..",
                     "..bb..",
                     ".baab.",
                     "..cc.."]),
}
# where each swatch sits in the 32x32 atlas (pixel rectangles on the right half the template keeps for colours)
SWATCH = {"a": (16, 0), "b": (24, 0), "c": (16, 8), "d": (24, 8)}


def hexrgb(text):
    text = text.lstrip("#")
    return tuple(int(text[i:i + 2], 16) for i in (0, 2, 4))


def recolour_crystal(atlas, colour):
    """The crystal's half of the atlas (left, top 16 rows) moved onto the new colour, keeping its light and dark."""
    target = colorsys.rgb_to_hls(*(c / 255 for c in hexrgb(colour)))
    px = atlas.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            rr, gg, bb = colorsys.hls_to_rgb(target[0], min(0.95, l * (0.6 + target[1] * 0.8)),
                                             min(1.0, max(0.25, target[2] * 0.9)))
            px[x, y] = (int(rr * 255), int(gg * 255), int(bb * 255), a)


def paint_swatches(atlas, swatches):
    for key, (x0, y0) in SWATCH.items():
        base = hexrgb(swatches[key])
        for y in range(8):
            for x in range(8):
                shade = 1.0 + (0.08 if (x + y) % 5 == 0 else 0) - (0.06 if (x * 3 + y) % 7 == 0 else 0)
                atlas.putpixel((x0 + x, y0 + y), tuple(max(0, min(255, int(c * shade))) for c in base) + (255,))


def emblem_elements(pattern, template_face):
    """Row runs of the same swatch merged into one box: 1 unit tall, on the crystal's back."""
    elements = []
    for row, line in enumerate(pattern):
        y1 = 9 - row
        col = 0
        while col < len(line):
            key = line[col]
            if key == ".":
                col += 1
                continue
            end = col
            while end + 1 < len(line) and line[end + 1] == key:
                end += 1
            x0, x1 = 5 + col, 5 + end + 1
            sx, sy = SWATCH[key]
            uv = [sx + 2, sy + 2, sx + 2 + (x1 - x0), sy + 3]
            faces = {side: dict(template_face, uv=uv) for side in ("north", "east", "south", "west", "up", "down")}
            elements.append({"name": "emblem_%d_%d" % (row, col), "from": [x0, y1 - 1, 9.5], "to": [x1, y1, 10.5],
                             "faces": faces})
            col = end + 1
    return elements


def main():
    template = json.load(open(os.path.join(HERE, TEMPLATE + ".bbmodel")))
    crystal = [e for e in template["elements"] if not e["name"].startswith("emblem")]
    source = template["textures"][0]["source"]
    atlas_base = Image.open(io.BytesIO(base64.b64decode(source.split(",", 1)[1]))).convert("RGBA")
    face = next(iter(crystal[0]["faces"].values()))
    java_template = json.load(open(os.path.join(PACK, "models", "item", "shard", TEMPLATE + ".json")))
    for shard, (colour, swatches, pattern) in SHARDS.items():
        atlas = atlas_base.copy()
        recolour_crystal(atlas, colour)
        paint_swatches(atlas, swatches)
        model = copy.deepcopy(template)
        model["name"] = shard
        emblem = emblem_elements(pattern, face)
        model["elements"] = copy.deepcopy(crystal) + emblem
        for element in model["elements"]:
            element.pop("uuid", None)
        model["outliner"] = []
        model["groups"] = []
        buffer = io.BytesIO()
        atlas.save(buffer, "PNG")
        texture = model["textures"][0]
        texture["name"] = shard
        texture["source"] = "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode()
        texture.pop("path", None)
        with open(os.path.join(HERE, shard + ".bbmodel"), "w") as out:
            json.dump(model, out)
        atlas.save(os.path.join(PACK, "textures", "item", "shard", shard + ".png"))
        java = {key: value for key, value in java_template.items() if key != "elements"}
        java["textures"] = {"0": "rainsenchants:item/shard/" + shard, "particle": "rainsenchants:item/shard/" + shard}
        java["elements"] = []
        for element in model["elements"]:
            java["elements"].append({
                "name": element["name"], "from": element["from"], "to": element["to"],
                **({"rotation": element["rotation"]} if isinstance(element.get("rotation"), dict) else {}),
                "faces": {side: {"uv": [v / 2 for v in f["uv"]], "texture": "#0"}
                          for side, f in element["faces"].items()}})
        with open(os.path.join(PACK, "models", "item", "shard", shard + ".json"), "w") as out:
            json.dump(java, out, indent="\t")
        print(shard, len(java["elements"]), "elements")


if __name__ == "__main__":
    main()

"""Coherent, looping lightning: every bolt drifts and bends a little per frame, its brightness breathes."""
import math, random, json, sys
from PIL import Image
OUT = sys.argv[1]
S, Q, F = 64, 32, 48
TAU = 2 * math.pi

def wave(f, m, ph):
    return math.sin(TAU * m * f / F + ph)

class Bolt:
    def __init__(self, rnd, jag, drift, branches, dim):
        self.sa = rnd.randrange(4); self.sb = (self.sa + rnd.choice([1, 2, 3])) % 4
        self.ta, self.tb = rnd.uniform(6, Q - 6), rnd.uniform(6, Q - 6)
        self.pa, self.pb = rnd.uniform(0, TAU), rnd.uniform(0, TAU)
        self.ma, self.mb = rnd.choice([1, 2]), rnd.choice([1, 2])
        self.jag, self.drift = jag, drift
        self.offs = [(rnd.choice([1, 2, 3]), rnd.uniform(0, TAU), rnd.uniform(-1, 1)) for _ in range(64)]
        self.light = (rnd.choice([1, 2]), rnd.uniform(0, TAU))
        self.dim = dim
        self.branches = [(rnd.uniform(0.25, 0.75), rnd.uniform(0, TAU), rnd.uniform(5, 10), rnd.uniform(0, TAU),
                          rnd.choice([1, 2])) for _ in range(branches)]

    @staticmethod
    def edge(side, t):
        t = max(2, min(Q - 3, t))
        return [(t, 0), (Q - 1, t), (t, Q - 1), (0, t)][side]

    def points(self, f, a, b, jag, depth, seed):
        pts = [a, b]
        span = math.dist(a, b)
        k = seed
        for d in range(depth):
            new = []
            for i in range(len(pts) - 1):
                (x0, y0), (x1, y1) = pts[i], pts[i + 1]
                dx, dy = x1 - x0, y1 - y0; L = math.hypot(dx, dy) or 1
                m, ph, base = self.offs[k % len(self.offs)]; k += 1
                off = (0.6 * base + 0.4 * wave(f, m, ph)) * jag * span / (2 ** (d + 2))
                new += [pts[i], ((x0 + x1) / 2 - dy / L * off, (y0 + y1) / 2 + dx / L * off)]
            new.append(pts[-1]); pts = new
        return pts

    def brightness(self, f):
        m, ph = self.light
        return self.dim * (0.62 + 0.38 * wave(f, m, ph))

    def paths(self, f):
        a = self.edge(self.sa, self.ta + self.drift * wave(f, self.ma, self.pa))
        b = self.edge(self.sb, self.tb + self.drift * wave(f, self.mb, self.pb))
        main = self.points(f, a, b, self.jag, 5, 0)
        out = [main]
        for i, (at, ang, r, ph, m) in enumerate(self.branches):
            p = main[int(at * (len(main) - 1))]
            ang2 = ang + 0.5 * wave(f, m, ph)
            end = (p[0] + math.cos(ang2) * r, p[1] + math.sin(ang2) * r)
            out.append(self.points(f, p, end, self.jag, 3, 20 + 7 * i))
        return out

def segment(a, b):
    n = int(max(abs(b[0] - a[0]), abs(b[1] - a[1]))) + 1
    return {(round(a[0] + (b[0] - a[0]) * i / n), round(a[1] + (b[1] - a[1]) * i / n)) for i in range(n + 1)}

def paint(img, path, light, glow):
    px = img.load()
    core = set()
    for i in range(len(path) - 1):
        core |= segment(path[i], path[i + 1])
    def put(p, rgb, alpha):
        x, y = p
        if 0 <= x < Q and 0 <= y < Q:
            a = int(alpha * light)
            if px[x, y][3] < a: px[x, y] = rgb + (a,)
    if glow:
        for (x, y) in core:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1): put((x + dx, y + dy), (110, 195, 255), 150)
    for p in core: put(p, (225, 248, 255), 255)

def texture(name, quads, glow):
    sheet = Image.new("RGBA", (S, S * F), (0, 0, 0, 0))
    for f in range(F):
        frame = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        for qi, (x, y) in enumerate([(0, 0), (Q, 0), (0, Q), (Q, Q)]):
            q = Image.new("RGBA", (Q, Q), (0, 0, 0, 0))
            for bolt in quads[qi]:
                light = bolt.brightness(f)
                for path in bolt.paths(f):
                    paint(q, path, light, glow)
            frame.paste(q, (x, y))
        sheet.paste(frame, (0, S * f))
    sheet.save(f"{OUT}/{name}.png")
    json.dump({"animation": {"frametime": 1}}, open(f"{OUT}/{name}.png.mcmeta", "w"), indent=2)

# Recharge: one pool of bolts per quadrant; each stage shows more of the same bolts, so it builds instead of reshuffling.
rnd = random.Random(2026)
pool = [[Bolt(rnd, 1.1, 4, b, d) for b, d in ((0, 0.55), (1, 0.8), (0, 0.9), (1, 1.0))] for _ in range(4)]
for stage, count in ((1, 1), (2, 2), (3, 3), (4, 4)):
    texture(f"mjolnir_lightning_{stage}", [q[:count] for q in pool], glow=True)
# Ready: a quiet cage close round the head, slower and thinner.
rnd = random.Random(118)
texture("mjolnir_cage", [[Bolt(rnd, 1.2, 2.5, 1, 0.95), Bolt(rnd, 1.2, 2.5, 0, 0.8)] for _ in range(4)], glow=False)
texture("mjolnir_cage_outer", [[Bolt(rnd, 1.3, 3, 1, 0.7)] for _ in range(4)], glow=False)

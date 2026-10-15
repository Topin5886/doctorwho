"""
Aperçu 3D rapide des modèles GeckoLib (.geo.json + texture), sans lancer Minecraft.

    python tools/preview.py dalek/bronze cyberman ...   -> tools/preview.png

Rendu logiciel simple (pose de repos, sans animation) vu de face, légèrement de côté.
Sert à itérer vite sur le design des créatures.
"""
import json
import math
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(__file__))
from generate_assets import Image, ROOT  # noqa: E402


def read_png(path):
    d = open(path, "rb").read()
    i, idat, w, h = 8, b"", 0, 0
    while i < len(d):
        n = struct.unpack(">I", d[i:i + 4])[0]
        t, c = d[i + 4:i + 8], d[i + 8:i + 8 + n]
        if t == b"IHDR":
            w, h = struct.unpack(">II", c[:8])
        elif t == b"IDAT":
            idat += c
        i += 12 + n
    raw = zlib.decompress(idat)
    stride = w * 4 + 1
    return w, h, [raw[y * stride + 1:(y + 1) * stride] for y in range(h)]


def rot_y(p, pivot, deg):
    a = math.radians(deg)
    x, z = p[0] - pivot[0], p[2] - pivot[2]
    return (pivot[0] + x * math.cos(a) + z * math.sin(a), p[1], pivot[2] - x * math.sin(a) + z * math.cos(a))


def rot_x(p, pivot, deg):
    a = math.radians(deg)
    y, z = p[1] - pivot[1], p[2] - pivot[2]
    return (p[0], pivot[1] + y * math.cos(a) - z * math.sin(a), pivot[2] + y * math.sin(a) + z * math.cos(a))


def bone_transform(bones, bone, p, pose):
    """Applique la rotation de l'os (et de ses parents) à un point. Convention Blockbench : x négatif = vers l'avant."""
    b = bone
    while b is not None:
        rx, ry, rz = [a + c for a, c in zip(b.get("rotation", [0, 0, 0]), pose.get(b["name"], [0, 0, 0]))]
        pv = b.get("pivot", [0, 0, 0])
        if rx:
            p = rot_x(p, pv, -rx)
        if ry:
            p = rot_y(p, pv, ry)
        if rz:
            a = math.radians(rz)
            x, y = p[0] - pv[0], p[1] - pv[1]
            p = (pv[0] + x * math.cos(a) - y * math.sin(a), pv[1] + x * math.sin(a) + y * math.cos(a), p[2])
        b = bones.get(b.get("parent"))
    return p


def faces_of(cube):
    (ox, oy, oz), (sx, sy, sz) = cube["origin"], cube["size"]
    x0, x1, y0, y1, z0, z1 = ox, ox + sx, oy, oy + sy, oz, oz + sz
    # (nom, origine du rectangle, vecteur « droite » = sens de u, vecteur « bas » = sens de v)
    return {
        "north": ((x0, y1, z0), (sx, 0, 0), (0, -sy, 0)),
        "south": ((x1, y1, z1), (-sx, 0, 0), (0, -sy, 0)),
        "east": ((x1, y1, z0), (0, 0, sz), (0, -sy, 0)),
        "west": ((x0, y1, z1), (0, 0, -sz), (0, -sy, 0)),
        "up": ((x0, y1, z0), (sx, 0, 0), (0, 0, sz)),
        "down": ((x0, y0, z1), (sx, 0, 0), (0, 0, -sz)),
    }


def render(geo_path, tex_path, size=300, yaw=-32, pitch=18, zoom=None, pose=None):
    geo = json.load(open(geo_path, encoding="utf-8"))["minecraft:geometry"][0]
    bones = {b["name"]: b for b in geo["bones"]}
    pose = pose or {}
    tw, th, rows = read_png(tex_path)
    pts = []
    for bone in geo["bones"]:
        for cube in bone.get("cubes", []):
            for name, (o, r, d) in faces_of(cube).items():
                uvd = cube["uv"].get(name)
                if not uvd:
                    continue
                pts.append((bone, cube, name, o, r, d, uvd))
    zbuf = {}
    out = Image(size, size)
    cy, sy_ = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    maxy = max(max(c["origin"][1] + c["size"][1] for c in b.get("cubes", [{"origin": [0, 0, 0], "size": [0, 1, 0]}])) for b in geo["bones"])
    scale = zoom or (size * 0.82 / max(maxy, 24))
    normals = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}

    def cam(p):
        x, y, z = p
        x, z = x * cy + z * sy_, -x * sy_ + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return x, y, z

    for bone, cube, name, o, r, d, uvd in pts:
        u0, v0 = uvd["uv"]
        uw, vh = uvd["uv_size"]
        nrm = normals[name]
        # normale après rotation de l'os / caméra
        n = cam((nrm[0], nrm[1], nrm[2]))
        light = 0.55 + 0.45 * max(0.0, -n[0] * 0.3 + n[1] * 0.7 + -n[2] * 0.6)
        if n[2] > 0.02:      # face tournée à l'opposé de la caméra
            continue
        ru = max(2, int(math.hypot(*r) * scale * 1.2))
        rv = max(2, int(math.hypot(*d) * scale * 1.2))
        for i in range(ru + 1):
            for j in range(rv + 1):
                a, b = i / ru, j / rv
                p = (o[0] + r[0] * a + d[0] * b, o[1] + r[1] * a + d[1] * b, o[2] + r[2] * a + d[2] * b)
                p = bone_transform(bones, bone, p, pose)
                x, y, z = cam(p)
                sx, sy = int(size / 2 + x * scale), int(size * 0.92 - y * scale)
                if not (0 <= sx < size and 0 <= sy < size):
                    continue
                if z < zbuf.get((sx, sy), 1e9):
                    tx = min(tw - 1, int(u0 + min(a, 0.999) * uw))
                    ty = min(th - 1, int(v0 + min(b, 0.999) * vh))
                    px = rows[ty][tx * 4:tx * 4 + 4]
                    if px[3] > 20:
                        zbuf[(sx, sy)] = z
                        out.set(sx, sy, (int(px[0] * light), int(px[1] * light), int(px[2] * light)))
    return out


def load_pose(name, anim, t=None):
    path = os.path.join(ROOT, "animations", "entity", name + ".animation.json")
    data = json.load(open(path, encoding="utf-8"))["animations"]["animation.%s.%s" % (name, anim)]
    pose = {}
    for bone, ch in data["bones"].items():
        keys = ch.get("rotation")
        if keys:
            times = sorted(keys, key=float)
            k = min(times, key=lambda x: abs(float(x) - (t if t is not None else 0)))
            v = keys[k]
            pose[bone] = v if isinstance(v, list) else v.get("post", [0, 0, 0])
    return pose


def sheet(entries, cell=300, cols=4):
    rows = (len(entries) + cols - 1) // cols
    sheet_img = Image(cell * cols, cell * rows)
    sheet_img.rect(0, 0, sheet_img.w, sheet_img.h, (46, 52, 66))
    for k, (label, geo, tex, pose) in enumerate(entries):
        img = render(geo, tex, cell, pose=pose)
        ox, oy = (k % cols) * cell, (k // cols) * cell
        for y in range(cell):
            for x in range(cell):
                i = (y * cell + x) * 4
                if img.px[i + 3]:
                    sheet_img.set(ox + x, oy + y, tuple(img.px[i:i + 3]))
    return sheet_img


if __name__ == "__main__":
    ent = os.path.join(ROOT, "geo", "entity")
    tx = os.path.join(ROOT, "textures", "entity")
    names = sys.argv[1:] or ["dalek/bronze", "cyberman", "weeping_angel", "ood", "adipose", "k9", "sontarian", "silence", "zygon", "slitheen"]
    entries = []
    for n in names:
        n, _, anim = n.partition("@")
        base, _, variant = n.partition("/")
        pose = None
        if anim:
            anim, _, t = anim.partition("@")
            pose = load_pose(base, anim, float(t) if t else None)
        entries.append((n, os.path.join(ent, base + ".geo.json"),
                        os.path.join(tx, base, variant + ".png") if variant else os.path.join(tx, base + ".png"), pose))
    out = os.path.join(os.path.dirname(__file__), "preview.png")
    sheet(entries).save(out)
    print("OK", out)

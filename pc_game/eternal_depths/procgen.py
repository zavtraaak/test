"""Procedural dungeon generation. Multiple biomes and shapes."""
from __future__ import annotations
import random
from . import tiles, gamemap
from .constants import MAP_W, MAP_H


BIOME_BY_LEVEL = [
    (1, "cavern"), (2, "cavern"), (3, "sewer"), (4, "sewer"),
    (5, "crypt"), (6, "crypt"), (7, "library"), (8, "forest"),
    (9, "forest"), (10, "ice"), (11, "ice"), (12, "crypt"),
    (13, "lava"), (14, "lava"), (15, "lava"), (16, "void"),
    (17, "void"), (18, "cavern"), (19, "crypt"), (20, "ice"),
    (21, "lava"), (22, "void"), (23, "void"), (24, "library"),
    (25, "void"), (26, "lava"), (27, "ice"), (28, "void"),
    (29, "void"), (30, "void"),
]


def biome_for(level: int) -> str:
    for lv, b in BIOME_BY_LEVEL:
        if lv == level:
            return b
    return "cavern"


class Rect:
    def __init__(self, x, y, w, h):
        self.x1, self.y1 = x, y
        self.x2, self.y2 = x + w, y + h

    def center(self):
        return ((self.x1 + self.x2) // 2, (self.y1 + self.y2) // 2)

    def intersects(self, other, pad=1):
        return (self.x1 - pad <= other.x2 and self.x2 + pad >= other.x1
                and self.y1 - pad <= other.y2 and self.y2 + pad >= other.y1)

    def random_inside(self, rng):
        return (rng.randint(self.x1 + 1, self.x2 - 2),
                rng.randint(self.y1 + 1, self.y2 - 2))


def carve_room(gm, room, biome):
    for x in range(room.x1 + 1, room.x2):
        for y in range(room.y1 + 1, room.y2):
            gm.tiles[x][y] = tiles.make_floor(biome)


def carve_h(gm, x1, x2, y, biome):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        gm.tiles[x][y] = tiles.make_floor(biome)


def carve_v(gm, y1, y2, x, biome):
    for y in range(min(y1, y2), max(y1, y2) + 1):
        gm.tiles[x][y] = tiles.make_floor(biome)


def generate_rooms_corridors(level, rng, w=MAP_W, h=MAP_H, max_rooms=20,
                             room_min=4, room_max=10, biome=None):
    biome = biome or biome_for(level)
    gm = gamemap.GameMap(w, h, biome=biome, level=level)
    rooms = []
    for _ in range(max_rooms):
        rw = rng.randint(room_min, room_max)
        rh = rng.randint(room_min, room_max)
        x = rng.randint(1, w - rw - 2)
        y = rng.randint(1, h - rh - 2)
        new_room = Rect(x, y, rw, rh)
        if any(new_room.intersects(r) for r in rooms):
            continue
        carve_room(gm, new_room, biome)
        if rooms:
            (px, py) = rooms[-1].center()
            (nx, ny) = new_room.center()
            if rng.random() < 0.5:
                carve_h(gm, px, nx, py, biome)
                carve_v(gm, py, ny, nx, biome)
            else:
                carve_v(gm, py, ny, px, biome)
                carve_h(gm, px, nx, ny, biome)
        rooms.append(new_room)
    if not rooms:
        # fallback: full open area
        for x in range(1, w - 1):
            for y in range(1, h - 1):
                gm.tiles[x][y] = tiles.make_floor(biome)
        rooms = [Rect(1, 1, w - 2, h - 2)]
    # Stairs
    sx, sy = rooms[0].center()
    if level > 1:
        gm.tiles[sx][sy] = tiles.make_stairs_up(biome)
        gm.up_pos = (sx, sy)
    else:
        gm.up_pos = (sx, sy)
    if level < 30:
        ex, ey = rooms[-1].center()
        gm.tiles[ex][ey] = tiles.make_stairs_down(biome)
        gm.down_pos = (ex, ey)
    # Doors
    add_doors(gm, rng, biome)
    # Features
    add_features(gm, rng, rooms, biome, level)
    gm.rooms = rooms
    return gm


def add_doors(gm, rng, biome):
    for x in range(1, gm.w - 1):
        for y in range(1, gm.h - 1):
            if gm.tiles[x][y].name != "floor":
                continue
            n = sum(
                1 for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1))
                if gm.tiles[x + dx][y + dy].name == "wall"
            )
            if n == 2:
                wallx = (gm.tiles[x - 1][y].name == "wall"
                         and gm.tiles[x + 1][y].name == "wall")
                wally = (gm.tiles[x][y - 1].name == "wall"
                         and gm.tiles[x][y + 1].name == "wall")
                if (wallx or wally) and rng.random() < 0.10:
                    gm.tiles[x][y] = tiles.make_door(biome, open_=False)


def cellular_cave(level, rng, w=MAP_W, h=MAP_H, biome=None, fill=0.45,
                  iterations=4):
    biome = biome or biome_for(level)
    gm = gamemap.GameMap(w, h, biome=biome, level=level)
    grid = [[1 if (rng.random() < fill or x == 0 or y == 0
                   or x == w - 1 or y == h - 1) else 0
             for y in range(h)] for x in range(w)]
    for _ in range(iterations):
        new = [[0] * h for _ in range(w)]
        for x in range(w):
            for y in range(h):
                if x == 0 or y == 0 or x == w - 1 or y == h - 1:
                    new[x][y] = 1
                    continue
                n = 0
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        if dx == 0 and dy == 0:
                            continue
                        if grid[x + dx][y + dy] == 1:
                            n += 1
                new[x][y] = 1 if n >= 5 or grid[x][y] == 1 and n >= 4 else 0
        grid = new
    for x in range(w):
        for y in range(h):
            if grid[x][y] == 0:
                gm.tiles[x][y] = tiles.make_floor(biome)
    # Find largest connected region of floors
    seen = [[False] * h for _ in range(w)]
    components = []
    for sx in range(w):
        for sy in range(h):
            if grid[sx][sy] == 0 and not seen[sx][sy]:
                stack = [(sx, sy)]
                comp = []
                while stack:
                    cx, cy = stack.pop()
                    if seen[cx][cy]:
                        continue
                    seen[cx][cy] = True
                    if grid[cx][cy] == 0:
                        comp.append((cx, cy))
                        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                            nx, ny = cx + dx, cy + dy
                            if 0 <= nx < w and 0 <= ny < h and not seen[nx][ny]:
                                stack.append((nx, ny))
                components.append(comp)
    if not components:
        # fallback
        return generate_rooms_corridors(level, rng, w, h, biome=biome)
    components.sort(key=len, reverse=True)
    main = set(components[0])
    # Wall off other components
    for comp in components[1:]:
        for x, y in comp:
            gm.tiles[x][y] = tiles.make_wall(biome)
    # Stairs
    main_list = list(main)
    rng.shuffle(main_list)
    sx, sy = main_list[0]
    gm.tiles[sx][sy] = tiles.make_stairs_up(biome) if level > 1 else tiles.make_floor(biome)
    gm.up_pos = (sx, sy)
    if level < 30:
        farthest = max(main_list, key=lambda p: (p[0] - sx) ** 2 + (p[1] - sy) ** 2)
        gm.tiles[farthest[0]][farthest[1]] = tiles.make_stairs_down(biome)
        gm.down_pos = farthest
    # Build rooms list (largest cluster split into pseudo rooms by quadrants)
    gm.rooms = [type("R", (), {"center": lambda self=None, p=p: p,
                               "random_inside": lambda self=None, rng=rng, p=p: p})()
                for p in main_list[:30]]
    add_features_caves(gm, rng, main_list, biome, level)
    return gm


def add_features(gm, rng, rooms, biome, level):
    # Skip first and last rooms (stairs)
    candidate_rooms = rooms[1:-1] if len(rooms) > 2 else rooms[:]
    if candidate_rooms:
        # Fountains
        if rng.random() < 0.5:
            r = rng.choice(candidate_rooms)
            x, y = r.random_inside(rng)
            if gm.tiles[x][y].name == "floor":
                gm.tiles[x][y] = tiles.make_fountain(biome)
        # Altars
        if rng.random() < 0.25:
            r = rng.choice(candidate_rooms)
            x, y = r.random_inside(rng)
            if gm.tiles[x][y].name == "floor":
                gm.tiles[x][y] = tiles.make_altar(biome)
        # Chests
        for _ in range(rng.randint(1, 3)):
            r = rng.choice(candidate_rooms)
            x, y = r.random_inside(rng)
            if gm.tiles[x][y].name == "floor":
                gm.tiles[x][y] = tiles.make_chest(biome)
        # Shop on level 2+
        if level >= 2 and rng.random() < 0.18:
            r = rng.choice(candidate_rooms)
            for x in range(r.x1 + 1, r.x2):
                for y in range(r.y1 + 1, r.y2):
                    if gm.tiles[x][y].name == "floor":
                        gm.tiles[x][y] = tiles.make_shop_floor(biome)
            gm.shop_origin = r.center()


def add_features_caves(gm, rng, floors, biome, level):
    for _ in range(rng.randint(2, 4)):
        x, y = rng.choice(floors)
        if gm.tiles[x][y].name == "floor":
            gm.tiles[x][y] = tiles.make_chest(biome)
    if rng.random() < 0.4:
        x, y = rng.choice(floors)
        if gm.tiles[x][y].name == "floor":
            gm.tiles[x][y] = tiles.make_fountain(biome)
    if rng.random() < 0.2:
        x, y = rng.choice(floors)
        if gm.tiles[x][y].name == "floor":
            gm.tiles[x][y] = tiles.make_altar(biome)


def generate(level, rng):
    biome = biome_for(level)
    # Bosses fight on dedicated arena rooms
    if level == 30:
        return generate_rooms_corridors(level, rng, max_rooms=8,
                                        room_min=8, room_max=14, biome=biome)
    if biome in ("cavern", "lava", "void"):
        gm = cellular_cave(level, rng, biome=biome)
    else:
        gm = generate_rooms_corridors(level, rng, biome=biome)
    return gm

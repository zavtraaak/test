"""Map structure with tiles, FOV memory, entities."""
from __future__ import annotations
from . import tiles
from .constants import MAP_W, MAP_H


class GameMap:
    def __init__(self, w=MAP_W, h=MAP_H, biome="cavern", level=1):
        self.w = w
        self.h = h
        self.biome = biome
        self.level = level
        self.tiles: list[list[tiles.Tile]] = [
            [tiles.make_wall(biome) for _ in range(h)] for _ in range(w)
        ]
        self.explored: list[list[bool]] = [[False] * h for _ in range(w)]
        self.visible: list[list[bool]] = [[False] * h for _ in range(w)]
        self.entities = []  # list of Entity
        self.items = []  # ground items: list[(x,y,item)]
        self.up_pos = None
        self.down_pos = None
        self.shop_origin = None  # (x,y) origin of shop room (used by NPCs)

    def in_bounds(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def walkable(self, x, y):
        if not self.in_bounds(x, y):
            return False
        return self.tiles[x][y].walkable

    def transparent(self, x, y):
        if not self.in_bounds(x, y):
            return False
        return self.tiles[x][y].transparent

    def tile(self, x, y):
        return self.tiles[x][y]

    def set_tile(self, x, y, t):
        self.tiles[x][y] = t

    def blocking_entity_at(self, x, y):
        for e in self.entities:
            if e.alive and e.blocks and e.x == x and e.y == y:
                return e
        return None

    def entity_at(self, x, y):
        for e in self.entities:
            if e.alive and e.x == x and e.y == y:
                return e
        return None

    def items_at(self, x, y):
        return [it for (ix, iy, it) in self.items if ix == x and iy == y]

    def add_item(self, x, y, item):
        self.items.append((x, y, item))

    def remove_item(self, x, y, item):
        for i, (ix, iy, it) in enumerate(self.items):
            if ix == x and iy == y and it is item:
                self.items.pop(i)
                return

    def to_dict(self):
        return {
            "w": self.w, "h": self.h,
            "biome": self.biome, "level": self.level,
            "tiles": [[t.to_dict() for t in col] for col in self.tiles],
            "explored": self.explored,
            "up_pos": self.up_pos, "down_pos": self.down_pos,
            "shop_origin": self.shop_origin,
        }

    @classmethod
    def from_dict(cls, d):
        gm = cls(d["w"], d["h"], d["biome"], d["level"])
        gm.tiles = [[tiles.Tile.from_dict(t) for t in col] for col in d["tiles"]]
        gm.explored = [list(r) for r in d["explored"]]
        gm.up_pos = tuple(d["up_pos"]) if d.get("up_pos") else None
        gm.down_pos = tuple(d["down_pos"]) if d.get("down_pos") else None
        gm.shop_origin = tuple(d["shop_origin"]) if d.get("shop_origin") else None
        return gm

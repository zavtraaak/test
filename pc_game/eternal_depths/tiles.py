"""Tile types."""
from __future__ import annotations
from dataclasses import dataclass
from . import colors


@dataclass
class Tile:
    name: str
    glyph: str
    fg: tuple
    bg: tuple
    walkable: bool
    transparent: bool
    door: bool = False
    open: bool = True
    stairs_down: bool = False
    stairs_up: bool = False
    altar: bool = False
    fountain: bool = False
    chest: bool = False
    shop: bool = False
    trap: str | None = None
    biome: str = "cavern"

    def to_dict(self):
        return {
            "name": self.name,
            "glyph": self.glyph,
            "fg": list(self.fg),
            "bg": list(self.bg),
            "walkable": self.walkable,
            "transparent": self.transparent,
            "door": self.door,
            "open": self.open,
            "stairs_down": self.stairs_down,
            "stairs_up": self.stairs_up,
            "altar": self.altar,
            "fountain": self.fountain,
            "chest": self.chest,
            "shop": self.shop,
            "trap": self.trap,
            "biome": self.biome,
        }

    @classmethod
    def from_dict(cls, d):
        return cls(
            name=d["name"], glyph=d["glyph"],
            fg=tuple(d["fg"]), bg=tuple(d["bg"]),
            walkable=d["walkable"], transparent=d["transparent"],
            door=d["door"], open=d.get("open", True),
            stairs_down=d.get("stairs_down", False),
            stairs_up=d.get("stairs_up", False),
            altar=d.get("altar", False),
            fountain=d.get("fountain", False),
            chest=d.get("chest", False),
            shop=d.get("shop", False),
            trap=d.get("trap"),
            biome=d.get("biome", "cavern"),
        )


def make_floor(biome="cavern"):
    palettes = {
        "cavern": (colors.FLOOR_FG, colors.FLOOR_BG),
        "crypt": ((100, 100, 130), (12, 12, 20)),
        "forest": ((90, 130, 70), (10, 16, 12)),
        "sewer": ((100, 130, 70), (12, 16, 10)),
        "ice": ((180, 220, 240), (20, 30, 40)),
        "lava": ((220, 130, 60), (40, 10, 10)),
        "void": ((180, 80, 220), (10, 8, 16)),
        "library": ((180, 150, 90), (20, 16, 10)),
    }
    fg, bg = palettes.get(biome, palettes["cavern"])
    return Tile("floor", ".", fg, bg, True, True, biome=biome)


def make_wall(biome="cavern"):
    palettes = {
        "cavern": (colors.WALL_FG, colors.WALL_BG),
        "crypt": ((150, 150, 175), (24, 22, 32)),
        "forest": ((60, 150, 60), (10, 20, 10)),
        "sewer": ((90, 130, 70), (16, 22, 14)),
        "ice": ((220, 235, 245), (30, 50, 70)),
        "lava": ((230, 90, 40), (60, 16, 10)),
        "void": ((220, 130, 240), (16, 10, 24)),
        "library": ((200, 160, 100), (40, 28, 18)),
    }
    fg, bg = palettes.get(biome, palettes["cavern"])
    return Tile("wall", "#", fg, bg, False, False, biome=biome)


def make_door(biome="cavern", open_=False):
    fg = (200, 150, 80)
    bg = (30, 24, 18)
    glyph = "/" if open_ else "+"
    return Tile("door", glyph, fg, bg, open_, open_, door=True, open=open_, biome=biome)


def make_stairs_down(biome="cavern"):
    return Tile("stairs down", ">", (240, 240, 240), (20, 20, 30), True, True,
                stairs_down=True, biome=biome)


def make_stairs_up(biome="cavern"):
    return Tile("stairs up", "<", (240, 240, 240), (20, 20, 30), True, True,
                stairs_up=True, biome=biome)


def make_altar(biome="cavern"):
    return Tile("altar", "_", (240, 240, 200), (40, 30, 20), True, True,
                altar=True, biome=biome)


def make_fountain(biome="cavern"):
    return Tile("fountain", "{", (130, 200, 240), (20, 30, 50), True, True,
                fountain=True, biome=biome)


def make_chest(biome="cavern"):
    return Tile("chest", "=", (220, 180, 90), (30, 24, 14), True, True,
                chest=True, biome=biome)


def make_shop_floor(biome="cavern"):
    t = make_floor(biome)
    t.shop = True
    return t


def make_void():
    return Tile("void", " ", (0, 0, 0), (0, 0, 0), False, False)

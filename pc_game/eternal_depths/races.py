"""Character races."""
from __future__ import annotations
from .stats import Stats, Resists


RACES = {
    "Human": {
        "desc": "Versatile and adaptable. +1 to all stats and faster XP.",
        "stats": Stats(1, 1, 1, 1, 1, 1, 1),
        "resists": Resists(),
        "xp_bonus": 0.10,
        "hp_per_con": 1, "mp_per_int": 1,
        "innate": [],
    },
    "Elf": {
        "desc": "Graceful, wise, sees in the dark. +DEX, +INT, +WIS.",
        "stats": Stats(0, 2, -1, 2, 2, 0, 1),
        "resists": Resists(arcane=10),
        "xp_bonus": 0.0,
        "hp_per_con": 1, "mp_per_int": 1,
        "innate": ["dark_vision"],
    },
    "Dwarf": {
        "desc": "Stout, hardy, master craftsman. +STR, +CON, fire resist.",
        "stats": Stats(2, -1, 3, 0, 1, -1, 0),
        "resists": Resists(fire=10, poison=10),
        "xp_bonus": 0.0,
        "hp_per_con": 2, "mp_per_int": 1,
        "innate": ["dark_vision", "smithing"],
    },
    "Orc": {
        "desc": "Brutal warrior, savage strength.",
        "stats": Stats(3, 1, 2, -2, -1, -2, 0),
        "resists": Resists(physical=5),
        "xp_bonus": -0.05,
        "hp_per_con": 2, "mp_per_int": 1,
        "innate": ["bloodrage"],
    },
    "Halfling": {
        "desc": "Small, lucky, nimble.",
        "stats": Stats(-1, 3, 0, 0, 0, 1, 3),
        "resists": Resists(),
        "xp_bonus": 0.05,
        "hp_per_con": 1, "mp_per_int": 1,
        "innate": ["lucky_dodge"],
    },
    "Tiefling": {
        "desc": "Infernal heritage. Fire and dark resist.",
        "stats": Stats(0, 1, 0, 1, 0, 2, 0),
        "resists": Resists(fire=20, dark=20, holy=-10),
        "xp_bonus": 0.0,
        "hp_per_con": 1, "mp_per_int": 1,
        "innate": ["dark_vision", "infernal_blood"],
    },
    "Dragonborn": {
        "desc": "Draconic blood. Breath weapon.",
        "stats": Stats(2, 0, 2, 0, 0, 1, 0),
        "resists": Resists(fire=20, lightning=10, cold=10),
        "xp_bonus": 0.0,
        "hp_per_con": 2, "mp_per_int": 1,
        "innate": ["draconic_breath"],
    },
    "Gnome": {
        "desc": "Tinkerer, scholar. Bonus to magic.",
        "stats": Stats(-1, 1, 0, 3, 1, 1, 1),
        "resists": Resists(arcane=10),
        "xp_bonus": 0.05,
        "hp_per_con": 1, "mp_per_int": 2,
        "innate": ["arcane_savant"],
    },
    "Vampire": {
        "desc": "Undead. Drains life on hit. Hurt by holy. Hated by sun.",
        "stats": Stats(2, 2, 1, 1, 0, 2, 0),
        "resists": Resists(cold=20, dark=30, poison=30, holy=-30),
        "xp_bonus": -0.10,
        "hp_per_con": 1, "mp_per_int": 1,
        "innate": ["dark_vision", "lifedrain", "undead"],
    },
    "Goblin": {
        "desc": "Sneaky, cunning, opportunistic.",
        "stats": Stats(-1, 3, -1, 0, 0, -1, 2),
        "resists": Resists(poison=10),
        "xp_bonus": 0.10,
        "hp_per_con": 1, "mp_per_int": 1,
        "innate": ["sneaky"],
    },
}


def race_keys():
    return list(RACES.keys())

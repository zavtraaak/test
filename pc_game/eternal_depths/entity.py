"""Entity base + Player + Monster classes."""
from __future__ import annotations
from dataclasses import dataclass, field
import random
from .stats import Stats, Resists
from .status_effects import StatusBag
from .skills import SkillBook
from .items import Item, ITEMS, init_charges
from . import items as items_mod
from .constants import PLAYER_BASE_HP, PLAYER_BASE_MP, XP_BASE, XP_GROWTH


def xp_required(level):
    return int(XP_BASE * (XP_GROWTH ** (level - 1)))


@dataclass
class Equipment:
    weapon: Item | None = None
    shield: Item | None = None
    head: Item | None = None
    body: Item | None = None
    hands: Item | None = None
    legs: Item | None = None
    feet: Item | None = None
    neck: Item | None = None
    ring1: Item | None = None
    ring2: Item | None = None
    cloak: Item | None = None

    def slots(self):
        return ["weapon", "shield", "head", "body", "hands", "legs",
                "feet", "neck", "ring1", "ring2", "cloak"]

    def get(self, slot):
        return getattr(self, slot, None)

    def set(self, slot, item):
        setattr(self, slot, item)

    def all_items(self):
        return [(s, self.get(s)) for s in self.slots() if self.get(s)]

    def to_dict(self):
        return {s: (i.to_dict() if i else None) for s, i in
                [(s, self.get(s)) for s in self.slots()]}

    @classmethod
    def from_dict(cls, d):
        eq = cls()
        for s in eq.slots():
            v = d.get(s)
            if v:
                eq.set(s, Item.from_dict(v))
        return eq


@dataclass
class Inventory:
    items: list[Item] = field(default_factory=list)
    capacity: int = 26

    def add(self, item: Item):
        if item.d.stackable:
            for i in self.items:
                if i.def_key == item.def_key and i.cursed == item.cursed and i.blessed == item.blessed:
                    i.stack += item.stack
                    return True
        if len(self.items) >= self.capacity:
            return False
        self.items.append(item)
        return True

    def remove(self, item: Item, n=1):
        if item not in self.items:
            return False
        if item.stack > n:
            item.stack -= n
            return True
        self.items.remove(item)
        return True

    def to_dict(self):
        return {"items": [i.to_dict() for i in self.items],
                "capacity": self.capacity}

    @classmethod
    def from_dict(cls, d):
        inv = cls(capacity=d.get("capacity", 26))
        inv.items = [Item.from_dict(x) for x in d.get("items", [])]
        return inv


class Entity:
    def __init__(self, x, y, glyph, color, name, blocks=True):
        self.x = x
        self.y = y
        self.glyph = glyph
        self.color = color
        self.name = name
        self.blocks = blocks
        self.alive = True
        self.faction = "neutral"
        self.hp = 1
        self.max_hp = 1
        self.mp = 0
        self.max_mp = 0
        self.stats = Stats()
        self.resists = Resists()
        self.statuses = StatusBag()
        self.dr = 0  # damage reduction
        self.ac = 10
        self.attack_dice = "1d2"
        self.dtype = "physical"
        self.crit = 5
        self.xp = 0
        self.kill_xp = 0
        self.speed = 100
        self.energy = 0
        self.flags = set()  # 'undead', 'flying', 'aquatic', 'demon', 'dragon'
        self.death_drops = []
        self.summoned = False
        self.summon_owner = None
        self.summon_duration = 0

    def at(self):
        return (self.x, self.y)

    def alive_status(self):
        return self.alive and self.hp > 0

    def gain_energy(self):
        self.energy += self.speed
        if self.statuses.has("haste"):
            self.energy += 50
        if self.statuses.has("slow"):
            self.energy -= 30

    def take_turn_ready(self):
        return self.energy >= 100

    def consume_turn(self):
        self.energy -= 100


class Player(Entity):
    def __init__(self, x, y, name, race, cls):
        super().__init__(x, y, "@", (240, 230, 200), name)
        self.faction = "player"
        self.race = race
        self.cls = cls  # class name
        from .races import RACES
        from .classes import CLASSES
        race_def = RACES[race]
        cls_def = CLASSES[cls]
        base = Stats(10, 10, 10, 10, 10, 10, 10)
        self.stats = base.add(race_def["stats"]).add(cls_def["stat_bonus"])
        self.resists = race_def["resists"]
        self.race_innate = list(race_def["innate"])
        self.level = 1
        self.xp = 0
        self.xp_bonus = race_def.get("xp_bonus", 0.0)
        self.hp_per_con = race_def.get("hp_per_con", 1)
        self.mp_per_int = race_def.get("mp_per_int", 1)
        self.hp_per_level = cls_def["hp_per_level"]
        self.mp_per_level = cls_def["mp_per_level"]
        self.max_hp = PLAYER_BASE_HP + self.stats.mod("CON") * 3 + self.hp_per_level
        self.max_mp = PLAYER_BASE_MP + self.stats.mod("INT") * 2 + self.mp_per_level
        self.hp = self.max_hp
        self.mp = self.max_mp
        self.ac = 10
        self.crit = 5
        self.attack_dice = "1d2"
        self.inventory = Inventory()
        self.equipment = Equipment()
        self.skills = SkillBook()
        for sk in cls_def["starting_skills"]:
            self.skills.set(sk, 1)
        self.spells = list(cls_def["starting_spells"])
        self.abilities = list(cls_def["abilities"]) + self.race_innate
        self.gold = 0
        self.hunger = 1000
        self.depth = 1
        self.turns = 0
        self.kills = 0
        self.deepest = 1
        self.identifications = set()
        self.god = None
        self.alignment = 0  # -100..100
        self.piety = 0
        self.companions = []  # list of Monster ids? simpler: track summoned via flags
        self.cooldowns = {}  # ability cooldowns
        self.score = 0
        self.achievements = set()
        self.starting_class = cls
        self.starting_race = race

    def gain_xp(self, amount):
        amount = int(amount * (1.0 + self.xp_bonus))
        self.xp += amount
        leveled = False
        while self.xp >= xp_required(self.level):
            self.xp -= xp_required(self.level)
            self.level_up()
            leveled = True
        return leveled

    def level_up(self):
        self.level += 1
        hp_gain = self.hp_per_level + self.hp_per_con * max(1, self.stats.mod("CON"))
        mp_gain = self.mp_per_level + self.mp_per_int * max(1, self.stats.mod("INT"))
        self.max_hp += hp_gain
        self.max_mp += mp_gain
        self.hp = self.max_hp
        self.mp = self.max_mp
        # Stat bump every 4 levels
        if self.level % 4 == 0:
            for s in ["STR", "DEX", "CON", "INT", "WIS"]:
                pass

    def equip_total_stats(self):
        s = Stats()
        for slot, it in self.equipment.all_items():
            s = s.add(it.d.stats)
        return s

    def equip_total_resists(self):
        r = Resists()
        for slot, it in self.equipment.all_items():
            r = r.add(it.d.resists)
        return r

    def effective_stats(self):
        return self.stats.add(self.equip_total_stats())

    def effective_resists(self):
        return self.resists.add(self.equip_total_resists())

    def total_ac(self):
        ac = 10 + self.stats.mod("DEX")
        for slot, it in self.equipment.all_items():
            ac += it.d.ac + it.enchant
        if self.statuses.has("barkskin"):
            ac += 2
        return ac

    def attack_damage(self):
        w = self.equipment.weapon
        if w:
            return w.d.damage, w.d.dtype, (w.d.crit + (w.enchant or 0)), w.enchant
        return "1d3", "physical", 5, 0

    def attack_bonus(self):
        w = self.equipment.weapon
        bonus = self.stats.mod("STR")
        if w and w.d.ranged:
            bonus = self.stats.mod("DEX")
        if self.statuses.has("blessed"):
            bonus += 1
        if self.statuses.has("rage"):
            bonus += 3
        if self.statuses.has("weakened"):
            bonus -= 2
        if w:
            bonus += w.enchant
        return bonus

    def to_dict(self):
        d = {
            "x": self.x, "y": self.y, "name": self.name,
            "race": self.race, "cls": self.cls,
            "stats": self.stats.to_dict(),
            "resists": self.resists.to_dict(),
            "level": self.level, "xp": self.xp,
            "max_hp": self.max_hp, "hp": self.hp,
            "max_mp": self.max_mp, "mp": self.mp,
            "spells": list(self.spells), "abilities": list(self.abilities),
            "skills": self.skills.to_dict(),
            "inventory": self.inventory.to_dict(),
            "equipment": self.equipment.to_dict(),
            "gold": self.gold, "hunger": self.hunger,
            "depth": self.depth, "deepest": self.deepest,
            "turns": self.turns, "kills": self.kills,
            "statuses": self.statuses.to_dict(),
            "identifications": list(self.identifications),
            "alignment": self.alignment, "piety": self.piety,
            "god": self.god, "score": self.score,
            "starting_class": self.starting_class,
            "starting_race": self.starting_race,
            "achievements": list(self.achievements),
            "race_innate": self.race_innate,
            "hp_per_level": self.hp_per_level,
            "mp_per_level": self.mp_per_level,
            "hp_per_con": self.hp_per_con,
            "mp_per_int": self.mp_per_int,
            "xp_bonus": self.xp_bonus,
            "cooldowns": self.cooldowns,
        }
        return d

    @classmethod
    def from_dict(cls, d):
        p = cls(d["x"], d["y"], d["name"], d["race"], d["cls"])
        p.stats = Stats.from_dict(d["stats"])
        p.resists = Resists.from_dict(d["resists"])
        p.level = d["level"]
        p.xp = d["xp"]
        p.max_hp = d["max_hp"]; p.hp = d["hp"]
        p.max_mp = d["max_mp"]; p.mp = d["mp"]
        p.spells = list(d["spells"])
        p.abilities = list(d["abilities"])
        p.skills = SkillBook.from_dict(d["skills"])
        p.inventory = Inventory.from_dict(d["inventory"])
        p.equipment = Equipment.from_dict(d["equipment"])
        p.gold = d["gold"]; p.hunger = d["hunger"]
        p.depth = d["depth"]; p.deepest = d["deepest"]
        p.turns = d["turns"]; p.kills = d["kills"]
        p.statuses = StatusBag.from_dict(d["statuses"])
        p.identifications = set(d["identifications"])
        p.alignment = d["alignment"]; p.piety = d["piety"]
        p.god = d.get("god")
        p.score = d.get("score", 0)
        p.starting_class = d.get("starting_class", p.cls)
        p.starting_race = d.get("starting_race", p.race)
        p.achievements = set(d.get("achievements", []))
        p.race_innate = d.get("race_innate", [])
        p.hp_per_level = d.get("hp_per_level", 6)
        p.mp_per_level = d.get("mp_per_level", 3)
        p.hp_per_con = d.get("hp_per_con", 1)
        p.mp_per_int = d.get("mp_per_int", 1)
        p.xp_bonus = d.get("xp_bonus", 0.0)
        p.cooldowns = d.get("cooldowns", {})
        return p


class Monster(Entity):
    def __init__(self, x, y, mdef):
        super().__init__(x, y, mdef["glyph"], mdef["color"], mdef["name"])
        self.mdef_key = mdef["key"]
        self.faction = mdef.get("faction", "monster")
        self.max_hp = mdef["hp"]
        self.hp = self.max_hp
        self.attack_dice = mdef.get("dmg", "1d4")
        self.dtype = mdef.get("dtype", "physical")
        self.ac = mdef.get("ac", 10)
        self.dr = mdef.get("dr", 0)
        self.crit = mdef.get("crit", 5)
        self.kill_xp = mdef.get("xp", 5)
        self.speed = mdef.get("speed", 100)
        self.flags = set(mdef.get("flags", []))
        self.death_drops = mdef.get("drops", [])
        self.resists = Resists(**mdef.get("resists", {}))
        self.ai = mdef.get("ai", "basic")
        self.spells = mdef.get("spells", [])
        self.special = mdef.get("special", [])
        self.gold = mdef.get("gold", 0)
        self.path = []

    def to_dict(self):
        return {
            "x": self.x, "y": self.y, "key": self.mdef_key,
            "hp": self.hp, "max_hp": self.max_hp,
            "alive": self.alive,
            "energy": self.energy, "speed": self.speed,
            "statuses": self.statuses.to_dict(),
            "summoned": self.summoned,
            "summon_duration": self.summon_duration,
            "faction": self.faction,
        }

    @classmethod
    def from_dict(cls, d, monsters_db):
        m = cls(d["x"], d["y"], monsters_db[d["key"]])
        m.hp = d["hp"]; m.max_hp = d["max_hp"]
        m.alive = d.get("alive", True)
        m.energy = d.get("energy", 0)
        m.speed = d.get("speed", 100)
        m.statuses = StatusBag.from_dict(d["statuses"])
        m.summoned = d.get("summoned", False)
        m.summon_duration = d.get("summon_duration", 0)
        m.faction = d.get("faction", m.faction)
        return m

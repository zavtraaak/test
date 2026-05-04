"""Status effects (buffs/debuffs)."""
from __future__ import annotations

STATUS_DEFS = {
    "poisoned": {"name": "Poisoned", "color": (140, 220, 90), "tick_dmg": ("poison", 1)},
    "burning": {"name": "Burning", "color": (235, 130, 50), "tick_dmg": ("fire", 2)},
    "frozen": {"name": "Frozen", "color": (130, 200, 240), "skip_turn": True},
    "stunned": {"name": "Stunned", "color": (240, 240, 90), "skip_turn": True},
    "blinded": {"name": "Blinded", "color": (90, 90, 90), "fov_shrink": 4},
    "confused": {"name": "Confused", "color": (200, 100, 200), "random_move": True},
    "feared": {"name": "Feared", "color": (160, 90, 220), "flee": True},
    "asleep": {"name": "Asleep", "color": (140, 140, 200), "skip_turn": True},
    "diseased": {"name": "Diseased", "color": (180, 200, 110), "max_hp_pct": -10},
    "haste": {"name": "Hasted", "color": (240, 220, 90), "extra_turn": True},
    "slow": {"name": "Slowed", "color": (130, 130, 200), "lose_turn": True},
    "regen": {"name": "Regenerating", "color": (90, 220, 150), "tick_heal": 2},
    "shield": {"name": "Shielded", "color": (180, 200, 240), "dmg_reduce": 3},
    "rage": {"name": "Raging", "color": (220, 60, 60), "atk_bonus": 3, "def_penalty": 2},
    "blessed": {"name": "Blessed", "color": (245, 240, 200), "atk_bonus": 1, "def_bonus": 1},
    "barkskin": {"name": "Barkskin", "color": (90, 130, 70), "dmg_reduce": 2},
    "invisible": {"name": "Invisible", "color": (200, 200, 220), "invisible": True},
    "see_invis": {"name": "See Invisible", "color": (200, 200, 220)},
    "detect_magic": {"name": "Detect Magic", "color": (180, 130, 240)},
    "telepathy": {"name": "Telepathy", "color": (240, 130, 200)},
    "drained": {"name": "Drained", "color": (90, 90, 130), "stat_penalty": 2},
    "wet": {"name": "Wet", "color": (130, 200, 240)},
    "oiled": {"name": "Oiled", "color": (180, 180, 90)},
    "marked": {"name": "Marked", "color": (220, 80, 80), "dmg_taken_pct": 25},
    "weakened": {"name": "Weakened", "color": (180, 130, 90), "atk_penalty": 2},
    "vampiric": {"name": "Vampiric", "color": (220, 60, 60), "lifesteal_pct": 25},
    "reflect": {"name": "Reflecting", "color": (200, 200, 240)},
    "concentration": {"name": "Concentrating", "color": (180, 130, 240)},
    "stealth": {"name": "Hidden", "color": (90, 130, 200), "invisible": True},
    "petrified": {"name": "Petrified", "color": (160, 160, 160), "skip_turn": True},
    "deafened": {"name": "Deafened", "color": (140, 140, 140)},
    "encumbered": {"name": "Encumbered", "color": (160, 130, 90), "lose_turn": True},
}


class Status:
    def __init__(self, key, duration, magnitude=1):
        self.key = key
        self.duration = duration
        self.magnitude = magnitude

    def tick(self):
        self.duration -= 1

    @property
    def name(self):
        return STATUS_DEFS[self.key]["name"]

    @property
    def color(self):
        return STATUS_DEFS[self.key]["color"]

    def to_dict(self):
        return {"key": self.key, "duration": self.duration, "magnitude": self.magnitude}

    @classmethod
    def from_dict(cls, d):
        return cls(d["key"], d["duration"], d.get("magnitude", 1))


class StatusBag:
    def __init__(self):
        self.statuses: list[Status] = []

    def add(self, key, duration, magnitude=1):
        for s in self.statuses:
            if s.key == key:
                s.duration = max(s.duration, duration)
                s.magnitude = max(s.magnitude, magnitude)
                return
        self.statuses.append(Status(key, duration, magnitude))

    def has(self, key):
        return any(s.key == key for s in self.statuses)

    def get(self, key):
        for s in self.statuses:
            if s.key == key:
                return s
        return None

    def remove(self, key):
        self.statuses = [s for s in self.statuses if s.key != key]

    def tick_all(self):
        for s in self.statuses:
            s.tick()
        self.statuses = [s for s in self.statuses if s.duration > 0]

    def names(self):
        return [(s.name, s.color, s.duration) for s in self.statuses]

    def clear_negative(self):
        bad = ["poisoned", "burning", "frozen", "stunned", "blinded",
               "confused", "feared", "asleep", "diseased", "weakened",
               "drained", "marked", "slow", "petrified", "encumbered"]
        self.statuses = [s for s in self.statuses if s.key not in bad]

    def to_dict(self):
        return {"s": [s.to_dict() for s in self.statuses]}

    @classmethod
    def from_dict(cls, d):
        bag = cls()
        for sd in d.get("s", []):
            bag.statuses.append(Status.from_dict(sd))
        return bag

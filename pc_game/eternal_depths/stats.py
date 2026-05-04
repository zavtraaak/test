"""Stats helpers."""
from __future__ import annotations
from dataclasses import dataclass, field, asdict


STAT_NAMES = ["STR", "DEX", "CON", "INT", "WIS", "CHA", "LUCK"]


@dataclass
class Stats:
    STR: int = 10
    DEX: int = 10
    CON: int = 10
    INT: int = 10
    WIS: int = 10
    CHA: int = 10
    LUCK: int = 10

    def mod(self, name):
        return (getattr(self, name) - 10) // 2

    def to_dict(self):
        return asdict(self)

    @classmethod
    def from_dict(cls, d):
        return cls(**d)

    def add(self, other: "Stats"):
        return Stats(
            STR=self.STR + other.STR,
            DEX=self.DEX + other.DEX,
            CON=self.CON + other.CON,
            INT=self.INT + other.INT,
            WIS=self.WIS + other.WIS,
            CHA=self.CHA + other.CHA,
            LUCK=self.LUCK + other.LUCK,
        )


@dataclass
class Resists:
    physical: int = 0
    fire: int = 0
    cold: int = 0
    lightning: int = 0
    poison: int = 0
    holy: int = 0
    dark: int = 0
    arcane: int = 0
    acid: int = 0

    def to_dict(self):
        return asdict(self)

    @classmethod
    def from_dict(cls, d):
        return cls(**d)

    def get(self, dtype):
        return getattr(self, dtype, 0)

    def add(self, other: "Resists"):
        return Resists(
            physical=self.physical + other.physical,
            fire=self.fire + other.fire,
            cold=self.cold + other.cold,
            lightning=self.lightning + other.lightning,
            poison=self.poison + other.poison,
            holy=self.holy + other.holy,
            dark=self.dark + other.dark,
            arcane=self.arcane + other.arcane,
            acid=self.acid + other.acid,
        )

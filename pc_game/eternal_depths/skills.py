"""Skill tree. Skills grow with use and level-ups."""
from __future__ import annotations

SKILLS = {
    # Weapon
    "weapon_sword": "Swords (longsword, rapier, scimitar)",
    "weapon_axe": "Axes (hand axe, battleaxe, greataxe)",
    "weapon_blunt": "Maces, hammers, clubs",
    "weapon_dagger": "Daggers and short blades",
    "weapon_polearm": "Spears, halberds, glaives",
    "weapon_staff": "Quarterstaff and arcane staves",
    "weapon_bow": "Bows and crossbows",
    "weapon_unarmed": "Fists, claws, martial arts",
    # Defense
    "shield": "Block with shields",
    "armor": "Wear heavy armor without penalty",
    "evasion": "Dodge attacks",
    # Magic
    "evocation": "Damage spells (fire, frost, lightning)",
    "abjuration": "Wards and protective spells",
    "necromancy": "Death and undead magic",
    "conjuration": "Summon and teleport spells",
    "divination": "Sight, identify, mapping",
    "transmutation": "Change form and matter",
    "enchantment": "Mind-affecting spells",
    "illusion": "Trickery, invisibility",
    "holy": "Divine spells, smite, turn undead",
    "concentration": "Hold spells while taking damage",
    # Utility
    "stealth": "Move unseen",
    "lockpick": "Open locks",
    "trapping": "Detect/disarm traps",
    "tracking": "Find creatures and items on map",
    "nature": "Wilderness skills",
    "shapeshift": "Become beasts",
    "alchemy": "Brew potions, identify herbs",
    "smithing": "Craft and improve weapons/armor",
    "athletics": "Climb, run, push doors",
    "performance": "Bardic music",
    "lore": "Identify items by knowledge",
    "healing": "Bandage wounds, brew remedies",
}


class SkillBook:
    def __init__(self, levels=None):
        self.levels: dict[str, int] = levels or {}

    def get(self, name):
        return self.levels.get(name, 0)

    def set(self, name, value):
        self.levels[name] = max(0, value)

    def gain(self, name, amount=1):
        self.levels[name] = self.get(name) + amount

    def all(self):
        return dict(sorted(self.levels.items(), key=lambda kv: -kv[1]))

    def to_dict(self):
        return {"levels": dict(self.levels)}

    @classmethod
    def from_dict(cls, d):
        return cls(d.get("levels", {}))

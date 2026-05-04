"""Spell catalog. 50+ spells, with logic encoded by callback names."""
from __future__ import annotations
from dataclasses import dataclass, field


@dataclass
class Spell:
    key: str
    name: str
    school: str
    level: int  # spell tier 1-9
    mp: int
    target: str  # "self", "enemy", "tile", "area", "ray", "ally", "summon", "buff"
    range: int = 6
    radius: int = 0
    desc: str = ""
    damage: str | None = None
    dtype: str = "physical"
    status: tuple | None = None  # (key, duration, magnitude)
    heal: int = 0
    summon_key: str | None = None
    custom: str | None = None  # special handler key
    duration: int = 0  # for buff/debuff status spells
    stat_buff: tuple | None = None  # (stat, amount, duration)


SPELLS = {}


def reg(s: Spell):
    SPELLS[s.key] = s
    return s


# Cantrips / level 1
reg(Spell("magic_missile", "Magic Missile", "evocation", 1, 3, "enemy",
         range=8, damage="1d4+1", dtype="arcane", desc="Auto-hit dart of force."))
reg(Spell("spark", "Spark", "evocation", 1, 2, "enemy", range=6,
         damage="1d6", dtype="lightning"))
reg(Spell("ignite", "Ignite", "evocation", 1, 2, "enemy", range=6,
         damage="1d4", dtype="fire", status=("burning", 3, 1)))
reg(Spell("frost_ray", "Frost Ray", "evocation", 1, 3, "enemy", range=6,
         damage="1d6", dtype="cold"))
reg(Spell("shield_arcane", "Arcane Shield", "abjuration", 1, 4, "self",
         status=("shield", 30, 1), desc="Reduce incoming damage."))
reg(Spell("detect_magic", "Detect Magic", "divination", 1, 2, "self",
         status=("detect_magic", 30, 1)))
reg(Spell("identify", "Identify", "divination", 1, 5, "self",
         custom="identify_inv", desc="Identify items."))
reg(Spell("light", "Light", "divination", 1, 2, "self",
         custom="cast_light", desc="Illuminate the area."))
reg(Spell("heal_self_minor", "Cure Light Wounds", "holy", 1, 3, "self",
         heal=12))
reg(Spell("heal_minor", "Heal", "holy", 1, 4, "self", heal=20))
reg(Spell("healing_word", "Healing Word", "holy", 1, 3, "self", heal=15))
reg(Spell("bless", "Bless", "holy", 1, 3, "self",
         status=("blessed", 30, 1)))
reg(Spell("cure_poison", "Cure Poison", "holy", 1, 3, "self",
         custom="cure_poisoned"))
reg(Spell("cure_disease", "Cure Disease", "holy", 1, 4, "self",
         custom="cure_disease"))
reg(Spell("smite", "Smite", "holy", 1, 4, "enemy", range=1,
         damage="1d8+2", dtype="holy"))
reg(Spell("turn_undead", "Turn Undead", "holy", 2, 6, "self",
         custom="turn_undead", radius=4,
         desc="Frightens undead nearby."))

# Conjuration / summoning
reg(Spell("blink", "Blink", "conjuration", 1, 4, "self",
         custom="blink", desc="Teleport short distance."))
reg(Spell("teleport", "Teleport", "conjuration", 3, 8, "self",
         custom="teleport_self", desc="Teleport randomly."))
reg(Spell("magic_mapping", "Magic Mapping", "divination", 2, 6, "self",
         custom="magic_mapping"))
reg(Spell("summon_wolf", "Summon Wolf", "conjuration", 2, 6, "self",
         summon_key="wolf"))
reg(Spell("summon_bear", "Summon Bear", "conjuration", 3, 10, "self",
         summon_key="bear"))
reg(Spell("summon_elemental", "Summon Elemental", "conjuration", 5, 18,
         "self", summon_key="elemental_fire"))

# Necromancy
reg(Spell("bone_dart", "Bone Dart", "necromancy", 1, 2, "enemy", range=6,
         damage="1d6", dtype="dark"))
reg(Spell("drain_life", "Drain Life", "necromancy", 1, 4, "enemy", range=4,
         damage="1d6+1", dtype="dark", custom="lifedrain_cast"))
reg(Spell("raise_skeleton", "Raise Skeleton", "necromancy", 2, 6, "self",
         summon_key="skeleton"))
reg(Spell("raise_zombie", "Raise Zombie", "necromancy", 2, 7, "self",
         summon_key="zombie"))
reg(Spell("fear", "Fear", "necromancy", 2, 6, "enemy", range=6,
         status=("feared", 6, 1)))
reg(Spell("shadow_bolt", "Shadow Bolt", "necromancy", 2, 5, "enemy",
         range=8, damage="2d6", dtype="dark"))
reg(Spell("animate_dead", "Animate Dead", "necromancy", 4, 14, "self",
         custom="animate_dead"))
reg(Spell("death_ray", "Death Ray", "necromancy", 6, 22, "ray", range=10,
         damage="6d8", dtype="dark"))

# Damage spells (higher tier)
reg(Spell("fireball", "Fireball", "evocation", 3, 8, "tile", range=8,
         radius=2, damage="3d6+2", dtype="fire", desc="Big boom."))
reg(Spell("lightning_bolt", "Lightning Bolt", "evocation", 3, 7, "ray",
         range=10, damage="3d6", dtype="lightning"))
reg(Spell("cone_of_cold", "Cone of Cold", "evocation", 3, 8, "area",
         range=4, damage="2d8", dtype="cold",
         status=("frozen", 1, 1)))
reg(Spell("acid_splash", "Acid Splash", "evocation", 1, 3, "enemy",
         range=4, damage="1d6", dtype="acid"))
reg(Spell("flame_strike", "Flame Strike", "evocation", 4, 12, "tile",
         range=8, radius=1, damage="5d6", dtype="fire"))
reg(Spell("chain_lightning", "Chain Lightning", "evocation", 5, 16,
         "enemy", range=8, damage="4d6", dtype="lightning",
         custom="chain_lightning"))
reg(Spell("meteor", "Meteor Swarm", "evocation", 8, 35, "tile", range=10,
         radius=3, damage="10d6", dtype="fire"))

# Druid / nature
reg(Spell("entangle", "Entangle", "transmutation", 1, 4, "enemy", range=4,
         status=("frozen", 4, 1), dtype="physical",
         desc="Vines hold target."))
reg(Spell("barkskin", "Barkskin", "transmutation", 2, 6, "self",
         status=("barkskin", 50, 1)))
reg(Spell("regrowth", "Regrowth", "transmutation", 2, 6, "self",
         status=("regen", 30, 1)))
reg(Spell("thorn_whip", "Thorn Whip", "transmutation", 1, 3, "enemy",
         range=2, damage="1d6", dtype="physical"))
reg(Spell("hunter_mark", "Hunter's Mark", "divination", 1, 3, "enemy",
         range=8, status=("marked", 30, 1)))
reg(Spell("polymorph_target", "Polymorph", "transmutation", 4, 14, "enemy",
         range=6, custom="polymorph_target"))

# Bard / charm
reg(Spell("song_courage", "Song of Courage", "enchantment", 1, 4, "self",
         status=("blessed", 25, 1)))
reg(Spell("vicious_mockery", "Vicious Mockery", "enchantment", 1, 3,
         "enemy", range=4, damage="1d4", dtype="arcane",
         status=("weakened", 4, 1)))
reg(Spell("charm", "Charm Person", "enchantment", 2, 5, "enemy", range=5,
         status=("feared", 5, 1)))
reg(Spell("minor_illusion", "Minor Illusion", "illusion", 1, 3, "self",
         custom="minor_illusion"))
reg(Spell("invisibility", "Invisibility", "illusion", 2, 8, "self",
         status=("invisible", 25, 1)))

# Abjuration / utility
reg(Spell("counterspell", "Counterspell", "abjuration", 3, 8, "enemy",
         range=8, custom="counterspell"))
reg(Spell("globe_invuln", "Globe of Invulnerability", "abjuration", 6,
         24, "self", status=("shield", 20, 5),
         desc="Greatly reduces all damage."))
reg(Spell("mind_blank", "Mind Blank", "abjuration", 4, 14, "self",
         custom="cure_mind"))
reg(Spell("haste", "Haste", "transmutation", 3, 8, "self",
         status=("haste", 15, 1)))
reg(Spell("slow", "Slow", "transmutation", 3, 8, "enemy", range=6,
         status=("slow", 12, 1)))
reg(Spell("prismatic_spray", "Prismatic Spray", "evocation", 7, 28, "area",
         range=6, damage="6d8", dtype="arcane"))
reg(Spell("disintegrate", "Disintegrate", "transmutation", 6, 22, "enemy",
         range=8, damage="10d6+10", dtype="arcane"))
reg(Spell("time_stop", "Time Stop", "transmutation", 9, 50, "self",
         status=("haste", 6, 2), desc="Dramatically extra turns."))
reg(Spell("wish", "Wish", "conjuration", 9, 60, "self", custom="wish"))


# Helper categorisations
def spells_by_school(school):
    return [s for s in SPELLS.values() if s.school == school]


def spells_for_class(cls_key):
    """Available learnable spells for class."""
    mapping = {
        "Mage": ["evocation", "abjuration", "transmutation"],
        "Wizard": list({s.school for s in SPELLS.values()}),
        "Priest": ["holy", "abjuration"],
        "Paladin": ["holy"],
        "Druid": ["transmutation", "conjuration"],
        "Necromancer": ["necromancy", "conjuration"],
        "Ranger": ["divination", "transmutation"],
        "Bard": ["enchantment", "illusion", "holy"],
        "Warrior": [],
        "Rogue": ["illusion"],
        "Monk": [],
        "Berserker": [],
        "Alchemist": ["transmutation"],
    }
    schools = mapping.get(cls_key, [])
    return [s for s in SPELLS.values() if s.school in schools]

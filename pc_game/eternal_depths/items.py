"""Item definitions and helpers. 100+ items."""
from __future__ import annotations
from dataclasses import dataclass, field, asdict
from .stats import Stats, Resists
from . import colors as C


SLOTS = ["weapon", "shield", "head", "body", "hands", "legs",
         "feet", "neck", "ring1", "ring2", "cloak"]


@dataclass
class ItemDef:
    key: str
    name: str
    glyph: str
    color: tuple
    category: str  # weapon, armor, potion, scroll, wand, ring, amulet, food, misc, ammo, tool, gem, book
    slot: str | None = None
    damage: str | None = None
    dtype: str = "physical"
    crit: int = 5  # crit chance percent
    weight: int = 1
    value: int = 5
    ac: int = 0
    enchant: int = 0
    stats: Stats = field(default_factory=Stats)
    resists: Resists = field(default_factory=Resists)
    desc: str = ""
    use: str | None = None  # for consumables: callback key
    use_args: dict = field(default_factory=dict)
    stackable: bool = False
    light: int = 0  # light radius
    rarity: str = "common"  # common/uncommon/rare/epic/legendary/artifact
    on_hit: list = field(default_factory=list)  # list of (status, duration, chance)
    two_handed: bool = False
    ranged: bool = False
    range: int = 0
    ammo_type: str | None = None
    cursed: bool = False
    blessed: bool = False
    charges: int = 0
    spell: str | None = None  # for wands
    book_spells: list = field(default_factory=list)
    food_value: int = 0


# Complete item catalog. Use ItemDef factories below for brevity.
def _W(key, name, glyph, color, dmg, weight=3, value=20, slot="weapon",
       crit=5, two_handed=False, dtype="physical", on_hit=None, desc=""):
    return ItemDef(key=key, name=name, glyph=glyph, color=color,
                   category="weapon", slot=slot, damage=dmg, dtype=dtype,
                   weight=weight, value=value, crit=crit, two_handed=two_handed,
                   on_hit=on_hit or [], desc=desc)


def _A(key, name, slot, ac, glyph, color, weight=4, value=20,
       resists=None, stats=None, desc=""):
    return ItemDef(key=key, name=name, glyph=glyph, color=color,
                   category="armor", slot=slot, ac=ac, weight=weight,
                   value=value, resists=resists or Resists(),
                   stats=stats or Stats(), desc=desc)


def _P(key, name, color, use, use_args=None, value=30, desc="", glyph="!"):
    return ItemDef(key=key, name=name, glyph=glyph, color=color,
                   category="potion", weight=1, value=value, use=use,
                   use_args=use_args or {}, stackable=True, desc=desc)


def _S(key, name, use, use_args=None, value=30, desc=""):
    return ItemDef(key=key, name=name, glyph="?", color=(245, 230, 180),
                   category="scroll", weight=1, value=value, use=use,
                   use_args=use_args or {}, stackable=True, desc=desc)


def _Wand(key, name, spell, charges=5, value=80, desc=""):
    return ItemDef(key=key, name=name, glyph="-", color=(220, 130, 240),
                   category="wand", weight=1, value=value, charges=charges,
                   spell=spell, desc=desc)


def _R(key, name, stats=None, resists=None, value=120, desc="", abil=None):
    return ItemDef(key=key, name=name, glyph="=", color=(200, 200, 240),
                   category="ring", slot="ring1", weight=0, value=value,
                   stats=stats or Stats(), resists=resists or Resists(),
                   desc=desc, rarity="uncommon")


def _Am(key, name, stats=None, resists=None, value=200, desc="", rarity="rare"):
    return ItemDef(key=key, name=name, glyph='"', color=(240, 220, 80),
                   category="amulet", slot="neck", weight=0, value=value,
                   stats=stats or Stats(), resists=resists or Resists(),
                   desc=desc, rarity=rarity)


def _F(key, name, food=200, glyph="%", color=(200, 160, 90), value=5, desc=""):
    return ItemDef(key=key, name=name, glyph=glyph, color=color,
                   category="food", weight=1, value=value, stackable=True,
                   food_value=food, desc=desc)


def _Misc(key, name, glyph, color, desc="", value=10, weight=1, use=None,
          use_args=None, category="misc"):
    return ItemDef(key=key, name=name, glyph=glyph, color=color,
                   category=category, weight=weight, value=value,
                   use=use, use_args=use_args or {}, stackable=False, desc=desc)


def _Ammo(key, name, dmg, range_=8, value=2, desc=""):
    return ItemDef(key=key, name=name, glyph="(", color=(180, 130, 90),
                   category="ammo", weight=0, value=value, damage=dmg,
                   range=range_, stackable=True, desc=desc)


# ----- DATABASE -----
ITEMS = {}


def reg(it):
    ITEMS[it.key] = it
    return it


# Weapons (melee)
reg(_W("dagger", "Dagger", ")", C.SILVER, "1d4", weight=1, value=10, crit=10,
       desc="Quick light blade."))
reg(_W("short_sword", "Short Sword", ")", C.SILVER, "1d6", weight=2, value=20,
       desc="Versatile blade."))
reg(_W("longsword", "Longsword", ")", C.SILVER, "1d8", weight=3, value=40,
       desc="A trusty blade."))
reg(_W("rapier", "Rapier", ")", C.SILVER, "1d8", weight=2, value=45, crit=12,
       desc="Slender, fast, deadly."))
reg(_W("scimitar", "Scimitar", ")", (240, 200, 80), "1d8", weight=2, value=42,
       desc="Curved blade of the dunes."))
reg(_W("greatsword", "Greatsword", ")", C.SILVER, "2d6", weight=6, value=70,
       two_handed=True, desc="Massive blade for two hands."))
reg(_W("hand_axe", "Hand Axe", "(", (180, 130, 90), "1d6", weight=2, value=18))
reg(_W("battleaxe", "Battleaxe", "(", (180, 130, 90), "1d10", weight=4, value=55))
reg(_W("greataxe", "Greataxe", "(", (180, 130, 90), "1d12", weight=7, value=80,
       two_handed=True, desc="Berserker's choice."))
reg(_W("club", "Club", "/", (130, 100, 70), "1d4", weight=2, value=4))
reg(_W("mace", "Mace", "/", (160, 130, 90), "1d8", weight=3, value=35))
reg(_W("warhammer", "Warhammer", "/", (200, 200, 220), "1d10", weight=5, value=60))
reg(_W("flail", "Flail", "/", (140, 140, 140), "1d8+1", weight=4, value=55,
       on_hit=[("stunned", 1, 8)]))
reg(_W("morningstar", "Morningstar", "/", (140, 140, 140), "1d10", weight=4, value=65))
reg(_W("staff", "Quarterstaff", "/", (140, 110, 80), "1d6", weight=3, value=10,
       two_handed=True))
reg(_W("spear", "Spear", "/", (160, 130, 90), "1d8", weight=3, value=20))
reg(_W("halberd", "Halberd", "/", (160, 160, 160), "2d6", weight=5, value=70,
       two_handed=True))
reg(_W("trident", "Trident", "/", (160, 160, 200), "1d8+1", weight=4, value=55))
reg(_W("whip", "Whip", "/", (140, 100, 70), "1d4", weight=2, value=20))
reg(_W("katana", "Katana", ")", (240, 230, 200), "1d10", weight=2, value=120,
       crit=15, two_handed=False, desc="Folded steel."))

# Magical / elemental weapons
reg(_W("flaming_sword", "Flaming Sword", ")", C.ORANGE, "1d8+1", weight=3,
       value=200, dtype="fire", on_hit=[("burning", 3, 30)],
       desc="Wreathed in flame.", ))
reg(_W("frost_sword", "Frost Sword", ")", C.CYAN, "1d8+1", weight=3, value=200,
       dtype="cold", on_hit=[("frozen", 1, 15)]))
reg(_W("thunder_axe", "Thunder Axe", "(", C.YELLOW, "1d10+1", weight=4,
       value=220, dtype="lightning", on_hit=[("stunned", 1, 20)]))
reg(_W("vorpal_blade", "Vorpal Blade", ")", (240, 240, 240), "2d6+2", weight=3,
       value=600, crit=20, desc="A legend among blades."))
reg(_W("vampiric_dagger", "Vampiric Dagger", ")", C.RED, "1d4+1", weight=1,
       value=300, on_hit=[("vampiric", 1, 100)],
       desc="Drinks the life of its victims."))
reg(_W("holy_avenger", "Holy Avenger", ")", (245, 240, 200), "2d6+1", weight=3,
       value=500, dtype="holy",
       desc="Bane of evil."))
reg(_W("staff_of_arcanum", "Staff of Arcanum", "/", C.PURPLE, "1d8+1",
       weight=3, value=400, two_handed=True, dtype="arcane",
       desc="Channels arcane power."))
reg(_W("scythe", "Scythe", "(", (160, 90, 220), "2d6", weight=4, value=180,
       two_handed=True))
reg(_W("dragon_slayer", "Dragon Slayer", ")", (220, 100, 60), "2d8", weight=5,
       value=800, two_handed=True))
reg(_W("excalibur", "Excalibur", ")", (240, 240, 200), "3d6+3", weight=3,
       value=2000, crit=15, dtype="holy",
       desc="The legendary blade.", ))

# Ranged
reg(ItemDef(key="bow", name="Shortbow", glyph="}", color=(180, 130, 90),
            category="weapon", slot="weapon", damage="1d6", weight=2,
            value=30, two_handed=True, ranged=True, range=10,
            ammo_type="arrow", desc="Pulls back smoothly."))
reg(ItemDef(key="longbow", name="Longbow", glyph="}", color=(180, 130, 90),
            category="weapon", slot="weapon", damage="1d8", weight=3,
            value=70, two_handed=True, ranged=True, range=12,
            ammo_type="arrow"))
reg(ItemDef(key="crossbow", name="Crossbow", glyph="}", color=(180, 130, 90),
            category="weapon", slot="weapon", damage="1d10", weight=4,
            value=80, two_handed=True, ranged=True, range=10,
            ammo_type="bolt"))
reg(ItemDef(key="elven_bow", name="Elven Bow", glyph="}", color=(180, 240, 90),
            category="weapon", slot="weapon", damage="1d8+1", weight=2,
            value=180, two_handed=True, ranged=True, range=12,
            ammo_type="arrow", desc="Light and lethal."))
# Ammo
reg(_Ammo("arrow", "Arrow", "1d4", range_=10, value=1))
reg(_Ammo("bolt", "Crossbow Bolt", "1d4+1", range_=10, value=1))
reg(_Ammo("flaming_arrow", "Flaming Arrow", "1d4+1", range_=10, value=4))
reg(_Ammo("throwing_knife", "Throwing Knife", "1d4", range_=6, value=3))
reg(_Ammo("arrow_bundle", "Bundle of Arrows", "1d4", range_=10, value=10))

# Shields
reg(_A("wooden_shield", "Wooden Shield", "shield", 1, "[", (180, 130, 90),
       weight=4, value=15))
reg(_A("iron_shield", "Iron Shield", "shield", 2, "[", (200, 200, 220),
       weight=6, value=40))
reg(_A("tower_shield", "Tower Shield", "shield", 3, "[", (200, 200, 220),
       weight=10, value=80))
reg(_A("aegis", "Aegis", "shield", 4, "[", (240, 240, 200),
       weight=6, value=400, resists=Resists(physical=10, holy=15),
       desc="Shield of legend."))

# Body Armor
reg(_A("robe", "Robe", "body", 0, "[", (90, 90, 200), weight=1, value=4,
       stats=Stats(INT=1)))
reg(_A("leather_armor", "Leather Armor", "body", 2, "[", (180, 130, 90),
       weight=4, value=20))
reg(_A("studded_leather", "Studded Leather", "body", 3, "[", (160, 130, 90),
       weight=5, value=40))
reg(_A("scale_mail", "Scale Mail", "body", 4, "[", (180, 180, 200),
       weight=8, value=80))
reg(_A("chain_mail", "Chain Mail", "body", 5, "[", (180, 180, 200),
       weight=10, value=120))
reg(_A("plate_mail", "Plate Mail", "body", 6, "[", (200, 200, 220),
       weight=14, value=200))
reg(_A("dragonscale", "Dragon Scale Armor", "body", 7, "[", (220, 100, 60),
       weight=10, value=900, resists=Resists(fire=30, physical=10),
       desc="Forged from a dragon's hide."))
reg(_A("mithril_chain", "Mithril Chain", "body", 5, "[", (200, 230, 240),
       weight=4, value=500, resists=Resists(physical=5)))
reg(_A("ethereal_robe", "Ethereal Robe", "body", 1, "[", (180, 130, 240),
       weight=1, value=400, stats=Stats(INT=2),
       resists=Resists(arcane=20, dark=10)))

# Helmets, gloves, boots, cloaks, legs
reg(_A("leather_cap", "Leather Cap", "head", 1, "^", (180, 130, 90)))
reg(_A("iron_helmet", "Iron Helmet", "head", 2, "^", (200, 200, 220), value=40))
reg(_A("great_helm", "Great Helm", "head", 3, "^", (200, 200, 220), value=80))
reg(_A("crown_of_kings", "Crown of Kings", "head", 2, "^", C.GOLD,
       value=600, stats=Stats(CHA=3), desc="Royal crown."))

reg(_A("leather_gloves", "Leather Gloves", "hands", 1, "[", (160, 110, 70),
       value=10))
reg(_A("gauntlets", "Iron Gauntlets", "hands", 1, "[", (200, 200, 220),
       value=40, stats=Stats(STR=1)))

reg(_A("leather_boots", "Leather Boots", "feet", 0, "[", (160, 110, 70),
       value=8))
reg(_A("iron_boots", "Iron Boots", "feet", 1, "[", (200, 200, 220),
       value=30))
reg(_A("boots_of_speed", "Boots of Speed", "feet", 0, "[", (240, 220, 90),
       value=400, stats=Stats(DEX=2), desc="Hum with quickness."))

reg(_A("cloak", "Cloak", "cloak", 0, "(", (90, 90, 130), value=10))
reg(_A("cloak_protection", "Cloak of Protection", "cloak", 1, "(",
       (130, 200, 240), value=200, resists=Resists(arcane=10),
       stats=Stats(LUCK=1)))
reg(_A("cloak_invisibility", "Cloak of Invisibility", "cloak", 1, "(",
       (200, 200, 240), value=900,
       desc="Vanish from sight."))

reg(_A("leather_pants", "Leather Pants", "legs", 1, "[", (160, 110, 70),
       value=10))
reg(_A("plate_legs", "Plate Greaves", "legs", 2, "[", (200, 200, 220),
       value=80))

# Rings
reg(_R("ring_str", "Ring of Strength", stats=Stats(STR=2), value=200,
       desc="Bulges your muscles."))
reg(_R("ring_dex", "Ring of Dexterity", stats=Stats(DEX=2), value=200))
reg(_R("ring_con", "Ring of Constitution", stats=Stats(CON=2), value=200))
reg(_R("ring_int", "Ring of Intelligence", stats=Stats(INT=2), value=200))
reg(_R("ring_wis", "Ring of Wisdom", stats=Stats(WIS=2), value=200))
reg(_R("ring_cha", "Ring of Charisma", stats=Stats(CHA=2), value=200))
reg(_R("ring_luck", "Ring of Luck", stats=Stats(LUCK=3), value=300))
reg(_R("ring_fire_res", "Ring of Fire Resistance",
       resists=Resists(fire=25), value=180))
reg(_R("ring_cold_res", "Ring of Cold Resistance",
       resists=Resists(cold=25), value=180))
reg(_R("ring_protection", "Ring of Protection",
       resists=Resists(physical=5, arcane=10), value=400))
reg(_R("ring_regen", "Ring of Regeneration", value=600,
       desc="Slow regen on wearer."))
reg(_R("ring_mana", "Ring of Mana", value=300, desc="Mana regen.",
       stats=Stats(INT=1)))

# Amulets
reg(_Am("am_health", "Amulet of Health", stats=Stats(CON=3), value=400))
reg(_Am("am_intellect", "Amulet of Intellect", stats=Stats(INT=3), value=400))
reg(_Am("am_resistance", "Amulet of Resistance",
        resists=Resists(fire=10, cold=10, lightning=10, poison=10),
        value=500))
reg(_Am("am_lifesaving", "Amulet of Life Saving", value=2000,
        desc="Saves wearer from death once.", rarity="legendary"))
reg(_Am("am_reflection", "Amulet of Reflection", value=600,
        desc="Reflects spells back."))
reg(_Am("am_holy", "Holy Amulet", stats=Stats(WIS=2),
        resists=Resists(holy=10, dark=15), value=400))
reg(_Am("am_dark", "Dark Amulet", stats=Stats(INT=2),
        resists=Resists(dark=15, holy=-10), value=400))

# Potions
reg(_P("potion_minor_healing", "Potion of Minor Healing", C.RED, "heal",
       {"amount": 15}, value=30, desc="Restores some HP."))
reg(_P("potion_healing", "Potion of Healing", C.RED, "heal",
       {"amount": 35}, value=80))
reg(_P("potion_full_healing", "Potion of Full Healing", C.RED, "heal_full",
       value=300, desc="Restores HP fully."))
reg(_P("potion_minor_mana", "Potion of Minor Mana", C.BLUE, "mana",
       {"amount": 10}, value=40))
reg(_P("potion_mana", "Potion of Mana", C.BLUE, "mana",
       {"amount": 25}, value=80))
reg(_P("potion_speed", "Potion of Speed", C.YELLOW, "buff",
       {"status": "haste", "duration": 12}, value=120))
reg(_P("potion_invisibility", "Potion of Invisibility", (200, 200, 240),
       "buff", {"status": "invisible", "duration": 25}, value=140))
reg(_P("potion_levitation", "Potion of Levitation", C.CYAN, "noop",
       value=50, desc="Float over hazards."))
reg(_P("potion_stoneskin", "Potion of Stoneskin", (160, 160, 160), "buff",
       {"status": "barkskin", "duration": 30}, value=120))
reg(_P("potion_regeneration", "Potion of Regeneration", C.GREEN, "buff",
       {"status": "regen", "duration": 20}, value=120))
reg(_P("potion_resistance", "Potion of Fire Resistance", C.ORANGE, "buff",
       {"status": "shield", "duration": 30}, value=80))
reg(_P("potion_strength", "Potion of Strength", C.RED, "stat_buff",
       {"stat": "STR", "amount": 2, "duration": 50}, value=150))
reg(_P("potion_might", "Potion of Might", (240, 80, 80), "stat_buff",
       {"stat": "STR", "amount": 4, "duration": 60}, value=300))
reg(_P("potion_cure_poison", "Potion of Cure Poison", C.GREEN, "cure",
       {"status": "poisoned"}, value=40))
reg(_P("potion_cure_disease", "Potion of Cure Disease", C.GREEN, "cure",
       {"status": "diseased"}, value=60))
reg(_P("potion_purify", "Potion of Purification", (240, 240, 240), "purify",
       value=200))
reg(_P("potion_extra_healing", "Potion of Extra Healing", C.RED, "heal",
       {"amount": 60}, value=140))
reg(_P("potion_xp", "Elixir of Knowledge", C.PURPLE, "xp",
       {"amount": 100}, value=500))
reg(_P("potion_gain_level", "Potion of Gain Level", C.GOLD, "level_up",
       value=2000, desc="Instantly gain a level."))
reg(_P("potion_polymorph", "Potion of Polymorph", C.PINK, "polymorph",
       value=200))
reg(_P("potion_acid", "Potion of Acid", (190, 240, 60), "throw_acid",
       value=80))
reg(_P("potion_oil", "Flask of Oil", (200, 180, 90), "oil_self", value=15,
       desc="Slippery."))

# Scrolls
reg(_S("scroll_identify", "Scroll of Identify", "identify", value=40))
reg(_S("scroll_magic_mapping", "Scroll of Magic Mapping", "magic_mapping",
       value=80))
reg(_S("scroll_teleport", "Scroll of Teleport", "teleport", value=120))
reg(_S("scroll_fireball", "Scroll of Fireball", "cast",
       {"spell": "fireball"}, value=120))
reg(_S("scroll_light", "Scroll of Light", "light", value=20))
reg(_S("scroll_summon_skeleton", "Scroll of Summon Skeleton",
       "cast", {"spell": "raise_skeleton"}, value=80))
reg(_S("scroll_blink", "Scroll of Blink", "blink", value=60))
reg(_S("scroll_remove_curse", "Scroll of Remove Curse", "remove_curse",
       value=120))
reg(_S("scroll_enchant_weapon", "Scroll of Enchant Weapon",
       "enchant_weapon", value=200))
reg(_S("scroll_enchant_armor", "Scroll of Enchant Armor",
       "enchant_armor", value=200))
reg(_S("scroll_recharge", "Scroll of Recharge", "recharge", value=120))
reg(_S("scroll_protection", "Scroll of Protection", "protection",
       value=160))
reg(_S("scroll_genocide", "Scroll of Genocide", "genocide", value=1500))
reg(_S("scroll_word_of_recall", "Scroll of Word of Recall",
       "word_of_recall", value=80))
reg(_S("scroll_destroy_armor", "Scroll of Destroy Armor",
       "destroy_armor", value=10, desc="Cursed?"))
reg(_S("scroll_amnesia", "Scroll of Amnesia", "amnesia", value=10))
reg(_S("scroll_summon_monster", "Scroll of Summon Monster",
       "summon_monster", value=120))

# Wands
reg(_Wand("wand_magic_missile", "Wand of Magic Missile", "magic_missile",
          charges=8, value=120))
reg(_Wand("wand_fire", "Wand of Fire", "fireball", charges=6, value=200))
reg(_Wand("wand_cold", "Wand of Cold", "cone_of_cold", charges=6, value=200))
reg(_Wand("wand_lightning", "Wand of Lightning", "lightning_bolt",
          charges=6, value=220))
reg(_Wand("wand_polymorph", "Wand of Polymorph", "polymorph_target",
          charges=4, value=300))
reg(_Wand("wand_teleport", "Wand of Teleport", "teleport_target",
          charges=4, value=200))
reg(_Wand("wand_drain", "Wand of Drain Life", "drain_life",
          charges=6, value=180))
reg(_Wand("wand_sleep", "Wand of Sleep", "sleep", charges=6, value=150))
reg(_Wand("wand_petrify", "Wand of Petrification", "petrify",
          charges=3, value=400))
reg(_Wand("wand_dig", "Wand of Digging", "dig", charges=5, value=160))

# Food
reg(_F("ration", "Iron Ration", food=600, value=8, color=(200, 160, 90)))
reg(_F("bread", "Loaf of Bread", food=300, value=4))
reg(_F("apple", "Apple", food=150, value=2, color=C.RED))
reg(_F("meat", "Meat", food=400, value=6))
reg(_F("cheese", "Cheese", food=250, value=4, color=C.YELLOW))
reg(_F("mushroom", "Mushroom", food=100, value=2, color=C.PURPLE,
       desc="Could be poisonous."))
reg(_F("royal_jelly", "Royal Jelly", food=500, value=120, color=C.GOLD,
       desc="Restores HP and cures disease."))
reg(_F("dragon_steak", "Dragon Steak", food=800, value=200,
       color=(220, 100, 60), desc="Resistance to fire on eat."))
reg(_F("waybread", "Elven Waybread", food=1000, value=80, color=(240, 240, 200)))
reg(_F("cookie", "Lucky Cookie", food=80, value=50, color=(200, 160, 90),
       desc="Tells fortunes."))

# Books / spellbooks
reg(_Misc("spellbook_basic", "Spellbook of Basics", "+", (200, 130, 60),
          desc="Teaches basic cantrips.", value=60, category="book"))
reg(_Misc("spellbook_evocation", "Spellbook of Evocation", "+", C.RED,
          desc="Burns, freezes, shocks.", value=200, category="book"))
reg(_Misc("spellbook_conjuration", "Spellbook of Conjuration", "+",
          C.PURPLE, desc="Summon and teleport.", value=200, category="book"))
reg(_Misc("spellbook_necromancy", "Spellbook of Necromancy", "+",
          (160, 90, 220), desc="Death and undead.", value=200, category="book"))
reg(_Misc("spellbook_holy", "Tome of Light", "+", (245, 240, 200),
          desc="Holy magics.", value=200, category="book"))
reg(_Misc("spellbook_master", "Master Tome", "+", C.GOLD,
          desc="Forbidden lore.", value=1000, category="book"))

# Gems
reg(_Misc("gem_ruby", "Ruby", "*", C.RED, desc="A precious red gem.",
          value=200, category="gem"))
reg(_Misc("gem_emerald", "Emerald", "*", C.GREEN, value=220, category="gem"))
reg(_Misc("gem_sapphire", "Sapphire", "*", C.BLUE, value=240, category="gem"))
reg(_Misc("gem_diamond", "Diamond", "*", (240, 240, 240), value=400,
          category="gem"))
reg(_Misc("gem_pearl", "Pearl", "*", (240, 230, 220), value=120, category="gem"))
reg(_Misc("gem_amethyst", "Amethyst", "*", C.PURPLE, value=200, category="gem"))

# Tools
reg(_Misc("lockpick", "Lockpick", "{", (200, 200, 220),
          value=15, category="tool", desc="Pick locks."))
reg(_Misc("torch", "Torch", "/", C.ORANGE,
          value=8, category="tool", use="light_torch", desc="Lights the dark."))
reg(_Misc("rope", "Rope", "{", (160, 110, 70),
          value=20, category="tool"))
reg(_Misc("pickaxe", "Pickaxe", "(", (160, 110, 70),
          value=40, category="tool", desc="Dig walls."))
reg(_Misc("alchemy_kit", "Alchemy Kit", "{", C.GREEN,
          value=80, category="tool", desc="Brew potions."))
reg(_Misc("smithing_hammer", "Smithing Hammer", "/", (160, 130, 90),
          value=80, category="tool", desc="Smith weapons and armor."))
reg(_Misc("holy_symbol", "Holy Symbol", "*", (240, 240, 200),
          value=40, category="tool", desc="Channel divine power."))
reg(_Misc("lute", "Lute", "/", (180, 130, 90),
          value=40, category="tool", desc="Play bardic songs."))
reg(_Misc("smoke_bomb", "Smoke Bomb", "*", (140, 140, 140),
          value=60, category="tool", use="smoke_bomb", desc="Vanish in smoke."))
reg(_Misc("bomb_fire", "Fire Bomb", "*", C.ORANGE,
          value=80, category="tool", use="throw_fire_bomb"))
reg(_Misc("bomb_frost", "Frost Bomb", "*", C.CYAN,
          value=80, category="tool", use="throw_frost_bomb"))
reg(_Misc("bomb_acid", "Acid Bomb", "*", (190, 240, 60),
          value=80, category="tool", use="throw_acid_bomb"))
reg(_Misc("herb", "Healing Herb", "*", C.GREEN,
          value=20, category="misc", desc="Used in alchemy."))
reg(_Misc("crystal", "Mana Crystal", "*", C.BLUE,
          value=40, category="misc", desc="Pulses with magic."))
reg(_Misc("bone", "Bone", "%", (220, 220, 200),
          value=2, category="misc"))


def make(key, **kwargs):
    """Get an Item instance based on def, possibly with overrides."""
    base = ITEMS[key]
    return Item(def_key=key, **kwargs)


@dataclass
class Item:
    def_key: str
    enchant: int = 0
    charges: int | None = None
    identified: bool = False
    cursed: bool = False
    blessed: bool = False
    stack: int = 1
    custom_name: str | None = None

    @property
    def d(self) -> ItemDef:
        return ITEMS[self.def_key]

    @property
    def name(self):
        if self.custom_name:
            return self.custom_name
        n = self.d.name
        if self.identified:
            prefix = ""
            if self.cursed:
                prefix = "cursed "
            elif self.blessed:
                prefix = "blessed "
            ench = ""
            if self.enchant:
                ench = f" {'+' if self.enchant >= 0 else ''}{self.enchant}"
            return f"{prefix}{n}{ench}"
        return n

    @property
    def display_name(self):
        return self.name + (f" (x{self.stack})" if self.stack > 1 else "")

    @property
    def total_value(self):
        v = self.d.value
        if self.identified and self.enchant:
            v += self.enchant * 50
        if self.cursed:
            v //= 2
        if self.blessed:
            v *= 2
        return max(1, v) * max(1, self.stack)

    def to_dict(self):
        return {
            "def_key": self.def_key, "enchant": self.enchant,
            "charges": self.charges, "identified": self.identified,
            "cursed": self.cursed, "blessed": self.blessed,
            "stack": self.stack, "custom_name": self.custom_name,
        }

    @classmethod
    def from_dict(cls, d):
        return cls(**d)


def init_charges(item: Item):
    """If wand, set charges based on def."""
    d = item.d
    if d.category == "wand" and item.charges is None:
        item.charges = d.charges
    return item

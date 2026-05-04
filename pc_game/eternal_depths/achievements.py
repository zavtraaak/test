"""Achievement system."""
from __future__ import annotations

ACHIEVEMENTS = {
    "first_blood": ("First Blood", "Kill your first enemy."),
    "ten_kills": ("Slayer I", "Kill 10 enemies."),
    "hundred_kills": ("Slayer II", "Kill 100 enemies."),
    "thousand_kills": ("Slayer III", "Kill 1000 enemies."),
    "depth_5": ("Spelunker", "Reach depth 5."),
    "depth_10": ("Adventurer", "Reach depth 10."),
    "depth_15": ("Veteran", "Reach depth 15."),
    "depth_20": ("Hero", "Reach depth 20."),
    "depth_25": ("Champion", "Reach depth 25."),
    "depth_30": ("Eternal", "Reach the bottom."),
    "boss_orc": ("Orc Slayer", "Defeat the Orc Chieftain."),
    "boss_lord": ("Crypt Cleanser", "Defeat the Crypt Lord."),
    "boss_ice": ("Frost Bane", "Defeat the Ice Queen."),
    "boss_fire": ("Flame Vanquisher", "Defeat the Flame Lord."),
    "boss_void": ("Void Tamer", "Defeat the Avatar of the Void."),
    "boss_eternal": ("Eternally Yours", "Defeat the Eternal One."),
    "win": ("Victorious", "Complete the game."),
    "no_armor": ("Naked Hero", "Reach depth 10 without armor."),
    "rich": ("Tycoon", "Carry 10,000 gold."),
    "scholar": ("Scholar", "Know 30 spells."),
    "collector": ("Collector", "Identify 50 items."),
    "polyglot": ("Polyglot", "Reach level 10 in 3 schools."),
    "level_20": ("Level 20", "Reach character level 20."),
    "no_potion": ("Sober", "Beat depth 10 without quaffing a potion."),
    "vegetarian": ("Vegetarian", "Beat depth 5 without eating meat."),
}


def grant(state, key):
    if key in state.player.achievements:
        return
    state.player.achievements.add(key)
    name, desc = ACHIEVEMENTS[key]
    state.log.add(f"Achievement unlocked: {name}!", (240, 220, 90))


def on_kill(state, target):
    state.player.kills_total = getattr(state.player, "kills_total", 0) + 1
    if state.player.kills == 1:
        grant(state, "first_blood")
    if state.player.kills >= 10:
        grant(state, "ten_kills")
    if state.player.kills >= 100:
        grant(state, "hundred_kills")
    if state.player.kills >= 1000:
        grant(state, "thousand_kills")
    bosses = {
        "orc_chief": "boss_orc",
        "crypt_lord": "boss_lord",
        "ice_queen": "boss_ice",
        "flame_lord": "boss_fire",
        "void_avatar": "boss_void",
        "eternal_one": "boss_eternal",
    }
    if hasattr(target, "mdef_key") and target.mdef_key in bosses:
        grant(state, bosses[target.mdef_key])
    if hasattr(target, "mdef_key") and target.mdef_key == "eternal_one":
        grant(state, "win")
        state.victory = True


def on_depth(state):
    d = state.player.depth
    if d >= 30:
        grant(state, "depth_30")
    elif d >= 25:
        grant(state, "depth_25")
    elif d >= 20:
        grant(state, "depth_20")
    elif d >= 15:
        grant(state, "depth_15")
    elif d >= 10:
        grant(state, "depth_10")
    elif d >= 5:
        grant(state, "depth_5")


def on_levelup(state):
    if state.player.level >= 20:
        grant(state, "level_20")

"""Player actions: move, pickup, drop, equip, use, etc."""
from __future__ import annotations
import random
from .utils import chebyshev, line, roll, percent_chance


def move_or_attack(state, dx, dy):
    p = state.player
    nx, ny = p.x + dx, p.y + dy
    if not state.gm.in_bounds(nx, ny):
        return False
    e = state.gm.blocking_entity_at(nx, ny)
    if e and e is not p:
        if e.faction in ("monster",):
            from . import combat
            combat.melee_attack(state, p, e)
            return True
        if e.faction == "npc":
            interact_npc(state, e)
            return True
    t = state.gm.tile(nx, ny)
    if t.door and not t.open:
        # try open
        from . import tiles
        biome = t.biome
        new = tiles.make_door(biome, open_=True)
        state.gm.set_tile(nx, ny, new)
        state.log.add("You open the door.", (200, 200, 200))
        return True
    if not t.walkable:
        if state.player_swing_walls() and percent_chance(20):
            state.log.add("You hack at the wall.", (160, 130, 90))
        return False
    p.x, p.y = nx, ny
    items = state.gm.items_at(nx, ny)
    for it in items:
        state.log.add(f"You see here: {it.display_name}", (200, 200, 220))
    on_step_tile(state, t)
    return True


def on_step_tile(state, t):
    if t.fountain:
        state.log.add("You see a fountain. (q to quaff)", (130, 200, 240))
    if t.altar:
        state.log.add("You see an altar. (#pray)", (240, 240, 200))
    if t.chest:
        state.log.add("You see a chest. (o to open)", (220, 180, 90))
    if t.stairs_down:
        state.log.add("Stairs down. (>)", (240, 240, 240))
    if t.stairs_up:
        state.log.add("Stairs up. (<)", (240, 240, 240))


def pickup(state):
    p = state.player
    items = state.gm.items_at(p.x, p.y)
    if not items:
        state.log.add("Nothing here.", (180, 180, 180))
        return False
    for it in list(items):
        if it.def_key == "gold_pile":
            p.gold += it.stack
            state.log.add(f"You pick up {it.stack} gold.", (240, 200, 80))
            state.gm.remove_item(p.x, p.y, it)
            continue
        ok = p.inventory.add(it)
        if ok:
            state.log.add(f"You pick up {it.display_name}.", (200, 220, 200))
            state.gm.remove_item(p.x, p.y, it)
        else:
            state.log.add("Inventory full.", (220, 80, 80))
    return True


def drop(state, item):
    p = state.player
    if p.equipment.weapon is item or any(it is item for _, it in p.equipment.all_items()):
        state.log.add("It is equipped.", (220, 80, 80))
        return False
    p.inventory.remove(item, item.stack)
    state.gm.add_item(p.x, p.y, item)
    state.log.add(f"You drop {item.display_name}.", (200, 200, 200))
    return True


def equip(state, item):
    p = state.player
    slot = item.d.slot
    if not slot:
        state.log.add("Cannot equip.", (220, 80, 80))
        return False
    if slot == "ring1":
        if p.equipment.ring1 is None:
            slot = "ring1"
        elif p.equipment.ring2 is None:
            slot = "ring2"
        else:
            slot = "ring1"  # replace
    cur = p.equipment.get(slot)
    if cur is item:
        unequip(state, slot)
        return True
    # If two-handed weapon, also unequip shield
    if slot == "weapon" and item.d.two_handed and p.equipment.shield:
        unequip(state, "shield")
    if cur:
        unequip(state, slot)
    p.equipment.set(slot, item)
    p.inventory.remove(item, item.stack)
    state.log.add(f"You equip {item.display_name}.", (200, 220, 200))
    return True


def unequip(state, slot):
    p = state.player
    cur = p.equipment.get(slot)
    if not cur:
        return False
    if cur.cursed:
        state.log.add(f"The {cur.display_name} is cursed!", (220, 60, 60))
        cur.identified = True
        return False
    p.equipment.set(slot, None)
    p.inventory.add(cur)
    state.log.add(f"You unequip {cur.display_name}.", (200, 200, 200))
    return True


def use_item(state, item):
    """Use a potion/scroll/wand/food."""
    p = state.player
    d = item.d
    if d.category == "potion":
        return _use_potion(state, item)
    if d.category == "scroll":
        return _use_scroll(state, item)
    if d.category == "wand":
        return _use_wand(state, item)
    if d.category == "food":
        return _eat(state, item)
    if d.category == "book":
        return _read_book(state, item)
    if d.category == "tool":
        return _use_tool(state, item)
    state.log.add("Cannot use this.", (180, 180, 180))
    return False


def _use_potion(state, item):
    p = state.player
    use = item.d.use
    args = item.d.use_args
    if use == "heal":
        amt = args["amount"] + (15 if item.blessed else 0)
        amt = amt // 2 if item.cursed else amt
        p.hp = min(p.max_hp, p.hp + amt)
        state.log.add(f"You quaff and heal {amt}.", (90, 200, 90))
    elif use == "heal_full":
        p.hp = p.max_hp
        state.log.add("You feel fully restored!", (90, 200, 90))
    elif use == "mana":
        amt = args["amount"]
        p.mp = min(p.max_mp, p.mp + amt)
        state.log.add(f"You feel mana surge ({amt}).", (90, 130, 220))
    elif use == "buff":
        p.statuses.add(args["status"], args["duration"])
        state.log.add(f"You feel {args['status']}.", (180, 200, 240))
    elif use == "stat_buff":
        p.statuses.add("blessed", args["duration"])
    elif use == "cure":
        if p.statuses.has(args["status"]):
            p.statuses.remove(args["status"])
            state.log.add("You feel better.", (90, 200, 90))
    elif use == "purify":
        p.statuses.clear_negative()
        state.log.add("You feel purified.", (240, 240, 240))
    elif use == "xp":
        p.gain_xp(args["amount"])
        state.log.add("You feel wiser.", (180, 130, 240))
    elif use == "level_up":
        p.gain_xp(item.d.use_args.get("amount", 0) or _xp_to_next(p))
        state.log.add("You suddenly understand more.", (240, 220, 90))
    elif use == "polymorph":
        state.log.add("You feel changed!", (200, 130, 200))
    elif use == "throw_acid":
        state.pending_action = ("throw_potion", item)
        return False
    elif use == "oil_self":
        p.statuses.add("oiled", 30)
    elif use == "noop":
        state.log.add("Nothing happens.", (180, 180, 180))
    item.identified = True
    p.identifications.add(item.def_key)
    p.inventory.remove(item, 1)
    return True


def _xp_to_next(p):
    from .entity import xp_required
    return xp_required(p.level) - p.xp


def _use_scroll(state, item):
    p = state.player
    use = item.d.use
    if use == "identify":
        state.pending_action = "identify_choice"
        state.log.add("Choose item to identify.", (180, 130, 240))
    elif use == "magic_mapping":
        for x in range(state.gm.w):
            for y in range(state.gm.h):
                state.gm.explored[x][y] = True
        state.log.add("You learn the layout.", (180, 130, 240))
    elif use == "teleport":
        from . import effects
        effects._teleport_self(state, p, p, None)
        state.log.add("You teleport.", (180, 130, 240))
    elif use == "blink":
        from . import effects
        effects._blink(state, p, p, None)
        state.log.add("You blink.", (180, 130, 240))
    elif use == "cast":
        spell_key = item.d.use_args.get("spell")
        from .spells import SPELLS
        sp = SPELLS[spell_key]
        from . import combat
        if sp.target == "self":
            combat.apply_spell_effect(state, p, p, sp)
        elif sp.target in ("enemy", "tile", "ray", "area"):
            state.pending_action = ("cast_target", sp)
            return False
    elif use == "light":
        state.light_radius = max(state.light_radius, 12)
        state.log.add("Light fills the area.", (240, 240, 200))
    elif use == "remove_curse":
        for slot, it in p.equipment.all_items():
            if it.cursed:
                it.cursed = False
                state.log.add(f"Your {it.d.name} feels lighter.", (240, 240, 200))
    elif use == "enchant_weapon":
        if p.equipment.weapon:
            p.equipment.weapon.enchant += 1
            p.equipment.weapon.identified = True
            state.log.add(f"Your {p.equipment.weapon.d.name} glows.", (240, 220, 90))
    elif use == "enchant_armor":
        slot = "body" if p.equipment.body else "shield"
        cur = p.equipment.get(slot)
        if cur:
            cur.enchant += 1
            cur.identified = True
            state.log.add(f"Your {cur.d.name} glows.", (240, 220, 90))
    elif use == "recharge":
        for it in p.inventory.items:
            if it.d.category == "wand":
                it.charges = (it.charges or 0) + 3
        state.log.add("Wands hum with energy.", (180, 130, 240))
    elif use == "protection":
        p.statuses.add("shield", 50, 1)
        state.log.add("A shimmer surrounds you.", (180, 200, 240))
    elif use == "genocide":
        # remove all of one type
        if state.gm.entities:
            kind = max(set(e.glyph for e in state.gm.entities if e.alive),
                       key=lambda k: sum(1 for e in state.gm.entities
                                         if e.alive and e.glyph == k))
            for e in state.gm.entities:
                if e.glyph == kind:
                    e.alive = False
                    e.blocks = False
            state.log.add(f"All '{kind}' vanish.", (240, 240, 240))
    elif use == "word_of_recall":
        state.log.add("You feel a tug...", (180, 130, 240))
    elif use == "destroy_armor":
        if p.equipment.body:
            state.log.add(f"Your {p.equipment.body.d.name} crumbles!", (220, 60, 60))
            p.equipment.body = None
    elif use == "amnesia":
        state.log.add("You forget where you are.", (180, 180, 180))
        for x in range(state.gm.w):
            for y in range(state.gm.h):
                state.gm.explored[x][y] = False
    elif use == "summon_monster":
        from . import effects
        effects.summon(state, p, "wolf")
    item.identified = True
    p.identifications.add(item.def_key)
    p.inventory.remove(item, 1)
    return True


def _use_wand(state, item):
    p = state.player
    if (item.charges or 0) <= 0:
        state.log.add("Wand has no charges.", (180, 180, 180))
        return False
    from .spells import SPELLS
    sp = SPELLS.get(item.d.spell)
    if sp is None:
        state.log.add("Wand fizzles.", (180, 180, 180))
        return False
    state.pending_action = ("wand_target", item)
    return False


def _eat(state, item):
    p = state.player
    p.hunger += item.d.food_value
    if p.hunger > 1500:
        p.hunger = 1500
    state.log.add(f"You eat {item.d.name}.", (200, 200, 200))
    if item.def_key == "royal_jelly":
        p.statuses.remove("diseased")
        p.hp = min(p.max_hp, p.hp + 30)
    if item.def_key == "dragon_steak":
        p.statuses.add("shield", 30)
    if item.def_key == "mushroom" and percent_chance(30):
        p.statuses.add("confused", 8)
        state.log.add("Bad mushroom!", (200, 130, 200))
    if item.def_key == "cookie":
        state.log.add("Lucky day ahead!", (240, 220, 90))
    p.inventory.remove(item, 1)
    return True


def _read_book(state, item):
    p = state.player
    rng = state.rng
    pool = []
    if item.def_key == "spellbook_basic":
        pool = ["magic_missile", "shield_arcane", "spark", "ignite", "frost_ray"]
    elif item.def_key == "spellbook_evocation":
        pool = ["fireball", "lightning_bolt", "cone_of_cold",
                "flame_strike", "chain_lightning"]
    elif item.def_key == "spellbook_conjuration":
        pool = ["blink", "teleport", "summon_wolf", "summon_bear",
                "summon_elemental"]
    elif item.def_key == "spellbook_necromancy":
        pool = ["bone_dart", "drain_life", "raise_skeleton", "fear",
                "shadow_bolt", "animate_dead", "death_ray"]
    elif item.def_key == "spellbook_holy":
        pool = ["heal_self_minor", "bless", "smite", "turn_undead", "cure_disease"]
    elif item.def_key == "spellbook_master":
        pool = list({"meteor", "disintegrate", "time_stop", "wish",
                     "prismatic_spray"})
    new = [s for s in pool if s not in p.spells]
    if not new:
        state.log.add("You already know these spells.", (180, 180, 180))
        return False
    sp = rng.choice(new)
    p.spells.append(sp)
    state.log.add(f"You learn {sp.replace('_', ' ').title()}!", (180, 130, 240))
    return True


def _use_tool(state, item):
    p = state.player
    key = item.def_key
    if key == "torch":
        state.light_radius = max(state.light_radius, 10)
        state.log.add("You light a torch.", (235, 150, 60))
        return True
    if key == "smoke_bomb":
        p.statuses.add("invisible", 6)
        state.log.add("You vanish in smoke.", (200, 200, 220))
        p.inventory.remove(item, 1)
        return True
    if key in ("bomb_fire", "bomb_frost", "bomb_acid"):
        state.pending_action = ("throw_bomb", item)
        return False
    if key == "alchemy_kit":
        state.pending_action = "brew"
        return False
    if key == "smithing_hammer":
        state.pending_action = "forge"
        return False
    if key == "lockpick":
        state.pending_action = "pick_lock"
        return False
    if key == "pickaxe":
        state.pending_action = "dig"
        return False
    return False


def open_chest(state):
    p = state.player
    t = state.gm.tile(p.x, p.y)
    if not t.chest:
        state.log.add("No chest here.", (180, 180, 180))
        return False
    rng = state.rng
    from . import tiles
    state.gm.set_tile(p.x, p.y, tiles.make_floor(t.biome))
    n = rng.randint(1, 4)
    from .items import Item, ITEMS, init_charges
    pool = list(ITEMS.keys())
    rng.shuffle(pool)
    for k in pool:
        if n <= 0:
            break
        if k in ("gold_pile",):
            continue
        if rng.chance(50):
            it = init_charges(Item(k))
            state.gm.add_item(p.x, p.y, it)
            n -= 1
    p.gold += rng.randint(20, 80) * p.depth
    state.log.add("Treasure spills out!", (240, 200, 80))
    return True


def quaff_fountain(state):
    p = state.player
    t = state.gm.tile(p.x, p.y)
    if not t.fountain:
        state.log.add("No fountain.", (180, 180, 180))
        return False
    rng = state.rng
    r = rng.random()
    if r < 0.3:
        p.hp = min(p.max_hp, p.hp + 15)
        state.log.add("You feel restored.", (90, 200, 90))
    elif r < 0.5:
        p.gain_xp(50)
        state.log.add("You feel wiser.", (180, 130, 240))
    elif r < 0.7:
        p.statuses.add("regen", 30)
        state.log.add("Slow healing flows.", (90, 200, 130))
    elif r < 0.85:
        p.statuses.add("poisoned", 6)
        state.log.add("Bitter water!", (140, 220, 90))
    else:
        from . import effects
        effects._teleport_self(state, p, p, None)
        state.log.add("You teleport.", (180, 130, 240))
    if rng.chance(30):
        from . import tiles
        state.gm.set_tile(p.x, p.y, tiles.make_floor(t.biome))
        state.log.add("The fountain dries up.", (180, 180, 180))
    return True


def pray_altar(state):
    p = state.player
    t = state.gm.tile(p.x, p.y)
    if not t.altar:
        state.log.add("No altar.", (180, 180, 180))
        return False
    rng = state.rng
    p.piety += 1
    if rng.chance(40):
        p.hp = p.max_hp
        state.log.add("Light heals you.", (245, 240, 200))
    elif rng.chance(50):
        # Bless an item
        for it in p.inventory.items:
            if rng.chance(20):
                it.blessed = True
                it.identified = True
                state.log.add(f"{it.d.name} is blessed.", (240, 240, 200))
                break
    else:
        state.log.add("Silence answers.", (180, 180, 180))
    return True


def descend(state):
    p = state.player
    t = state.gm.tile(p.x, p.y)
    if not t.stairs_down:
        state.log.add("No stairs down here.", (180, 180, 180))
        return False
    state.descend_level()
    return True


def ascend(state):
    p = state.player
    t = state.gm.tile(p.x, p.y)
    if not t.stairs_up:
        state.log.add("No stairs up here.", (180, 180, 180))
        return False
    state.ascend_level()
    return True


def interact_npc(state, npc):
    state.log.add(f"You meet {npc.name}.", (240, 220, 90))
    state.pending_action = ("npc_dialog", npc)


def cast_spell(state, spell_key):
    p = state.player
    from .spells import SPELLS
    sp = SPELLS[spell_key]
    if p.mp < sp.mp:
        state.log.add("Not enough mana.", (180, 180, 180))
        return False
    p.mp -= sp.mp
    p.skills.gain(sp.school, 1)
    if sp.target == "self":
        from . import combat
        combat.apply_spell_effect(state, p, p, sp)
        return True
    state.pending_action = ("cast_target", sp)
    return False


def use_ability(state, ability):
    p = state.player
    if p.cooldowns.get(ability, 0) > 0:
        state.log.add("On cooldown.", (180, 180, 180))
        return False
    if ability == "lay_hands":
        p.hp = p.max_hp
        p.cooldowns[ability] = 80
        state.log.add("You lay hands and heal.", (245, 240, 200))
        return True
    if ability == "rage":
        p.statuses.add("rage", 15)
        p.cooldowns[ability] = 60
        return True
    if ability == "second_wind":
        p.hp = min(p.max_hp, p.hp + 20)
        p.cooldowns[ability] = 60
        return True
    if ability == "vanish":
        p.statuses.add("invisible", 12)
        p.cooldowns[ability] = 50
        return True
    if ability == "shadow_step":
        state.pending_action = "shadow_step"
        return False
    if ability == "called_shot":
        state.pending_action = "called_shot"
        return False
    if ability == "lay_hands":
        p.hp = p.max_hp
        p.cooldowns[ability] = 80
        return True
    if ability == "smite_evil":
        state.pending_action = "smite_target"
        return False
    if ability == "wholeness":
        p.hp = p.max_hp
        p.cooldowns[ability] = 80
        state.log.add("You center yourself.", (245, 240, 200))
        return True
    if ability == "channel_divinity":
        for e in state.gm.entities:
            if e.alive and "undead" in getattr(e, "flags", set()):
                from .combat import deal_damage
                deal_damage(state, p, e, 30, "holy")
        p.cooldowns[ability] = 100
        state.log.add("Divine wave erupts!", (245, 240, 200))
        return True
    state.log.add(f"Use ability: {ability}", (200, 200, 200))
    return True


def search(state):
    """Reveal hidden tiles around player."""
    p = state.player
    found = 0
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            x, y = p.x + dx, p.y + dy
            if not state.gm.in_bounds(x, y):
                continue
            t = state.gm.tile(x, y)
            if t.trap:
                state.log.add(f"You find a trap!", (220, 80, 80))
                found += 1
    if not found:
        state.log.add("You search, but find nothing.", (180, 180, 180))
    return True


def rest(state):
    """Rest until full HP/MP or interrupted."""
    p = state.player
    while p.hp < p.max_hp or p.mp < p.max_mp:
        if any(state.gm.visible[e.x][e.y] for e in state.gm.entities
               if e.alive and e.faction == "monster"):
            state.log.add("Monsters nearby!", (220, 80, 80))
            break
        p.hp = min(p.max_hp, p.hp + 1)
        p.mp = min(p.max_mp, p.mp + 1)
        p.hunger -= 1
        p.turns += 1
        if p.hunger <= 0:
            break
    state.log.add("You rest.", (180, 200, 180))
    return True

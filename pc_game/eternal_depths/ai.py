"""Monster AI: chase, ranged, caster, fleeing, etc."""
from __future__ import annotations
import random
from .pathfinding import astar
from .utils import chebyshev, line


def take_turn(state, mon):
    """Decide and execute monster's action this turn."""
    if not mon.alive_status():
        return
    if mon.statuses.has("asleep") or mon.statuses.has("petrified") or mon.statuses.has("frozen") or mon.statuses.has("stunned"):
        return
    if mon.statuses.has("feared"):
        flee_from(state, mon, state.player)
        return
    target = pick_target(state, mon)
    if target is None:
        wander(state, mon)
        return
    dist = chebyshev(mon.at(), target.at())
    # Confused
    if mon.statuses.has("confused"):
        rand_step(state, mon)
        return
    # Ranged or caster: try spells/ranged at distance
    if mon.spells and dist <= 8 and has_los(state.gm, mon.x, mon.y, target.x, target.y):
        spell_key = state.rng.choice(mon.spells)
        if cast_spell_at(state, mon, target, spell_key):
            return
    # Special abilities
    if mon.special:
        for sp, chance in mon.special:
            if state.rng.chance(chance):
                if exec_special(state, mon, target, sp):
                    return
    # Melee approach
    if dist <= 1:
        from . import combat
        combat.melee_attack(state, mon, target)
        return
    move_toward(state, mon, target)


def pick_target(state, mon):
    p = state.player
    if mon.faction == "ally":
        # ally: target nearest hostile
        cand = [e for e in state.gm.entities
                if e.alive and e.faction == "monster"]
        if not cand:
            return None
        cand.sort(key=lambda e: chebyshev(mon.at(), e.at()))
        return cand[0]
    # Hostile: target player or allies
    if state.gm.visible[mon.x][mon.y] or chebyshev(mon.at(), p.at()) < 8:
        return p
    return None


def has_los(gm, x0, y0, x1, y1):
    pts = line(x0, y0, x1, y1)
    for (x, y) in pts[1:-1]:
        if not gm.transparent(x, y):
            return False
    return True


def move_toward(state, mon, target):
    blockers = {(e.x, e.y) for e in state.gm.entities
                if e.alive and e.blocks and e is not mon}
    path = astar(state.gm, mon.at(), target.at(), blockers=blockers)
    if path and len(path) > 1:
        nx, ny = path[1]
        if state.gm.walkable(nx, ny) and not state.gm.blocking_entity_at(nx, ny):
            mon.x, mon.y = nx, ny
        else:
            rand_step(state, mon)
    else:
        rand_step(state, mon)


def flee_from(state, mon, target):
    dx = mon.x - target.x
    dy = mon.y - target.y
    dx = (1 if dx > 0 else -1) if dx else state.rng.choice([-1, 1])
    dy = (1 if dy > 0 else -1) if dy else state.rng.choice([-1, 1])
    nx, ny = mon.x + dx, mon.y + dy
    if state.gm.walkable(nx, ny) and not state.gm.blocking_entity_at(nx, ny):
        mon.x, mon.y = nx, ny


def wander(state, mon):
    if state.rng.chance(60):
        rand_step(state, mon)


def rand_step(state, mon):
    dx = state.rng.choice([-1, 0, 1])
    dy = state.rng.choice([-1, 0, 1])
    if dx == 0 and dy == 0:
        return
    nx, ny = mon.x + dx, mon.y + dy
    if state.gm.walkable(nx, ny) and not state.gm.blocking_entity_at(nx, ny):
        mon.x, mon.y = nx, ny


def cast_spell_at(state, mon, target, spell_key):
    from .spells import SPELLS
    sp = SPELLS.get(spell_key)
    if sp is None:
        return False
    from . import combat
    if sp.target == "self":
        combat.apply_spell_effect(state, mon, mon, sp)
        state.log.add(f"{mon.name} casts {sp.name}.", (180, 130, 240))
        return True
    if sp.target in ("enemy", "ray", "tile", "area"):
        if not has_los(state.gm, mon.x, mon.y, target.x, target.y):
            return False
        combat.apply_spell_effect(state, mon, target, sp)
        state.log.add(f"{mon.name} casts {sp.name}!", (180, 130, 240))
        return True
    return False


def exec_special(state, mon, target, special):
    from . import combat
    if special == "poison_bite":
        if chebyshev(mon.at(), target.at()) <= 1:
            combat.melee_attack(state, mon, target)
            target.statuses.add("poisoned", 8, 1)
            return True
    elif special == "paralyze_touch":
        if chebyshev(mon.at(), target.at()) <= 1:
            combat.melee_attack(state, mon, target)
            target.statuses.add("stunned", 2, 1)
            return True
    elif special == "rust_armor":
        if chebyshev(mon.at(), target.at()) <= 1:
            armor = getattr(target, "equipment", None)
            if armor and armor.body and armor.body.enchant > -3:
                armor.body.enchant -= 1
                state.log.add(f"Your {armor.body.d.name} rusts!", (180, 130, 90))
                return True
    elif special == "regen":
        mon.hp = min(mon.max_hp, mon.hp + 2)
        return False
    elif special == "petrify_gaze":
        if chebyshev(mon.at(), target.at()) <= 4 and has_los(state.gm, mon.x, mon.y, target.x, target.y):
            target.statuses.add("petrified", 2, 1)
            state.log.add(f"{mon.name}'s gaze petrifies!", (160, 160, 160))
            return True
    elif special == "level_drain":
        if chebyshev(mon.at(), target.at()) <= 1:
            combat.melee_attack(state, mon, target)
            if hasattr(target, "level") and target.level > 1:
                target.statuses.add("drained", 30, 1)
                target.xp = max(0, target.xp - 5)
                state.log.add("You feel drained!", (90, 90, 130))
                return True
    elif special == "life_drain":
        if chebyshev(mon.at(), target.at()) <= 1:
            dmg = combat.melee_attack(state, mon, target)
            if dmg > 0:
                heal = dmg // 2
                mon.hp = min(mon.max_hp, mon.hp + heal)
                return True
    elif special == "tail_spike":
        if chebyshev(mon.at(), target.at()) <= 6 and has_los(state.gm, mon.x, mon.y, target.x, target.y):
            from .utils import roll
            d = roll("2d6")
            combat.deal_damage(state, mon, target, d, "physical")
            state.log.add(f"{mon.name} hurls a spike!", (220, 60, 60))
            return True
    elif special == "ambush":
        return False
    elif special == "engulf":
        if chebyshev(mon.at(), target.at()) <= 1:
            combat.melee_attack(state, mon, target)
            target.statuses.add("frozen", 1, 1)
            return True
    elif special == "spore_burst":
        if chebyshev(mon.at(), target.at()) <= 1:
            target.statuses.add("poisoned", 4, 1)
            return True
    elif special == "paralyze_gaze":
        if chebyshev(mon.at(), target.at()) <= 6 and has_los(state.gm, mon.x, mon.y, target.x, target.y):
            target.statuses.add("stunned", 2, 1)
            state.log.add("Floating eye stares!", (220, 220, 90))
            return True
    return False

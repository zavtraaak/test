"""Special effects: summons, mapping, identify, etc."""
from __future__ import annotations
import random
from .monsters import MONSTERS
from .entity import Monster


def run(state, caster, target, spell):
    handler = HANDLERS.get(spell.custom)
    if handler:
        handler(state, caster, target, spell)


def _identify_inv(state, caster, target, spell):
    if state.player is caster:
        state.pending_action = "identify_choice"
    else:
        # NPC identifies; we don't bother
        pass


def _cast_light(state, caster, target, spell):
    state.light_radius = max(state.light_radius, 12)
    state.log.add("Bright light floods the area.", (240, 240, 200))


def _blink(state, caster, target, spell):
    rng = state.rng
    for _ in range(40):
        nx = caster.x + rng.randint(-4, 4)
        ny = caster.y + rng.randint(-4, 4)
        if state.gm.walkable(nx, ny) and not state.gm.blocking_entity_at(nx, ny):
            caster.x, caster.y = nx, ny
            return


def _teleport_self(state, caster, target, spell):
    rng = state.rng
    for _ in range(200):
        nx = rng.randint(2, state.gm.w - 3)
        ny = rng.randint(2, state.gm.h - 3)
        if state.gm.walkable(nx, ny) and not state.gm.blocking_entity_at(nx, ny):
            caster.x, caster.y = nx, ny
            return


def _magic_mapping(state, caster, target, spell):
    for x in range(state.gm.w):
        for y in range(state.gm.h):
            state.gm.explored[x][y] = True
    state.log.add("The map of the level fills your mind.", (180, 130, 240))


def _polymorph_target(state, caster, target, spell):
    rng = state.rng
    if target is state.player:
        state.log.add("You feel strange...", (200, 130, 200))
        return
    pool = [k for k, m in MONSTERS.items() if m["dlvl"] <= state.player.depth + 2]
    new_key = rng.choice(pool)
    new = Monster(target.x, target.y, MONSTERS[new_key])
    new.faction = target.faction
    state.gm.entities.remove(target)
    state.gm.entities.append(new)
    state.log.add(f"{target.name} becomes {new.name}!", (200, 130, 200))


def _counterspell(state, caster, target, spell):
    state.log.add("Spell countered!", (180, 130, 240))


def _cure_poisoned(state, caster, target, spell):
    if caster.statuses.has("poisoned"):
        caster.statuses.remove("poisoned")
        state.log.add("You feel cleansed.", (90, 200, 90))


def _cure_disease(state, caster, target, spell):
    if caster.statuses.has("diseased"):
        caster.statuses.remove("diseased")
        state.log.add("You feel healthy.", (90, 200, 90))


def _cure_mind(state, caster, target, spell):
    for s in ("confused", "feared", "asleep"):
        caster.statuses.remove(s)


def _lifedrain_cast(state, caster, target, spell):
    if hasattr(target, "hp"):
        heal = max(2, (target.hp - target.hp) // 2)
        # Already handled by damage. Heal a flat 6.
        caster.hp = min(getattr(caster, "max_hp", caster.hp + 6), caster.hp + 6)


def _animate_dead(state, caster, target, spell):
    summon(state, caster, "skeleton")
    summon(state, caster, "zombie")


def _chain_lightning(state, caster, target, spell):
    from .combat import deal_damage
    from .utils import roll, chebyshev
    seen = {target}
    cur = target
    chains = 4
    while chains > 0 and cur:
        d = roll(spell.damage)
        deal_damage(state, caster, cur, d, "lightning")
        # Find nearest unseen enemy
        nxt = None
        bestd = 999
        for e in state.gm.entities:
            if e in seen or not e.alive:
                continue
            if e.faction == "player":
                continue
            dist = chebyshev(cur.at(), e.at())
            if dist < bestd and dist <= 4:
                bestd = dist
                nxt = e
        if nxt:
            seen.add(nxt)
        cur = nxt
        chains -= 1


def _wish(state, caster, target, spell):
    state.pending_action = "wish_choice"


def _minor_illusion(state, caster, target, spell):
    state.log.add("You create a fleeting image.", (180, 130, 240))


def _turn_undead(state, caster, target, spell):
    from .utils import chebyshev
    for e in list(state.gm.entities):
        if not e.alive:
            continue
        if "undead" in getattr(e, "flags", set()):
            if chebyshev(caster.at(), e.at()) <= spell.radius:
                e.statuses.add("feared", 8, 1)
                state.log.add(f"{e.name} flees!", (245, 240, 200))


HANDLERS = {
    "identify_inv": _identify_inv,
    "cast_light": _cast_light,
    "blink": _blink,
    "teleport_self": _teleport_self,
    "magic_mapping": _magic_mapping,
    "polymorph_target": _polymorph_target,
    "counterspell": _counterspell,
    "cure_poisoned": _cure_poisoned,
    "cure_disease": _cure_disease,
    "cure_mind": _cure_mind,
    "lifedrain_cast": _lifedrain_cast,
    "animate_dead": _animate_dead,
    "chain_lightning": _chain_lightning,
    "wish": _wish,
    "minor_illusion": _minor_illusion,
    "turn_undead": _turn_undead,
}


def summon(state, caster, mkey):
    rng = state.rng
    base = MONSTERS.get(mkey)
    if not base:
        return None
    # Find adjacent free tile
    for _ in range(40):
        dx = rng.randint(-2, 2); dy = rng.randint(-2, 2)
        if dx == 0 and dy == 0:
            continue
        nx, ny = caster.x + dx, caster.y + dy
        if state.gm.walkable(nx, ny) and not state.gm.blocking_entity_at(nx, ny):
            mon = Monster(nx, ny, base)
            mon.summoned = True
            mon.summon_owner = caster
            mon.summon_duration = 30
            mon.faction = "ally" if caster is state.player else "monster"
            state.gm.entities.append(mon)
            if caster is state.player:
                state.log.add(f"You summon a {base['name']}!", (180, 130, 240))
            return mon
    return None

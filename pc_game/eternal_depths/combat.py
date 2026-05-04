"""Combat resolution: melee, ranged, spells, damage."""
from __future__ import annotations
import random
from .utils import roll, chebyshev, line


def to_hit(attacker, target):
    """Roll d20 + bonus vs AC."""
    rng = getattr(target, "_rng", None)
    bonus = 0
    if hasattr(attacker, "attack_bonus"):
        bonus += attacker.attack_bonus()
    else:
        bonus += attacker.stats.mod("STR")
    target_ac = target.total_ac() if hasattr(target, "total_ac") else target.ac
    r = random.randint(1, 20)
    if r == 20:
        return True, True  # natural 20 = crit
    if r == 1:
        return False, False
    if attacker.statuses.has("blessed"):
        bonus += 1
    if attacker.statuses.has("rage"):
        bonus += 2
    if target.statuses.has("invisible") and not attacker.statuses.has("see_invis"):
        bonus -= 4
    return r + bonus >= target_ac, False


def deal_damage(state, attacker, target, amount, dtype="physical", crit=False):
    if amount <= 0:
        return 0
    # Resists
    res = 0
    if hasattr(target, "effective_resists"):
        res = target.effective_resists().get(dtype)
    else:
        res = target.resists.get(dtype)
    amount = max(0, int(amount * (1.0 - res / 100.0)))
    # DR for physical
    if dtype == "physical":
        amount = max(0, amount - getattr(target, "dr", 0))
    # Marked status: more damage
    if target.statuses.has("marked"):
        amount = int(amount * 1.25)
    # Shield status reduces
    sh = target.statuses.get("shield")
    if sh:
        amount = max(0, amount - 3 * sh.magnitude)
    if target.statuses.has("barkskin"):
        amount = max(0, amount - 2)
    if target.statuses.has("rage"):
        amount = max(0, amount - 1)
    if amount <= 0:
        if state.player is target:
            state.log.add(f"You shrug off the blow.", (180, 180, 200))
        return 0
    target.hp -= amount
    if state.player is target:
        state.log.add(f"You take {amount} {dtype} damage!", (220, 80, 80))
    elif state.player is attacker:
        state.log.add(f"You hit {target.name} for {amount} {dtype}.{' CRIT!' if crit else ''}",
                      (240, 200, 80) if crit else (200, 220, 200))
    else:
        # NPC vs NPC
        pass
    if target.hp <= 0:
        kill(state, attacker, target)
    return amount


def kill(state, attacker, target):
    target.alive = False
    target.blocks = False
    if state.player is target:
        state.log.add("You die...", (220, 60, 60))
        state.game_over = True
        state.death_cause = f"slain by {attacker.name}"
        return
    state.log.add(f"{target.name} dies!", (90, 200, 90))
    if state.player is attacker:
        attacker.kills += 1
        leveled = attacker.gain_xp(target.kill_xp)
        if leveled:
            state.log.add(f"You reach level {attacker.level}!", (240, 220, 90))
        attacker.score += target.kill_xp
        # Track achievements
        from .achievements import on_kill
        on_kill(state, target)
    # Drop items / gold
    drop_loot(state, target)


def drop_loot(state, target):
    from .items import Item, ITEMS, init_charges
    rng = state.rng
    if target.gold:
        # Place gold on tile
        amt = max(1, int(target.gold * (0.7 + rng.random())))
        from .items import _Misc
        # Reuse coin item by registering once
        if "gold_pile" not in ITEMS:
            ITEMS["gold_pile"] = _Misc(
                "gold_pile", "Gold", "$",
                (240, 200, 80),
                desc="Shiny coins.", value=1, weight=0,
                category="gold")
            ITEMS["gold_pile"].stackable = True
        coin = Item("gold_pile", stack=amt)
        state.gm.add_item(target.x, target.y, coin)
    for key, chance in target.death_drops:
        if rng.chance(chance):
            it = init_charges(Item(key))
            state.gm.add_item(target.x, target.y, it)


def melee_attack(state, attacker, target):
    """Resolve a melee attack. Returns total damage dealt."""
    hit, crit = to_hit(attacker, target)
    if not hit:
        if state.player is attacker:
            state.log.add(f"You miss the {target.name}.", (140, 140, 140))
        elif state.player is target:
            state.log.add(f"{attacker.name} misses you.", (140, 140, 140))
        return 0
    # Damage
    if hasattr(attacker, "attack_damage"):
        dice, dtype, crit_chance, ench = attacker.attack_damage()
    else:
        dice = attacker.attack_dice
        dtype = attacker.dtype
        crit_chance = attacker.crit
        ench = 0
    # Crit chance from weapon (additional to nat 20)
    if not crit and random.random() * 100 < crit_chance:
        crit = True
    dmg = roll(dice)
    dmg += attacker.stats.mod("STR") if dtype == "physical" else 0
    dmg += ench
    if crit:
        dmg = dmg * 2
    # weapon on-hit effects
    if hasattr(attacker, "equipment") and attacker.equipment.weapon:
        for effect in attacker.equipment.weapon.d.on_hit:
            key, dur, ch = effect
            if random.random() * 100 < ch:
                target.statuses.add(key, dur)
    # Vampiric
    if attacker.statuses.has("vampiric") and dmg > 0:
        attacker.hp = min(getattr(attacker, "max_hp", 999),
                          attacker.hp + max(1, dmg // 4))
    # Race innate: lifedrain (vampire race)
    if hasattr(attacker, "race_innate") and "lifedrain" in attacker.race_innate:
        attacker.hp = min(attacker.max_hp, attacker.hp + max(1, dmg // 5))
    actual = deal_damage(state, attacker, target, dmg, dtype, crit=crit)
    return actual


def ranged_attack(state, attacker, target, weapon, ammo=None):
    if chebyshev(attacker.at(), target.at()) > weapon.d.range:
        return 0
    if not los(state.gm, attacker.x, attacker.y, target.x, target.y):
        if state.player is attacker:
            state.log.add("No clear shot.", (180, 130, 90))
        return 0
    hit, crit = to_hit(attacker, target)
    if not hit:
        if state.player is attacker:
            state.log.add(f"You miss the {target.name}.", (140, 140, 140))
        return 0
    dice = weapon.d.damage
    dtype = weapon.d.dtype
    if ammo:
        dice = ammo.d.damage or dice
        dtype = ammo.d.dtype
    dmg = roll(dice)
    dmg += attacker.stats.mod("DEX")
    dmg += weapon.enchant
    if crit:
        dmg = dmg * 2
    actual = deal_damage(state, attacker, target, dmg, dtype, crit)
    return actual


def los(gm, x0, y0, x1, y1):
    pts = line(x0, y0, x1, y1)
    for (x, y) in pts[1:-1]:
        if not gm.transparent(x, y):
            return False
    return True


def apply_spell_effect(state, caster, target, spell):
    """Apply spell mechanics."""
    from .spells import SPELLS
    rng = state.rng
    # Heal
    if spell.heal and target is caster:
        amt = spell.heal + caster.stats.mod("WIS") * 2
        target.hp = min(target.max_hp, target.hp + amt)
        if state.player is caster:
            state.log.add(f"You heal {amt} HP.", (90, 200, 90))
        return
    # Damage
    if spell.damage:
        bonus = caster.stats.mod("INT") if spell.school in ("evocation", "necromancy", "abjuration", "transmutation", "conjuration", "divination", "illusion", "enchantment") else caster.stats.mod("WIS")
        d = roll(spell.damage) + bonus
        if spell.target in ("tile", "area") and spell.radius > 0:
            tx, ty = target.at() if hasattr(target, "at") else target
            for x in range(tx - spell.radius, tx + spell.radius + 1):
                for y in range(ty - spell.radius, ty + spell.radius + 1):
                    if not state.gm.in_bounds(x, y):
                        continue
                    if (x - tx) ** 2 + (y - ty) ** 2 > spell.radius ** 2:
                        continue
                    e = state.gm.entity_at(x, y)
                    if e and e is not caster:
                        deal_damage(state, caster, e, d, spell.dtype)
            state.particles.append((tx, ty, spell.radius, spell.dtype, 6))
        elif spell.target == "ray":
            pts = line(caster.x, caster.y, target.x, target.y)
            for (x, y) in pts[1:]:
                e = state.gm.entity_at(x, y)
                if e and e is not caster:
                    deal_damage(state, caster, e, d, spell.dtype)
                if not state.gm.transparent(x, y):
                    break
            state.particles.append((target.x, target.y, 1, spell.dtype, 4))
        else:
            deal_damage(state, caster, target, d, spell.dtype)
            state.particles.append((target.x, target.y, 1, spell.dtype, 4))
    # Status
    if spell.status:
        key, dur, mag = spell.status
        if spell.target == "self":
            caster.statuses.add(key, dur, mag)
        else:
            target.statuses.add(key, dur, mag)
    # Custom
    if spell.custom:
        from . import effects
        effects.run(state, caster, target, spell)
    # Summon
    if spell.summon_key:
        from . import effects
        effects.summon(state, caster, spell.summon_key)

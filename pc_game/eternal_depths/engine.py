"""Main game loop, input handling, turn coordination."""
from __future__ import annotations
import os
import sys
import pygame
from . import render, fov, ai, save_load, high_score, achievements
from . import actions, combat
from .constants import (SCREEN_W, SCREEN_H, FPS, GAME_TITLE, FOV_RADIUS,
                        VIEW_W, VIEW_H, TILE_SIZE)
from .state import GameState
from .entity import Player, Monster, xp_required
from .items import Item, ITEMS, init_charges
from .utils import chebyshev


_DIRS = {
    "h": (-1, 0), "j": (0, 1), "k": (0, -1), "l": (1, 0),
    "y": (-1, -1), "u": (1, -1), "b": (-1, 1), "n": (1, 1),
    pygame.K_LEFT: (-1, 0), pygame.K_RIGHT: (1, 0),
    pygame.K_UP: (0, -1), pygame.K_DOWN: (0, 1),
    pygame.K_KP4: (-1, 0), pygame.K_KP6: (1, 0),
    pygame.K_KP8: (0, -1), pygame.K_KP2: (0, 1),
    pygame.K_KP7: (-1, -1), pygame.K_KP9: (1, -1),
    pygame.K_KP1: (-1, 1), pygame.K_KP3: (1, 1),
}


class Engine:
    def __init__(self):
        pygame.init()
        os.environ.setdefault("SDL_VIDEO_CENTERED", "1")
        self.screen = pygame.display.set_mode((SCREEN_W, SCREEN_H))
        pygame.display.set_caption(GAME_TITLE)
        self.clock = pygame.time.Clock()
        self.state = GameState()
        self.state.screen = self.screen
        self.state.menu_state = "main"
        # Char create staging
        self.state.cc_name = ""
        self.state.cc_race_idx = 0
        self.state.cc_class_idx = 0
        self.state.current_npc = None
        self.state.shop_mode = "buy"
        self.state.boss_intro_name = None
        self.running = True

    def run(self):
        while self.running:
            self.handle_events()
            render.render(self.state)
            self.clock.tick(FPS)
        pygame.quit()

    def handle_events(self):
        for ev in pygame.event.get():
            if ev.type == pygame.QUIT:
                self.running = False
            elif ev.type == pygame.KEYDOWN:
                self.on_key(ev)
            elif ev.type == pygame.MOUSEBUTTONDOWN:
                self.on_mouse(ev)

    def on_mouse(self, ev):
        # Translate mouse to tile
        if self.state.menu_state not in ("in_game", "look"):
            return
        mx, my = ev.pos
        if mx >= VIEW_W * TILE_SIZE or my >= VIEW_H * TILE_SIZE:
            return
        cx = mx // TILE_SIZE
        cy = my // TILE_SIZE
        p = self.state.player
        cam_x = max(0, min(self.state.gm.w - VIEW_W, p.x - VIEW_W // 2))
        cam_y = max(0, min(self.state.gm.h - VIEW_H, p.y - VIEW_H // 2))
        tx, ty = cx + cam_x, cy + cam_y
        self.state.target_pos = (tx, ty)
        self.state.menu_state = "look"

    def on_key(self, ev):
        ms = self.state.menu_state
        if ms == "main":
            self.key_main_menu(ev)
        elif ms == "char_create":
            self.key_char_create(ev)
        elif ms in ("scores", "achievements", "options"):
            if ev.key == pygame.K_ESCAPE:
                self.state.menu_state = "main"
        elif ms == "in_game":
            self.key_in_game(ev)
        elif ms == "inventory":
            self.key_inventory(ev)
        elif ms == "equipment":
            self.key_equipment(ev)
        elif ms == "spells":
            self.key_spells(ev)
        elif ms == "abilities":
            self.key_abilities(ev)
        elif ms == "skills":
            if ev.key == pygame.K_ESCAPE:
                self.state.menu_state = "in_game"
        elif ms == "char_sheet":
            if ev.key == pygame.K_ESCAPE:
                self.state.menu_state = "in_game"
        elif ms == "quests":
            if ev.key == pygame.K_ESCAPE:
                self.state.menu_state = "in_game"
        elif ms == "help":
            if ev.key == pygame.K_ESCAPE:
                self.state.menu_state = "in_game"
        elif ms == "death":
            if ev.key == pygame.K_n:
                self.state.menu_state = "char_create"
                self.state.cc_name = ""
                self.state.cc_race_idx = 0
                self.state.cc_class_idx = 0
            elif ev.key == pygame.K_ESCAPE:
                self.state.menu_state = "main"
        elif ms == "victory":
            if ev.key == pygame.K_n:
                self.state.menu_state = "char_create"
            elif ev.key == pygame.K_ESCAPE:
                self.state.menu_state = "main"
        elif ms == "level_up":
            self.key_level_up(ev)
        elif ms == "identify_choice":
            self.key_identify(ev)
        elif ms == "drop_choice":
            self.key_drop_choice(ev)
        elif ms == "remove_choice":
            self.key_remove_choice(ev)
        elif ms == "look":
            self.key_look(ev)
        elif ms == "shop":
            self.key_shop(ev)
        elif ms == "npc_dialog":
            self.key_npc_dialog(ev)
        elif ms in ("cast_target", "wand_target", "throw_potion", "throw_bomb"):
            self.key_target(ev)
        elif ms == "wish_choice":
            self.key_wish(ev)
        elif ms == "boss_intro":
            if ev.key == pygame.K_ESCAPE or ev.key == pygame.K_RETURN:
                self.state.menu_state = "in_game"

    # ---- MAIN MENU ----
    def key_main_menu(self, ev):
        if ev.key == pygame.K_n:
            self.state.menu_state = "char_create"
        elif ev.key == pygame.K_l:
            data = save_load.load_game()
            if data:
                self.state = GameState.from_dict(data)
                self.state.screen = self.screen
                self.state.menu_state = "in_game"
        elif ev.key == pygame.K_h:
            self.state.menu_state = "scores"
        elif ev.key == pygame.K_a:
            from .entity import Player
            if not self.state.player:
                # show empty
                self.state.player = Player(0, 0, "Visitor", "Human", "Warrior")
            self.state.menu_state = "achievements"
        elif ev.key == pygame.K_o:
            self.state.menu_state = "options"
        elif ev.key == pygame.K_q or ev.key == pygame.K_ESCAPE:
            self.running = False

    # ---- CHAR CREATE ----
    def key_char_create(self, ev):
        from .races import race_keys
        from .classes import class_keys
        rks = race_keys()
        cks = class_keys()
        if ev.key == pygame.K_ESCAPE:
            self.state.menu_state = "main"
            return
        if ev.key == pygame.K_RETURN:
            if not self.state.cc_name:
                self.state.cc_name = "Hero"
            self.start_new_game(self.state.cc_name,
                                rks[self.state.cc_race_idx],
                                cks[self.state.cc_class_idx])
            return
        if ev.key == pygame.K_BACKSPACE:
            self.state.cc_name = self.state.cc_name[:-1]
            return
        # Letters select race/class
        if ev.unicode and ev.unicode.isalpha():
            ch = ev.unicode
            if ch.lower() == ch:
                # lowercase -> race or class? We split: a..max(rks) treats race; A..Z treats class
                idx = ord(ch) - ord('a')
                # priority: race if idx < len(rks) and not already set; else class
                if idx < len(rks):
                    self.state.cc_race_idx = idx
                    return
            elif ch.isupper():
                idx = ord(ch) - ord('A')
                if idx < len(cks):
                    self.state.cc_class_idx = idx
                    return
            # else treat as name input
            if len(self.state.cc_name) < 16:
                self.state.cc_name += ev.unicode
        elif ev.unicode and ev.unicode.isprintable():
            if len(self.state.cc_name) < 16:
                self.state.cc_name += ev.unicode

    def start_new_game(self, name, race, cls):
        self.state = GameState()
        self.state.screen = self.screen
        p = Player(0, 0, name, race, cls)
        from .classes import CLASSES
        # Equip starter
        cd = CLASSES[cls]
        for ikey in cd["starting_items"]:
            if ikey in ITEMS:
                it = init_charges(Item(ikey, identified=True))
                p.inventory.add(it)
        # Auto-equip best basics
        auto_equip_starters(p)
        self.state.player = p
        self.state.menu_state = "in_game"
        self.state._enter_level(1)

    # ---- IN-GAME ----
    def key_in_game(self, ev):
        s = self.state
        p = s.player
        if s.game_over:
            return
        # Movement
        key = ev.key
        ch = ev.unicode
        moved = False
        if ch in ("h", "j", "k", "l", "y", "u", "b", "n"):
            d = _DIRS[ch]
            moved = actions.move_or_attack(s, *d)
        elif key in _DIRS:
            d = _DIRS[key]
            moved = actions.move_or_attack(s, *d)
        elif ch == ".":
            moved = True
        elif key == pygame.K_KP5 or ch == "5":
            actions.rest(s)
            moved = True
        elif ch == "g" or key == pygame.K_COMMA:
            moved = actions.pickup(s)
        elif ch == "i":
            s.menu_state = "inventory"
        elif ch == "e":
            s.menu_state = "equipment"
        elif ch == "r":
            s.menu_state = "remove_choice"
        elif ch == "z":
            s.menu_state = "spells"
        elif ch == "Z":
            s.menu_state = "abilities"
        elif ch == "s":
            s.menu_state = "skills"
        elif ch == "C":
            s.menu_state = "char_sheet"
        elif ch == "Q":
            s.menu_state = "quests"
        elif ch == "A":
            s.menu_state = "achievements"
        elif ch == "?":
            s.menu_state = "help"
        elif ch == "q":
            t = s.gm.tile(p.x, p.y)
            if t.fountain:
                actions.quaff_fountain(s); moved = True
            else:
                s.menu_state = "use_choice"
                s.use_filter = "potion"
                s.menu_state = "inventory"
        elif ch == "R":
            s.menu_state = "inventory"
            s.use_filter = "scroll"
        elif ch == "f":
            self.start_ranged_attack(s)
        elif ch == "t":
            s.menu_state = "inventory"
            s.use_filter = "throw"
        elif ch == "x":
            s.target_pos = (p.x, p.y)
            s.menu_state = "look"
        elif ch == "#":
            t = s.gm.tile(p.x, p.y)
            if t.altar:
                actions.pray_altar(s); moved = True
        elif ch == "o":
            actions.open_chest(s); moved = True
        elif ch == ">":
            if actions.descend(s):
                s.menu_state = "in_game"
        elif ch == "<":
            if actions.ascend(s):
                s.menu_state = "in_game"
        elif ch == "S":
            save_load.save_game(s)
            s.log.add("Game saved.", (200, 220, 200))
        elif ch == "d":
            s.menu_state = "drop_choice"
        elif key == pygame.K_ESCAPE:
            save_load.save_game(s)
            s.menu_state = "main"
        if moved:
            self.advance_turn()

    def start_ranged_attack(self, s):
        p = s.player
        w = p.equipment.weapon
        if not w or not w.d.ranged:
            s.log.add("No ranged weapon equipped.", (180, 180, 180))
            return
        # Find first enemy in range
        target = nearest_enemy(s, w.d.range)
        if target is None:
            s.log.add("No target in range.", (180, 180, 180))
            return
        s.target_pos = target.at()
        s.menu_state = "wand_target"
        s.targeting_action = ("ranged", w)

    # ---- INVENTORY ----
    def key_inventory(self, ev):
        s = self.state
        p = s.player
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"
            s.use_filter = None
            return
        if ev.unicode and ev.unicode.isalpha():
            idx = ord(ev.unicode.lower()) - ord('a')
            if 0 <= idx < len(p.inventory.items):
                it = p.inventory.items[idx]
                # Equip if equipment
                if it.d.category in ("weapon", "armor", "ring", "amulet"):
                    actions.equip(s, it)
                    s.menu_state = "in_game"
                    self.advance_turn()
                else:
                    ok = actions.use_item(s, it)
                    if ok:
                        s.menu_state = "in_game"
                        self.advance_turn()
                    else:
                        # use queued targeting; close menu
                        s.menu_state = "in_game"

    def key_equipment(self, ev):
        s = self.state
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"

    def key_spells(self, ev):
        s = self.state
        p = s.player
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"
            return
        if ev.unicode and ev.unicode.isalpha():
            idx = ord(ev.unicode.lower()) - ord('a')
            if 0 <= idx < len(p.spells):
                actions.cast_spell(s, p.spells[idx])
                s.menu_state = "in_game"

    def key_abilities(self, ev):
        s = self.state
        p = s.player
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"
            return
        if ev.unicode and ev.unicode.isalpha():
            idx = ord(ev.unicode.lower()) - ord('a')
            if 0 <= idx < len(p.abilities):
                actions.use_ability(s, p.abilities[idx])
                s.menu_state = "in_game"
                self.advance_turn()

    def key_drop_choice(self, ev):
        s = self.state
        p = s.player
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"
            return
        if ev.unicode and ev.unicode.isalpha():
            idx = ord(ev.unicode.lower()) - ord('a')
            if 0 <= idx < len(p.inventory.items):
                actions.drop(s, p.inventory.items[idx])
                s.menu_state = "in_game"
                self.advance_turn()

    def key_remove_choice(self, ev):
        s = self.state
        p = s.player
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"
            return
        if ev.unicode and ev.unicode.isalpha():
            idx = ord(ev.unicode.lower()) - ord('a')
            slots = [slot for slot, _ in p.equipment.all_items()]
            if 0 <= idx < len(slots):
                actions.unequip(s, slots[idx])
                s.menu_state = "in_game"

    def key_identify(self, ev):
        s = self.state
        p = s.player
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"
            return
        if ev.unicode and ev.unicode.isalpha():
            idx = ord(ev.unicode.lower()) - ord('a')
            if 0 <= idx < len(p.inventory.items):
                it = p.inventory.items[idx]
                it.identified = True
                p.identifications.add(it.def_key)
                s.log.add(f"That is {it.display_name}.", (180, 130, 240))
                s.menu_state = "in_game"

    def key_level_up(self, ev):
        s = self.state
        p = s.player
        m = {
            pygame.K_s: "STR", pygame.K_d: "DEX", pygame.K_c: "CON",
            pygame.K_i: "INT", pygame.K_w: "WIS", pygame.K_h: "CHA",
            pygame.K_l: "LUCK",
        }
        if ev.key in m:
            stat = m[ev.key]
            setattr(p.stats, stat, getattr(p.stats, stat) + 1)
            s.menu_state = "in_game"
            achievements.on_levelup(s)

    def key_look(self, ev):
        s = self.state
        if ev.key == pygame.K_ESCAPE:
            s.target_pos = None
            s.menu_state = "in_game"
            return
        if ev.unicode in _DIRS:
            d = _DIRS[ev.unicode]
            tx, ty = s.target_pos
            s.target_pos = (tx + d[0], ty + d[1])
        elif ev.key in _DIRS:
            d = _DIRS[ev.key]
            tx, ty = s.target_pos
            s.target_pos = (tx + d[0], ty + d[1])

    def key_shop(self, ev):
        s = self.state
        sk = s.current_npc
        p = s.player
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"
            return
        if ev.unicode == "b":
            s.shop_mode = "buy"
            return
        if ev.unicode == "s":
            s.shop_mode = "sell"
            return
        if ev.unicode and ev.unicode.isalpha():
            idx = ord(ev.unicode.lower()) - ord('a')
            if s.shop_mode == "buy":
                if 0 <= idx < len(sk.inventory_items):
                    it = sk.inventory_items[idx]
                    cost = it.total_value
                    if p.gold >= cost:
                        p.gold -= cost
                        sk.inventory_items.pop(idx)
                        if not p.inventory.add(it):
                            s.log.add("Inventory full.", (220, 80, 80))
                            sk.inventory_items.append(it)
                            p.gold += cost
                    else:
                        s.log.add("Not enough gold.", (220, 80, 80))
            else:
                if 0 <= idx < len(p.inventory.items):
                    it = p.inventory.items[idx]
                    p.gold += it.total_value // 2
                    p.inventory.remove(it, it.stack)
                    sk.inventory_items.append(it)

    def key_npc_dialog(self, ev):
        s = self.state
        npc = s.current_npc
        p = s.player
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"
            return
        if npc.npc_type == "shopkeeper" and ev.unicode == "t":
            s.menu_state = "shop"
            s.shop_mode = "buy"
            return
        if npc.npc_type == "quest":
            if ev.unicode == "a" and npc.quest not in s.quests.active:
                s.quests.accept(npc.quest)
                s.log.add(f"Quest accepted: {npc.quest['desc']}",
                          (240, 220, 90))
                # Track inventory for fetch
                if npc.quest["type"] == "fetch":
                    s.quests.progress_fetch({it.def_key for it in p.inventory.items})
                s.menu_state = "in_game"
                return
            if ev.unicode == "c" and npc.quest in s.quests.active:
                if s.quests.is_complete(npc.quest):
                    p.gold += npc.quest.get("reward_gold", 0)
                    p.gain_xp(npc.quest.get("reward_xp", 0))
                    s.quests.completed.append(npc.quest)
                    s.quests.active.remove(npc.quest)
                    s.log.add("Reward!", (240, 220, 90))
                    if npc.quest["type"] == "fetch":
                        for it in list(p.inventory.items):
                            if it.def_key == npc.quest["item"]:
                                p.inventory.remove(it, 1)
                                break
                s.menu_state = "in_game"

    def key_target(self, ev):
        s = self.state
        p = s.player
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"
            s.target_pos = None
            return
        if ev.unicode in _DIRS:
            d = _DIRS[ev.unicode]
            tx, ty = s.target_pos or p.at()
            s.target_pos = (tx + d[0], ty + d[1])
        elif ev.key in _DIRS:
            d = _DIRS[ev.key]
            tx, ty = s.target_pos or p.at()
            s.target_pos = (tx + d[0], ty + d[1])
        elif ev.key == pygame.K_TAB:
            # cycle enemies
            enemies = [e for e in s.gm.entities
                       if e.alive and e.faction == "monster"
                       and s.gm.visible[e.x][e.y]]
            if enemies:
                if s.target_pos:
                    cur = next((i for i, e in enumerate(enemies)
                                if (e.x, e.y) == s.target_pos), -1)
                    s.target_pos = enemies[(cur + 1) % len(enemies)].at()
                else:
                    s.target_pos = enemies[0].at()
        elif ev.key == pygame.K_RETURN or ev.unicode == ".":
            # confirm
            self.confirm_target()

    def confirm_target(self):
        s = self.state
        p = s.player
        ms = s.menu_state
        tx, ty = s.target_pos
        if ms == "cast_target":
            sp = s.pending_action[1]
            target = s.gm.entity_at(tx, ty) or type("Tgt", (), {"x": tx, "y": ty,
                                                                "at": lambda self: (tx, ty),
                                                                "name": "tile",
                                                                "alive": True,
                                                                "statuses": __import__("eternal_depths.status_effects",
                                                                                       fromlist=["StatusBag"]).StatusBag(),
                                                                "hp": 0, "max_hp": 0,
                                                                "resists": __import__("eternal_depths.stats",
                                                                                       fromlist=["Resists"]).Resists()})()
            combat.apply_spell_effect(s, p, target, sp)
            s.menu_state = "in_game"
            s.pending_action = None
            self.advance_turn()
        elif ms == "wand_target":
            if isinstance(s.pending_action, tuple) and s.pending_action[0] == "wand_target":
                wand = s.pending_action[1]
                from .spells import SPELLS
                sp = SPELLS[wand.d.spell]
                target = s.gm.entity_at(tx, ty)
                if target:
                    combat.apply_spell_effect(s, p, target, sp)
                wand.charges -= 1
                if wand.charges <= 0:
                    s.log.add(f"{wand.d.name} crumbles.", (180, 180, 180))
                    p.inventory.remove(wand, 1)
                s.menu_state = "in_game"
                s.pending_action = None
                self.advance_turn()
            elif isinstance(s, object) and getattr(s, "targeting_action", None):
                kind, w = s.targeting_action
                if kind == "ranged":
                    target = s.gm.entity_at(tx, ty)
                    if target and target.alive:
                        combat.ranged_attack(s, p, target, w)
                s.targeting_action = None
                s.menu_state = "in_game"
                self.advance_turn()

    def key_wish(self, ev):
        s = self.state
        wish_pool = ["potion_full_healing", "potion_gain_level",
                     "ring_protection", "am_lifesaving",
                     "scroll_enchant_weapon", "scroll_enchant_armor",
                     "wand_fire", "vorpal_blade", "holy_avenger",
                     "excalibur", "dragonscale", "boots_of_speed"]
        if ev.key == pygame.K_ESCAPE:
            s.menu_state = "in_game"
            return
        if ev.unicode and ev.unicode.isalpha():
            idx = ord(ev.unicode.lower()) - ord('a')
            if 0 <= idx < len(wish_pool):
                it = init_charges(Item(wish_pool[idx], identified=True))
                s.player.inventory.add(it)
                s.log.add("You receive your wish.", (240, 220, 90))
                s.menu_state = "in_game"
                self.advance_turn()

    # ---- TURN SYSTEM ----
    def advance_turn(self):
        s = self.state
        p = s.player
        if not p:
            return
        # Player tick
        p.turns += 1
        p.statuses.tick_all()
        # Status damage
        for st in list(p.statuses.statuses):
            tick_status(s, p, st)
        # Hunger
        p.hunger -= 1
        if p.hunger <= 0:
            p.hp -= 1
            s.log.add("You starve!", (220, 80, 80))
        # Cooldowns
        for k in list(p.cooldowns.keys()):
            p.cooldowns[k] = max(0, p.cooldowns[k] - 1)
        # Mana / hp regen slow
        if p.turns % 12 == 0:
            p.mp = min(p.max_mp, p.mp + 1)
        if p.turns % 18 == 0:
            p.hp = min(p.max_hp, p.hp + 1)
        # FOV
        light = FOV_RADIUS
        if p.statuses.has("blinded"):
            light = 1
        fov.compute_fov(s.gm, p.x, p.y, light)
        # Monsters & NPCs
        for e in list(s.gm.entities):
            if not e.alive:
                continue
            if e is p:
                continue
            e.statuses.tick_all()
            for st in list(e.statuses.statuses):
                tick_status(s, e, st)
            if isinstance(e, Monster):
                e.gain_energy()
                while e.take_turn_ready():
                    if not e.alive:
                        break
                    ai.take_turn(s, e)
                    e.consume_turn()
                # Summon timer
                if e.summoned:
                    e.summon_duration -= 1
                    if e.summon_duration <= 0:
                        e.alive = False
                        e.blocks = False
        # Cull dead
        s.gm.entities = [e for e in s.gm.entities if e.alive or e is p]
        # Auto level up dialog
        from .entity import xp_required
        # If player gained level via xp gain, give stat-up dialog
        if hasattr(p, "_pending_levelup") and p._pending_levelup:
            s.menu_state = "level_up"
            p._pending_levelup = False
        # Death
        if p.hp <= 0 or s.game_over:
            p.score += p.gold + p.deepest * 100 + p.kills * 5
            high_score.add_score(p.name, p.race, p.cls, p.score,
                                 p.deepest, p.level,
                                 s.death_cause if s.game_over else "exhaustion")
            save_load.delete_save()
            s.menu_state = "death"
        # Victory
        if s.victory:
            s.menu_state = "victory"
        # Quest progress: fetch
        s.quests.progress_fetch({it.def_key for it in p.inventory.items})


def tick_status(state, ent, st):
    defn = __import__("eternal_depths.status_effects",
                      fromlist=["STATUS_DEFS"]).STATUS_DEFS.get(st.key)
    if not defn:
        return
    td = defn.get("tick_dmg")
    if td:
        dt, amt = td
        from . import combat
        combat.deal_damage(state, ent, ent, amt, dt)
    th = defn.get("tick_heal")
    if th:
        ent.hp = min(getattr(ent, "max_hp", ent.hp + th), ent.hp + th)


def nearest_enemy(state, max_range=20):
    p = state.player
    best = None
    bestd = 999
    for e in state.gm.entities:
        if not e.alive or e.faction != "monster":
            continue
        if not state.gm.visible[e.x][e.y]:
            continue
        d = chebyshev(p.at(), e.at())
        if d < bestd and d <= max_range:
            bestd = d
            best = e
    return best


def auto_equip_starters(p):
    # equip best in slot from inventory
    for it in list(p.inventory.items):
        slot = it.d.slot
        if slot:
            cur = p.equipment.get(slot)
            if cur is None:
                p.equipment.set(slot, it)
                p.inventory.remove(it, it.stack)

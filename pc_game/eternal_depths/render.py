"""Rendering using pygame."""
from __future__ import annotations
import os
import pygame
from . import colors as C
from .constants import (TILE_SIZE, FONT_SIZE, VIEW_W, VIEW_H, SCREEN_W,
                        SCREEN_H, SIDE_PANEL_W, LOG_LINES, GAME_TITLE)


_FONT_CACHE = {}


def get_font(size=FONT_SIZE, bold=False):
    key = (size, bold)
    if key in _FONT_CACHE:
        return _FONT_CACHE[key]
    # Prefer monospace
    candidates = ["DejaVu Sans Mono", "Liberation Mono", "Consolas",
                  "Courier New", None]
    f = None
    for name in candidates:
        try:
            if name is None:
                f = pygame.font.SysFont(pygame.font.get_default_font(),
                                        size, bold=bold)
            else:
                f = pygame.font.SysFont(name, size, bold=bold)
            if f:
                break
        except Exception:
            pass
    _FONT_CACHE[key] = f
    return f


def draw_text(surf, text, pos, color=C.UI_FG, size=FONT_SIZE, bold=False):
    f = get_font(size, bold)
    img = f.render(text, True, color)
    surf.blit(img, pos)
    return img.get_size()


def draw_glyph(surf, glyph, x, y, fg, bg=None, size=FONT_SIZE, bold=False):
    f = get_font(size, bold)
    if bg:
        pygame.draw.rect(surf, bg, (x, y, size, size))
    img = f.render(glyph, True, fg)
    rect = img.get_rect()
    surf.blit(img, (x + (size - rect.w) // 2, y + (size - rect.h) // 2))


def render_map(surf, state):
    p = state.player
    gm = state.gm
    # Camera centered on player
    cam_x = p.x - VIEW_W // 2
    cam_y = p.y - VIEW_H // 2
    cam_x = max(0, min(gm.w - VIEW_W, cam_x))
    cam_y = max(0, min(gm.h - VIEW_H, cam_y))
    # Background
    pygame.draw.rect(surf, C.BLACK, (0, 0, VIEW_W * TILE_SIZE,
                                     VIEW_H * TILE_SIZE))
    for vx in range(VIEW_W):
        for vy in range(VIEW_H):
            mx = cam_x + vx
            my = cam_y + vy
            if not gm.in_bounds(mx, my):
                continue
            if not gm.explored[mx][my]:
                continue
            t = gm.tile(mx, my)
            visible = gm.visible[mx][my]
            fg = t.fg if visible else _dim(t.fg, 0.4)
            bg = t.bg if visible else _dim(t.bg, 0.4)
            draw_glyph(surf, t.glyph, vx * TILE_SIZE, vy * TILE_SIZE,
                       fg, bg, size=TILE_SIZE)
    # Items
    for x, y, it in gm.items:
        if not gm.in_bounds(x, y) or not gm.visible[x][y]:
            continue
        vx, vy = x - cam_x, y - cam_y
        if 0 <= vx < VIEW_W and 0 <= vy < VIEW_H:
            draw_glyph(surf, it.d.glyph, vx * TILE_SIZE, vy * TILE_SIZE,
                       it.d.color, gm.tile(x, y).bg, size=TILE_SIZE,
                       bold=True)
    # Entities
    for e in gm.entities:
        if not e.alive:
            continue
        if not gm.in_bounds(e.x, e.y):
            continue
        if not gm.visible[e.x][e.y] and not (e is state.player):
            continue
        if e.statuses.has("invisible") and e is not state.player:
            continue
        vx, vy = e.x - cam_x, e.y - cam_y
        if 0 <= vx < VIEW_W and 0 <= vy < VIEW_H:
            draw_glyph(surf, e.glyph, vx * TILE_SIZE, vy * TILE_SIZE,
                       e.color, gm.tile(e.x, e.y).bg, size=TILE_SIZE,
                       bold=True)
    # Particles
    new_parts = []
    for (px, py, r, dtype, ttl) in state.particles:
        col = _dtype_color(dtype)
        for dx in range(-r, r + 1):
            for dy in range(-r, r + 1):
                if dx * dx + dy * dy > r * r:
                    continue
                vx = px + dx - cam_x
                vy = py + dy - cam_y
                if 0 <= vx < VIEW_W and 0 <= vy < VIEW_H:
                    s = pygame.Surface((TILE_SIZE, TILE_SIZE),
                                       pygame.SRCALPHA)
                    s.fill((col[0], col[1], col[2], min(180, ttl * 25)))
                    surf.blit(s, (vx * TILE_SIZE, vy * TILE_SIZE))
        if ttl > 1:
            new_parts.append((px, py, r, dtype, ttl - 1))
    state.particles = new_parts
    # Targeting reticle
    if state.target_pos:
        tx, ty = state.target_pos
        vx, vy = tx - cam_x, ty - cam_y
        if 0 <= vx < VIEW_W and 0 <= vy < VIEW_H:
            pygame.draw.rect(surf, (240, 220, 90),
                             (vx * TILE_SIZE - 1, vy * TILE_SIZE - 1,
                              TILE_SIZE + 2, TILE_SIZE + 2), 2)


def _dtype_color(dtype):
    return {
        "fire": C.COL_FIRE, "cold": C.COL_COLD,
        "lightning": C.COL_LIGHTNING, "poison": C.COL_POISON,
        "holy": C.COL_HOLY, "dark": C.COL_DARK,
        "arcane": C.COL_ARCANE, "acid": C.COL_ACID,
        "physical": C.COL_PHYSICAL,
    }.get(dtype, C.WHITE)


def _dim(c, factor):
    return (int(c[0] * factor), int(c[1] * factor), int(c[2] * factor))


def render_panel(surf, state):
    px = VIEW_W * TILE_SIZE
    pygame.draw.rect(surf, C.UI_BG, (px, 0, SIDE_PANEL_W, SCREEN_H))
    pygame.draw.line(surf, C.UI_BORDER, (px, 0), (px, SCREEN_H), 1)
    p = state.player
    y = 8
    draw_text(surf, p.name, (px + 8, y), C.UI_HIGHLIGHT, size=20, bold=True)
    y += 22
    draw_text(surf, f"{p.race} {p.cls}", (px + 8, y), C.UI_FG, size=14)
    y += 18
    draw_text(surf, f"Lvl {p.level}  XP {p.xp}", (px + 8, y))
    y += 18
    # HP / MP bars
    bar(surf, px + 8, y, 240, 12,
        p.hp, p.max_hp, (60, 30, 30), (220, 60, 60))
    draw_text(surf, f"HP {p.hp}/{p.max_hp}", (px + 8, y - 1), C.WHITE, size=14)
    y += 16
    bar(surf, px + 8, y, 240, 12,
        p.mp, p.max_mp, (30, 30, 60), (90, 130, 220))
    draw_text(surf, f"MP {p.mp}/{p.max_mp}", (px + 8, y - 1), C.WHITE, size=14)
    y += 16
    bar(surf, px + 8, y, 240, 8,
        p.hunger, 1500, (30, 30, 30), (240, 200, 80))
    draw_text(surf, f"Food {p.hunger}", (px + 8, y - 4), C.WHITE, size=12)
    y += 14
    # Stats
    s = p.effective_stats()
    draw_text(surf, f"STR {s.STR}  DEX {s.DEX}  CON {s.CON}",
              (px + 8, y))
    y += 16
    draw_text(surf, f"INT {s.INT}  WIS {s.WIS}  CHA {s.CHA}",
              (px + 8, y))
    y += 16
    draw_text(surf, f"LUCK {s.LUCK}  AC {p.total_ac()}", (px + 8, y))
    y += 18
    draw_text(surf, f"Depth {p.depth}/30  Gold {p.gold}",
              (px + 8, y), (240, 220, 90))
    y += 16
    draw_text(surf, f"Turn {p.turns}  Kills {p.kills}", (px + 8, y))
    y += 22
    # Statuses
    if p.statuses.statuses:
        draw_text(surf, "Effects:", (px + 8, y), C.UI_HIGHLIGHT)
        y += 16
        for name, color, dur in p.statuses.names()[:8]:
            draw_text(surf, f"  {name} ({dur})", (px + 8, y), color)
            y += 14
        y += 4
    # Equipment summary
    draw_text(surf, "Equipment:", (px + 8, y), C.UI_HIGHLIGHT)
    y += 16
    for slot, it in p.equipment.all_items():
        draw_text(surf, f"  {slot[:5]}: {it.display_name}",
                  (px + 8, y), it.d.color, size=13)
        y += 14
    y += 6
    # Spells
    if p.spells:
        draw_text(surf, "Spells:", (px + 8, y), C.UI_HIGHLIGHT)
        y += 16
        from .spells import SPELLS
        for sk in p.spells[:6]:
            sp = SPELLS.get(sk)
            if sp:
                draw_text(surf, f"  {sp.name} ({sp.mp})",
                          (px + 8, y), (180, 180, 240), size=13)
                y += 14
    # Hint footer
    draw_text(surf, "?: help  i: inv  z: cast",
              (px + 8, SCREEN_H - 40), C.GREY, size=12)
    draw_text(surf, "g: pickup  >/<: stairs",
              (px + 8, SCREEN_H - 24), C.GREY, size=12)


def bar(surf, x, y, w, h, val, max_val, bg, fg):
    pygame.draw.rect(surf, bg, (x, y, w, h))
    if max_val > 0:
        pygame.draw.rect(surf, fg, (x, y, int(w * val / max_val), h))
    pygame.draw.rect(surf, C.UI_BORDER, (x, y, w, h), 1)


def render_log(surf, state):
    y0 = VIEW_H * TILE_SIZE
    pygame.draw.rect(surf, C.UI_BG, (0, y0, VIEW_W * TILE_SIZE,
                                     SCREEN_H - y0))
    pygame.draw.line(surf, C.UI_BORDER, (0, y0),
                     (VIEW_W * TILE_SIZE, y0), 1)
    entries = state.log.tail(LOG_LINES)
    for i, (text, color) in enumerate(entries):
        draw_text(surf, text, (8, y0 + 6 + i * 18), color, size=14)


def render_overlay_text(surf, lines, title=None):
    box_w = 700
    box_h = min(SCREEN_H - 80, 60 + 22 * len(lines))
    bx = (SCREEN_W - box_w) // 2
    by = (SCREEN_H - box_h) // 2
    s = pygame.Surface((box_w, box_h), pygame.SRCALPHA)
    s.fill((10, 10, 18, 230))
    surf.blit(s, (bx, by))
    pygame.draw.rect(surf, C.UI_BORDER, (bx, by, box_w, box_h), 2)
    y = by + 14
    if title:
        draw_text(surf, title, (bx + 16, y), C.UI_HIGHLIGHT,
                  size=22, bold=True)
        y += 30
    for line in lines:
        if isinstance(line, tuple):
            text, color = line
        else:
            text, color = line, C.UI_FG
        draw_text(surf, text, (bx + 16, y), color, size=15)
        y += 20


def render(state):
    surf = state.screen
    surf.fill(C.BLACK)
    if state.menu_state in ("main", "char_create", "options", "scores",
                            "load_game", "credits"):
        render_menu(surf, state)
    else:
        render_map(surf, state)
        render_log(surf, state)
        render_panel(surf, state)
        if state.menu_state in ("inventory", "equipment", "spells",
                                "abilities", "skills", "help",
                                "quests", "char_sheet", "achievements",
                                "shop", "death", "victory", "level_up",
                                "wish_choice", "identify_choice",
                                "drop_choice", "use_choice", "throw_choice",
                                "spell_choice", "ability_choice",
                                "race_pick", "class_pick", "name_input",
                                "npc_dialog", "wand_target", "cast_target",
                                "throw_potion", "throw_bomb", "shop_buy",
                                "shop_sell", "remove_choice", "look",
                                "boss_intro"):
            render_overlay(surf, state)
    pygame.display.flip()


def render_menu(surf, state):
    surf.fill((6, 6, 12))
    title = "ETERNAL DEPTHS"
    draw_text(surf, title, (SCREEN_W // 2 - 200, 80), (240, 220, 90),
              size=64, bold=True)
    draw_text(surf, "A roguelike of immense depth",
              (SCREEN_W // 2 - 180, 160), (200, 200, 220), size=18)
    if state.menu_state == "main":
        opts = ["[N]ew Game", "[L]oad Game", "[H]igh Scores",
                "[A]chievements", "[O]ptions", "[Q]uit"]
        y = 260
        for o in opts:
            draw_text(surf, o, (SCREEN_W // 2 - 80, y), C.UI_FG, size=24)
            y += 36
    elif state.menu_state == "scores":
        from .high_score import load_scores
        scores = load_scores()
        draw_text(surf, "HIGH SCORES", (SCREEN_W // 2 - 100, 220),
                  (240, 220, 90), size=28, bold=True)
        y = 260
        for s in scores[:25]:
            line = (f"{s['score']:>6} - {s['name']} the {s['race']} "
                    f"{s['class']} (Lv {s['level']}, D{s['depth']}) "
                    f"[{s['cause']}]")
            draw_text(surf, line, (160, y), C.UI_FG, size=14)
            y += 18
        draw_text(surf, "[Esc] back", (SCREEN_W // 2 - 60, SCREEN_H - 50),
                  C.GREY, size=18)
    elif state.menu_state == "achievements":
        from .achievements import ACHIEVEMENTS
        draw_text(surf, "ACHIEVEMENTS", (SCREEN_W // 2 - 100, 200),
                  (240, 220, 90), size=24, bold=True)
        y = 240
        ach = state.player.achievements if state.player else set()
        for k, (name, desc) in ACHIEVEMENTS.items():
            color = (240, 220, 90) if k in ach else C.GREY
            mark = "[X]" if k in ach else "[ ]"
            draw_text(surf, f"{mark} {name}: {desc}", (160, y), color, size=14)
            y += 18
        draw_text(surf, "[Esc] back", (SCREEN_W // 2 - 60, SCREEN_H - 40),
                  C.GREY, size=18)
    elif state.menu_state == "options":
        draw_text(surf, "OPTIONS", (SCREEN_W // 2 - 60, 240),
                  (240, 220, 90), size=24, bold=True)
        draw_text(surf, "(no toggleable settings yet)",
                  (SCREEN_W // 2 - 140, 280), C.GREY)
        draw_text(surf, "[Esc] back", (SCREEN_W // 2 - 60, SCREEN_H - 80),
                  C.GREY, size=18)
    elif state.menu_state == "char_create":
        render_char_create(surf, state)


def render_char_create(surf, state):
    from .races import RACES, race_keys
    from .classes import CLASSES, class_keys
    draw_text(surf, "Create Hero", (80, 30), C.UI_HIGHLIGHT, size=32, bold=True)
    draw_text(surf, f"Name: {state.cc_name or '_'}",
              (80, 80), C.UI_FG, size=22)
    draw_text(surf, "Race:", (80, 130), C.UI_HIGHLIGHT, size=18, bold=True)
    rks = race_keys()
    for i, k in enumerate(rks):
        sel = state.cc_race_idx == i
        col = (240, 220, 90) if sel else C.UI_FG
        draw_text(surf, f"  {chr(ord('a') + i)}) {k}", (80, 154 + i * 18),
                  col, size=14)
    if state.cc_race_idx is not None:
        rk = rks[state.cc_race_idx]
        rd = RACES[rk]
        draw_text(surf, f"Race: {rk}", (380, 130), C.UI_HIGHLIGHT,
                  size=18, bold=True)
        draw_text(surf, rd["desc"], (380, 154), C.UI_FG, size=14)
        s = rd["stats"]
        draw_text(surf, (f"STR{s.STR:+d} DEX{s.DEX:+d} CON{s.CON:+d} "
                         f"INT{s.INT:+d} WIS{s.WIS:+d} CHA{s.CHA:+d} "
                         f"LUCK{s.LUCK:+d}"),
                  (380, 174), (200, 220, 200), size=14)

    draw_text(surf, "Class:", (80, 380), C.UI_HIGHLIGHT, size=18, bold=True)
    cks = class_keys()
    for i, k in enumerate(cks):
        sel = state.cc_class_idx == i
        col = (240, 220, 90) if sel else C.UI_FG
        draw_text(surf, f"  {chr(ord('a') + i)}) {k}", (80, 404 + i * 18),
                  col, size=14)
    if state.cc_class_idx is not None:
        ck = cks[state.cc_class_idx]
        cd = CLASSES[ck]
        draw_text(surf, f"Class: {ck}", (380, 380), C.UI_HIGHLIGHT,
                  size=18, bold=True)
        draw_text(surf, cd["desc"], (380, 404), C.UI_FG, size=14)
        draw_text(surf, f"Primary: {cd['primary']}",
                  (380, 424), (200, 220, 200), size=14)
        draw_text(surf, "Skills: " + ", ".join(cd["starting_skills"]),
                  (380, 444), (200, 200, 240), size=12)
        if cd["starting_spells"]:
            draw_text(surf, "Spells: " + ", ".join(cd["starting_spells"]),
                      (380, 460), (200, 200, 240), size=12)
        draw_text(surf, "Items: " + ", ".join(cd["starting_items"]),
                  (380, 478), (200, 200, 240), size=12)

    draw_text(surf, "Type name then Enter; pick race & class with letters; Enter to start",
              (80, SCREEN_H - 60), C.GREY, size=14)
    draw_text(surf, "[Esc] cancel    [Enter] begin",
              (80, SCREEN_H - 36), C.GREY, size=14)


def render_overlay(surf, state):
    ms = state.menu_state
    p = state.player
    if ms == "inventory":
        lines = ["[Esc] close  [letter] use/equip  [d] drop"]
        if not p.inventory.items:
            lines.append("(empty)")
        for i, it in enumerate(p.inventory.items):
            ch = chr(ord('a') + i)
            equipped = ""
            for slot, ei in p.equipment.all_items():
                if ei is it:
                    equipped = f" ({slot})"
            lines.append((f"{ch}) {it.display_name}{equipped}",
                          it.d.color))
        render_overlay_text(surf, lines, "Inventory")
    elif ms == "equipment":
        lines = []
        for slot in ["weapon", "shield", "head", "body", "hands",
                     "legs", "feet", "neck", "ring1", "ring2", "cloak"]:
            it = p.equipment.get(slot)
            if it:
                lines.append((f"{slot:8s}: {it.display_name}", it.d.color))
            else:
                lines.append((f"{slot:8s}: -", C.GREY))
        render_overlay_text(surf, lines, "Equipment")
    elif ms == "spells":
        from .spells import SPELLS
        lines = ["[letter] cast  [Esc] close"]
        for i, sk in enumerate(p.spells):
            sp = SPELLS.get(sk)
            if not sp:
                continue
            ch = chr(ord('a') + i)
            lines.append((f"{ch}) {sp.name} (MP {sp.mp}, {sp.school})",
                          (180, 180, 240)))
        render_overlay_text(surf, lines, "Spells")
    elif ms == "abilities":
        lines = ["[letter] use  [Esc] close"]
        for i, ab in enumerate(p.abilities):
            ch = chr(ord('a') + i)
            cd = p.cooldowns.get(ab, 0)
            lines.append((f"{ch}) {ab} {('(cd ' + str(cd) + ')') if cd else ''}",
                          (200, 200, 240)))
        render_overlay_text(surf, lines, "Abilities")
    elif ms == "skills":
        from .skills import SKILLS
        lines = []
        for k, lvl in p.skills.all().items():
            desc = SKILLS.get(k, k)
            lines.append((f"{lvl:3d}  {desc}", C.UI_FG))
        render_overlay_text(surf, lines, "Skills")
    elif ms == "char_sheet":
        s = p.effective_stats()
        r = p.effective_resists()
        lines = [
            f"{p.name} the {p.race} {p.cls}",
            f"Level {p.level}  XP {p.xp}",
            f"HP {p.hp}/{p.max_hp}    MP {p.mp}/{p.max_mp}    AC {p.total_ac()}",
            f"STR {s.STR}  DEX {s.DEX}  CON {s.CON}  INT {s.INT}  WIS {s.WIS}  CHA {s.CHA}  LUCK {s.LUCK}",
            (f"Resists  fire {r.fire}  cold {r.cold}  light {r.lightning} "
             f"poi {r.poison}  holy {r.holy}  dark {r.dark}  arc {r.arcane}  "
             f"acid {r.acid}"),
            f"Depth {p.depth}/30  Deepest {p.deepest}",
            f"Gold {p.gold}  Hunger {p.hunger}",
            f"Kills {p.kills}  Turns {p.turns}",
        ]
        if p.god:
            lines.append(f"God {p.god}  Piety {p.piety}")
        render_overlay_text(surf, lines, "Character")
    elif ms == "quests":
        lines = []
        for q in state.quests.active:
            prog = ""
            if q["type"] == "kill":
                prog = f" ({q.get('progress', 0)}/{q['count']})"
            done = " [DONE]" if state.quests.is_complete(q) else ""
            lines.append(f"- {q['desc']}{prog}{done}")
        for q in state.quests.completed:
            lines.append(f"[completed] {q['desc']}")
        if not lines:
            lines.append("No quests.")
        render_overlay_text(surf, lines, "Quests")
    elif ms == "achievements":
        from .achievements import ACHIEVEMENTS
        lines = []
        for k, (n, d) in ACHIEVEMENTS.items():
            mark = "[X]" if k in p.achievements else "[ ]"
            lines.append((f"{mark} {n} - {d}",
                          (240, 220, 90) if k in p.achievements else C.GREY))
        render_overlay_text(surf, lines, "Achievements")
    elif ms == "help":
        lines = HELP_TEXT
        render_overlay_text(surf, lines, "Controls")
    elif ms == "death":
        lines = [
            f"You died: {state.death_cause}",
            f"Reached depth {p.deepest}",
            f"Score {p.score}",
            f"{p.kills} kills, {p.turns} turns",
            "",
            "[N] new run  [Esc] main menu",
        ]
        render_overlay_text(surf, lines, "GAME OVER")
    elif ms == "victory":
        lines = [
            "You have defeated the Eternal One!",
            "The depths are still.",
            f"Score {p.score}",
            "[N] new run  [Esc] main menu",
        ]
        render_overlay_text(surf, lines, "VICTORY")
    elif ms == "boss_intro":
        bn = state.boss_intro_name or "BOSS"
        render_overlay_text(surf, [f"You meet {bn}!", "Prepare for battle.",
                                   "[Esc] continue"], "DANGER")
    elif ms == "level_up":
        s = p.stats
        lines = [
            f"Level {p.level}!",
            "",
            "Choose a stat to permanently increase:",
            f"  s) STR ({s.STR})",
            f"  d) DEX ({s.DEX})",
            f"  c) CON ({s.CON})",
            f"  i) INT ({s.INT})",
            f"  w) WIS ({s.WIS})",
            f"  h) CHA ({s.CHA})",
            f"  l) LUCK ({s.LUCK})",
        ]
        render_overlay_text(surf, lines, "Level Up!")
    elif ms == "identify_choice":
        lines = ["Choose item to identify  [Esc] cancel"]
        for i, it in enumerate(p.inventory.items):
            ch = chr(ord('a') + i)
            lines.append(f"{ch}) {it.display_name}")
        render_overlay_text(surf, lines, "Identify")
    elif ms == "drop_choice":
        lines = ["Choose item to drop  [Esc] cancel"]
        for i, it in enumerate(p.inventory.items):
            ch = chr(ord('a') + i)
            lines.append(f"{ch}) {it.display_name}")
        render_overlay_text(surf, lines, "Drop")
    elif ms == "remove_choice":
        lines = ["Choose slot to unequip  [Esc] cancel"]
        for i, (slot, it) in enumerate(p.equipment.all_items()):
            ch = chr(ord('a') + i)
            lines.append(f"{ch}) {slot} = {it.display_name}")
        render_overlay_text(surf, lines, "Unequip")
    elif ms == "shop":
        sk = state.current_npc
        lines = [f"Gold: {p.gold}",
                 "[b] buy  [s] sell  [Esc] leave",
                 ""]
        if state.shop_mode == "buy":
            lines.append("BUY:")
            for i, it in enumerate(sk.inventory_items):
                ch = chr(ord('a') + i)
                lines.append((f"{ch}) {it.display_name} - {it.total_value} gp",
                              it.d.color))
        elif state.shop_mode == "sell":
            lines.append("SELL:")
            for i, it in enumerate(p.inventory.items):
                ch = chr(ord('a') + i)
                lines.append((f"{ch}) {it.display_name} - {it.total_value//2} gp",
                              it.d.color))
        render_overlay_text(surf, lines, "Shop")
    elif ms == "npc_dialog":
        npc = state.current_npc
        lines = [npc.dialogue]
        if npc.npc_type == "shopkeeper":
            lines.append("")
            lines.append("[t] trade  [Esc] leave")
        elif npc.npc_type == "quest":
            done = state.quests.is_complete(npc.quest) if npc.quest in state.quests.active else False
            if npc.quest in state.quests.active and done:
                lines.append("[c] claim reward")
            elif npc.quest not in state.quests.active:
                lines.append("[a] accept quest")
            lines.append("[Esc] leave")
        render_overlay_text(surf, lines, npc.name)
    elif ms in ("cast_target", "wand_target", "throw_potion", "throw_bomb"):
        lines = ["Move cursor with movement keys; Enter to confirm",
                 "Tab cycles enemies", "[Esc] cancel"]
        render_overlay_text(surf, lines, "Choose Target")
    elif ms == "look":
        tx, ty = state.target_pos or p.at()
        gm = state.gm
        lines = []
        if gm.in_bounds(tx, ty) and gm.explored[tx][ty]:
            t = gm.tile(tx, ty)
            lines.append(f"Tile: {t.name}")
            e = gm.entity_at(tx, ty)
            if e and gm.visible[tx][ty]:
                hp = f" {e.hp}/{e.max_hp}HP" if hasattr(e, "max_hp") else ""
                lines.append(f"Creature: {e.name}{hp}")
            for it in gm.items_at(tx, ty):
                lines.append(f"Item: {it.display_name}")
        lines.append("[Esc] back")
        render_overlay_text(surf, lines, "Look")
    elif ms == "wish_choice":
        wish_pool = ["potion_full_healing", "potion_gain_level",
                     "ring_protection", "am_lifesaving",
                     "scroll_enchant_weapon", "scroll_enchant_armor",
                     "wand_fire", "vorpal_blade", "holy_avenger",
                     "excalibur", "dragonscale", "boots_of_speed"]
        lines = ["Wish granted! Choose:"]
        for i, k in enumerate(wish_pool[:12]):
            from .items import ITEMS
            it = ITEMS[k]
            ch = chr(ord('a') + i)
            lines.append((f"{ch}) {it.name}", it.color))
        render_overlay_text(surf, lines, "Wish")


HELP_TEXT = [
    "Movement: arrow keys, hjkl (vi keys) or numpad",
    "Diagonals: yubn (vi keys) or numpad 7,9,1,3",
    ".: wait one turn   5: wait/search",
    "g: pickup item from floor",
    "i: inventory   e: equipment   r: remove/unequip",
    "z: cast spell   Z: abilities  s: skills  C: char sheet",
    "q: quaff potion (or fountain on tile)",
    "R: read scroll/book",
    "f: fire ranged weapon  t: throw item",
    "x: examine/look   #: pray at altar",
    "o: open chest    >: stairs down  <: stairs up",
    "S: save game    Q: quit  ?: this help",
    ".: rest/skip turn  5: rest until full",
    "p: pets list  Q: quests  A: achievements",
    "Mouse: click to look",
    "[Esc] close menus / cancel targeting",
]

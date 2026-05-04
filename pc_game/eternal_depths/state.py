"""Central game state."""
from __future__ import annotations
import random
from .messages import MessageLog
from .rng import GameRNG
from .quests import QuestLog
from . import procgen, fov, monsters, items as items_mod
from .entity import Player, Monster
from .npc import NPC, make_shopkeeper, make_quest_npc
from .constants import FOV_RADIUS, MAX_DUNGEON_LEVEL


class GameState:
    def __init__(self):
        self.rng = GameRNG()
        self.log = MessageLog()
        self.player: Player | None = None
        self.gm = None
        self.levels: dict[int, dict] = {}  # cache map snapshots
        self.turn = 0
        self.game_over = False
        self.victory = False
        self.death_cause = "unknown"
        self.pending_action = None
        self.particles = []  # list of (x, y, r, dtype, ttl)
        self.light_radius = FOV_RADIUS
        self.quests = QuestLog()
        self.target_pos = None  # (x, y) used by targeting prompts
        self.menu_state = "main"  # main, in_game, char_create, etc.
        self.identified_kinds: set[str] = set()
        self.god = None

    # --- LEVEL MANAGEMENT ---
    def descend_level(self):
        depth = self.player.depth + 1
        self.snapshot_current()
        self._enter_level(depth, from_below=False)

    def ascend_level(self):
        depth = max(1, self.player.depth - 1)
        self.snapshot_current()
        self._enter_level(depth, from_below=True)

    def _enter_level(self, depth, from_below=False):
        self.player.depth = depth
        self.player.deepest = max(self.player.deepest, depth)
        if depth in self.levels:
            self.gm = self._restore_level(depth)
        else:
            self.gm = procgen.generate(depth, self.rng)
            self._populate_level(self.gm)
        # Place player
        if from_below and self.gm.down_pos:
            self.player.x, self.player.y = self.gm.down_pos
        elif self.gm.up_pos:
            self.player.x, self.player.y = self.gm.up_pos
        # Ensure player is in entity list
        if self.player not in self.gm.entities:
            self.gm.entities.insert(0, self.player)
        from .achievements import on_depth
        on_depth(self)
        self.quests.progress_descend(depth)
        self.log.add(f"-- Depth {depth} --", (240, 200, 80))
        fov.compute_fov(self.gm, self.player.x, self.player.y, self.light_radius)

    def _populate_level(self, gm):
        rng = self.rng
        depth = gm.level
        # Boss
        from .monsters import BOSS_BY_LEVEL, MONSTERS
        if depth in BOSS_BY_LEVEL:
            key = BOSS_BY_LEVEL[depth]
            mdef = MONSTERS[key]
            # Place near down stairs
            x, y = gm.down_pos or (gm.w // 2, gm.h // 2)
            for _ in range(40):
                bx = x + rng.randint(-3, 3)
                by = y + rng.randint(-3, 3)
                if gm.walkable(bx, by) and not gm.blocking_entity_at(bx, by):
                    boss = Monster(bx, by, mdef)
                    gm.entities.append(boss)
                    break
        # Regular monsters
        n = 6 + depth + rng.randint(0, 6)
        attempts = 0
        while n > 0 and attempts < 600:
            attempts += 1
            x = rng.randint(1, gm.w - 2)
            y = rng.randint(1, gm.h - 2)
            if not gm.walkable(x, y) or gm.blocking_entity_at(x, y):
                continue
            keys = monsters.by_dlvl(depth, 1, rng)
            mdef = monsters.MONSTERS[keys[0]]
            mon = Monster(x, y, mdef)
            gm.entities.append(mon)
            n -= 1
        # Items scattered
        ni = 4 + depth // 2 + rng.randint(0, 4)
        attempts = 0
        keys = list(items_mod.ITEMS.keys())
        while ni > 0 and attempts < 600:
            attempts += 1
            x = rng.randint(1, gm.w - 2)
            y = rng.randint(1, gm.h - 2)
            if not gm.walkable(x, y):
                continue
            if gm.tile(x, y).stairs_up or gm.tile(x, y).stairs_down:
                continue
            kind = _weighted_item_kind(rng, depth)
            it = items_mod.init_charges(items_mod.Item(kind))
            # Random enchant on weapons/armor
            if it.d.category in ("weapon", "armor"):
                e = 0
                if rng.chance(20):
                    e = rng.choice([1, 1, 2, 3])
                if rng.chance(8):
                    e = -rng.choice([1, 1, 2])
                    it.cursed = True
                it.enchant = e
            gm.add_item(x, y, it)
            ni -= 1
        # NPCs: shopkeeper if shop room, occasional quest-giver
        if gm.shop_origin:
            sx, sy = gm.shop_origin
            shopkeeper = make_shopkeeper(rng, sx, sy, depth)
            gm.entities.append(shopkeeper)
        if depth >= 2 and rng.chance(35):
            for _ in range(40):
                x = rng.randint(2, gm.w - 3)
                y = rng.randint(2, gm.h - 3)
                if gm.walkable(x, y) and not gm.blocking_entity_at(x, y):
                    qn = make_quest_npc(rng, x, y, depth)
                    gm.entities.append(qn)
                    break

    def snapshot_current(self):
        if not self.gm:
            return
        gm = self.gm
        depth = gm.level
        # Strip player
        ents = [e for e in gm.entities if e is not self.player]
        snap = {
            "map": gm.to_dict(),
            "monsters": [],
            "npcs": [],
            "items": [(x, y, it.to_dict()) for x, y, it in gm.items],
        }
        for e in ents:
            if isinstance(e, NPC):
                snap["npcs"].append(e.to_dict())
            elif isinstance(e, Monster):
                snap["monsters"].append(e.to_dict())
        self.levels[depth] = snap

    def _restore_level(self, depth):
        from . import gamemap
        snap = self.levels[depth]
        gm = gamemap.GameMap.from_dict(snap["map"])
        for md in snap["monsters"]:
            mon = Monster.from_dict(md, monsters.MONSTERS)
            gm.entities.append(mon)
        for nd in snap["npcs"]:
            n = NPC.from_dict(nd)
            gm.entities.append(n)
        for x, y, itd in snap["items"]:
            gm.items.append((x, y, items_mod.Item.from_dict(itd)))
        return gm

    def player_swing_walls(self):
        return self.player.equipment.weapon and self.player.equipment.weapon.def_key == "pickaxe"

    def to_dict(self):
        self.snapshot_current()
        return {
            "rng_seed": self.rng.seed,
            "log": self.log.to_dict(),
            "player": self.player.to_dict(),
            "depth": self.player.depth,
            "turn": self.turn,
            "levels": {str(k): v for k, v in self.levels.items()},
            "quests": self.quests.to_dict(),
            "identified_kinds": list(self.identified_kinds),
            "god": self.god,
        }

    @classmethod
    def from_dict(cls, d):
        gs = cls()
        gs.rng = GameRNG(d.get("rng_seed"))
        gs.log = MessageLog.from_dict(d["log"])
        gs.player = Player.from_dict(d["player"])
        gs.turn = d.get("turn", 0)
        gs.levels = {int(k): v for k, v in d["levels"].items()}
        gs.quests = QuestLog.from_dict(d.get("quests", {}))
        gs.identified_kinds = set(d.get("identified_kinds", []))
        gs.god = d.get("god")
        gs._enter_level(d["depth"])
        return gs


def _weighted_item_kind(rng, depth):
    pool = []
    from .items import ITEMS
    for k, it in ITEMS.items():
        if k == "gold_pile":
            continue
        # weight by category & rarity
        cat = it.category
        w = 5
        if cat == "potion":
            w = 8
        elif cat == "scroll":
            w = 6
        elif cat == "food":
            w = 5
        elif cat == "weapon":
            w = 3
        elif cat == "armor":
            w = 3
        elif cat == "wand":
            w = 2
        elif cat == "ring":
            w = 1
        elif cat == "amulet":
            w = 1
        elif cat == "gem":
            w = 1
        # Rarity tax
        if it.rarity == "rare":
            w = max(1, w - 2)
        if it.rarity == "epic":
            w = 1 if depth >= 10 else 0
        if it.rarity == "legendary":
            w = 1 if depth >= 15 else 0
        if it.rarity == "artifact":
            w = 1 if depth >= 20 else 0
        if w <= 0:
            continue
        pool.extend([k] * w)
    return rng.choice(pool)

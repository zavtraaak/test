"""NPCs (shopkeepers, quest-givers)."""
from __future__ import annotations
import random
from .entity import Entity
from .items import Item, ITEMS, init_charges


class NPC(Entity):
    def __init__(self, x, y, npc_type, name):
        super().__init__(x, y, "@", (240, 220, 90), name)
        self.faction = "npc"
        self.npc_type = npc_type
        self.dialogue = ""
        self.inventory_items: list[Item] = []
        self.quest = None  # type: dict | None

    def to_dict(self):
        return {
            "x": self.x, "y": self.y, "npc_type": self.npc_type,
            "name": self.name, "color": list(self.color),
            "inventory": [i.to_dict() for i in self.inventory_items],
            "quest": self.quest,
        }

    @classmethod
    def from_dict(cls, d):
        n = cls(d["x"], d["y"], d["npc_type"], d["name"])
        n.color = tuple(d["color"])
        n.inventory_items = [Item.from_dict(x) for x in d.get("inventory", [])]
        n.quest = d.get("quest")
        return n


SHOPKEEPER_NAMES = ["Boris", "Tilda", "Marcus", "Elinor", "Grogg",
                    "Sylvia", "Brem", "Halina", "Dunwin", "Carbry"]
QUEST_NAMES = ["Old Sage", "Ranger Wynn", "Captain Velt", "Scholar Hesh",
               "Knight Aelric", "Witch Halia", "Hermit Folmar"]


def make_shopkeeper(rng, x, y, depth):
    name = rng.choice(SHOPKEEPER_NAMES)
    sk = NPC(x, y, "shopkeeper", f"{name} the Trader")
    sk.color = (240, 220, 90)
    sk.dialogue = "Welcome to my shop!"
    pool = [k for k, it in ITEMS.items()
            if it.category in ("potion", "scroll", "wand", "weapon", "armor",
                               "ring", "amulet", "food", "tool")]
    rng.shuffle(pool)
    n = rng.randint(8, 14)
    for k in pool[:n]:
        it = init_charges(Item(k, identified=True))
        sk.inventory_items.append(it)
    return sk


def make_quest_npc(rng, x, y, depth):
    name = rng.choice(QUEST_NAMES)
    n = NPC(x, y, "quest", name)
    n.color = (130, 200, 240)
    quest_pool = [
        {"type": "kill", "target": "rat", "count": 5,
         "reward_gold": 50, "reward_xp": 30,
         "desc": "Slay 5 giant rats."},
        {"type": "kill", "target": "goblin", "count": 8,
         "reward_gold": 80, "reward_xp": 60,
         "desc": "Slay 8 goblins."},
        {"type": "descend", "depth": min(30, depth + 5),
         "reward_gold": 200, "reward_xp": 200,
         "desc": f"Reach dungeon level {min(30, depth + 5)}."},
        {"type": "fetch", "item": "gem_ruby",
         "reward_gold": 200, "reward_xp": 100,
         "desc": "Bring me a Ruby."},
        {"type": "fetch", "item": "scroll_identify",
         "reward_gold": 100, "reward_xp": 60,
         "desc": "Bring me a Scroll of Identify."},
    ]
    n.quest = rng.choice(quest_pool)
    n.dialogue = n.quest["desc"]
    return n

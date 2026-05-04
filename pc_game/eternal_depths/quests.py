"""Quest tracking."""
from __future__ import annotations


class QuestLog:
    def __init__(self):
        self.active: list[dict] = []
        self.completed: list[dict] = []

    def accept(self, quest):
        if quest in self.active:
            return False
        self.active.append(dict(quest))
        return True

    def progress_kill(self, key):
        for q in self.active:
            if q["type"] == "kill" and q.get("target") == key:
                q["progress"] = q.get("progress", 0) + 1

    def progress_descend(self, depth):
        for q in self.active:
            if q["type"] == "descend" and depth >= q["depth"]:
                q["completed"] = True

    def progress_fetch(self, items_keys):
        for q in self.active:
            if q["type"] == "fetch" and q["item"] in items_keys:
                q["completed"] = True

    def is_complete(self, q):
        if q["type"] == "kill":
            return q.get("progress", 0) >= q["count"]
        return q.get("completed", False)

    def to_dict(self):
        return {"active": list(self.active), "completed": list(self.completed)}

    @classmethod
    def from_dict(cls, d):
        ql = cls()
        ql.active = list(d.get("active", []))
        ql.completed = list(d.get("completed", []))
        return ql

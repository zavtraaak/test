"""Message log."""
from __future__ import annotations
from . import colors


class MessageLog:
    def __init__(self, capacity=200):
        self.capacity = capacity
        self.entries: list[tuple[str, tuple[int, int, int]]] = []

    def add(self, text: str, color=colors.WHITE):
        if self.entries and self.entries[-1][0].startswith(text + " (x"):
            # Stack repeats
            prev, c = self.entries[-1]
            n = int(prev.split("(x")[1].rstrip(")")) + 1
            self.entries[-1] = (f"{text} (x{n})", c)
        elif self.entries and self.entries[-1][0] == text:
            self.entries[-1] = (f"{text} (x2)", color)
        else:
            self.entries.append((text, color))
        if len(self.entries) > self.capacity:
            self.entries.pop(0)

    def tail(self, n: int):
        return self.entries[-n:]

    def to_dict(self):
        return [(t, list(c)) for t, c in self.entries]

    @classmethod
    def from_dict(cls, data):
        ml = cls()
        for t, c in data:
            ml.entries.append((t, tuple(c)))
        return ml

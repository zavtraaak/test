"""Generic utilities."""
from __future__ import annotations
import math
import random


def chebyshev(a, b):
    return max(abs(a[0] - b[0]), abs(a[1] - b[1]))


def manhattan(a, b):
    return abs(a[0] - b[0]) + abs(a[1] - b[1])


def euclid(a, b):
    return math.hypot(a[0] - b[0], a[1] - b[1])


def line(x0, y0, x1, y1):
    """Bresenham line."""
    pts = []
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    while True:
        pts.append((x0, y0))
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy
    return pts


def clamp(v, lo, hi):
    return max(lo, min(hi, v))


def weighted_choice(items, rng=None):
    rng = rng or random
    total = sum(w for _, w in items)
    r = rng.uniform(0, total)
    upto = 0
    for it, w in items:
        if upto + w >= r:
            return it
        upto += w
    return items[-1][0]


def roll(dice_str, rng=None):
    """Parse and roll dice expression like '2d6+3' or '1d8'."""
    rng = rng or random
    bonus = 0
    s = dice_str.replace(" ", "")
    if "+" in s:
        s, b = s.split("+", 1)
        bonus = int(b)
    elif "-" in s:
        s, b = s.split("-", 1)
        bonus = -int(b)
    n, d = s.split("d")
    n = int(n) if n else 1
    d = int(d)
    total = sum(rng.randint(1, d) for _ in range(n))
    return total + bonus


def percent_chance(pct, rng=None):
    rng = rng or random
    return rng.random() * 100 < pct


def signed(n):
    return f"+{n}" if n >= 0 else f"{n}"

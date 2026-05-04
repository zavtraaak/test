"""Field of view using shadowcasting (recursive)."""
from __future__ import annotations
from .constants import FOV_RADIUS

_OCTANTS = [
    (1, 0, 0, 1), (0, 1, 1, 0), (0, -1, 1, 0), (-1, 0, 0, 1),
    (-1, 0, 0, -1), (0, -1, -1, 0), (0, 1, -1, 0), (1, 0, 0, -1),
]


def compute_fov(gm, ox, oy, radius=FOV_RADIUS):
    # Clear visible
    for x in range(gm.w):
        for y in range(gm.h):
            gm.visible[x][y] = False
    if gm.in_bounds(ox, oy):
        gm.visible[ox][oy] = True
        gm.explored[ox][oy] = True
    for octant in range(8):
        _cast_light(gm, ox, oy, 1, 1.0, 0.0, radius,
                    _OCTANTS[octant][0], _OCTANTS[octant][1],
                    _OCTANTS[octant][2], _OCTANTS[octant][3])


def _cast_light(gm, cx, cy, row, start, end, radius, xx, xy, yx, yy):
    if start < end:
        return
    new_start = 0.0
    blocked = False
    for j in range(row, radius + 1):
        if blocked:
            break
        dx, dy = -j - 1, -j
        while dx <= 0:
            dx += 1
            X = cx + dx * xx + dy * xy
            Y = cy + dx * yx + dy * yy
            l_slope = (dx - 0.5) / (dy + 0.5)
            r_slope = (dx + 0.5) / (dy - 0.5)
            if start < r_slope:
                continue
            elif end > l_slope:
                break
            else:
                if dx * dx + dy * dy <= radius * radius:
                    if gm.in_bounds(X, Y):
                        gm.visible[X][Y] = True
                        gm.explored[X][Y] = True
                if blocked:
                    if gm.in_bounds(X, Y) and not gm.transparent(X, Y):
                        new_start = r_slope
                        continue
                    else:
                        blocked = False
                        start = new_start
                else:
                    if gm.in_bounds(X, Y) and not gm.transparent(X, Y) and j < radius:
                        blocked = True
                        _cast_light(gm, cx, cy, j + 1, start, l_slope, radius,
                                    xx, xy, yx, yy)
                        new_start = r_slope

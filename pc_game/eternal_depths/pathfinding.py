"""A* pathfinding."""
from __future__ import annotations
import heapq


def astar(gm, start, goal, blockers=None):
    blockers = blockers or set()
    if start == goal:
        return [start]
    open_heap = []
    heapq.heappush(open_heap, (0, start))
    came_from = {start: None}
    cost_so_far = {start: 0}
    while open_heap:
        _, cur = heapq.heappop(open_heap)
        if cur == goal:
            break
        cx, cy = cur
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if dx == 0 and dy == 0:
                    continue
                nx, ny = cx + dx, cy + dy
                if not gm.in_bounds(nx, ny):
                    continue
                if not gm.walkable(nx, ny):
                    continue
                if (nx, ny) in blockers and (nx, ny) != goal:
                    continue
                step = 14 if dx and dy else 10
                ncost = cost_so_far[cur] + step
                if (nx, ny) not in cost_so_far or ncost < cost_so_far[(nx, ny)]:
                    cost_so_far[(nx, ny)] = ncost
                    h = max(abs(nx - goal[0]), abs(ny - goal[1])) * 10
                    heapq.heappush(open_heap, (ncost + h, (nx, ny)))
                    came_from[(nx, ny)] = cur
    if goal not in came_from:
        return None
    path = []
    cur = goal
    while cur is not None:
        path.append(cur)
        cur = came_from[cur]
    path.reverse()
    return path

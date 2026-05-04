"""High score table."""
from __future__ import annotations
import json
import os
from .constants import SAVE_DIR, SCORES_FILE


def scores_path():
    here = os.path.dirname(os.path.abspath(__file__))
    base = os.path.dirname(here)
    save_dir = os.path.join(base, SAVE_DIR)
    os.makedirs(save_dir, exist_ok=True)
    return os.path.join(save_dir, SCORES_FILE)


def load_scores():
    path = scores_path()
    if not os.path.exists(path):
        return []
    with open(path) as f:
        try:
            return json.load(f)
        except json.JSONDecodeError:
            return []


def save_scores(scores):
    with open(scores_path(), "w") as f:
        json.dump(scores, f)


def add_score(name, race, cls, score, depth, level, cause):
    scores = load_scores()
    scores.append({
        "name": name, "race": race, "class": cls,
        "score": score, "depth": depth, "level": level, "cause": cause,
    })
    scores.sort(key=lambda s: -s["score"])
    scores = scores[:30]
    save_scores(scores)

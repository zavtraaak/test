"""Save and load."""
from __future__ import annotations
import json
import os
from .constants import SAVE_DIR, SAVE_FILE


def save_path():
    here = os.path.dirname(os.path.abspath(__file__))
    base = os.path.dirname(here)
    save_dir = os.path.join(base, SAVE_DIR)
    os.makedirs(save_dir, exist_ok=True)
    return os.path.join(save_dir, SAVE_FILE)


def save_game(state):
    data = state.to_dict()
    with open(save_path(), "w") as f:
        json.dump(data, f)


def load_game():
    path = save_path()
    if not os.path.exists(path):
        return None
    with open(path, "r") as f:
        return json.load(f)


def delete_save():
    path = save_path()
    if os.path.exists(path):
        os.remove(path)

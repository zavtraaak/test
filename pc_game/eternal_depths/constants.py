"""Game constants and configuration."""
from __future__ import annotations

# Display
SCREEN_W = 1280
SCREEN_H = 800
FPS = 60
TILE_SIZE = 18
FONT_SIZE = 18

# Map
MAP_W = 80
MAP_H = 40
VIEW_W = 60
VIEW_H = 36
SIDE_PANEL_W = SCREEN_W - VIEW_W * TILE_SIZE
LOG_LINES = 6

# Gameplay
MAX_DUNGEON_LEVEL = 30
FOV_RADIUS = 8
START_HUNGER = 1000
HUNGER_PER_TURN = 1
STARVATION_DAMAGE = 1
PLAYER_BASE_HP = 30
PLAYER_BASE_MP = 10
XP_BASE = 20
XP_GROWTH = 1.45

# Save
SAVE_DIR = "saves"
SAVE_FILE = "savegame.json"
SCORES_FILE = "highscores.json"
ACHIEVEMENTS_FILE = "achievements.json"
SETTINGS_FILE = "settings.json"

# Misc
GAME_TITLE = "Eternal Depths"
SEED_DEFAULT = None

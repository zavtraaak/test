"""Entrypoint."""
from __future__ import annotations
from .engine import Engine


def main():
    eng = Engine()
    eng.run()


if __name__ == "__main__":
    main()

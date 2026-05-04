"""Centralized RNG."""
import random


class GameRNG:
    def __init__(self, seed=None):
        self.seed = seed
        self._r = random.Random(seed)

    def randint(self, a, b):
        return self._r.randint(a, b)

    def random(self):
        return self._r.random()

    def choice(self, seq):
        return self._r.choice(seq)

    def choices(self, population, weights=None, k=1):
        return self._r.choices(population, weights=weights, k=k)

    def shuffle(self, seq):
        self._r.shuffle(seq)
        return seq

    def uniform(self, a, b):
        return self._r.uniform(a, b)

    def chance(self, pct):
        return self._r.random() * 100 < pct

    def gauss(self, mu, sigma):
        return self._r.gauss(mu, sigma)

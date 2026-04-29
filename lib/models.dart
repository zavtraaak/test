import 'package:flutter/material.dart';

/// A passive coin generator that produces coins per second when owned.
class GeneratorDef {
  final String id;
  final String name;
  final String description;
  final IconData icon;
  final Color color;
  final double baseCost;
  final double baseProduction;
  final double costGrowth;

  const GeneratorDef({
    required this.id,
    required this.name,
    required this.description,
    required this.icon,
    required this.color,
    required this.baseCost,
    required this.baseProduction,
    this.costGrowth = 1.15,
  });

  double costAt(int owned) => baseCost * _pow(costGrowth, owned);

  /// Total cost to buy `count` more after currently owning `owned`.
  double bulkCost(int owned, int count) {
    if (count <= 0) return 0;
    if (costGrowth == 1.0) return baseCost * count;
    final r = costGrowth;
    return baseCost *
        _pow(r, owned) *
        ((_pow(r, count) - 1) / (r - 1));
  }
}

double _pow(double base, int exp) {
  double result = 1;
  double b = base;
  int e = exp;
  while (e > 0) {
    if ((e & 1) == 1) result *= b;
    b *= b;
    e >>= 1;
  }
  return result;
}

/// Categories of upgrades that affect different aspects of the economy.
enum UpgradeKind {
  /// Multiplier applied to per-click coin gain.
  clickMultiplier,

  /// Flat amount added to per-click coin gain.
  clickFlat,

  /// Multiplier applied to a single generator's production.
  generatorMultiplier,

  /// Multiplier applied to all generators' production.
  globalMultiplier,

  /// Adds a percentage of CPS to each click.
  clickFromCps,
}

class UpgradeDef {
  final String id;
  final String name;
  final String description;
  final IconData icon;
  final UpgradeKind kind;
  final double cost;
  final double value;
  final String? generatorId;
  final UpgradeRequirement? requirement;

  const UpgradeDef({
    required this.id,
    required this.name,
    required this.description,
    required this.icon,
    required this.kind,
    required this.cost,
    required this.value,
    this.generatorId,
    this.requirement,
  });
}

/// Optional requirement that must be met before an upgrade is offered for sale.
class UpgradeRequirement {
  final String? generatorId;
  final int? minOwned;
  final int? minTotalClicks;
  final double? minTotalEarned;

  const UpgradeRequirement({
    this.generatorId,
    this.minOwned,
    this.minTotalClicks,
    this.minTotalEarned,
  });
}

class AchievementDef {
  final String id;
  final String name;
  final String description;
  final IconData icon;
  final AchievementCheck check;
  final double bonusPercent;

  const AchievementDef({
    required this.id,
    required this.name,
    required this.description,
    required this.icon,
    required this.check,
    this.bonusPercent = 1.0,
  });
}

/// Predicate over the current game state used to decide if an achievement unlocks.
class AchievementCheck {
  final double? totalEarned;
  final int? totalClicks;
  final String? generatorId;
  final int? generatorOwned;
  final int? totalGenerators;
  final int? prestigeLevel;
  final double? cps;

  const AchievementCheck({
    this.totalEarned,
    this.totalClicks,
    this.generatorId,
    this.generatorOwned,
    this.totalGenerators,
    this.prestigeLevel,
    this.cps,
  });
}

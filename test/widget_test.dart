// Smoke test for Circle Clicker.

import 'package:circle_clicker/data.dart';
import 'package:circle_clicker/models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('GameData has generators, upgrades and achievements', () {
    expect(GameData.generators, isNotEmpty);
    expect(GameData.clickUpgrades, isNotEmpty);
    expect(GameData.generatorUpgrades, isNotEmpty);
    expect(GameData.achievements, isNotEmpty);
  });

  test('Generator bulk cost matches geometric series', () {
    final g = GameData.generators.first;
    // Buying 1 from 0 is just baseCost.
    expect(g.bulkCost(0, 1), closeTo(g.baseCost, 1e-6));
    // Buying 1 from 5 is baseCost * growth^5.
    final expected = g.baseCost *
        [g.costGrowth, g.costGrowth, g.costGrowth, g.costGrowth, g.costGrowth]
            .reduce((a, b) => a * b);
    expect(g.bulkCost(5, 1), closeTo(expected, expected * 1e-9));
  });

  test('Achievement defs reference real generators when applicable', () {
    final ids = GameData.generators.map((g) => g.id).toSet();
    for (final a in GameData.achievements) {
      final gid = a.check.generatorId;
      if (gid != null) {
        expect(ids.contains(gid), isTrue, reason: 'Unknown generator: $gid');
      }
    }
  });

  test('Upgrade kinds are well-formed', () {
    for (final up in GameData.clickUpgrades) {
      expect(
        const [
          UpgradeKind.clickFlat,
          UpgradeKind.clickMultiplier,
          UpgradeKind.clickFromCps,
        ].contains(up.kind),
        isTrue,
      );
    }
    for (final up in GameData.generatorUpgrades) {
      expect(
        const [
          UpgradeKind.generatorMultiplier,
          UpgradeKind.globalMultiplier,
        ].contains(up.kind),
        isTrue,
      );
    }
  });
}

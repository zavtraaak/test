import 'package:flutter/material.dart';

import '../data.dart';
import '../game_state.dart';
import '../models.dart';
import '../utils/format.dart';

class ShopScreen extends StatefulWidget {
  final GameState state;
  const ShopScreen({super.key, required this.state});

  @override
  State<ShopScreen> createState() => _ShopScreenState();
}

class _ShopScreenState extends State<ShopScreen> {
  int _bulk = 1; // 1, 10, 100, or -1 for max

  String get _bulkLabel {
    switch (_bulk) {
      case 1:
        return 'x1';
      case 10:
        return 'x10';
      case 100:
        return 'x100';
      default:
        return 'MAX';
    }
  }

  void _cycleBulk() {
    setState(() {
      _bulk = switch (_bulk) {
        1 => 10,
        10 => 100,
        100 => -1,
        _ => 1,
      };
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        // Hide generators that are still way out of reach to keep the list tidy.
        final visible = <GeneratorDef>[];
        for (final g in GameData.generators) {
          final unlocked = (state.owned[g.id] ?? 0) > 0 ||
              state.totalEarned >= g.baseCost * 0.5;
          if (unlocked) visible.add(g);
          if (!unlocked) {
            // also show the next-locked generator as teaser
            visible.add(g);
            break;
          }
        }
        return Column(
          children: [
            _Header(coins: state.coins, cps: state.cps, onBulkTap: _cycleBulk,
                bulkLabel: _bulkLabel),
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.symmetric(vertical: 8),
                itemCount: visible.length,
                itemBuilder: (context, index) {
                  final g = visible[index];
                  return _GeneratorTile(
                    state: state,
                    g: g,
                    bulk: _bulk,
                  );
                },
              ),
            ),
          ],
        );
      },
    );
  }
}

class _Header extends StatelessWidget {
  final double coins;
  final double cps;
  final VoidCallback onBulkTap;
  final String bulkLabel;
  const _Header({
    required this.coins,
    required this.cps,
    required this.onBulkTap,
    required this.bulkLabel,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 12, 12, 12),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.25),
        border: Border(
          bottom: BorderSide(color: Colors.white.withValues(alpha: 0.05)),
        ),
      ),
      child: Row(
        children: [
          const Icon(Icons.savings, color: Colors.amber),
          const SizedBox(width: 8),
          Text(
            NumberFormatter.format(coins),
            style: const TextStyle(
              color: Colors.amber,
              fontWeight: FontWeight.bold,
              fontSize: 20,
            ),
          ),
          const SizedBox(width: 12),
          Text(
            '+${NumberFormatter.format(cps)}/с',
            style: const TextStyle(color: Colors.white70),
          ),
          const Spacer(),
          OutlinedButton(
            onPressed: onBulkTap,
            style: OutlinedButton.styleFrom(
              foregroundColor: Colors.amber,
              side: const BorderSide(color: Colors.amber),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
            ),
            child: Text(bulkLabel),
          ),
        ],
      ),
    );
  }
}

class _GeneratorTile extends StatelessWidget {
  final GameState state;
  final GeneratorDef g;
  final int bulk;
  const _GeneratorTile({
    required this.state,
    required this.g,
    required this.bulk,
  });

  @override
  Widget build(BuildContext context) {
    final owned = state.owned[g.id] ?? 0;
    final amount = bulk == -1
        ? state.affordableAmount(g, 1000).clamp(1, 1000)
        : bulk;
    final cost = state.generatorCost(g, amount: amount);
    final canBuy = state.coins >= cost && amount > 0;
    final unlocked = owned > 0 || state.totalEarned >= g.baseCost * 0.5;
    final mult = _generatorMultiplier(g.id);
    final perUnitProduction =
        g.baseProduction * mult * state.globalMultiplier;

    return Opacity(
      opacity: unlocked ? 1.0 : 0.6,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        child: Material(
          color: Colors.white.withValues(alpha: 0.04),
          borderRadius: BorderRadius.circular(14),
          child: InkWell(
            borderRadius: BorderRadius.circular(14),
            onTap: unlocked && canBuy
                ? () => state.buyGenerator(g, amount: amount)
                : null,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  Container(
                    width: 56,
                    height: 56,
                    decoration: BoxDecoration(
                      color: g.color.withValues(alpha: 0.18),
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: g.color),
                    ),
                    child: Icon(g.icon, color: g.color, size: 30),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                unlocked ? g.name : '???',
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 16,
                                ),
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 8, vertical: 2),
                              decoration: BoxDecoration(
                                color: Colors.white.withValues(alpha: 0.08),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(
                                'x$owned',
                                style: const TextStyle(
                                  color: Colors.amber,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                          ],
                        ),
                        Text(
                          unlocked ? g.description : 'Откроется при достижении',
                          style: const TextStyle(
                            color: Colors.white70,
                            fontSize: 12,
                          ),
                        ),
                        const SizedBox(height: 4),
                        if (unlocked) ...[
                          Text(
                            '${NumberFormatter.format(perUnitProduction)} монет/сек за каждый',
                            style: TextStyle(
                              color: Colors.greenAccent.shade100,
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        amount > 1 ? 'купить $amount' : 'купить',
                        style: const TextStyle(
                          color: Colors.white60,
                          fontSize: 11,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        NumberFormatter.format(cost),
                        style: TextStyle(
                          color: canBuy
                              ? Colors.amber
                              : Colors.redAccent.shade100,
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  double _generatorMultiplier(String id) {
    double m = 1;
    for (final up in GameData.generatorUpgrades) {
      if (up.kind == UpgradeKind.generatorMultiplier &&
          up.generatorId == id &&
          state.purchasedUpgrades.contains(up.id)) {
        m *= up.value;
      }
    }
    return m;
  }
}

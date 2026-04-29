import 'package:flutter/material.dart';

import '../data.dart';
import '../game_state.dart';
import '../models.dart';
import '../utils/format.dart';

class UpgradesScreen extends StatelessWidget {
  final GameState state;
  const UpgradesScreen({super.key, required this.state});

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        final clickAvail = GameData.clickUpgrades
            .where((u) => state.isUpgradeVisible(u))
            .toList();
        final genAvail = GameData.generatorUpgrades
            .where((u) => state.isUpgradeVisible(u))
            .toList();
        return DefaultTabController(
          length: 2,
          child: Column(
            children: [
              const TabBar(
                indicatorColor: Colors.amber,
                labelColor: Colors.amber,
                unselectedLabelColor: Colors.white60,
                tabs: [
                  Tab(text: 'Клик'),
                  Tab(text: 'Генераторы'),
                ],
              ),
              Expanded(
                child: TabBarView(
                  children: [
                    _UpgradeList(state: state, items: clickAvail),
                    _UpgradeList(state: state, items: genAvail),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _UpgradeList extends StatelessWidget {
  final GameState state;
  final List<UpgradeDef> items;
  const _UpgradeList({required this.state, required this.items});

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(32),
          child: Text(
            'Сейчас нет доступных улучшений.\nПродолжай играть, чтобы открыть новые!',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.white60),
          ),
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.symmetric(vertical: 8),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final up = items[index];
        return _UpgradeTile(state: state, up: up);
      },
    );
  }
}

class _UpgradeTile extends StatelessWidget {
  final GameState state;
  final UpgradeDef up;
  const _UpgradeTile({required this.state, required this.up});

  @override
  Widget build(BuildContext context) {
    final canBuy = state.coins >= up.cost;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
      child: Material(
        color: Colors.white.withValues(alpha: 0.04),
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: canBuy ? () => state.buyUpgrade(up) : null,
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: Colors.amber.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(up.icon, color: Colors.amber.shade300),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        up.name,
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontSize: 15,
                        ),
                      ),
                      Text(
                        up.description,
                        style: const TextStyle(color: Colors.white70, fontSize: 12),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 10),
                Text(
                  NumberFormatter.format(up.cost),
                  style: TextStyle(
                    color: canBuy ? Colors.amber : Colors.redAccent.shade100,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

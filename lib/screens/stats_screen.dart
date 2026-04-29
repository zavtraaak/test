import 'package:flutter/material.dart';

import '../data.dart';
import '../game_state.dart';
import '../utils/format.dart';

class StatsScreen extends StatelessWidget {
  final GameState state;
  const StatsScreen({super.key, required this.state});

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        final sessionDur = state.sessionStart != null
            ? DateTime.now().difference(state.sessionStart!)
            : Duration.zero;
        final perGenerator = <Widget>[];
        for (final g in GameData.generators) {
          final cnt = state.owned[g.id] ?? 0;
          if (cnt == 0) continue;
          perGenerator.add(_row(g.name, '$cnt', icon: g.icon, color: g.color));
        }
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            const Text('Общая статистика',
                style: TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            _row('Монеты сейчас', NumberFormatter.format(state.coins),
                icon: Icons.savings, color: Colors.amber),
            _row('Заработано всего',
                NumberFormatter.format(state.totalEarned),
                icon: Icons.account_balance, color: Colors.amber),
            _row('Кликов', NumberFormatter.formatInt(state.totalClicks),
                icon: Icons.touch_app, color: Colors.lightBlueAccent),
            _row('Монет в секунду', NumberFormatter.format(state.cps),
                icon: Icons.speed, color: Colors.greenAccent),
            _row('Сила клика',
                NumberFormatter.format(
                    state.clickValue + state.cps * state.clickFromCps),
                icon: Icons.front_hand, color: Colors.orangeAccent),
            _row('Глобальный множитель',
                'x${state.globalMultiplier.toStringAsFixed(2)}',
                icon: Icons.trending_up, color: Colors.purpleAccent),
            _row('Звёзды', NumberFormatter.format(state.stardust),
                icon: Icons.auto_awesome, color: Colors.purpleAccent),
            _row('Перерождений', state.prestigeLevel.toString(),
                icon: Icons.refresh, color: Colors.tealAccent),
            _row('Достижений',
                '${state.unlockedAchievements.length} / ${GameData.achievements.length}',
                icon: Icons.military_tech, color: Colors.amber),
            _row('Длина сессии', NumberFormatter.formatTime(sessionDur),
                icon: Icons.timer, color: Colors.white70),
            if (perGenerator.isNotEmpty) ...[
              const SizedBox(height: 16),
              const Text('Генераторы',
                  style: TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              ...perGenerator,
            ],
          ],
        );
      },
    );
  }

  Widget _row(String label, String value,
      {required IconData icon, required Color color}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.04),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Row(
          children: [
            Icon(icon, color: color, size: 20),
            const SizedBox(width: 10),
            Expanded(
              child: Text(label,
                  style: const TextStyle(color: Colors.white70)),
            ),
            Text(value,
                style: const TextStyle(
                    color: Colors.white, fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }
}

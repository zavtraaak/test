import 'package:flutter/material.dart';

import '../data.dart';
import '../game_state.dart';

class AchievementsScreen extends StatelessWidget {
  final GameState state;
  const AchievementsScreen({super.key, required this.state});

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        final unlocked = state.unlockedAchievements.length;
        final total = GameData.achievements.length;
        return Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  const Icon(Icons.military_tech, color: Colors.amber),
                  const SizedBox(width: 8),
                  Text(
                    '$unlocked / $total',
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    '+${state.achievementBonusPercent.toStringAsFixed(1)}% бонус',
                    style: const TextStyle(color: Colors.amber),
                  ),
                ],
              ),
            ),
            Expanded(
              child: GridView.builder(
                padding: const EdgeInsets.all(12),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  childAspectRatio: 1.4,
                  crossAxisSpacing: 10,
                  mainAxisSpacing: 10,
                ),
                itemCount: GameData.achievements.length,
                itemBuilder: (context, index) {
                  final a = GameData.achievements[index];
                  final isUnlocked = state.unlockedAchievements.contains(a.id);
                  return Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: isUnlocked
                          ? Colors.amber.withValues(alpha: 0.12)
                          : Colors.white.withValues(alpha: 0.04),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: isUnlocked
                            ? Colors.amber.withValues(alpha: 0.6)
                            : Colors.white.withValues(alpha: 0.08),
                      ),
                    ),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(
                          a.icon,
                          color: isUnlocked
                              ? Colors.amber
                              : Colors.white24,
                          size: 32,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                a.name,
                                style: TextStyle(
                                  color: isUnlocked
                                      ? Colors.white
                                      : Colors.white54,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                a.description,
                                style: TextStyle(
                                  color: isUnlocked
                                      ? Colors.white70
                                      : Colors.white38,
                                  fontSize: 11,
                                ),
                              ),
                              if (a.bonusPercent > 0)
                                Padding(
                                  padding: const EdgeInsets.only(top: 4),
                                  child: Text(
                                    '+${a.bonusPercent}% к доходу',
                                    style: TextStyle(
                                      color: isUnlocked
                                          ? Colors.greenAccent.shade100
                                          : Colors.white24,
                                      fontSize: 11,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                      ],
                    ),
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

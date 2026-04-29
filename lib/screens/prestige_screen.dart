import 'package:flutter/material.dart';

import '../game_state.dart';
import '../utils/format.dart';

class PrestigeScreen extends StatelessWidget {
  final GameState state;
  const PrestigeScreen({super.key, required this.state});

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        final pending = state.computePendingStardust();
        final canPrestige = pending > 0;
        return Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 8),
              Row(
                children: [
                  const Icon(Icons.auto_awesome,
                      color: Colors.purpleAccent, size: 32),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Звёзд: ${NumberFormatter.format(state.stardust)}',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 22,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          'Перерождений: ${state.prestigeLevel}',
                          style: const TextStyle(color: Colors.white70),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.04),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                      color: Colors.purpleAccent.withValues(alpha: 0.4)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Перерождение',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Сбросит монеты, генераторы и улучшения, '
                      'но даст звёзды. Каждая звезда даёт +2% '
                      'ко всему производству навсегда. Достижения и звёзды сохраняются.',
                      style: TextStyle(color: Colors.white70),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'Сейчас доступно: ${NumberFormatter.format(pending)} ★',
                      style: TextStyle(
                        color: canPrestige
                            ? Colors.purpleAccent
                            : Colors.white38,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      'Текущий бонус от звёзд: +${(2 * state.stardust).toStringAsFixed(0)}%',
                      style: const TextStyle(color: Colors.white60),
                    ),
                    const SizedBox(height: 6),
                    const Text(
                      'Для первого перерождения нужно заработать 1B монет за всё время.',
                      style: TextStyle(color: Colors.white38, fontSize: 12),
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton.icon(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.purpleAccent,
                          foregroundColor: Colors.white,
                          padding:
                              const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        icon: const Icon(Icons.refresh),
                        label: Text(canPrestige
                            ? 'Переродиться (+${NumberFormatter.format(pending)} ★)'
                            : 'Недостаточно для перерождения'),
                        onPressed: canPrestige
                            ? () => _confirmPrestige(context)
                            : null,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              const Text(
                'Опасная зона',
                style: TextStyle(
                  color: Colors.redAccent,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  icon: const Icon(Icons.delete_forever),
                  label: const Text('Полный сброс прогресса'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.redAccent,
                    side: const BorderSide(color: Colors.redAccent),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                  onPressed: () => _confirmHardReset(context),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  void _confirmPrestige(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E2230),
        title: const Text('Подтвердить перерождение?',
            style: TextStyle(color: Colors.white)),
        content: const Text(
          'Все монеты, генераторы и улучшения будут сброшены, '
          'но ты получишь звёзды.',
          style: TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Отмена'),
          ),
          TextButton(
            onPressed: () {
              state.prestige();
              Navigator.of(ctx).pop();
            },
            child: const Text('Переродиться',
                style: TextStyle(color: Colors.purpleAccent)),
          ),
        ],
      ),
    );
  }

  void _confirmHardReset(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E2230),
        title: const Text('Сбросить прогресс?',
            style: TextStyle(color: Colors.redAccent)),
        content: const Text(
          'Это удалит ВСЁ: монеты, генераторы, улучшения, '
          'достижения, звёзды и перерождения. Действие нельзя отменить.',
          style: TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Отмена'),
          ),
          TextButton(
            onPressed: () async {
              await state.hardReset();
              if (ctx.mounted) Navigator.of(ctx).pop();
            },
            child: const Text('Сбросить',
                style: TextStyle(color: Colors.redAccent)),
          ),
        ],
      ),
    );
  }
}

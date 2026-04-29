import 'package:flutter/material.dart';

import '../game_state.dart';
import '../utils/format.dart';
import '../widgets/click_circle.dart';
import '../widgets/floating_text.dart';

class GameScreen extends StatefulWidget {
  final GameState state;
  const GameScreen({super.key, required this.state});

  @override
  State<GameScreen> createState() => _GameScreenState();
}

class _GameScreenState extends State<GameScreen> {
  final GlobalKey<FloatingTextLayerState> _floatKey =
      GlobalKey<FloatingTextLayerState>();
  final GlobalKey _circleKey = GlobalKey();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _maybeShowOfflineDialog();
    });
  }

  void _maybeShowOfflineDialog() {
    final s = widget.state;
    if (s.lastOfflineEarnings <= 0) return;
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E2230),
        title: const Text('С возвращением!',
            style: TextStyle(color: Colors.amber)),
        content: Text(
          'Пока тебя не было ${NumberFormatter.formatTime(Duration(seconds: s.lastOfflineSeconds))} '
          'твои генераторы заработали ${NumberFormatter.format(s.lastOfflineEarnings)} монет '
          '(50% от обычной скорости).',
          style: const TextStyle(color: Colors.white),
        ),
        actions: [
          TextButton(
            onPressed: () {
              s.clearOfflineNotice();
              Navigator.of(ctx).pop();
            },
            child: const Text('Забрать'),
          ),
        ],
      ),
    );
  }

  void _onCircleTap() {
    final state = widget.state;
    final gained = state.click();
    final ctx = _circleKey.currentContext;
    if (ctx != null) {
      final box = ctx.findRenderObject() as RenderBox;
      final center = box.localToGlobal(box.size.center(Offset.zero));
      final stackBox = (_floatKey.currentContext?.findRenderObject()
          as RenderBox?);
      final localPos =
          stackBox != null ? stackBox.globalToLocal(center) : center;
      _floatKey.currentState?.spawn(localPos, gained);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        return LayoutBuilder(
          builder: (context, constraints) {
            final size = constraints.biggest.shortestSide * 0.62;
            return Stack(
              children: [
                Container(
                  decoration: const BoxDecoration(
                    gradient: RadialGradient(
                      colors: [Color(0xFF1E2230), Color(0xFF0B0E14)],
                      radius: 1.0,
                    ),
                  ),
                ),
                Column(
                  children: [
                    const SizedBox(height: 16),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: Column(
                        children: [
                          Text(
                            NumberFormatter.format(state.coins),
                            style: const TextStyle(
                              fontSize: 44,
                              color: Colors.amber,
                              fontWeight: FontWeight.w900,
                              shadows: [
                                Shadow(blurRadius: 6, color: Colors.black54),
                              ],
                            ),
                          ),
                          const Text('монет',
                              style: TextStyle(color: Colors.white70)),
                          const SizedBox(height: 4),
                          Text(
                            '${NumberFormatter.format(state.cps)} / сек',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 18,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'за клик: ${NumberFormatter.format(state.clickValue + state.cps * state.clickFromCps)}',
                            style: const TextStyle(
                              color: Colors.white60,
                              fontSize: 13,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Expanded(
                      child: Center(
                        child: KeyedSubtree(
                          key: _circleKey,
                          child: ClickCircle(
                            onTap: _onCircleTap,
                            size: size,
                            clicksTotal: state.totalClicks,
                          ),
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          _StatChip(
                            icon: Icons.touch_app,
                            label: 'кликов',
                            value: NumberFormatter.formatInt(state.totalClicks),
                          ),
                          _StatChip(
                            icon: Icons.auto_awesome,
                            label: 'звёзд',
                            value: NumberFormatter.format(state.stardust),
                          ),
                          _StatChip(
                            icon: Icons.refresh,
                            label: 'престиж',
                            value: state.prestigeLevel.toString(),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                FloatingTextLayer(key: _floatKey),
              ],
            );
          },
        );
      },
    );
  }
}

class _StatChip extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  const _StatChip({
    required this.icon,
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.05),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
      ),
      child: Column(
        children: [
          Icon(icon, color: Colors.amber.shade200, size: 18),
          const SizedBox(height: 4),
          Text(value,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
              )),
          Text(label, style: const TextStyle(color: Colors.white54, fontSize: 11)),
        ],
      ),
    );
  }
}

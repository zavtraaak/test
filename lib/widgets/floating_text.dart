import 'dart:math' as math;
import 'package:flutter/material.dart';

import '../utils/format.dart';

/// Spawns short-lived floating "+N" labels at the location of each click.
class FloatingTextLayer extends StatefulWidget {
  const FloatingTextLayer({super.key});

  @override
  State<FloatingTextLayer> createState() => FloatingTextLayerState();
}

class FloatingTextLayerState extends State<FloatingTextLayer>
    with TickerProviderStateMixin {
  final List<_FloatingItem> _items = [];

  void spawn(Offset position, double amount) {
    final controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    );
    final dx = (math.Random().nextDouble() - 0.5) * 60;
    final item = _FloatingItem(
      controller: controller,
      position: position,
      drift: dx,
      label: '+${NumberFormatter.format(amount)}',
    );
    setState(() => _items.add(item));
    controller.forward().whenComplete(() {
      if (!mounted) {
        controller.dispose();
        return;
      }
      setState(() => _items.remove(item));
      controller.dispose();
    });
  }

  @override
  void dispose() {
    for (final i in _items) {
      i.controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: Stack(
        children: _items.map((i) {
          return AnimatedBuilder(
            animation: i.controller,
            builder: (context, _) {
              final t = i.controller.value;
              final dy = -120 * t;
              final opacity = 1.0 - t;
              return Positioned(
                left: i.position.dx - 40 + i.drift * t,
                top: i.position.dy + dy - 12,
                width: 80,
                child: Opacity(
                  opacity: opacity.clamp(0, 1),
                  child: Text(
                    i.label,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: Colors.amber.shade100,
                      fontSize: 22 + 6 * (1 - t),
                      fontWeight: FontWeight.w800,
                      shadows: const [
                        Shadow(
                          blurRadius: 6,
                          color: Colors.black87,
                          offset: Offset(0, 2),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            },
          );
        }).toList(),
      ),
    );
  }
}

class _FloatingItem {
  final AnimationController controller;
  final Offset position;
  final double drift;
  final String label;
  _FloatingItem({
    required this.controller,
    required this.position,
    required this.drift,
    required this.label,
  });
}

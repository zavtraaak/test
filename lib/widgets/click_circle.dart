import 'dart:math' as math;
import 'package:flutter/material.dart';

/// Big tappable circle in the center of the game screen. Pulses on press and
/// emits a tiny outward shock-wave to give clicks tactile feedback.
class ClickCircle extends StatefulWidget {
  final VoidCallback onTap;
  final double size;
  final int clicksTotal;

  const ClickCircle({
    super.key,
    required this.onTap,
    required this.size,
    required this.clicksTotal,
  });

  @override
  State<ClickCircle> createState() => _ClickCircleState();
}

class _ClickCircleState extends State<ClickCircle>
    with TickerProviderStateMixin {
  late final AnimationController _press = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 120),
    lowerBound: 0.0,
    upperBound: 1.0,
  );
  late final AnimationController _spin = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 30),
  )..repeat();

  void _onTap() {
    _press.forward(from: 0).then((_) => _press.reverse());
    widget.onTap();
  }

  @override
  void dispose() {
    _press.dispose();
    _spin.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) => _onTap(),
      child: AnimatedBuilder(
        animation: Listenable.merge([_press, _spin]),
        builder: (context, _) {
          final scale = 1.0 - 0.06 * _press.value;
          return Transform.scale(
            scale: scale,
            child: SizedBox(
              width: widget.size,
              height: widget.size,
              child: CustomPaint(
                painter: _CirclePainter(
                  spin: _spin.value,
                  pulse: _press.value,
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

class _CirclePainter extends CustomPainter {
  final double spin;
  final double pulse;

  _CirclePainter({required this.spin, required this.pulse});

  @override
  void paint(Canvas canvas, Size size) {
    final c = Offset(size.width / 2, size.height / 2);
    final r = size.shortestSide / 2;

    // Outer glow.
    final glow = Paint()
      ..shader = RadialGradient(
        colors: [
          const Color(0xFFFFD54F).withValues(alpha: 0.5 + 0.4 * pulse),
          Colors.transparent,
        ],
      ).createShader(Rect.fromCircle(center: c, radius: r * 1.2));
    canvas.drawCircle(c, r * 1.2, glow);

    // Body gradient.
    final body = Paint()
      ..shader = RadialGradient(
        colors: const [Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFEF6C00)],
        stops: const [0.0, 0.6, 1.0],
      ).createShader(Rect.fromCircle(center: c, radius: r));
    canvas.drawCircle(c, r, body);

    // Animated golden ring.
    final ring = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 6
      ..shader = SweepGradient(
        startAngle: 0,
        endAngle: 2 * math.pi,
        transform: GradientRotation(spin * 2 * math.pi),
        colors: const [
          Color(0xFFFFFFFF),
          Color(0xFFFFD54F),
          Color(0xFFFFA000),
          Color(0xFFFFD54F),
          Color(0xFFFFFFFF),
        ],
      ).createShader(Rect.fromCircle(center: c, radius: r * 0.95));
    canvas.drawCircle(c, r * 0.95, ring);

    // Highlight cap.
    final highlight = Paint()
      ..shader = RadialGradient(
        colors: [
          Colors.white.withValues(alpha: 0.65),
          Colors.white.withValues(alpha: 0.0),
        ],
      ).createShader(
          Rect.fromCircle(center: c.translate(0, -r * 0.3), radius: r * 0.55));
    canvas.drawCircle(c.translate(0, -r * 0.3), r * 0.55, highlight);

    // Coin "$" symbol.
    final tp = TextPainter(
      text: TextSpan(
        text: '\$',
        style: TextStyle(
          fontSize: r * 0.9,
          fontWeight: FontWeight.w900,
          color: const Color(0xFF6D4C00),
          shadows: const [
            Shadow(blurRadius: 8, color: Colors.black26, offset: Offset(0, 2)),
          ],
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    tp.paint(canvas, c.translate(-tp.width / 2, -tp.height / 2));
  }

  @override
  bool shouldRepaint(covariant _CirclePainter oldDelegate) =>
      oldDelegate.spin != spin || oldDelegate.pulse != pulse;
}

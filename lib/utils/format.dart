import 'dart:math' as math;

/// Formats large numbers using suffixes for readability in idle/clicker games.
/// Examples: 1234 -> "1.23K", 1500000 -> "1.50M", 1.2e18 -> "1.20Qi".
class NumberFormatter {
  static const List<String> _suffixes = [
    '',
    'K',
    'M',
    'B',
    'T',
    'Qa',
    'Qi',
    'Sx',
    'Sp',
    'Oc',
    'No',
    'Dc',
    'UDc',
    'DDc',
    'TDc',
    'QaDc',
    'QiDc',
    'SxDc',
    'SpDc',
    'OcDc',
    'NoDc',
    'Vg',
  ];

  static String format(double value, {int decimals = 2}) {
    if (value.isNaN || value.isInfinite) return '∞';
    if (value < 0) return '-${format(-value, decimals: decimals)}';
    if (value < 1000) {
      if (value == value.truncateToDouble()) {
        return value.toStringAsFixed(0);
      }
      return value.toStringAsFixed(decimals);
    }
    final magnitude = (math.log(value) / math.ln10 / 3).floor();
    final scaled = value / math.pow(1000, magnitude);
    final suffix = magnitude < _suffixes.length
        ? _suffixes[magnitude]
        : 'e${magnitude * 3}';
    return '${scaled.toStringAsFixed(decimals)}$suffix';
  }

  static String formatInt(int value) => format(value.toDouble(), decimals: 0);

  static String formatTime(Duration d) {
    if (d.inSeconds < 60) return '${d.inSeconds}s';
    if (d.inMinutes < 60) return '${d.inMinutes}m ${d.inSeconds % 60}s';
    if (d.inHours < 24) {
      return '${d.inHours}h ${d.inMinutes % 60}m';
    }
    return '${d.inDays}d ${d.inHours % 24}h';
  }
}

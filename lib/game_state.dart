import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'data.dart';
import 'models.dart';

const String _saveKey = 'circle_clicker_save_v1';

/// Maximum offline time (in seconds) that is paid out when re-opening the app.
const int _offlineCapSeconds = 12 * 3600;

/// How aggressively offline progress is paid out (0..1, where 1 = 100% of CPS).
const double _offlineEfficiency = 0.5;

/// Holds all gameplay state and exposes mutating methods that notify listeners.
class GameState extends ChangeNotifier {
  GameState();

  // --- Currency / lifetime stats ---
  double coins = 0;
  double totalEarned = 0;
  int totalClicks = 0;
  int prestigeLevel = 0;
  double stardust = 0; // prestige currency, kept across resets
  double pendingStardust = 0; // computed each frame, claimed on prestige
  DateTime? sessionStart;
  DateTime? lastTick;
  DateTime? lastSaved;

  // owned[generatorId] -> count
  final Map<String, int> owned = {};

  // upgradeId -> purchased
  final Set<String> purchasedUpgrades = {};

  // achievementId -> unlocked
  final Set<String> unlockedAchievements = {};

  // --- Derived (computed) ---
  double cps = 0;
  double clickValue = 1;
  double globalMultiplier = 1;
  double clickFromCps = 0;

  bool _loaded = false;
  bool get loaded => _loaded;

  Timer? _ticker;
  Timer? _saveTimer;

  // ----- Lifecycle -----

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_saveKey);
    sessionStart = DateTime.now();
    if (raw != null) {
      try {
        final data = json.decode(raw) as Map<String, dynamic>;
        coins = (data['coins'] as num?)?.toDouble() ?? 0;
        totalEarned = (data['totalEarned'] as num?)?.toDouble() ?? 0;
        totalClicks = (data['totalClicks'] as num?)?.toInt() ?? 0;
        prestigeLevel = (data['prestigeLevel'] as num?)?.toInt() ?? 0;
        stardust = (data['stardust'] as num?)?.toDouble() ?? 0;
        owned.clear();
        final ownedRaw = data['owned'] as Map<String, dynamic>? ?? {};
        ownedRaw.forEach((k, v) => owned[k] = (v as num).toInt());
        purchasedUpgrades
          ..clear()
          ..addAll((data['upgrades'] as List<dynamic>? ?? []).cast<String>());
        unlockedAchievements
          ..clear()
          ..addAll(
              (data['achievements'] as List<dynamic>? ?? []).cast<String>());
        final lastSavedMs = data['lastSaved'] as int?;
        if (lastSavedMs != null) {
          lastSaved = DateTime.fromMillisecondsSinceEpoch(lastSavedMs);
        }
      } catch (_) {
        // Corrupt save – start fresh, do not throw.
      }
    }
    _recompute();
    _grantOfflineProgress();
    _loaded = true;
    lastTick = DateTime.now();
    _startTicker();
    notifyListeners();
  }

  void _startTicker() {
    _ticker?.cancel();
    _ticker = Timer.periodic(const Duration(milliseconds: 100), (_) => _tick());
    _saveTimer?.cancel();
    _saveTimer =
        Timer.periodic(const Duration(seconds: 10), (_) => save());
  }

  @override
  void dispose() {
    _ticker?.cancel();
    _saveTimer?.cancel();
    super.dispose();
  }

  void _tick() {
    final now = DateTime.now();
    final last = lastTick ?? now;
    final dt = now.difference(last).inMilliseconds / 1000.0;
    lastTick = now;
    if (dt <= 0) return;
    if (cps > 0) {
      final delta = cps * dt;
      coins += delta;
      totalEarned += delta;
    }
    pendingStardust = computePendingStardust();
    _checkAchievements();
    notifyListeners();
  }

  Future<void> save() async {
    if (!_loaded) return;
    final prefs = await SharedPreferences.getInstance();
    final data = {
      'coins': coins,
      'totalEarned': totalEarned,
      'totalClicks': totalClicks,
      'prestigeLevel': prestigeLevel,
      'stardust': stardust,
      'owned': owned,
      'upgrades': purchasedUpgrades.toList(),
      'achievements': unlockedAchievements.toList(),
      'lastSaved': DateTime.now().millisecondsSinceEpoch,
    };
    lastSaved = DateTime.now();
    await prefs.setString(_saveKey, json.encode(data));
  }

  Future<void> hardReset() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_saveKey);
    coins = 0;
    totalEarned = 0;
    totalClicks = 0;
    prestigeLevel = 0;
    stardust = 0;
    owned.clear();
    purchasedUpgrades.clear();
    unlockedAchievements.clear();
    _recompute();
    notifyListeners();
  }

  // ----- Derived recomputation -----

  void _recompute() {
    double cpsAcc = 0;
    double globalMult =
        1.0 + 0.02 * stardust + 0.05 * _achievementBonusPercent();
    for (final up in GameData.generatorUpgrades) {
      if (up.kind == UpgradeKind.globalMultiplier &&
          purchasedUpgrades.contains(up.id)) {
        globalMult *= up.value;
      }
    }

    for (final g in GameData.generators) {
      final count = owned[g.id] ?? 0;
      if (count == 0) continue;
      double mult = 1.0;
      for (final up in GameData.generatorUpgrades) {
        if (up.kind == UpgradeKind.generatorMultiplier &&
            up.generatorId == g.id &&
            purchasedUpgrades.contains(up.id)) {
          mult *= up.value;
        }
      }
      cpsAcc += g.baseProduction * count * mult * globalMult;
    }

    double click = 1.0;
    double clickMult = 1.0;
    double clickFromCpsAcc = 0;
    for (final up in GameData.clickUpgrades) {
      if (!purchasedUpgrades.contains(up.id)) continue;
      switch (up.kind) {
        case UpgradeKind.clickFlat:
          click += up.value;
          break;
        case UpgradeKind.clickMultiplier:
          clickMult *= up.value;
          break;
        case UpgradeKind.clickFromCps:
          clickFromCpsAcc += up.value;
          break;
        default:
          break;
      }
    }
    click = click * clickMult * globalMult;

    cps = cpsAcc;
    clickValue = click;
    globalMultiplier = globalMult;
    clickFromCps = clickFromCpsAcc;
  }

  double _achievementBonusPercent() {
    double total = 0;
    for (final a in GameData.achievements) {
      if (unlockedAchievements.contains(a.id)) total += a.bonusPercent;
    }
    return total;
  }

  double get achievementBonusPercent => _achievementBonusPercent();

  // ----- Click -----

  /// Performs a click and returns the amount of coins gained, useful for the
  /// floating-text animation in the UI.
  double click() {
    final gained = clickValue + cps * clickFromCps;
    coins += gained;
    totalEarned += gained;
    totalClicks += 1;
    _checkAchievements();
    notifyListeners();
    return gained;
  }

  // ----- Generators -----

  double generatorCost(GeneratorDef g, {int amount = 1}) {
    final count = owned[g.id] ?? 0;
    return g.bulkCost(count, amount);
  }

  bool canBuyGenerator(GeneratorDef g, {int amount = 1}) {
    return coins >= generatorCost(g, amount: amount);
  }

  void buyGenerator(GeneratorDef g, {int amount = 1}) {
    final cost = generatorCost(g, amount: amount);
    if (coins < cost) return;
    coins -= cost;
    owned[g.id] = (owned[g.id] ?? 0) + amount;
    _recompute();
    _checkAchievements();
    notifyListeners();
  }

  /// Returns the largest amount (≤ desired) the player can afford, or 0 if none.
  int affordableAmount(GeneratorDef g, int desired) {
    int lo = 0, hi = desired;
    while (lo < hi) {
      final mid = (lo + hi + 1) >> 1;
      if (g.bulkCost(owned[g.id] ?? 0, mid) <= coins) {
        lo = mid;
      } else {
        hi = mid - 1;
      }
    }
    return lo;
  }

  // ----- Upgrades -----

  bool isUpgradeVisible(UpgradeDef up) {
    if (purchasedUpgrades.contains(up.id)) return false;
    final req = up.requirement;
    if (req == null) return true;
    if (req.minTotalClicks != null && totalClicks < req.minTotalClicks!) {
      return false;
    }
    if (req.minTotalEarned != null && totalEarned < req.minTotalEarned!) {
      return false;
    }
    if (req.generatorId != null && req.minOwned != null) {
      if ((owned[req.generatorId!] ?? 0) < req.minOwned!) return false;
    }
    return true;
  }

  void buyUpgrade(UpgradeDef up) {
    if (purchasedUpgrades.contains(up.id)) return;
    if (coins < up.cost) return;
    coins -= up.cost;
    purchasedUpgrades.add(up.id);
    _recompute();
    notifyListeners();
  }

  // ----- Achievements -----

  void _checkAchievements() {
    bool changed = false;
    for (final a in GameData.achievements) {
      if (unlockedAchievements.contains(a.id)) continue;
      if (_meetsCheck(a.check)) {
        unlockedAchievements.add(a.id);
        changed = true;
      }
    }
    if (changed) {
      _recompute();
    }
  }

  bool _meetsCheck(AchievementCheck c) {
    if (c.totalClicks != null && totalClicks < c.totalClicks!) return false;
    if (c.totalEarned != null && totalEarned < c.totalEarned!) return false;
    if (c.prestigeLevel != null && prestigeLevel < c.prestigeLevel!) {
      return false;
    }
    if (c.cps != null && cps < c.cps!) return false;
    if (c.generatorId != null && c.generatorOwned != null) {
      if ((owned[c.generatorId!] ?? 0) < c.generatorOwned!) return false;
    }
    if (c.totalGenerators != null) {
      for (final g in GameData.generators) {
        if ((owned[g.id] ?? 0) < c.totalGenerators!) return false;
      }
    }
    return true;
  }

  // ----- Prestige -----

  /// Stardust gained on next prestige. The 1e9 base means a player has to earn
  /// at least one billion coins lifetime before getting any stardust.
  double computePendingStardust() {
    if (totalEarned < 1e9) return 0;
    final raw = math.pow(totalEarned / 1e9, 0.5).toDouble();
    final result = raw.floor().toDouble() - stardust;
    return result < 0 ? 0 : result;
  }

  void prestige() {
    final reward = computePendingStardust();
    if (reward <= 0) return;
    stardust += reward;
    prestigeLevel += 1;
    coins = 0;
    owned.clear();
    purchasedUpgrades.clear();
    // totalEarned is preserved so we can compute future prestige rewards.
    _recompute();
    _checkAchievements();
    notifyListeners();
  }

  // ----- Offline progress -----

  void _grantOfflineProgress() {
    if (lastSaved == null) return;
    final secs = DateTime.now().difference(lastSaved!).inSeconds;
    if (secs <= 1) return;
    final capped = secs > _offlineCapSeconds ? _offlineCapSeconds : secs;
    final earnings = cps * capped * _offlineEfficiency;
    if (earnings <= 0) return;
    coins += earnings;
    totalEarned += earnings;
    _lastOfflineEarnings = earnings;
    _lastOfflineSeconds = capped;
  }

  double _lastOfflineEarnings = 0;
  int _lastOfflineSeconds = 0;
  double get lastOfflineEarnings => _lastOfflineEarnings;
  int get lastOfflineSeconds => _lastOfflineSeconds;

  void clearOfflineNotice() {
    _lastOfflineEarnings = 0;
    _lastOfflineSeconds = 0;
    notifyListeners();
  }
}

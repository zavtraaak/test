import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'game_state.dart';
import 'screens/achievements_screen.dart';
import 'screens/game_screen.dart';
import 'screens/prestige_screen.dart';
import 'screens/shop_screen.dart';
import 'screens/stats_screen.dart';
import 'screens/upgrades_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  final state = GameState();
  await state.load();
  runApp(CircleClickerApp(state: state));
}

class CircleClickerApp extends StatefulWidget {
  final GameState state;
  const CircleClickerApp({super.key, required this.state});

  @override
  State<CircleClickerApp> createState() => _CircleClickerAppState();
}

class _CircleClickerAppState extends State<CircleClickerApp>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    widget.state.save();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.inactive ||
        state == AppLifecycleState.detached) {
      widget.state.save();
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Circle Clicker',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.amber,
          brightness: Brightness.dark,
        ),
        scaffoldBackgroundColor: const Color(0xFF0B0E14),
        textTheme: const TextTheme().apply(
          bodyColor: Colors.white,
          displayColor: Colors.white,
        ),
      ),
      home: HomeShell(state: widget.state),
    );
  }
}

class HomeShell extends StatefulWidget {
  final GameState state;
  const HomeShell({super.key, required this.state});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    final pages = [
      GameScreen(state: widget.state),
      ShopScreen(state: widget.state),
      UpgradesScreen(state: widget.state),
      AchievementsScreen(state: widget.state),
      PrestigeScreen(state: widget.state),
      StatsScreen(state: widget.state),
    ];
    return Scaffold(
      body: SafeArea(child: pages[_index]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        backgroundColor: const Color(0xFF14181F),
        indicatorColor: Colors.amber.withValues(alpha: 0.3),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.adjust),
            selectedIcon: Icon(Icons.adjust, color: Colors.amber),
            label: 'Игра',
          ),
          NavigationDestination(
            icon: Icon(Icons.storefront),
            selectedIcon: Icon(Icons.storefront, color: Colors.amber),
            label: 'Магазин',
          ),
          NavigationDestination(
            icon: Icon(Icons.upgrade),
            selectedIcon: Icon(Icons.upgrade, color: Colors.amber),
            label: 'Улучш.',
          ),
          NavigationDestination(
            icon: Icon(Icons.military_tech),
            selectedIcon: Icon(Icons.military_tech, color: Colors.amber),
            label: 'Достиж.',
          ),
          NavigationDestination(
            icon: Icon(Icons.auto_awesome),
            selectedIcon: Icon(Icons.auto_awesome, color: Colors.amber),
            label: 'Престиж',
          ),
          NavigationDestination(
            icon: Icon(Icons.bar_chart),
            selectedIcon: Icon(Icons.bar_chart, color: Colors.amber),
            label: 'Статы',
          ),
        ],
      ),
    );
  }
}

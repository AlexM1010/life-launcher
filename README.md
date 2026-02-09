# life-launcher

Minimalist Android launcher for the Open-Closed life management system. Displays tasks from Life Manager's calendar in a clean, distraction-free interface.

## Architecture

Part of the hub-and-spoke architecture:
- **life-manager** (web) - Intelligence hub, planning algorithm, data aggregation
- **life-launcher** (this) - Simple display layer, gesture-based interaction
- **life-widgets** - Shared widget components

## Features

- OLauncher-style minimalism (time, date, battery, screen time)
- 0-8 configurable home apps (text-only)
- Swipe gestures: up (app drawer), down (notifications), left/right (quick apps)
- Widget slots (top/bottom) for displaying Life Manager data
- Auto-keyboard in app drawer with instant launch on single match
- Settings: alignment, app count, swipe gestures, screen time toggle

## Setup

1. Copy `local.properties.example` to `local.properties`
2. Set your Android SDK path
3. (Optional) Add Google OAuth client ID for calendar access

## Build

```bash
./gradlew assembleDebug
```

## License

GNU GPLv3

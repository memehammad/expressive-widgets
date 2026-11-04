# Expressive Widgets

Material 3 Expressive home screen widgets for Android: clocks, world clocks, battery and quick
toggles in 16 expressive shapes, coloured from your wallpaper. Optional
[Shizuku](https://shizuku.rikka.app/) support makes toggles switch directly and adds battery
health.

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="54">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/memehammad/expressive-widgets)

Android 12 or newer. Work in progress: weather, calendar and Home Assistant widgets are coming.

## Widgets

- **Shape clock:** the time inside a shape, hours over minutes or inline, with the date.
- **World clocks:** up to three cities with local time, day and time difference.
- **Battery:** a shape that fills with your charge; wider sizes show watts, time to full,
  temperature, cycles and, with Shizuku, battery health.
- **Quick toggles:** Wi-Fi, mobile data, Bluetooth, airplane mode, flashlight, battery saver,
  Do not disturb, dark theme, location, auto-rotate, smooth display and NFC.

Each widget lets you choose its shape, colour role, soft or bold fill and a card background.
Long-press a widget and open its settings to change them.

## Install

Use the Obtainium badge above, or download the APK from the latest
[release](../../releases/latest).

## Shizuku (optional)

Normal apps can't switch Wi-Fi, mobile data and similar settings, or read battery health. With
Shizuku running, Expressive Widgets uses a small shell with ADB rights for exactly those
commands. Without it, toggles open the matching settings panel instead.

## Building

JDK 17 or 21 and the Android SDK, then `./gradlew assembleRelease`. Shapes are generated with
`python3 tools/gen_shapes.py`.

## License

[MIT](LICENSE).

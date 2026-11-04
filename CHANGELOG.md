# Changelog

The newest section is used as the release notes on GitHub (and shown in Obtainium).

## 0.3

- **Steps widget:** today's steps fill a shape towards your goal (4k–15k). Hitting it brings out a
  turning sun with a tick. Bigger sizes add distance, calories and bars for the week.
- **Workout widget:** your latest workout in a shape for its sport (an arrow for runs, a gear for
  rides, a flower for yoga, ...) with distance, time and this week's total. Works with Strava,
  Fitbit, Samsung Health and Google Fit through Health Connect.
- **Home screen colour:** a colour choice for the shapes themselves that matches your themed
  app icons. Pick the backdrop and symbol tones (light and dark mode) once in the app; any
  widget set to "Home screen" uses them, for shapes and optionally the card.
- **Colours you pick now show up:** Pixel Launcher was replacing widget colours with colours
  from the wallpaper behind each widget. Colours are now fixed values, so shapes match the
  settings, the preview and your icons. Colour options show their actual colours.
- **Custom colour:** pick any wallpaper tone for a widget's shapes (light and dark mode); text
  colour is chosen for contrast automatically.
- **Better contrast:** numbers in liquid shapes move off the waterline instead of sitting half on
  each colour; text drawn without a card is white with a soft shadow, like icon labels.
- The shape picker shows every shape in the widget's own colour.
- **New styles:** Trio clock (hours, minutes and the date in three shapes), Pebbles battery
  (five little shapes filling up), Big temperature weather.


Expressive redesign.

- **The real Material 3 Expressive shapes:** all 35 official shapes (cookies, clovers, sunny,
  puffy, ghost-ish, bursts, pixel shapes and more), computed with Google's own shape library
  instead of drawn by hand.
- **Shape clock, four styles:** Duo (hours and minutes in two overlapping shapes and colours),
  Stacked, Inline and Analog (any shape as the dial, rounded hands, an orbiting seconds dot).
  Text is sized for the exact widget size, so it never spills out of its shape. Date as a chip.
- **Battery:** the shape fills with liquid and two moving waves. Charging adds a morphing badge
  with a bolt; low battery turns red.
- **Weather:** shows the weather as a shape (puffy clouds, drippy rain, a snowflake burst) and
  sunshine as a turning, breathing sun. Plus calendar **Agenda** widget.
- **Animation:** shapes can slowly turn (clock, weather, toggles, agenda). Toggles that are on
  get their own colour each.
- World clocks: each city in its own shape and colour.
- **Live preview** at the top of every widget's settings.
- **Weather for your current location** (rough location is enough; allow "all the time" for
  updates while the app is closed).
- Animations keep running when a widget updates or is tapped.
- Taps open the right app: weather app, calendar, clock (world clocks), alarms, battery usage.
- Fixed: agenda widget failed to draw on Android 14+.
- **Card colours:** "Match app icons" (the themed-icon colour, default), neutral or any colour role.

## 0.1

First preview.

- **Shape clock:** the time inside any of 16 expressive shapes, hours over minutes or inline,
  with the date.
- **World clocks:** up to three cities with local time, day and time difference.
- **Battery:** a shape that fills with your charge; wide sizes show watts, time to full,
  temperature, cycle count and (with Shizuku) battery health.
- **Quick toggles:** up to four of Wi-Fi, mobile data, Bluetooth, airplane mode, flashlight,
  battery saver, Do not disturb, dark theme, location, auto-rotate, smooth display and NFC. With
  Shizuku they switch directly; without it they open the setting. The shape changes when a
  toggle is on.
- Every widget: pick the shape, colour role (from your wallpaper), soft or bold fill and an
  optional card background. Long-press a widget to change it later.

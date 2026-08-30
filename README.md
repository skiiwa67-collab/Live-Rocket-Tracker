# Retro Command Center (CCOS Phase 1)

Kotlin-native Android live wallpaper + Command Center shell.

**Vector graphic command center** — Mercury-program rocket skin + System Metrics module.

## Modules (reskin on switch)

| Module | Skin | Buttons |
|--------|------|---------|
| **Vector Rocket** | Mercury instrument panel (charcoal, NASA red, amber, aluminum) | CMD ENG RKT PAD TEL SUB PER BUD |
| **System Metrics** | Cool / Warm / Neon / Amber + Analog/Digital | CMD BAT CPU RAM DIS NET SEN HOME |
| **Live Rocket Telemetry** | Agency reskin (SpaceX dark / NASA blue-red / CASC red / generic cyan) | CMD CDT TEL STS PAD VID MSK AUTO |

Switching modules changes **labels, colors, and main surface** automatically. Telemetry module tracks real upcoming launches via Launch Library 2 and shows countdown + status + profile gauges. Auto mode always locks to the next NET.

## Gestures

- **Swipe LEFT** → Vector Rocket (primary home) + show buttons  
- **Swipe RIGHT** → System Metrics + show buttons  
- **Vertical swipe** → hide side buttons  
- **CMD double-tap** → Settings shell  
- **DIS** (system) → Storage settings  
- **HOME** (system) → Launcher / home settings  

## Architecture

```
Engine  ⟂  Module System  ⟂  Skin System  ⟂  Data Provider
```

## Install

1. Unzip over `C:\Users\skiiw\AndroidStudioProjects\RetroCommandCenter` (or open folder)
2. Android Studio → Open → Sync → Run
3. App → SET AS LIVE WALLPAPER

## Decision Log

- 2026-08-12: CCOS Phase 1 Kotlin native; dual free modules; Mercury rocket skin; System Metrics vector HUD; module reskin on switch.
- 2026-08-13: Live Rocket Telemetry module (paid target) added. Data: The Space Devs LL2 (list mode, 4–5 min poll). Auto + manual launch select. Agency reskins (SpaceX / NASA / CASC / generic). Side buttons = CDT / TEL / STS / PAD / VID / MSK / AUTO. Passive T- badge on other home pages. No public live telemetry stream → countdown + status + typical Falcon profile gauges until better source.


# Trailer tracking + Starlink feasibility (Estes)

**Soft-PASS public research — September 19, 2026.** Yard inspection still needed for exact box on *your* trailer.

## a) What hardware is it?

| Claim | Soft-PASS finding |
|--------|-------------------|
| Driver: “probably Qualcomm” | **No public Soft-PASS** that Estes trailer pingers are Qualcomm/Omnitracs MCP. MCP is an in-cab unit, not the usual trailer tracker. |
| Fleetwide trailer track (2021) | **Spireon FleetLocate** on ~40k trailers, dollies, containers. Integrated with Trimble driver platform + Estes yard/dock apps. |
| Current expansion (late 2025 → through 2026) | **Samsara Asset Gateways** on **43,000+ trailers** + Asset Tags for dollies/pallet jacks/ramps. |

**Likely reality on the lot:** mix or transition — older Spireon boxes still pinging; Samsara AG rollout in progress. Exact make/model on one trailer = photo the sticker / ask maintenance / IT asset list.

## b) Data path / API + Starlink

- Spireon / Samsara trailer devices talk **cellular** (LTE-M / NB-IoT class), low power, battery + trailer 7-way when hooked.
- They push location into **vendor cloud** → Estes apps / Samsara dashboard / APIs.
- **Starlink (SpaceX)** is a high-power **broadband dish** for the tractor (Performance kit ~75–100 W). Telemetry API = dish health, not trailer GPS.
- You do **not** “plug the Qualcomm/Samsara trailer tracker into Starlink” like a USB stick. Options:
  1. **Tractor Starlink as Wi-Fi/backhaul** for in-cab devices when cellular is dead (helps phone/tablet, not usually the trailer’s own cellular modem).
  2. **Fix the flaky cellular trackers** (Samsara AG53 deployment, coverage, battery/7-way power, Find My settings).
  3. **Hybrid:** keep trailer cellular trackers; use Starlink on tractors for driver/dispatch apps in dead zones.

## c) Feasibility / cost (rough)

| Approach | Possible? | What it takes | Rough cost vibe |
|----------|-----------|---------------|-----------------|
| Rely on / finish **Samsara Asset Gateway** rollout | **Yes — Soft-PASS path** | IT/fleet complete AG + Asset Tags; train drivers on Find Asset; measure ping reliability | Already in Estes’ announced program — **company spend**, not a DIY Chris project |
| Put **Starlink dish on every trailer** | **Soft-FAIL / unrealistic** | Power, sky view, theft, $hardware×43k trailers | Prohibitive vs cellular asset gateways |
| **Starlink on tractors** + keep trailer trackers | **Yes** | Pilot Performance kits on dead-zone lanes; keep Samsara/Spireon for trailer GPS | Per-truck dish + monthly (sales quote); trailer trackers stay cellular |
| Custom “route trailer GPS through Starlink” | **Only via architecture** | Gateway that bridges local radios to Starlink LAN — engineering + vendor buy-in | High; not a tablet UI tweak |

## d) Short answer for Chris

**Possible to get more reliable trailer location?** Yes — but the winning Soft-PASS is almost certainly **making the existing/new Samsara (and residual Spireon) trailer trackers work**, not bolting Starlink onto each trailer.

**Starlink’s job:** better **truck** internet in cellular dead zones for the driver tablet / dispatch apps.

**Next Soft-PASS steps (no build yet):** (1) photo the tracker sticker on a pup at ARL193, (2) ask IT whether that trailer is Spireon or Samsara AG yet, (3) log 10 failed finds with trailer # and yard.


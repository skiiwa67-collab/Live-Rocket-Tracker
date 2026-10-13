# Gen 1 landing-gear pad — SPEC
Label: SPEC. Live numbers EST unless measured. Voltage class is inherited (see ports). No SKU price. No invented pad amps.

Owner: Strip Electrical
Project: Truck-Electrification-Network / DFW Node (Estes Loop 820, 201 doors EST)
Locked: Chris Olsen, 2026-08-20

## What it is
A flush, full-length run at the landing-gear line, parallel to the dock doors. Not the door threshold. Not a 10×10 floor tile. Not a raised bar.

The factory SKU is one snap-out cassette. Repeat it to make the run. Replace a chewed bay. Do not tear out the yard.

One cassette face. Both shoes: round sand foot and square plate. No second SKU.

Housing is recycled-tire crumb / tire-rubber. Cheap box. Worn cassette goes back to crumb.


## Site lock — trench band (Chris + Elon, 2026-08-20)
Do not pick a new alignment. Chris drew two lines on the real dock photo (bays 66–60, trailer on 62). Green = one cassette edge. Red = the other. The strip is the band between those lines. Not a bar at the bottom of the frame. Not a single centerline.

- Parallel to the door line. Full door-run.
- On the concrete apron. Passes under the spotted trailer’s shoes.
- Flush modular cassettes in a recessed pan under that band.
- Second photo is parked trailers, gear down. That is the contact. Shoe on strip. Conduit up the real legs.

This yard's shoes in the photo are round sand feet, not only the published Jost 10×10 / 12×12 square cushion. One cassette face takes both. No second SKU. Round diameter SPEC pending a tape on those shoes.

Band width (green to red) and offset from dock wall: SPEC pending tape. Same cassette family for 53-ft and 28-ft, different count / depth if the pup lands off this band.

Laterals still leave this band and run back to the dock wall.

Artist is stood down. Strip Electrical owns volts and the trench.

## What conducts (Gen 1)
Weight is the switch. Gear down + trailer mass closes a mechanical plunger / rocker / contact inside the cassette.

The rubber does not conduct. It is the chew face and the weather box.

Material (squish-to-conduct) and hybrid stay on the board as later families. They are not the first article. The pressure-conductive layer does not exist. Do not wait for it.

Idle pads stay dark. Only a loaded segment conducts.

## Power path
Building bank (under slab) → pad-bus in the pan → closed contact in that bay → gear shoe (on top of the flush face) → jumper UP INSIDE the landing-gear tubes → trailer junction → snap-in modules → Power Pod / BEV pack, and back into the building.

Skin PV can feed modules with the pad idle.

The steel leg is not the bus.

Orange is HV class, not a number.

## Cassette stack (top down)
1. Recycled-tire wear face, flush with concrete
2. Mechanical plunger / rocker
3. Sealed wipe contacts (wet / oil / ice)
4. Segment tap (that bay only)
5. Snap rails into the slab pan / pad bus
6. Status lamps on their own loop, not through HV

## Slab pan
Recessed steel pan. Cassette drops in. Wear face flush. Forklifts drive it. They still grind the face. That is why it snaps out.

Punch: a flush pan holds water and ice. Drain and slope or the plunger never compresses.

## In-tube jumper
Inside the landing-gear tubes. Not a hose taped on the outside.

Jost travel is published 14 / 17 / 19 in. The pair has to live through that telescope.

Water in the tube is the other hole. Sealed inner path. Gland at the shoe. Service loop still at the foot so crank-walk does not tear it.

Both feet must close or no watts (cocked trailer).

## Contact patch (published analog)
Jost cushion feet: 10×10 in and 12×12 in (catalog).

The contact that matters is that shoe on one bay, not the whole door-line copper.

SKU length SPEC: longer than that foot. Switch lives in the bay center. Seam is dead rubber. A shoe that sits on a joint does not close.

Lateral spacing (left/right feet) is frame-width. SPEC pending a tape on a real van and a real pup.

## 53-ft van vs 28-ft pup
Same cassette family. Different count / spacing.

The run sits on the GEAR LINE, parallel to the doors, not at the sill.

Van and pup land at different offsets from the dock wall. Two rows or extra depth. SPEC pending tape. Do not invent the offset.

## Lamps (input to Yard Software)
One lamp set per cassette. One bay dies, the run stays. That is how one forklift does not take 201 doors down.

| State | Color | Watts |
|---|---|---|
| CONDUCTING | green | may flow on that bay |
| IDLE | dim | no gear, no watts |
| WORN | amber | snap the cassette, no watts |
| FAULT | red | do not use that segment, no watts |

Ice is weather, not a lamp.
Island is a yard verb (amber), not a pad lamp.


## Laterals — fat pipe to the wall (Chris 2026-08-20)
The cassette run is the contact bus. It does not carry the yard the long way.

At intervals along the red line, laterals run back to the dock wall. Fatter pipe. Bidirectional: powerpacks in and out.

Interval is set by power-flow limits, not by door count:
- ampacity of the cassette bus between taps
- voltage drop along that span
- fault-current / protection zone per tap

Do not invent the spacing in feet. Do not invent AWG. Laterals are the more permanent slab run. Cassettes still snap out over them.

One dead bay does not kill the neighbors. A blown lateral takes its protection zone, not 201 doors.

## Factory
Make cassettes. Snap them in. Pull worn bays. Recycle crumb. That is its own plant.

Swap: one-person snap. No slab cut. No pour. Do not invent a minute count until a prototype exists.

## Not yet (do not fake these)
- Volts
- Amps / fault current
- Cassette length in inches
- Gear-line offset from the dock wall (53 and pup)
- Frame-width tape
- SKU price
- Service-life cycles
- Lateral spacing / conductor size

Fault current waits on the 10×10 (or 12×12) patch plus the in-tube jumper. Do not pick a voltage before that.

## Who consumes this
- Yard Software: conducting / idle / worn / fault per bay
- Power Pod: energy arrives from trailer modules that got it from the pad (or skin PV). Not a wall plug.
- Artist / Steve: flush run, seams, lamps on the face, shoe on top, no speed bump, no lightning at the wheels

## Pad job (Chris 2026-08-20)
Bidirectional in/out for building powerpacks. Power equalization. Not a charger gun. Not a Megacharger.

Loaded bay may pass watts either way:
- powerpacks → pad → trailer modules
- trailer modules → pad → powerpacks (equalize / backfeed)

Idle bay stays dark. Skin PV → modules still works with pad idle.
One bay WORN / FAULT does not take the run.

## Voltage class — inherited, not invented
The cassette does not pick V. It inherits from the ports. Analog only. No Tesla PO.

| Port | What we have | Label |
|---|---|---|
| Roof PV | Commercial rooftop strings are typically 1000 V or 1500 V DC class. Then DC-DC or invert into the building powerpacks. | typical, not this roof |
| Building powerpacks | Tesla Powerpack 2 4HR published analog: 960 VDC max, 66 A, 55 kW. Original Megapack max 960 VDC. Megapack 2 / 2 XL max <1230 VDC. Site AC interconnect is 480 VAC 3-phase — that is not the pad. | published analog |
| Pad in/out | Same DC class as the powerpacks so equalization is a bus, not a converter farm. | SPEC, inherits |
| Trailer snap-in modules | No published number for this kit. Spec them to the same class as the pad / powerpacks. | SPEC |
| Semi-class pack | Tesla.com Semi charging: Megacharger / Basecharger 180–1000 VDC (post also listed 0–1000 VDC). Pack energy published via CARB: 822 kWh / 548 kWh. 1000 V class analog. | published analog |

Pad current is NOT the Megacharger 1100 A. That would melt a shoe. Closest published building analog is 66 A / 55 kW on a Powerpack 2 4HR unit. Per-bay pad amps wait on the 10×10 (or 12×12) contact plus the in-tube jumper.

Warehouse drop cannot charge a fleet. Roof + powerpacks + loaded bays do the equalize.

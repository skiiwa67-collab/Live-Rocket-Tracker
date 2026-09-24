extends Node
## Absolute-time orbital mechanics for the Harrow system.
## Distances are meters. One sun, one planet, one station.

const PLANET_R = 1800.0
const ORBIT_R = 2550.0
const G_SURFACE = 8.2
const GM = G_SURFACE * PLANET_R * PLANET_R
const ATMO_H = 420.0
const SCALE_H = 170.0
const DAY_SECONDS = 480.0
const SUN_POS = Vector3(52000, 16000, 9000)

func now() -> float:
	return Time.get_unix_time_from_system()

func gravity_at(pos: Vector3) -> Vector3:
	var r = pos.length()
	if r < 8.0:
		return Vector3.ZERO
	return -pos / r * (GM / (r * r))

func altitude(pos: Vector3) -> float:
	return pos.length() - PLANET_R

func atmo_density(pos: Vector3) -> float:
	var alt = altitude(pos)
	if alt > ATMO_H:
		return 0.0
	if alt < 0.0:
		alt = 0.0
	return exp(-alt / SCALE_H)

func mean_motion() -> float:
	return sqrt(GM / pow(ORBIT_R, 3.0))

func station_angle(t: float) -> float:
	return mean_motion() * t

func station_pos(t: float) -> Vector3:
	var a = station_angle(t)
	return Vector3(cos(a), 0.0, sin(a)) * ORBIT_R

func station_vel(t: float) -> Vector3:
	var a = station_angle(t)
	var speed = sqrt(GM / ORBIT_R)
	return Vector3(-sin(a), 0.0, cos(a)) * speed

func station_basis(t: float) -> Basis:
	var pos = station_pos(t)
	var up = pos.normalized()
	var vel = station_vel(t)
	var forward = vel.normalized()
	var right = forward.cross(up).normalized()
	forward = up.cross(right).normalized()
	return Basis(right, up, -forward).orthonormalized()

func planet_angle(t: float) -> float:
	return TAU * fmod(t / DAY_SECONDS, 1.0)

func planet_surface_velocity(world_pos: Vector3) -> Vector3:
	var omega = TAU / DAY_SECONDS
	return Vector3(0, omega, 0).cross(world_pos)

func sun_dir() -> Vector3:
	return SUN_POS.normalized()

func transit_preview(distance: float, urgency: float, ap: Dictionary, pilot_bonus: float, engine: Dictionary) -> Dictionary:
	var u = clampf(urgency, 0.05, 1.0)
	var route = float(ap.get("route", 1.0))
	var fuel_eff = float(ap.get("fuel_eff", 1.0))
	var emergency = float(ap.get("emergency", 1.0))
	var pilot = 1.0 + pilot_bonus
	var cruise = 90.0
	var base_time = maxf(distance, 200.0) / cruise
	var duration = base_time / maxf(0.25, route * pilot * lerpf(0.55, 1.75, u))
	var fuel = maxf(distance, 200.0) * float(engine.get("fuel_factor", 1.0)) * lerpf(0.65, 1.9, u) / maxf(0.2, fuel_eff * pilot)
	fuel *= 0.004
	var wear = lerpf(0.35, 2.4, u) * float(engine.get("wear_factor", 1.0)) / maxf(0.2, emergency) * maxf(distance, 200.0) / 8000.0
	return {
		"duration": duration,
		"fuel": fuel,
		"wear": wear,
	}

func sample_arc(a: Vector3, b: Vector3, u: float, loft: float) -> Vector3:
	var k = clampf(u, 0.0, 1.0)
	var s = k * k * (3.0 - 2.0 * k)
	var mid = (a + b) * 0.5
	var away = mid.normalized()
	if away.length() < 0.1:
		away = Vector3.UP
	mid += away * loft
	var ab = a.lerp(mid, s)
	var bb = mid.lerp(b, s)
	return ab.lerp(bb, s)

func arc_velocity(a: Vector3, b: Vector3, u: float, duration: float, loft: float) -> Vector3:
	var du = 0.01
	var p0 = sample_arc(a, b, maxf(u - du, 0.0), loft)
	var p1 = sample_arc(a, b, minf(u + du, 1.0), loft)
	return (p1 - p0) / maxf(duration * du * 2.0, 0.001)

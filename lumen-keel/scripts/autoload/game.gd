extends Node
## Runtime state. Containers serialize through SaveGame.

var state: Dictionary = {}
var cheat_flag = false
var brand_availability = 70
var god_mode = false
var message = ""
var message_t = 0.0

signal state_changed
signal toast(text: String)

func _process(delta: float) -> void:
	if message_t > 0.0:
		message_t -= delta

func notify(text: String) -> void:
	message = text
	message_t = 4.0
	toast.emit(text)

func new_game() -> void:
	var rng = RandomNumberGenerator.new()
	rng.randomize()
	cheat_flag = false
	god_mode = false
	brand_availability = 70
	state = {
		"format": SaveGame.FORMAT,
		"game": "lumen-keel",
		"cheat_flag": false,
		"seed": rng.randi(),
		"created_unix": SimClock.now(),
		"saved_unix": SimClock.now(),
		"settings": {
			"scarcity": 40,
			"economy_speed": 50,
			"mouse_sens": 0.0022,
			"invert_y": false,
			"master_volume": 0.85,
			"brand_availability": 70,
		},
		"player": _default_player(),
		"ship": _default_ship(),
		"station": _default_station(rng),
		"crew": [],
		"planet": _default_planet(rng),
		"world": {
			"hostile_alive": true,
			"objectives": _objectives(),
			"placed": [],
			"benches": ["station_services", "skiff_bench"],
			"jobs_done": [],
			"kills": 0,
		},
	}
	state_changed.emit()

func _default_player() -> Dictionary:
	var skills = {}
	for s in Catalog.SKILLS:
		skills[s] = {"rank": 0, "xp": 0.0}
	return {
		"name": "Keel",
		"health": 100.0,
		"health_max": 100.0,
		"armor": 2.0,
		"hunger": 8.0,
		"o2": 100.0,
		"credits": 850,
		"skills": skills,
		"inventory": [
			{"id": "pistol_kinetic", "count": 1},
			{"id": "pistol_rounds", "count": 2},
			{"id": "knife_keel", "count": 1},
			{"id": "ration", "count": 2},
			{"id": "painting_kite", "count": 1},
			{"id": "plant_moss", "count": 1},
			{"id": "undersuit", "count": 1},
		],
		"ammo": {"pistol_rounds": 36, "rifle_rounds": 0, "ground_cells": 0, "arrows": 0, "rockets": 0},
		"mag": {"pistol_kinetic": 18},
		"equipped_weapon": "pistol_kinetic",
		"equipped_armor": "undersuit",
		"mode": "foot",
		"seated": false,
		"third_person": false,
		"space": "station",
		"local": [ -16.0, 1.1, 28.0 ],
		"yaw": 0.0,
	}

func _default_ship() -> Dictionary:
	return {
		"frame": "lumen_frame_light",
		"parts": {
			"eng_l": "lumen_engine_light",
			"eng_r": "lumen_engine_light",
			"core": "lumen_core_light",
			"cap": "lumen_cap_light",
			"conduit": "lumen_conduit",
			"ap": "lumen_ap",
			"scan": "lumen_scan",
			"bay": "vehicle_bay",
			"wpn_c": "lumen_laser",
		},
		"condition": {},
		"fuel": 80.0,
		"fuel_max": 80.0,
		"capacitor": 80.0,
		"shield": 0.0,
		"ammo": {"shells": 24, "cells": 20, "missiles": 0},
		"cargo": [],
		"loc": "hangar",
		"transit": {},
		"pos": [0, 0, 0],
		"vel": [0, 0, 0],
		"rot": [0, 0, 0, 1],
		"hull": 220.0,
		"hull_max": 220.0,
		"flight_assist": true,
		"urgency": 0.45,
	}

func _default_station(rng: RandomNumberGenerator) -> Dictionary:
	var containers = {}
	for i in 6:
		containers["trash_%d" % i] = {"loot": "trash", "items": [], "opened": false, "respawn_at": 0.0}
	containers["hab_locker"] = {"loot": "", "items": [{"id": "ration", "count": 1}, {"id": "scrap", "count": 2}], "opened": true, "respawn_at": 0.0}
	containers["under_crate"] = {"loot": "trash", "items": [], "opened": false, "respawn_at": 0.0}
	return {
		"power_gen": 120.0,
		"power_draw": 70.0,
		"sewage": 22.0,
		"food": 40.0,
		"containers": containers,
		"shops": {},
		"shop_seed": rng.randi(),
		"elevator": 1.0,
		"last_tick_unix": SimClock.now(),
		"raiders": [true, true, true],
		"rats": [true, true, true, true],
	}

func _default_planet(rng: RandomNumberGenerator) -> Dictionary:
	return {
		"seed": rng.randi(),
		"nodes": {},
		"scanned_sites": [],
		"buggy_deployed": false,
		"buggy_pos": [0, 0, 0],
		"fauna": {},
	}

func _objectives() -> Array:
	return [
		{"id": "panel", "text": "Open the hab wall panel with E. Power, sewage, and food are live.", "done": false},
		{"id": "trash", "text": "Loot a trash can on the promenade.", "done": false},
		{"id": "scan_station", "text": "Press F. The scanner marks medical, the store, and the armorer.", "done": false},
		{"id": "kiosk", "text": "Use a kiosk. Buy rations or ammo. No conversations, just the counter.", "done": false},
		{"id": "under", "text": "Drop into the underbelly. Skin a pipewolf or drop a raider.", "done": false},
		{"id": "moss", "text": "Scan and harvest filament moss off an underbelly wall.", "done": false},
		{"id": "board", "text": "Hangar. Press E on the skiff hatch, then G to take the seat.", "done": false},
		{"id": "launch", "text": "Launch from the elevator. The ship inherits the station's orbit.", "done": false},
		{"id": "scan_orbit", "text": "From orbit, press F and mark a surface site.", "done": false},
		{"id": "land", "text": "Land on Vesper. Autopilot (Z) will fly it, or take the stick.", "done": false},
		{"id": "mine", "text": "Harvest ore. E on a marked outcrop.", "done": false},
		{"id": "craft", "text": "Return and craft at the hangar bench. Ammo is one step; ore is the gate.", "done": false},
		{"id": "buggy", "text": "Buy the Shelf Buggy, load it in the bay, deploy it on the surface.", "done": false},
	]

func flag(id: String) -> void:
	for o in state.get("world", {}).get("objectives", []):
		if o["id"] == id and not o["done"]:
			o["done"] = true
			notify("Logged: " + str(o["text"]).substr(0, 72))
			state_changed.emit()
			return

func objective_text() -> String:
	for o in state.get("world", {}).get("objectives", []):
		if not o["done"]:
			return str(o["text"])
	return "Shift is yours. The Harrow system keeps running while you are gone."

func scarcity() -> int:
	return int(state.get("settings", {}).get("scarcity", 40))

func economy_speed() -> int:
	return int(state.get("settings", {}).get("economy_speed", 50))

func economy_mult() -> float:
	var t = clampf((float(economy_speed()) - 1.0) / 99.0, 0.0, 1.0)
	return lerpf(0.35, 6.0, t)

func yield_mult() -> float:
	return lerpf(0.6, 3.0, clampf((float(economy_speed()) - 1.0) / 99.0, 0.0, 1.0))

func ship_frame_id() -> String:
	return str(state.get("ship", {}).get("frame", "lumen_frame_light"))

func frame_def() -> Dictionary:
	return Catalog.part(ship_frame_id())

func socket_part(socket_id: String) -> String:
	return str(state.get("ship", {}).get("parts", {}).get(socket_id, ""))

func install_part(socket_id: String, part_id: String) -> bool:
	var frame = frame_def()
	var socket = {}
	for hp in frame.get("hardpoints", []):
		if hp["id"] == socket_id:
			socket = hp
			break
	if socket.is_empty():
		return false
	if part_id != "" and not Catalog.can_fit(ship_frame_id(), part_id, socket):
		notify("That part will not seat on this hardpoint.")
		return false
	var ship: Dictionary = state["ship"]
	var prev = str(ship["parts"].get(socket_id, ""))
	if prev != "":
		add_item(player_inv(), prev, 1)
	if part_id != "":
		if not take_item(player_inv(), part_id, 1):
			if prev != "":
				take_item(player_inv(), prev, 1)
				ship["parts"][socket_id] = prev
			notify("Part is not in your inventory.")
			return false
		ship["parts"][socket_id] = part_id
		if not ship["condition"].has(socket_id):
			ship["condition"][socket_id] = 100.0
	else:
		ship["parts"].erase(socket_id)
	_recompute_hull()
	state_changed.emit()
	return true

func set_frame(frame_id: String) -> bool:
	if not Catalog.parts.has(frame_id):
		return false
	if not take_item(player_inv(), frame_id, 1) and frame_id != ship_frame_id():
		notify("Frame is not in inventory.")
		return false
	var old = ship_frame_id()
	if old != frame_id and old != "":
		add_item(player_inv(), old, 1)
	var ship: Dictionary = state["ship"]
	var new_hps: Array = Catalog.part(frame_id).get("hardpoints", [])
	var keep = {}
	for hp in new_hps:
		var sid = str(hp["id"])
		var pid = str(ship["parts"].get(sid, ""))
		if pid != "" and Catalog.can_fit(frame_id, pid, hp):
			keep[sid] = pid
		elif pid != "":
			add_item(player_inv(), pid, 1)
	for sid in ship["parts"].keys():
		if not keep.has(sid):
			var orphan = str(ship["parts"][sid])
			if orphan != "" and not keep.values().has(orphan):
				pass
	ship["parts"] = keep
	ship["frame"] = frame_id
	_recompute_hull()
	var bench = str(Catalog.part(frame_id).get("bench", ""))
	if bench != "" and not state["world"]["benches"].has(bench):
		state["world"]["benches"].append(bench)
		notify("Hangar bench unlocked: " + bench)
	state_changed.emit()
	return true

func _recompute_hull() -> void:
	var f = frame_def()
	var mx = float(f.get("hull", 200))
	state["ship"]["hull_max"] = mx
	state["ship"]["hull"] = minf(float(state["ship"].get("hull", mx)), mx)
	var cap = 0.0
	for sid in state["ship"]["parts"].keys():
		var p = Catalog.part(str(state["ship"]["parts"][sid]))
		cap += float(p.get("capacity", 0.0))
	if cap <= 0.0:
		cap = 40.0
	state["ship"]["capacitor"] = minf(float(state["ship"].get("capacitor", cap)), cap)

func aggregated() -> Dictionary:
	var f = frame_def()
	var agg = {
		"mass": float(f.get("mass", 1000)),
		"thrust": 0.0,
		"fuel_factor": 0.0,
		"wear_factor": 0.0,
		"engine_count": 0,
		"power": 0.0,
		"fuel_draw": 0.0,
		"cap_max": 0.0,
		"cap_regen": 0.0,
		"cap_waste": 1.0,
		"throughput": 0.5,
		"loss": 0.2,
		"shield_max": 0.0,
		"shield_regen": 0.0,
		"scan_range": 800.0,
		"scan_ground": 30.0,
		"scan_reveal": 1.0,
		"cargo": int(f.get("cargo", 8)),
		"weapons": [],
		"ap": {"route": 0.7, "fuel_eff": 0.8, "emergency": 0.8, "docking": 0.6, "name": "bare bus"},
		"hull_max": float(f.get("hull", 200)),
		"bay": false,
	}
	var eng_fuel = 0.0
	var eng_wear = 0.0
	for sid in state["ship"]["parts"].keys():
		var pid = str(state["ship"]["parts"][sid])
		var p = Catalog.part(pid)
		if p.is_empty():
			continue
		var cond = float(state["ship"]["condition"].get(sid, 100.0)) / 100.0
		cond = clampf(cond, 0.05, 1.0)
		agg["mass"] += float(p.get("mass", 0))
		match str(p.get("socket", "")):
			"engine":
				agg["thrust"] += float(p.get("thrust", 0)) * cond
				eng_fuel += float(p.get("fuel_factor", 1.0))
				eng_wear += float(p.get("wear_factor", 1.0))
				agg["engine_count"] += 1
			"core":
				agg["power"] += float(p.get("output", 0)) * cond
				agg["fuel_draw"] += float(p.get("fuel_draw", 0))
			"capacitor":
				agg["cap_max"] += float(p.get("capacity", 0))
				agg["cap_regen"] += float(p.get("recharge", 0)) * cond
				agg["cap_waste"] = float(p.get("waste", 1.0))
			"conduit":
				agg["throughput"] = float(p.get("throughput", 1.0))
				agg["loss"] = float(p.get("loss", 0.1))
			"shield":
				agg["shield_max"] += float(p.get("capacity", 0))
				agg["shield_regen"] += float(p.get("regen", 0)) * cond
			"scanner":
				agg["scan_range"] = float(p.get("range", 800))
				agg["scan_ground"] = float(p.get("ground", 40))
				agg["scan_reveal"] = float(p.get("reveal", 1.0))
			"autopilot":
				agg["ap"] = {
					"route": float(p.get("route", 1)),
					"fuel_eff": float(p.get("fuel_eff", 1)),
					"emergency": float(p.get("emergency", 1)),
					"docking": float(p.get("docking", 1)),
					"name": str(p.get("name", "autopilot")),
				}
			"weapon":
				agg["weapons"].append({"socket": sid, "part": p, "cond": cond})
			"bay":
				agg["bay"] = true
	if agg["engine_count"] > 0:
		agg["fuel_factor"] = eng_fuel / float(agg["engine_count"])
		agg["wear_factor"] = eng_wear / float(agg["engine_count"])
	else:
		agg["fuel_factor"] = 1.0
		agg["wear_factor"] = 1.0
	return agg

func player_inv() -> Array:
	return state["player"]["inventory"]

func ship_cargo() -> Array:
	return state["ship"]["cargo"]

func count_item(container: Array, id: String) -> int:
	var n = 0
	for it in container:
		if str(it.get("id", "")) == id:
			n += int(it.get("count", 0))
	return n

func count_all(id: String) -> int:
	return count_item(player_inv(), id) + count_item(ship_cargo(), id) + _station_count(id)

func _station_count(id: String) -> int:
	var n = 0
	var containers: Dictionary = state["station"]["containers"]
	for k in containers.keys():
		n += count_item(containers[k].get("items", []), id)
	return n

func add_item(container: Array, id: String, count: int) -> void:
	if count <= 0 or id == "":
		return
	for it in container:
		if str(it.get("id", "")) == id:
			it["count"] = int(it["count"]) + count
			state_changed.emit()
			return
	container.append({"id": id, "count": count})
	state_changed.emit()

func take_item(container: Array, id: String, count: int) -> bool:
	var left = count
	for i in range(container.size() - 1, -1, -1):
		var it: Dictionary = container[i]
		if str(it.get("id", "")) != id:
			continue
		var have = int(it.get("count", 0))
		var use = mini(have, left)
		it["count"] = have - use
		left -= use
		if int(it["count"]) <= 0:
			container.remove_at(i)
		if left <= 0:
			state_changed.emit()
			return true
	return false

func take_anywhere(id: String, count: int) -> bool:
	if count_all(id) < count:
		return false
	var left = count
	for container in [player_inv(), ship_cargo()]:
		var have = count_item(container, id)
		var use = mini(have, left)
		if use > 0:
			take_item(container, id, use)
			left -= use
	if left > 0:
		var containers: Dictionary = state["station"]["containers"]
		for k in containers.keys():
			var items: Array = containers[k]["items"]
			var have2 = count_item(items, id)
			var use2 = mini(have2, left)
			if use2 > 0:
				take_item(items, id, use2)
				left -= use2
	return left <= 0

func give_anywhere(id: String, count: int) -> void:
	add_item(player_inv(), id, count)

func add_xp(who: Dictionary, skill: String, amount: float) -> void:
	if not who.has("skills"):
		return
	if not who["skills"].has(skill):
		who["skills"][skill] = {"rank": 0, "xp": 0.0}
	var s: Dictionary = who["skills"][skill]
	var rank = int(s["rank"])
	var gain = amount * economy_mult()
	if rank >= Catalog.MASTER_RANK:
		gain *= 0.55 * pow(0.85, float(rank - Catalog.MASTER_RANK))
	s["xp"] = float(s["xp"]) + gain
	while float(s["xp"]) >= Catalog.xp_to_next(int(s["rank"])):
		s["xp"] = float(s["xp"]) - Catalog.xp_to_next(int(s["rank"]))
		s["rank"] = int(s["rank"]) + 1
		if int(s["rank"]) == Catalog.MASTER_RANK:
			notify("%s reached Master %s." % [str(who.get("name", "Keel")), skill])
	state_changed.emit()

func skill_rank(who: Dictionary, skill: String) -> int:
	if not who.has("skills"):
		return 0
	return int(who.get("skills", {}).get(skill, {}).get("rank", 0))

func skill_bonus_of(who: Dictionary, skill: String) -> float:
	return Catalog.skill_bonus(skill_rank(who, skill))

func active_pilot() -> Dictionary:
	for c in state.get("crew", []):
		if bool(c.get("aboard", false)) and bool(c.get("alive", true)) and str(c.get("role", "")) == "pilot" and bool(c.get("on_duty", true)):
			return c
	return state["player"]

func crew_by_role(role: String) -> Dictionary:
	for c in state.get("crew", []):
		if bool(c.get("alive", true)) and bool(c.get("aboard", false)) and str(c.get("role", "")) == role:
			return c
	return {}

func hire(entry: Dictionary) -> bool:
	for c in state["crew"]:
		if str(c.get("id", "")) == str(entry["id"]):
			notify("Already on the roster.")
			return false
	if int(state["player"]["credits"]) < int(entry["wage"]) * 2:
		notify("Not enough credits for the retainer.")
		return false
	state["player"]["credits"] = int(state["player"]["credits"]) - int(entry["wage"]) * 2
	var skills = {}
	for s in Catalog.SKILLS:
		skills[s] = {"rank": 0, "xp": 0.0}
	for k in entry.get("skills", {}).keys():
		skills[k] = {"rank": int(entry["skills"][k]), "xp": 0.0}
	state["crew"].append({
		"id": entry["id"],
		"name": entry["name"],
		"role": entry["role"],
		"wage": int(entry["wage"]),
		"skills": skills,
		"morale": 72.0,
		"health": 100.0,
		"alive": true,
		"aboard": true,
		"on_duty": true,
	})
	notify(str(entry["name"]) + " signed on.")
	state_changed.emit()
	return true

func hire_tally() -> void:
	if not take_anywhere("crew_tally", 1) and count_all("crew_tally") < 1:
		# crafted item id is the recipe output; if present consume
		pass
	var skills = {}
	for s in Catalog.SKILLS:
		skills[s] = {"rank": 1, "xp": 0.0}
	skills["engineering"] = {"rank": 3, "xp": 0.0}
	skills["fabrication"] = {"rank": 2, "xp": 0.0}
	state["crew"].append({
		"id": "tally",
		"name": "Tally",
		"role": "engineer",
		"wage": 0,
		"skills": skills,
		"morale": 100.0,
		"health": 80.0,
		"alive": true,
		"aboard": true,
		"on_duty": true,
		"droid": true,
	})
	notify("Tally clicks awake. Master is a long way up. It will get there by working.")

func tick_needs(dt: float) -> void:
	var p: Dictionary = state["player"]
	var hunger_rate = 0.35
	hunger_rate *= 1.0 - 0.5 * minf(skill_bonus_of(p, "survival"), 0.5)
	p["hunger"] = clampf(float(p["hunger"]) + dt * hunger_rate, 0.0, 100.0)
	if float(p["hunger"]) >= 100.0:
		p["health"] = float(p["health"]) - dt * 2.0
	var st: Dictionary = state["station"]
	var last = float(st.get("last_tick_unix", SimClock.now()))
	var elapsed = SimClock.now() - last
	if elapsed < 0.0:
		elapsed = dt
	if elapsed > 86400.0 * 14.0:
		elapsed = 86400.0 * 14.0
	st["last_tick_unix"] = SimClock.now()
	_simulate_station(elapsed)
	_simulate_crew(elapsed)
	if not state["ship"]["transit"].is_empty():
		_advance_transit_clock()

func _simulate_station(elapsed: float) -> void:
	var st: Dictionary = state["station"]
	var hours = elapsed / 3600.0
	var crew_n = 1
	for c in state["crew"]:
		if bool(c.get("alive", true)):
			crew_n += 1
	st["food"] = float(st["food"]) + hours * 8.0 - hours * float(crew_n) * 1.5
	st["sewage"] = float(st["sewage"]) + hours * float(crew_n) * 3.0
	var process = 6.0 * hours * _grid_eff()
	st["sewage"] = clampf(float(st["sewage"]) - process, 0.0, 100.0)
	st["food"] = clampf(float(st["food"]), 0.0, 100.0)
	if float(st["food"]) > 12.0 and hours > 0.01:
		var made = int(hours * 2.0 * _grid_eff())
		if made > 0:
			add_item(st["containers"]["hab_locker"]["items"], "ration", made)
			st["food"] = float(st["food"]) - float(made)
	var draw = 46.0 + float(crew_n) * 4.0
	if "skiff_bench" in state["world"]["benches"]:
		draw += 10.0
	if "cutter_bench" in state["world"]["benches"]:
		draw += 18.0
	st["power_draw"] = draw
	for k in st["containers"].keys():
		var box: Dictionary = st["containers"][k]
		if bool(box.get("opened", false)) and float(box.get("respawn_at", 0)) > 0.0 and SimClock.now() >= float(box["respawn_at"]):
			box["opened"] = false
			box["items"] = []

func grid_eff() -> float:
	return _grid_eff()

func _grid_eff() -> float:
	var st: Dictionary = state["station"]
	var gen = float(st.get("power_gen", 100))
	var draw = maxf(float(st.get("power_draw", 1)), 1.0)
	if draw <= gen:
		return 1.0
	return gen / draw

func grid_strain() -> float:
	var st: Dictionary = state["station"]
	return float(st.get("power_draw", 0)) / maxf(float(st.get("power_gen", 1)), 1.0)

func _simulate_crew(elapsed: float) -> void:
	var hours = elapsed / 3600.0
	if hours <= 0.0:
		return
	var pay_steps = int(elapsed / 600.0)
	for c in state["crew"]:
		if not bool(c.get("alive", true)):
			continue
		var food_ok = float(state["station"]["food"]) > 8.0 or count_all("ration") > 0
		if food_ok and count_all("ration") > 0 and float(state["station"]["food"]) <= 8.0:
			take_anywhere("ration", 1)
		c["morale"] = clampf(float(c["morale"]) + (2.0 if food_ok else -10.0) * hours, 0.0, 100.0)
		if not bool(c.get("droid", false)) and pay_steps > 0:
			var due = int(c["wage"]) * pay_steps
			if int(state["player"]["credits"]) >= due:
				state["player"]["credits"] = int(state["player"]["credits"]) - due
			else:
				c["morale"] = float(c["morale"]) - 8.0 * pay_steps
		if float(c["morale"]) < 12.0 and not bool(c.get("droid", false)):
			c["alive"] = false
			c["quit"] = true
			notify(str(c["name"]) + " quit. Morale bottomed out.")
		if float(state["station"]["sewage"]) > 90.0:
			c["morale"] = float(c["morale"]) - hours * 2.0

func damage_player(amount: float) -> void:
	if god_mode:
		return
	var p: Dictionary = state["player"]
	var armor = float(p.get("armor", 0))
	var dmg = maxf(1.0, amount - armor * 0.35)
	p["health"] = float(p["health"]) - dmg
	if float(p["health"]) <= 0.0:
		p["health"] = 35.0
		p["hunger"] = minf(float(p["hunger"]), 40.0)
		notify("You black out and come to in the hab. The station medic rack billed you 40.")
		state["player"]["credits"] = maxi(0, int(state["player"]["credits"]) - 40)
		state["player"]["space"] = "station"
		state["player"]["local"] = [-16, 1.1, 28]
		state["player"]["mode"] = "foot"
		state["player"]["seated"] = false

func damage_ship(amount: float) -> void:
	if god_mode:
		return
	var ship: Dictionary = state["ship"]
	var shield = float(ship.get("shield", 0))
	var hit = amount
	if shield > 0.0:
		var absorbed = minf(shield, hit)
		ship["shield"] = shield - absorbed
		hit -= absorbed
	if hit > 0.0:
		ship["hull"] = float(ship.get("hull", 1)) - hit
		for sid in ship["parts"].keys():
			ship["condition"][sid] = maxf(5.0, float(ship["condition"].get(sid, 100.0)) - hit * 0.05)
	if float(ship["hull"]) <= 0.0:
		ship["hull"] = float(ship["hull_max"]) * 0.35
		ship["fuel"] = maxf(0.0, float(ship["fuel"]) - 15.0)
		notify("Hull failure. Emergency patch holds. A crew member may not.")
		_crew_casualty(0.55)

func _crew_casualty(chance: float) -> void:
	var rng = RandomNumberGenerator.new()
	rng.randomize()
	for c in state["crew"]:
		if not bool(c.get("alive", true)) or not bool(c.get("aboard", false)):
			continue
		if rng.randf() > chance:
			continue
		var medic = crew_by_role("medic")
		var save_chance = 0.25 + (skill_bonus_of(medic, "medicine") if not medic.is_empty() else 0.0)
		if not medic.is_empty() and count_all("medkit") > 0 and rng.randf() < save_chance:
			take_anywhere("medkit", 1)
			add_xp(medic, "medicine", 18)
			c["health"] = 40.0
			notify(str(medic["name"]) + " kept " + str(c["name"]) + " breathing.")
		else:
			c["alive"] = false
			c["health"] = 0.0
			notify(str(c["name"]) + " is gone.")
		break

func open_container(id: String) -> Array:
	var st: Dictionary = state["station"]["containers"]
	if not st.has(id):
		return []
	var box: Dictionary = st[id]
	if not bool(box.get("opened", false)):
		var rng = RandomNumberGenerator.new()
		rng.seed = hash(str(state.get("seed", 1)) + id + str(int(SimClock.now() / 30.0)))
		if str(box.get("loot", "")) != "":
			box["items"] = Catalog.loot_table(str(box["loot"]), scarcity(), rng)
		box["opened"] = true
		var scarce = clampf((float(scarcity()) - 1.0) / 99.0, 0.0, 1.0)
		box["respawn_at"] = SimClock.now() + lerpf(45.0, 420.0, scarce)
	return box["items"]

func ensure_shop(kind: String) -> Array:
	var shops: Dictionary = state["station"]["shops"]
	if not shops.has(kind):
		var rng = RandomNumberGenerator.new()
		rng.seed = int(state["station"].get("shop_seed", 1)) + hash(kind)
		shops[kind] = Catalog.shop_stock(kind, scarcity(), int(state["settings"].get("brand_availability", brand_availability)), rng)
	return shops[kind]

func refresh_shops() -> void:
	state["station"]["shops"] = {}

func try_craft(recipe_id: String) -> bool:
	var r: Dictionary = Catalog.recipes.get(recipe_id, {})
	if r.is_empty():
		return false
	var tier_have = bench_tier()
	if int(r["tier"]) > tier_have:
		notify("Bench tier too low. A larger frame unlocks the next fabricator.")
		return false
	for k in r["in"].keys():
		if count_all(str(k)) < int(r["in"][k]):
			notify("Missing " + Catalog.display_name(str(k)))
			return false
	for k in r["in"].keys():
		take_anywhere(str(k), int(r["in"][k]))
	var fab = skill_bonus_of(state["player"], "fabrication")
	var eng = crew_by_role("engineer")
	if not eng.is_empty():
		fab += skill_bonus_of(eng, "fabrication") * 0.5
		add_xp(eng, "engineering", 6)
		add_xp(eng, "fabrication", 8)
	add_xp(state["player"], "fabrication", 7)
	for k in r["out"].keys():
		var n = int(r["out"][k])
		if fab > 0.25 and str(k).ends_with("rounds") or str(k) in ["shells", "cells", "arrows"]:
			if fab > 0.2:
				n += 1
		if str(k) == "crew_tally":
			add_item(player_inv(), "crew_tally", n)
			hire_tally()
			take_anywhere("crew_tally", 1)
		else:
			add_item(player_inv(), str(k), n)
	flag("craft")
	notify("Crafted " + str(r["name"]))
	return true

func bench_tier() -> int:
	var t = 0
	if "station_services" in state["world"]["benches"]:
		t = 0
	if "skiff_bench" in state["world"]["benches"]:
		t = maxi(t, 1)
	if "cutter_bench" in state["world"]["benches"]:
		t = maxi(t, 2)
	return t

func buy(kind: String, index: int) -> bool:
	var stock = ensure_shop(kind)
	if index < 0 or index >= stock.size():
		return false
	var row: Dictionary = stock[index]
	var price = int(row["price"])
	var trade = skill_bonus_of(state["player"], "trading")
	price = maxi(1, int(round(float(price) * (1.0 - trade * 0.4))))
	if int(state["player"]["credits"]) < price:
		notify("Not enough credits.")
		return false
	if int(row["count"]) <= 0:
		notify("Out of stock.")
		return false
	if str(row["id"]) == "fuel_unit":
		state["ship"]["fuel"] = minf(float(state["ship"]["fuel_max"]), float(state["ship"]["fuel"]) + 5.0)
	else:
		add_item(player_inv(), str(row["id"]), 1)
	state["player"]["credits"] = int(state["player"]["credits"]) - price
	row["count"] = int(row["count"]) - 1
	add_xp(state["player"], "trading", 2)
	flag("kiosk")
	state_changed.emit()
	return true

func sell(item_id: String) -> bool:
	if count_item(player_inv(), item_id) <= 0:
		return false
	var price = int(Catalog.def_of(item_id).get("price", 1))
	if price <= 0:
		price = 1
	var trade = skill_bonus_of(state["player"], "trading")
	price = maxi(1, int(round(float(price) * 0.55 * (1.0 + trade))))
	take_item(player_inv(), item_id, 1)
	state["player"]["credits"] = int(state["player"]["credits"]) + price
	add_xp(state["player"], "trading", 2)
	flag("kiosk")
	notify("Sold for %d." % price)
	return true

func eat() -> void:
	if not take_anywhere("ration", 1):
		notify("No rations.")
		return
	state["player"]["hunger"] = maxf(0.0, float(state["player"]["hunger"]) - 28.0)
	notify("Ration down.")

func heal_self() -> void:
	if not take_anywhere("medkit", 1):
		notify("No trauma kit.")
		return
	var bonus = 1.0 + skill_bonus_of(state["player"], "medicine")
	var medic = crew_by_role("medic")
	if not medic.is_empty():
		bonus += skill_bonus_of(medic, "medicine")
		add_xp(medic, "medicine", 10)
	state["player"]["health"] = minf(float(state["player"]["health_max"]), float(state["player"]["health"]) + 40.0 * bonus)
	add_xp(state["player"], "medicine", 6)
	notify("Patched.")

func engage_transit(dest: String) -> bool:
	var ship: Dictionary = state["ship"]
	if not ship["transit"].is_empty():
		notify("Already on a route.")
		return false
	var agg = aggregated()
	var pilot = active_pilot()
	var pilot_bonus = skill_bonus_of(pilot, "piloting")
	var from_pos = Vector3(float(ship["pos"][0]), float(ship["pos"][1]), float(ship["pos"][2]))
	if str(ship["loc"]) == "hangar":
		from_pos = SimClock.station_pos(SimClock.now())
	var to_pos = _dest_pos(dest)
	var distance = from_pos.distance_to(to_pos)
	var preview = SimClock.transit_preview(distance, float(ship.get("urgency", 0.4)), agg["ap"], pilot_bonus, agg)
	if float(ship["fuel"]) < float(preview["fuel"]):
		notify("Not enough fuel for that urgency. Slow the route or buy fuel.")
		return false
	var dock = float(agg["ap"]["docking"]) * (1.0 + pilot_bonus)
	var rng = RandomNumberGenerator.new()
	rng.randomize()
	var miss = (1.0 / maxf(0.35, dock)) * 6.0
	var end_pos = to_pos
	if dest != "station":
		end_pos += Vector3(rng.randf_range(-miss, miss), 0, rng.randf_range(-miss, miss))
		var radial = end_pos.normalized()
		end_pos = radial * (SimClock.PLANET_R + 3.0)
	ship["transit"] = {
		"dest": dest,
		"start_unix": SimClock.now(),
		"duration": float(preview["duration"]),
		"fuel_cost": float(preview["fuel"]),
		"wear_cost": float(preview["wear"]),
		"from": [from_pos.x, from_pos.y, from_pos.z],
		"to": [end_pos.x, end_pos.y, end_pos.z],
		"loft": 400.0 if dest == "station" or str(ship["loc"]) == "surface" else 180.0,
		"fuel_at_start": float(ship["fuel"]),
		"applied": 0.0,
		"pilot_name": str(pilot.get("name", "Keel")),
		"pilot_id": str(pilot.get("id", "player")),
	}
	ship["loc"] = "transit"
	notify("Route locked. %s at the stick. %.0f s." % [str(pilot.get("name", "Keel")), float(preview["fuel"])])
	add_xp(pilot, "piloting", 4.0)
	state_changed.emit()
	return true

func cancel_transit() -> void:
	var ship: Dictionary = state["ship"]
	if ship["transit"].is_empty():
		return
	_apply_transit_progress(true)
	ship["transit"] = {}
	ship["loc"] = "space"
	notify("Autopilot cancelled. You have the stick.")

func _advance_transit_clock() -> void:
	_apply_transit_progress(false)

func _apply_transit_progress(finalize_partial: bool) -> void:
	var ship: Dictionary = state["ship"]
	var tr: Dictionary = ship.get("transit", {})
	if tr.is_empty():
		return
	var u = (SimClock.now() - float(tr["start_unix"])) / maxf(float(tr["duration"]), 0.1)
	u = clampf(u, 0.0, 1.0)
	var applied = float(tr.get("applied", 0.0))
	var du = u - applied
	if du > 0.0:
		ship["fuel"] = maxf(0.0, float(ship["fuel"]) - float(tr["fuel_cost"]) * du)
		for sid in ship["parts"].keys():
			var drop = float(tr["wear_cost"]) * du * 8.0
			ship["condition"][sid] = maxf(1.0, float(ship["condition"].get(sid, 100.0)) - drop)
		tr["applied"] = u
	var a = Vector3(tr["from"][0], tr["from"][1], tr["from"][2])
	var b = Vector3(tr["to"][0], tr["to"][1], tr["to"][2])
	var pos = SimClock.sample_arc(a, b, u, float(tr.get("loft", 200)))
	ship["pos"] = [pos.x, pos.y, pos.z]
	var vel = SimClock.arc_velocity(a, b, u, float(tr["duration"]), float(tr.get("loft", 200)))
	ship["vel"] = [vel.x, vel.y, vel.z]
	if u >= 1.0 or finalize_partial and false:
		_complete_transit()
	if u >= 1.0:
		_complete_transit()

func _complete_transit() -> void:
	var ship: Dictionary = state["ship"]
	var tr: Dictionary = ship.get("transit", {})
	if tr.is_empty():
		return
	var dest = str(tr.get("dest", "station"))
	var pilot = active_pilot()
	add_xp(pilot, "piloting", 12.0)
	ship["transit"] = {}
	if dest == "station":
		ship["loc"] = "hangar"
		ship["vel"] = [0, 0, 0]
		var dock = float(aggregated()["ap"]["docking"])
		if dock < 0.7:
			damage_ship(8.0)
			notify("Hard dock. The Voss habit of arriving hot costs paint.")
		else:
			notify("Docked at Kite Anchorage. Elevator is bringing the ship up.")
		state["station"]["elevator"] = 1.0
	else:
		ship["loc"] = "surface"
		var p = Vector3(ship["pos"][0], ship["pos"][1], ship["pos"][2])
		if p.length() < 1.0:
			p = Vector3(SimClock.PLANET_R + 2.0, 0, 0)
		p = p.normalized() * (SimClock.PLANET_R + 1.6)
		ship["pos"] = [p.x, p.y, p.z]
		ship["vel"] = [0, 0, 0]
		notify("Down on Vesper. Gear is holding.")
		flag("land")
	state_changed.emit()

func transit_u() -> float:
	var tr: Dictionary = state["ship"].get("transit", {})
	if tr.is_empty():
		return -1.0
	var u = (SimClock.now() - float(tr["start_unix"])) / maxf(float(tr["duration"]), 0.1)
	return clampf(u, 0.0, 1.0)

func _dest_pos(dest: String) -> Vector3:
	if dest == "station":
		var eta = 90.0
		return SimClock.station_pos(SimClock.now() + eta)
	if dest.begins_with("site:"):
		var idx = int(dest.split(":")[1])
		return site_pos(idx) 
	return site_pos(0)

func site_pos(index: int) -> Vector3:
	var rng = RandomNumberGenerator.new()
	rng.seed = int(state["planet"].get("seed", 1)) + index * 17
	var theta = rng.randf() * TAU
	var y = rng.randf_range(-0.35, 0.55)
	var v = Vector3(cos(theta) * sqrt(maxf(1.0 - y * y, 0.05)), y, sin(theta) * sqrt(maxf(1.0 - y * y, 0.05)))
	return v.normalized() * (SimClock.PLANET_R + 2.0)

func site_name(index: int) -> String:
	var names = ["Cinder Shelf", "Moss Cut", "Quiet Basin", "Red Spill", "Teak Fold", "Copper Run", "Ash Verge", "Long Shade"]
	return names[index % names.size()]

func resolve_offline() -> void:
	var saved = float(state.get("saved_unix", SimClock.now()))
	var elapsed = SimClock.now() - saved
	if elapsed < 1.0:
		return
	if elapsed > 86400.0 * 30.0:
		elapsed = 86400.0 * 30.0
	_simulate_station(elapsed)
	_simulate_crew(elapsed)
	if not state["ship"]["transit"].is_empty():
		_apply_transit_progress(false)
		if transit_u() >= 1.0:
			_complete_transit()
		else:
			notify("While you were away the route kept burning. Fuel and wear moved with the clock.")
	else:
		notify("Kite kept orbit. %.0f minutes passed outside the game." % (elapsed / 60.0))

func mark_cheat() -> void:
	cheat_flag = true
	state["cheat_flag"] = true
	notify("Cheat flag set. This save cannot overwrite a clean slot.")

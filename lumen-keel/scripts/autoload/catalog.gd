extends Node
## Brands, parts, items, recipes, creatures. Original Harrow Compact setting.

const SIZE_RANK = {"light": 1, "medium": 2, "heavy": 3}
const SKILLS = ["piloting", "gunnery", "engineering", "scanning", "medicine", "survival", "trading", "fabrication"]
const MASTER_RANK = 10

const BRANDS = {
	"voss": {
		"id": "voss",
		"name": "Voss Ordnance",
		"focus": "combat",
		"blurb": "Charcoal plate, arterial red, oversized bells. Loud on purpose.",
		"hull": Color(0.16, 0.14, 0.13),
		"trim": Color(0.76, 0.16, 0.12),
		"glow": Color(1.0, 0.42, 0.18),
		"logo": "chevron",
	},
	"lumen": {
		"id": "lumen",
		"name": "Lumen Survey",
		"focus": "discovery",
		"blurb": "Bone laminate and oxidized teal. Built to come home.",
		"hull": Color(0.78, 0.75, 0.66),
		"trim": Color(0.16, 0.52, 0.50),
		"glow": Color(0.55, 0.95, 0.9),
		"logo": "ring",
	},
}

var parts = {}
var items = {}
var recipes = {}

func _ready() -> void:
	_build_parts()
	_build_items()
	_build_recipes()

func size_rank(sz: String) -> int:
	return int(SIZE_RANK.get(sz, 1))

func brand(id: String) -> Dictionary:
	return BRANDS.get(id, BRANDS["lumen"])

func part(id: String) -> Dictionary:
	return parts.get(id, {})

func item(id: String) -> Dictionary:
	return items.get(id, {})

func def_of(id: String) -> Dictionary:
	if parts.has(id):
		return parts[id]
	return items.get(id, {})

func display_name(id: String) -> String:
	var d = def_of(id)
	return str(d.get("name", id))

func can_fit(frame_id: String, part_id: String, socket: Dictionary) -> bool:
	var frame = part(frame_id)
	var p = part(part_id)
	if frame.is_empty() or p.is_empty():
		return false
	if str(p.get("socket", "")) != str(socket.get("type", "")):
		return false
	if size_rank(str(p.get("size", "light"))) > size_rank(str(socket.get("size", "light"))):
		return false
	if size_rank(str(p.get("size", "light"))) > size_rank(str(frame.get("max_size", "light"))):
		return false
	if bool(p.get("internal_only", false)) and not bool(socket.get("internal", false)):
		return false
	if bool(p.get("external_only", false)) and bool(socket.get("internal", false)):
		return false
	return true

func placement_ok(item_id: String, surface: String, socket: Dictionary = {}) -> bool:
	var d = def_of(item_id)
	var profile: Dictionary = d.get("placement", {})
	var surfaces: Array = profile.get("surfaces", [])
	if not surfaces.has(surface):
		return false
	if surface == "socket":
		return can_fit(Game.ship_frame_id(), item_id, socket) if is_instance_valid(Game) else true
	return true

func skill_bonus(rank: int) -> float:
	if rank <= 0:
		return 0.0
	if rank <= MASTER_RANK:
		return float(rank) * 0.04
	var bonus = 0.40
	var step = 0.02
	for _i in range(rank - MASTER_RANK):
		bonus += step
		step *= 0.72
	return bonus

func xp_to_next(rank: int) -> float:
	var base = 40.0 + float(rank) * 18.0
	if rank >= MASTER_RANK:
		base *= pow(1.45, float(rank - MASTER_RANK + 1))
	return base

func perk_lines(skill: String, rank: int) -> PackedStringArray:
	var lines: PackedStringArray = []
	var tree: Dictionary = {
		"piloting": [["Throttle discipline", 3], ["Soft dock", 6], ["High-g brace", 10]],
		"engineering": [["Field repair", 3], ["Power routing", 6], ["Wear shedding", 10]],
		"scanning": [["Long gain", 3], ["Rare signature", 6], ["Fauna census", 10]],
		"medicine": [["Steady hands", 3], ["Trauma kit", 6], ["Revive protocol", 10]],
		"gunnery": [["Recoil settle", 3], ["Punch through", 6], ["Fast mag", 10]],
		"survival": [["Clean skin", 3], ["Slow hunger", 6], ["Long EVA", 10]],
		"trading": [["Counter price", 3], ["Stock sense", 6], ["Contract cut", 10]],
		"fabrication": [["Offcut save", 3], ["Bench speed", 6], ["Extra round", 10]],
	}
	for row in tree.get(skill, []):
		var need = int(row[1])
		var mark = "●" if rank >= need else "○"
		lines.append("%s %s (rank %d)" % [mark, row[0], need])
	if rank >= MASTER_RANK:
		lines.append("Title: Master  +%d post-master" % (rank - MASTER_RANK))
	return lines

func _hp(id: String, type: String, size: String, pos: Vector3, internal: bool) -> Dictionary:
	return {"id": id, "type": type, "size": size, "pos": pos, "internal": internal}

func _build_parts() -> void:
	var light_hp = [
		_hp("eng_l", "engine", "light", Vector3(-1.2, 0.12, 2.15), false),
		_hp("eng_r", "engine", "light", Vector3(1.2, 0.12, 2.15), false),
		_hp("core", "core", "light", Vector3(0, 0.32, 0.85), true),
		_hp("cap", "capacitor", "light", Vector3(0, 0.18, 0.15), true),
		_hp("conduit", "conduit", "light", Vector3(0, -0.05, 0.45), true),
		_hp("wpn_d", "weapon", "light", Vector3(0, 0.82, -0.15), false),
		_hp("wpn_c", "weapon", "light", Vector3(0, -0.28, -1.75), false),
		_hp("ap", "autopilot", "light", Vector3(0.35, 0.48, -0.55), true),
		_hp("scan", "scanner", "light", Vector3(0, 0.62, -1.9), false),
		_hp("shield", "shield", "light", Vector3(-0.45, 0.28, 0.25), true),
		_hp("bay", "bay", "light", Vector3(0, -0.15, 1.15), true),
	]
	var med_hp = [
		_hp("eng_l", "engine", "medium", Vector3(-1.55, 0.15, 2.8), false),
		_hp("eng_r", "engine", "medium", Vector3(1.55, 0.15, 2.8), false),
		_hp("eng_c", "engine", "light", Vector3(0, 0.2, 3.05), false),
		_hp("core", "core", "medium", Vector3(0, 0.4, 1.0), true),
		_hp("cap", "capacitor", "medium", Vector3(0.4, 0.2, 0.2), true),
		_hp("conduit", "conduit", "medium", Vector3(0, 0.0, 0.5), true),
		_hp("wpn_d", "weapon", "medium", Vector3(0, 1.05, -0.2), false),
		_hp("wpn_l", "weapon", "light", Vector3(-1.3, 0.15, -0.4), false),
		_hp("wpn_r", "weapon", "light", Vector3(1.3, 0.15, -0.4), false),
		_hp("ap", "autopilot", "light", Vector3(0.4, 0.55, -0.7), true),
		_hp("scan", "scanner", "medium", Vector3(0, 0.7, -2.4), false),
		_hp("shield", "shield", "medium", Vector3(-0.5, 0.3, 0.3), true),
		_hp("bay", "bay", "medium", Vector3(0, -0.2, 1.4), true),
	]
	_add_part({
		"id": "lumen_frame_light", "name": "Lumen Skiff", "brand": "lumen", "socket": "frame",
		"size": "light", "max_size": "light", "mass": 860, "cargo": 14, "hull": 220,
		"tier": 1, "price": 0, "mesh": "frame_lumen_light", "hardpoints": light_hp,
		"class_name": "skiff", "bench": "skiff_bench",
		"placement": {"surfaces": ["socket"], "socket_types": ["frame"]},
	})
	_add_part({
		"id": "voss_frame_light", "name": "Voss Splinter", "brand": "voss", "socket": "frame",
		"size": "light", "max_size": "light", "mass": 980, "cargo": 8, "hull": 280,
		"tier": 1, "price": 1400, "mesh": "frame_voss_light", "hardpoints": light_hp,
		"class_name": "skiff", "bench": "skiff_bench",
		"placement": {"surfaces": ["socket"], "socket_types": ["frame"]},
	})
	_add_part({
		"id": "lumen_frame_medium", "name": "Lumen Cutter", "brand": "lumen", "socket": "frame",
		"size": "medium", "max_size": "medium", "mass": 2100, "cargo": 36, "hull": 460,
		"tier": 2, "price": 4800, "mesh": "frame_lumen_medium", "hardpoints": med_hp,
		"class_name": "cutter", "bench": "cutter_bench",
		"placement": {"surfaces": ["socket"], "socket_types": ["frame"]},
	})
	_add_part({
		"id": "voss_frame_medium", "name": "Voss Harrier", "brand": "voss", "socket": "frame",
		"size": "medium", "max_size": "medium", "mass": 2400, "cargo": 22, "hull": 620,
		"tier": 2, "price": 5200, "mesh": "frame_voss_medium", "hardpoints": med_hp,
		"class_name": "cutter", "bench": "cutter_bench",
		"placement": {"surfaces": ["socket"], "socket_types": ["frame"]},
	})
	_engine("lumen_engine_light", "Lumen Driftfan", "lumen", "light", 1, 26000, 0.72, 0.68, 900)
	_engine("voss_engine_light", "Voss Bell-4", "voss", "light", 1, 46000, 1.35, 1.32, 1100)
	_engine("lumen_engine_medium", "Lumen Keelwell", "lumen", "medium", 2, 54000, 0.78, 0.7, 2600)
	_engine("voss_engine_medium", "Voss Ram-9", "voss", "medium", 2, 82000, 1.42, 1.4, 3100)
	_simple_part("lumen_cap_light", "Lumen Wellcap", "lumen", "capacitor", "light", 1, 640, {"capacity": 80, "recharge": 18, "waste": 0.85})
	_simple_part("voss_cap_light", "Voss Spikebank", "voss", "capacitor", "light", 1, 700, {"capacity": 140, "recharge": 10, "waste": 1.35})
	_simple_part("lumen_cap_medium", "Lumen Reservoir", "lumen", "capacitor", "medium", 2, 1600, {"capacity": 180, "recharge": 28, "waste": 0.8})
	_simple_part("voss_cap_medium", "Voss Surgehold", "voss", "capacitor", "medium", 2, 1750, {"capacity": 260, "recharge": 16, "waste": 1.3})
	_simple_part("lumen_core_light", "Lumen Hearth", "lumen", "core", "light", 1, 800, {"output": 42, "fuel_draw": 0.015})
	_simple_part("voss_core_light", "Voss Furnace", "voss", "core", "light", 1, 900, {"output": 70, "fuel_draw": 0.03})
	_simple_part("lumen_core_medium", "Lumen Kiln", "lumen", "core", "medium", 2, 1900, {"output": 90, "fuel_draw": 0.02})
	_simple_part("voss_core_medium", "Voss Crucible", "voss", "core", "medium", 2, 2100, {"output": 130, "fuel_draw": 0.045})
	_simple_part("lumen_conduit", "Lumen Thread", "lumen", "conduit", "light", 1, 420, {"throughput": 0.92, "loss": 0.08})
	_simple_part("voss_conduit", "Voss Bus", "voss", "conduit", "light", 1, 480, {"throughput": 1.25, "loss": 0.22})
	_simple_part("lumen_conduit_m", "Lumen Loom", "lumen", "conduit", "medium", 2, 980, {"throughput": 1.05, "loss": 0.07})
	_simple_part("voss_conduit_m", "Voss Arterial", "voss", "conduit", "medium", 2, 1100, {"throughput": 1.45, "loss": 0.2})
	_simple_part("lumen_ap", "Lumen Thread AP", "lumen", "autopilot", "light", 1, 750, {"route": 0.86, "fuel_eff": 1.38, "emergency": 1.28, "docking": 1.32})
	_simple_part("voss_ap", "Voss Hardline", "voss", "autopilot", "light", 1, 750, {"route": 1.28, "fuel_eff": 0.74, "emergency": 0.92, "docking": 0.58})
	_weapon_part("lumen_laser", "Lumen Needle", "lumen", "light", 1, 880, {"kind": "laser", "dps": 16, "heat": 8, "ammo": "cells"})
	_weapon_part("voss_cannon", "Voss Spur", "voss", "light", 1, 920, {"kind": "kinetic", "damage": 22, "rate": 3.2, "ammo": "shells"})
	_weapon_part("lumen_laser_m", "Lumen Lance", "lumen", "medium", 2, 2100, {"kind": "laser", "dps": 34, "heat": 12, "ammo": "cells"})
	_weapon_part("voss_cannon_m", "Voss Maul", "voss", "medium", 2, 2400, {"kind": "kinetic", "damage": 48, "rate": 1.6, "ammo": "shells"})
	_simple_part("lumen_scan", "Lumen Iris", "lumen", "scanner", "light", 1, 600, {"range": 2200, "ground": 80, "reveal": 1.2})
	_simple_part("voss_scan", "Voss Ping", "voss", "scanner", "light", 1, 540, {"range": 1400, "ground": 50, "reveal": 0.8})
	_simple_part("lumen_shield", "Lumen Veil", "lumen", "shield", "light", 1, 1000, {"capacity": 80, "regen": 6}, true, false)
	_simple_part("voss_shield", "Voss Buckler", "voss", "shield", "light", 1, 1100, {"capacity": 140, "regen": 3}, true, false)
	_simple_part("lumen_shield_m", "Lumen Canopy", "lumen", "shield", "medium", 2, 2200, {"capacity": 180, "regen": 10}, true, false)
	_simple_part("voss_shield_m", "Voss Aegis", "voss", "shield", "medium", 2, 2400, {"capacity": 280, "regen": 5}, true, false)
	_simple_part("vehicle_bay", "Skiff Vehicle Bay", "lumen", "bay", "light", 1, 500, {"buggy": true}, true, false)
	_simple_part("vehicle_bay_m", "Cutter Vehicle Bay", "lumen", "bay", "medium", 2, 900, {"buggy": true}, true, false)

func _engine(id: String, name: String, brand_id: String, size: String, tier: int, thrust: float, fuel_f: float, wear_f: float, price: int) -> void:
	_add_part({
		"id": id, "name": name, "brand": brand_id, "socket": "engine", "size": size,
		"mass": 180 if size == "light" else 340, "tier": tier, "price": price,
		"mesh": "engine_" + brand_id, "external_only": true,
		"thrust": thrust, "fuel_factor": fuel_f, "wear_factor": wear_f,
		"placement": {"surfaces": ["socket"], "socket_types": ["engine"]},
	})

func _simple_part(id: String, name: String, brand_id: String, socket: String, size: String, tier: int, price: int, stats: Dictionary, internal_only: bool = false, external_only: bool = false) -> void:
	var d = {
		"id": id, "name": name, "brand": brand_id, "socket": socket, "size": size,
		"mass": 40, "tier": tier, "price": price, "mesh": socket + "_" + brand_id,
		"internal_only": internal_only, "external_only": external_only,
		"placement": {"surfaces": ["socket"], "socket_types": [socket]},
	}
	for k in stats.keys():
		d[k] = stats[k]
	_add_part(d)

func _weapon_part(id: String, name: String, brand_id: String, size: String, tier: int, price: int, stats: Dictionary) -> void:
	var d = {
		"id": id, "name": name, "brand": brand_id, "socket": "weapon", "size": size,
		"mass": 70, "tier": tier, "price": price, "mesh": "weapon_" + brand_id,
		"external_only": true,
		"placement": {"surfaces": ["socket"], "socket_types": ["weapon"]},
	}
	for k in stats.keys():
		d[k] = stats[k]
	_add_part(d)

func _add_part(d: Dictionary) -> void:
	parts[d["id"]] = d
	items[d["id"]] = d

func _build_items() -> void:
	_ground_weapon("pistol_kinetic", "Keel Pistol", "kinetic", "pistol_rounds", 18, 0.22, 4.5, 1.1, "pistol", 0)
	_ground_weapon("carbine_kinetic", "Hab Carbine", "kinetic", "rifle_rounds", 30, 0.11, 7.0, 1.6, "carbine", 380)
	_ground_weapon("rifle_kinetic", "Shelf Rifle", "kinetic", "rifle_rounds", 24, 0.16, 12.0, 2.4, "rifle", 640)
	_ground_weapon("sniper_kinetic", "Ridge Glass", "kinetic", "rifle_rounds", 6, 0.9, 28.0, 4.2, "sniper", 1100)
	_ground_weapon("laser_pistol", "Thread Spark", "laser", "ground_cells", 20, 0.18, 6.0, 0.8, "laser", 520)
	_ground_weapon("plasma_rifle", "Cinder Spit", "plasma", "ground_cells", 12, 0.4, 16.0, 2.8, "plasma", 980)
	_ground_weapon("bow_filament", "Filament Bow", "bow", "arrows", 1, 0.55, 14.0, 1.2, "bow", 220)
	_ground_weapon("knife_keel", "Keel Knife", "melee", "none", 0, 0.45, 18.0, 0.4, "knife", 40)
	_ground_weapon("rocket_tube", "Pit Tube", "rocket", "rockets", 1, 1.1, 55.0, 6.0, "rocket", 1600)
	_item("undersuit", "Survey Undersuit", "armor", 0, {"armor": 2, "slot": "armor", "placement": {"surfaces": []}})
	_item("vest_scout", "Lumen Scout Vest", "armor", 260, {"armor": 10, "slot": "armor"})
	_item("vest_voss", "Voss Plate Vest", "armor", 340, {"armor": 18, "slot": "armor"})
	_item("pistol_rounds", "Pistol Rounds", "ammo_ground", 8, {"pack": 24})
	_item("rifle_rounds", "Rifle Rounds", "ammo_ground", 14, {"pack": 20})
	_item("ground_cells", "Ground Cells", "ammo_ground", 12, {"pack": 16})
	_item("arrows", "Filament Arrows", "ammo_ground", 6, {"pack": 12})
	_item("rockets", "Pit Rockets", "ammo_ground", 40, {"pack": 2})
	_item("shells", "Spur Shells", "ammo_ship", 18, {"pack": 20})
	_item("cells", "Needle Cells", "ammo_ship", 16, {"pack": 16})
	_item("missiles", "Hardline Missiles", "ammo_ship", 60, {"pack": 2})
	_item("ration", "Algae Ration", "food", 12, {"hunger": 28})
	_item("medkit", "Trauma Kit", "med", 35, {"heal": 40})
	_item("iron_ore", "Vesper Iron", "material", 10, {"tier": 1})
	_item("copper_ore", "Vesper Copper", "material", 14, {"tier": 1})
	_item("cinder_quartz", "Cinder Quartz", "material", 80, {"tier": 2, "rare": true})
	_item("moss_fiber", "Filament Moss", "material", 8, {"tier": 1})
	_item("rat_bone", "Pipewolf Bone", "material", 6, {"tier": 1})
	_item("rat_enzyme", "Pipewolf Enzyme", "material", 22, {"tier": 1})
	_item("hide", "Grazer Hide", "material", 18, {"tier": 1})
	_item("veil_teak", "Veil Teak", "material", 70, {"tier": 2, "rare": true})
	_item("painting_kite", "Print: Kite Over Vesper", "decor", 30, {"placement": {"surfaces": ["wall"]}})
	_item("plant_moss", "Pot of Filament", "decor", 20, {"placement": {"surfaces": ["floor", "table"]}})
	_item("seismic_spike", "Seismic Spike", "tool", 90, {"placement": {"surfaces": ["ground"]}})
	_item("buggy", "Shelf Buggy", "vehicle", 450, {"placement": {"surfaces": ["ground"]}})
	_item("skiff_bench", "Skiff Fabricator", "bench", 0, {"tier": 1, "placement": {"surfaces": ["floor"]}})
	_item("cutter_bench", "Cutter Fabricator", "bench", 1500, {"tier": 2, "placement": {"surfaces": ["floor"]}})
	_item("scrap", "Loose Scrap", "material", 4, {})
	_item("credits_chit", "Loose Chit", "cash", 15, {"credits": 15})

func _ground_weapon(id: String, name: String, kind: String, ammo: String, mag: int, rate: float, damage: float, mass: float, mesh: String, price: int) -> void:
	_item(id, name, "weapon_ground", price, {
		"kind": kind, "ammo": ammo, "mag": mag, "rate": rate, "damage": damage,
		"mass": mass, "mesh": mesh, "slot": "weapon",
	})

func _item(id: String, name: String, category: String, price: int, extra: Dictionary) -> void:
	var d = {"id": id, "name": name, "category": category, "price": price, "mass": float(extra.get("mass", 0.4))}
	for k in extra.keys():
		d[k] = extra[k]
	if not d.has("placement"):
		d["placement"] = {"surfaces": []}
	items[id] = d

func _build_recipes() -> void:
	_recipe("make_pistol_rounds", "Pistol rounds", 0, {"iron_ore": 1, "scrap": 1}, {"pistol_rounds": 2}, "station")
	_recipe("make_rifle_rounds", "Rifle rounds", 0, {"iron_ore": 1, "copper_ore": 1}, {"rifle_rounds": 2}, "station")
	_recipe("make_cells", "Ground cells", 0, {"copper_ore": 1, "moss_fiber": 1}, {"ground_cells": 2}, "station")
	_recipe("make_ration", "Press ration", 0, {"moss_fiber": 2}, {"ration": 2}, "station")
	_recipe("make_medkit", "Pack trauma kit", 0, {"rat_enzyme": 1, "moss_fiber": 1}, {"medkit": 1}, "station")
	_recipe("make_shells", "Spur shells", 1, {"iron_ore": 2, "copper_ore": 1}, {"shells": 2}, "skiff")
	_recipe("make_ship_cells", "Needle cells", 1, {"copper_ore": 2, "cinder_quartz": 1}, {"cells": 2}, "skiff")
	_recipe("make_missiles", "Hardline missile", 2, {"iron_ore": 3, "cinder_quartz": 1, "rat_enzyme": 1}, {"missiles": 1}, "cutter")
	_recipe("make_arrows", "Filament arrows", 0, {"moss_fiber": 1, "scrap": 1}, {"arrows": 2}, "station")
	_recipe("make_spike", "Seismic spike", 1, {"iron_ore": 2, "copper_ore": 1}, {"seismic_spike": 1}, "skiff")
	_recipe("make_lumen_engine", "Lumen Driftfan", 1, {"iron_ore": 6, "copper_ore": 4, "moss_fiber": 2}, {"lumen_engine_light": 1}, "skiff")
	_recipe("make_voss_engine", "Voss Bell-4", 1, {"iron_ore": 8, "copper_ore": 3, "rat_bone": 2}, {"voss_engine_light": 1}, "skiff")
	_recipe("make_lumen_cap", "Lumen Wellcap", 1, {"copper_ore": 4, "cinder_quartz": 1}, {"lumen_cap_light": 1}, "skiff")
	_recipe("make_voss_cap", "Voss Spikebank", 1, {"copper_ore": 3, "iron_ore": 3, "cinder_quartz": 1}, {"voss_cap_light": 1}, "skiff")
	_recipe("make_lumen_ap", "Lumen Thread AP", 1, {"copper_ore": 3, "veil_teak": 1, "moss_fiber": 2}, {"lumen_ap": 1}, "skiff")
	_recipe("make_voss_ap", "Voss Hardline", 1, {"copper_ore": 3, "iron_ore": 2, "rat_enzyme": 2}, {"voss_ap": 1}, "skiff")
	_recipe("make_lumen_laser", "Lumen Needle", 1, {"copper_ore": 4, "cinder_quartz": 1, "iron_ore": 2}, {"lumen_laser": 1}, "skiff")
	_recipe("make_voss_cannon", "Voss Spur", 1, {"iron_ore": 6, "copper_ore": 2}, {"voss_cannon": 1}, "skiff")
	_recipe("make_shield", "Lumen Veil", 1, {"copper_ore": 4, "hide": 2, "moss_fiber": 2}, {"lumen_shield": 1}, "skiff")
	_recipe("make_voss_shield", "Voss Buckler", 2, {"iron_ore": 6, "hide": 2, "cinder_quartz": 1}, {"voss_shield": 1}, "cutter")
	_recipe("make_med_engine_l", "Lumen Keelwell", 2, {"lumen_engine_light": 1, "iron_ore": 8, "cinder_quartz": 2, "veil_teak": 1}, {"lumen_engine_medium": 1}, "cutter")
	_recipe("make_med_engine_v", "Voss Ram-9", 2, {"voss_engine_light": 1, "iron_ore": 10, "cinder_quartz": 2}, {"voss_engine_medium": 1}, "cutter")
	_recipe("make_astromech", "Tally astromech", 1, {"copper_ore": 6, "iron_ore": 4, "rat_enzyme": 1, "veil_teak": 1}, {"crew_tally": 1}, "skiff")

func _recipe(id: String, name: String, tier: int, inputs: Dictionary, outputs: Dictionary, bench: String) -> void:
	recipes[id] = {"id": id, "name": name, "tier": tier, "in": inputs, "out": outputs, "bench": bench}

func loot_table(table: String, scarcity: int, rng: RandomNumberGenerator) -> Array:
	var t = clampf((float(scarcity) - 1.0) / 99.0, 0.0, 1.0)
	var plenty = 1.0 - t
	var out: Array = []
	match table:
		"trash":
			if rng.randf() < lerpf(0.95, 0.25, t):
				out.append({"id": "scrap", "count": rng.randi_range(1, 3)})
			if rng.randf() < lerpf(0.8, 0.15, t):
				out.append({"id": "credits_chit", "count": rng.randi_range(1, 3)})
			if rng.randf() < lerpf(0.35, 0.04, t):
				out.append({"id": "ration", "count": 1})
			if rng.randf() < lerpf(0.12, 0.01, t):
				out.append({"id": "cinder_quartz", "count": 1})
		"raider":
			out.append({"id": "pistol_rounds", "count": 1})
			out.append({"id": "credits_chit", "count": rng.randi_range(1, 4)})
			if rng.randf() < 0.3 + plenty * 0.4:
				out.append({"id": "scrap", "count": 1})
		"pipewolf":
			out.append({"id": "rat_bone", "count": 1 + (1 if rng.randf() < 0.4 + plenty * 0.4 else 0)})
			if rng.randf() < 0.55 + plenty * 0.35:
				out.append({"id": "rat_enzyme", "count": 1})
		"grazer":
			out.append({"id": "hide", "count": 1})
			if rng.randf() < 0.3 + plenty * 0.3:
				out.append({"id": "ration", "count": 1})
		"mineral_iron":
			out.append({"id": "iron_ore", "count": rng.randi_range(1, 2 + int(plenty * 2.0))})
		"mineral_copper":
			out.append({"id": "copper_ore", "count": rng.randi_range(1, 1 + int(plenty * 2.0))})
		"mineral_quartz":
			if rng.randf() < lerpf(0.85, 0.18, t):
				out.append({"id": "cinder_quartz", "count": 1})
			else:
				out.append({"id": "iron_ore", "count": 1})
		"moss":
			out.append({"id": "moss_fiber", "count": rng.randi_range(1, 2 + int(plenty * 2.0))})
		"teak":
			if rng.randf() < lerpf(0.9, 0.2, t):
				out.append({"id": "veil_teak", "count": 1})
			out.append({"id": "moss_fiber", "count": 1})
		_:
			out.append({"id": "scrap", "count": 1})
	return out

func shop_stock(kind: String, scarcity: int, brand_availability: int, rng: RandomNumberGenerator) -> Array:
	var t = clampf((float(scarcity) - 1.0) / 99.0, 0.0, 1.0)
	var avail = clampf(float(brand_availability) / 100.0, 0.0, 1.0)
	var qty = func(base: int) -> int:
		return maxi(1, int(round(float(base) * lerpf(2.2, 0.35, t))))
	var stock: Array = []
	match kind:
		"general":
			stock.append({"id": "ration", "count": qty.call(6), "price": 12})
			stock.append({"id": "scrap", "count": qty.call(8), "price": 6})
			stock.append({"id": "painting_kite", "count": qty.call(2), "price": 30})
			stock.append({"id": "plant_moss", "count": qty.call(2), "price": 20})
			stock.append({"id": "buggy", "count": 1, "price": 450})
			if avail > 0.25:
				stock.append({"id": "lumen_engine_light", "count": 1, "price": 900})
				stock.append({"id": "lumen_ap", "count": 1, "price": 750})
			if avail > 0.55:
				stock.append({"id": "voss_engine_light", "count": 1, "price": 1100})
				stock.append({"id": "voss_ap", "count": 1, "price": 750})
			if avail > 0.75:
				stock.append({"id": "lumen_frame_medium", "count": 1, "price": 4800})
				stock.append({"id": "voss_frame_light", "count": 1, "price": 1400})
		"armorer":
			stock.append({"id": "pistol_rounds", "count": qty.call(6), "price": 18})
			stock.append({"id": "carbine_kinetic", "count": 1, "price": 380})
			stock.append({"id": "vest_scout", "count": 1, "price": 260})
			stock.append({"id": "knife_keel", "count": 1, "price": 40})
			stock.append({"id": "shells", "count": qty.call(4), "price": 28})
			if avail > 0.4:
				stock.append({"id": "rifle_kinetic", "count": 1, "price": 640})
				stock.append({"id": "vest_voss", "count": 1, "price": 340})
			if avail > 0.7:
				stock.append({"id": "voss_cannon", "count": 1, "price": 920})
				stock.append({"id": "lumen_laser", "count": 1, "price": 880})
		"medical":
			stock.append({"id": "medkit", "count": qty.call(4), "price": 35})
			stock.append({"id": "ration", "count": qty.call(4), "price": 14})
		"fuel":
			stock.append({"id": "fuel_unit", "count": qty.call(40), "price": 3})
		_:
			pass
	if rng.randf() < 0.15 + (1.0 - t) * 0.3:
		stock.append({"id": "cinder_quartz", "count": 1, "price": 90})
	return stock

func hire_roster() -> Array:
	return [
		{"id": "ivo", "name": "Ivo Quell", "role": "pilot", "wage": 18, "skills": {"piloting": 4, "gunnery": 1}},
		{"id": "ness", "name": "Ness Hart", "role": "engineer", "wage": 16, "skills": {"engineering": 4, "fabrication": 2}},
		{"id": "pell", "name": "Pell Amin", "role": "scanner", "wage": 14, "skills": {"scanning": 5, "survival": 1}},
		{"id": "sura", "name": "Sura Venn", "role": "medic", "wage": 20, "skills": {"medicine": 5, "survival": 2}},
	]

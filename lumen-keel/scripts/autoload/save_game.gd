extends Node
## Versioned container save. Cheat-flagged sessions never overwrite the clean slot.

const FORMAT = 3
const CLEAN_PATH = "user://lumen_keel_slot.json"
const CHEAT_PATH = "user://lumen_keel_cheat.json"

func _ready() -> void:
	pass

func path_for(cheat: bool) -> String:
	return CHEAT_PATH if cheat else CLEAN_PATH

func has_clean() -> bool:
	return FileAccess.file_exists(CLEAN_PATH)

func has_cheat() -> bool:
	return FileAccess.file_exists(CHEAT_PATH)

func save_current() -> bool:
	if Game.state.is_empty():
		return false
	Game.state["format"] = FORMAT
	Game.state["saved_unix"] = SimClock.now()
	Game.state["cheat_flag"] = Game.cheat_flag or bool(Game.state.get("cheat_flag", false))
	var cheat = bool(Game.state["cheat_flag"])
	var target = path_for(cheat)
	if cheat and FileAccess.file_exists(CLEAN_PATH):
		# Never replace the clean playthrough with a test character.
		pass
	var wrapped = {
		"format": FORMAT,
		"game": "lumen-keel",
		"cheat_flag": cheat,
		"saved_unix": SimClock.now(),
		"containers": {
			"player": Game.state["player"],
			"ship": Game.state["ship"],
			"station": Game.state["station"],
			"planet": Game.state["planet"],
			"crew": Game.state["crew"],
			"world": Game.state["world"],
		},
		"settings": Game.state["settings"],
		"seed": Game.state.get("seed", 1),
		"created_unix": Game.state.get("created_unix", SimClock.now()),
	}
	var json = JSON.stringify(wrapped, "\t")
	var f = FileAccess.open(target, FileAccess.WRITE)
	if f == null:
		Game.notify("Save failed.")
		return false
	f.store_string(json)
	f.close()
	Game.notify("Saved %s." % ("CHEAT slot" if cheat else "clean slot"))
	return true

func load_slot(cheat: bool) -> bool:
	var target = path_for(cheat)
	if not FileAccess.file_exists(target):
		Game.notify("No save in that slot.")
		return false
	var f = FileAccess.open(target, FileAccess.READ)
	if f == null:
		return false
	var text = f.get_as_text()
	f.close()
	var parsed: Variant = JSON.parse_string(text)
	if typeof(parsed) != TYPE_DICTIONARY:
		Game.notify("Save was unreadable.")
		return false
	var data: Dictionary = migrate(parsed)
	if bool(data.get("cheat_flag", false)) and not cheat:
		Game.notify("Refused: cheat save cannot enter the clean slot.")
		return false
	var containers: Dictionary = data.get("containers", {})
	Game.cheat_flag = bool(data.get("cheat_flag", false))
	Game.god_mode = false
	Game.state = {
		"format": FORMAT,
		"game": "lumen-keel",
		"cheat_flag": Game.cheat_flag,
		"seed": data.get("seed", 1),
		"created_unix": data.get("created_unix", SimClock.now()),
		"saved_unix": data.get("saved_unix", SimClock.now()),
		"settings": data.get("settings", {}),
		"player": containers.get("player", {}),
		"ship": containers.get("ship", {}),
		"station": containers.get("station", {}),
		"planet": containers.get("planet", {}),
		"crew": containers.get("crew", []),
		"world": containers.get("world", {}),
	}
	_ensure_defaults()
	Game.resolve_offline()
	Game.state_changed.emit()
	return true

func migrate(data: Dictionary) -> Dictionary:
	var v = int(data.get("format", 1))
	if not data.has("containers"):
		data["containers"] = {}
	if v < 2:
		if not data["containers"].has("station"):
			data["containers"]["station"] = {}
		if not data["containers"]["station"].has("power_gen"):
			data["containers"]["station"]["power_gen"] = 120.0
			data["containers"]["station"]["power_draw"] = 70.0
			data["containers"]["station"]["sewage"] = 20.0
			data["containers"]["station"]["food"] = 40.0
		v = 2
	if v < 3:
		if data["containers"].has("player") and not data["containers"]["player"].has("o2"):
			data["containers"]["player"]["o2"] = 100.0
		if data["containers"].has("ship") and not data["containers"]["ship"].has("shield"):
			data["containers"]["ship"]["shield"] = 0.0
		if not data.has("settings"):
			data["settings"] = {"scarcity": 40, "economy_speed": 50}
		v = 3
	data["format"] = FORMAT
	return data

func _ensure_defaults() -> void:
	var p: Dictionary = Game.state["player"]
	if not p.has("skills"):
		p["skills"] = {}
	for s in Catalog.SKILLS:
		if not p["skills"].has(s):
			p["skills"][s] = {"rank": 0, "xp": 0.0}
	if not p.has("inventory"):
		p["inventory"] = []
	if not p.has("ammo"):
		p["ammo"] = {}
	var ship: Dictionary = Game.state["ship"]
	if not ship.has("parts"):
		ship["parts"] = {}
	if not ship.has("condition"):
		ship["condition"] = {}
	if not ship.has("cargo"):
		ship["cargo"] = []
	if not ship.has("ammo"):
		ship["ammo"] = {"shells": 0, "cells": 0, "missiles": 0}
	if not ship.has("transit"):
		ship["transit"] = {}
	if not Game.state.has("crew"):
		Game.state["crew"] = []
	if not Game.state["world"].has("placed"):
		Game.state["world"]["placed"] = []
	if not Game.state["world"].has("benches"):
		Game.state["world"]["benches"] = ["station_services", "skiff_bench"]
	if not Game.state["world"].has("objectives"):
		Game.state["world"]["objectives"] = Game._objectives()

extends Node

var title: Control
var world: Node

func _ready() -> void:
	var args = OS.get_cmdline_user_args()
	if args.has("--selftest"):
		var code = _selftest()
		get_tree().quit(code)
		return
	if args.has("--smoke"):
		Game.new_game()
		_start_world()
		await get_tree().create_timer(1.0).timeout
		var dist = world.station.global_position.length()
		var ship_ok = world.ship != null and world.player != null
		print("SMOKE dist=", dist, " ship=", ship_ok, " fuel=", Game.state["ship"]["fuel"])
		var bad = absf(dist - SimClock.ORBIT_R) > 30.0 or not ship_ok
		get_tree().quit(1 if bad else 0)
		return
	_show_title()

func _show_title() -> void:
	title = Control.new()
	title.set_anchors_preset(Control.PRESET_FULL_RECT)
	var bg = ColorRect.new()
	bg.color = Color(0.04, 0.035, 0.03)
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	title.add_child(bg)
	var v = VBoxContainer.new()
	v.set_anchors_preset(Control.PRESET_CENTER)
	v.position = Vector2(360, 180)
	title.add_child(v)
	var h = Label.new()
	h.text = "LUMEN KEEL"
	h.add_theme_font_size_override("font_size", 48)
	h.add_theme_color_override("font_color", Color(0.93, 0.78, 0.5))
	v.add_child(h)
	var sub = Label.new()
	sub.text = "Harrow system  ·  Vesper  ·  Kite Anchorage\nOne sun. One planet. One station. The clock does not stop."
	sub.add_theme_color_override("font_color", Color(0.7, 0.75, 0.7))
	v.add_child(sub)
	var begin = Button.new()
	begin.text = "Begin shift"
	begin.pressed.connect(_begin_new)
	v.add_child(begin)
	if SaveGame.has_clean():
		var cont = Button.new()
		cont.text = "Continue clean slot"
		cont.pressed.connect(func():
			if SaveGame.load_slot(false):
				_start_world()
		)
		v.add_child(cont)
	if SaveGame.has_cheat():
		var ch = Button.new()
		ch.text = "Continue CHEAT slot (flagged)"
		ch.pressed.connect(func():
			if SaveGame.load_slot(true):
				_start_world()
		)
		v.add_child(ch)
	var help = Label.new()
	help.text = "WASD move   mouse look   E interact   F scan   G seat   I pockets   Z autopilot   V camera   ` debug   Esc menu"
	help.add_theme_color_override("font_color", Color(0.6, 0.55, 0.45))
	v.add_child(help)
	add_child(title)

func _begin_new() -> void:
	Game.new_game()
	_start_world()

func _start_world() -> void:
	if title:
		title.queue_free()
		title = null
	world = preload("res://scripts/world/system_world.gd").new()
	add_child(world)
	Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed and Input.mouse_mode != Input.MOUSE_MODE_CAPTURED:
		if world and (world.ui == null or not world.ui.blocks_look()):
			Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	if event.is_action_pressed("pause") and title and title.visible:
		pass

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_CLOSE_REQUEST:
		if not Game.state.is_empty():
			SaveGame.save_current()
		get_tree().quit()

func _selftest() -> int:
	var fails = 0
	var fail = func(msg: String):
		print("FAIL ", msg)
		fails += 1
	Game.new_game()
	if not Catalog.can_fit("lumen_frame_light", "lumen_engine_light", {"type": "engine", "size": "light", "internal": false}):
		fail.call("light engine should fit")
	if Catalog.can_fit("lumen_frame_light", "voss_engine_medium", {"type": "engine", "size": "light", "internal": false}):
		fail.call("medium engine must not fit a light socket")
	if Catalog.can_fit("lumen_frame_light", "lumen_shield", {"type": "weapon", "size": "light", "internal": false}):
		fail.call("shield must not fit an external weapon socket")
	if not Catalog.placement_ok("painting_kite", "wall"):
		fail.call("painting should stick to walls")
	if Catalog.placement_ok("painting_kite", "floor"):
		fail.call("painting should not stick to floors")
	if not Catalog.placement_ok("plant_moss", "table"):
		fail.call("plant should sit on a table")
	if not Catalog.placement_ok("seismic_spike", "ground"):
		fail.call("spike should sit on ground")
	var old = {"format": 1, "cheat_flag": false, "containers": {"player": {}, "ship": {}, "station": {}, "planet": {}, "crew": [], "world": {}}}
	var mig = SaveGame.migrate(old)
	if int(mig["format"]) != SaveGame.FORMAT:
		fail.call("migrate did not reach current format")
	if not mig["containers"]["station"].has("power_gen"):
		fail.call("v1 save did not gain a power grid")
	var b0 = Catalog.skill_bonus(10)
	var b1 = Catalog.skill_bonus(11)
	var b2 = Catalog.skill_bonus(12)
	if not (b1 > b0 and (b2 - b1) < (b1 - b0)):
		fail.call("post-master gains should diminish")
	var p0 = SimClock.station_pos(1000.0).length()
	if absf(p0 - SimClock.ORBIT_R) > 1.0:
		fail.call("station orbit radius")
	var t_period = TAU / SimClock.mean_motion()
	var p1 = SimClock.station_pos(1000.0 + t_period)
	if p1.distance_to(SimClock.station_pos(1000.0)) > 2.0:
		fail.call("orbit did not close")
	var rng = RandomNumberGenerator.new()
	rng.seed = 3
	var rich = 0
	var poor = 0
	for i in 40:
		rich += Catalog.loot_table("trash", 5, rng).size()
		poor += Catalog.loot_table("trash", 95, rng).size()
	if rich <= poor:
		fail.call("low scarcity should yield more loot than high scarcity")
	Game.state["player"]["inventory"] = [{"id": "iron_ore", "count": 8}, {"id": "scrap", "count": 4}, {"id": "moss_fiber", "count": 4}]
	if not Game.try_craft("make_pistol_rounds"):
		fail.call("craft pistol rounds")
	if Game.count_item(Game.player_inv(), "pistol_rounds") < 1:
		fail.call("ammo not produced")
	var preview_fast = SimClock.transit_preview(8000, 1.0, {"route": 1, "fuel_eff": 1, "emergency": 1, "docking": 1}, 0.0, {"fuel_factor": 1, "wear_factor": 1})
	var preview_slow = SimClock.transit_preview(8000, 0.1, {"route": 1, "fuel_eff": 1, "emergency": 1, "docking": 1}, 0.0, {"fuel_factor": 1, "wear_factor": 1})
	if float(preview_fast["duration"]) >= float(preview_slow["duration"]):
		fail.call("fast route should be shorter")
	if float(preview_fast["fuel"]) <= float(preview_slow["fuel"]):
		fail.call("fast route should cost more fuel")
	Game.state["ship"]["fuel"] = 80
	Game.state["ship"]["loc"] = "space"
	Game.state["ship"]["pos"] = [SimClock.ORBIT_R, 0, 0]
	if not Game.engage_transit("site:0"):
		fail.call("engage transit")
	var tr: Dictionary = Game.state["ship"]["transit"]
	tr["start_unix"] = SimClock.now() - float(tr["duration"]) - 5.0
	Game._apply_transit_progress(false)
	if str(Game.state["ship"]["loc"]) != "surface":
		fail.call("offline transit should already be landed")
	Game.mark_cheat()
	SaveGame.save_current()
	if not FileAccess.file_exists(SaveGame.CHEAT_PATH):
		fail.call("cheat slot missing")
	# clean slot must not be the cheat file
	if FileAccess.file_exists(SaveGame.CLEAN_PATH):
		var txt = FileAccess.get_file_as_string(SaveGame.CLEAN_PATH)
		if "\"cheat_flag\": true" in txt or "\"cheat_flag\":true" in txt:
			fail.call("cheat leaked into clean slot")
	print("SELFTEST fails=", fails)
	return 0 if fails == 0 else 1

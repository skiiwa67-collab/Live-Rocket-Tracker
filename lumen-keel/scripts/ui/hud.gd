extends CanvasLayer

var world
var root: Control
var prompt: Label
var objective: Label
var stats: Label
var toast: Label
var panel: PanelContainer
var panel_box: VBoxContainer
var open = ""
var blocks = false
var _scan: Control
var _sliders = {}

func _ready() -> void:
	layer = 20
	root = Control.new()
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(root)
	_fill(root)
	var theme = _theme()
	root.theme = theme
	prompt = _label("", 18)
	prompt.mouse_filter = Control.MOUSE_FILTER_IGNORE
	prompt.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	root.add_child(prompt)
	_anchor_bottom(prompt, 0.5, 0.5, -320, -56, 320, -16)
	objective = _label("", 16)
	objective.size = Vector2(560, 80)
	objective.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	objective.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(objective)
	_anchor_top(objective, 0.0, 0.0, 24, 24, 584, 104)
	stats = _label("", 15)
	stats.size = Vector2(360, 220)
	stats.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	stats.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(stats)
	_anchor_top(stats, 1.0, 0.0, -384, 16, -16, 236)
	toast = _label("", 18)
	toast.size = Vector2(700, 40)
	toast.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(toast)
	_anchor_bottom(toast, 0.0, 1.0, 24, -48, 724, -12)
	_scan = Control.new()
	_scan.mouse_filter = Control.MOUSE_FILTER_IGNORE
	root.add_child(_scan)
	_fill(_scan)
	panel = PanelContainer.new()
	panel.custom_minimum_size = Vector2(520, 420)
	panel.visible = false
	root.add_child(panel)
	_anchor_center(panel, 400, 320)
	panel_box = VBoxContainer.new()
	panel.add_child(panel_box)
	Game.toast.connect(func(t): toast.text = t)

func _theme() -> Theme:
	var t = Theme.new()
	var sb = StyleBoxFlat.new()
	sb.bg_color = Color(0.05, 0.05, 0.06, 0.92)
	sb.border_color = Color(0.85, 0.62, 0.28)
	sb.set_border_width_all(1)
	sb.set_content_margin_all(10)
	t.set_stylebox("panel", "PanelContainer", sb)
	t.set_color("font_color", "Label", Color(0.95, 0.82, 0.55))
	t.set_font_size("font_size", "Label", 16)
	var btn = StyleBoxFlat.new()
	btn.bg_color = Color(0.14, 0.12, 0.1)
	btn.set_content_margin_all(6)
	t.set_stylebox("normal", "Button", btn)
	t.set_color("font_color", "Button", Color(0.95, 0.86, 0.7))
	return t

func _fill(c: Control) -> void:
	c.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	c.grow_horizontal = Control.GROW_DIRECTION_BOTH
	c.grow_vertical = Control.GROW_DIRECTION_BOTH

func _anchor_top(c: Control, ax: float, ay: float, l: float, t: float, r: float, b: float) -> void:
	c.anchor_left = ax
	c.anchor_top = ay
	c.anchor_right = ax
	c.anchor_bottom = ay
	c.offset_left = l
	c.offset_top = t
	c.offset_right = r
	c.offset_bottom = b

func _anchor_bottom(c: Control, ax: float, ay: float, l: float, t: float, r: float, b: float) -> void:
	_anchor_top(c, ax, ay, l, t, r, b)

func _anchor_center(c: Control, half_w: float, half_h: float) -> void:
	c.anchor_left = 0.5
	c.anchor_top = 0.5
	c.anchor_right = 0.5
	c.anchor_bottom = 0.5
	c.offset_left = -half_w
	c.offset_top = -half_h
	c.offset_right = half_w
	c.offset_bottom = half_h
	c.grow_horizontal = Control.GROW_DIRECTION_BOTH
	c.grow_vertical = Control.GROW_DIRECTION_BOTH

func _label(text: String, size: int) -> Label:
	var l = Label.new()
	l.text = text
	l.add_theme_font_size_override("font_size", size)
	l.add_theme_color_override("font_color", Color(0.96, 0.84, 0.58))
	return l

func blocks_look() -> bool:
	return blocks

func _process(_d: float) -> void:
	if Game.state.is_empty() or world == null or world.player == null:
		return
	if not blocks:
		prompt.text = world.player.probe_interact()
		if prompt.text != "":
			prompt.text = "[E] " + prompt.text
	objective.text = Game.objective_text()
	var p: Dictionary = Game.state["player"]
	var ship: Dictionary = Game.state["ship"]
	var agg = Game.aggregated()
	var alt = SimClock.altitude(world.player.global_position)
	stats.text = "CR %d   HP %.0f   HUN %.0f   O2 %.0f\nFUEL %.0f  CAP %.0f  SH %.0f  HULL %.0f\n%s  %s\nALT %.0f  SPD %.0f  G-GRID %.0f%%\n%s" % [
		int(p["credits"]), float(p["health"]), float(p["hunger"]), float(p["o2"]),
		float(ship["fuel"]), float(ship.get("capacitor", 0)), float(ship.get("shield", 0)), float(ship["hull"]),
		str(ship.get("loc", "")), ("FA ON" if ship.get("flight_assist", true) else "FA OFF"),
		alt, world.ship.velocity.length() if world.ship else 0.0, Game.grid_strain() * 100.0,
		_transit_line(),
	]
	if Game.message_t > 0.0:
		toast.text = Game.message
	_draw_scan()
	var cross = true
	if not has_node("cross"):
		var c = _label("+", 22)
		c.name = "cross"
		c.position = Vector2(628, 340)
		c.mouse_filter = Control.MOUSE_FILTER_IGNORE
		root.add_child(c)

func _transit_line() -> String:
	var tr: Dictionary = Game.state["ship"].get("transit", {})
	if tr.is_empty():
		return "AP idle  Z to plot"
	var u = Game.transit_u()
	var left = maxf(0.0, float(tr["duration"]) * (1.0 - u))
	return "AP %s  %.0fs  fuel %.1f  %s" % [str(tr.get("dest", "")), left, float(tr.get("fuel_cost", 0)), str(tr.get("pilot_name", ""))]

func _draw_scan() -> void:
	if world.player.scan_time <= 0.0:
		_scan.visible = false
		return
	_scan.visible = true
	for c in _scan.get_children():
		c.queue_free()
	var cam: Camera3D = world.player.cam
	for m in world.player.scan_marks:
		var pos: Vector3 = m["pos"]
		if cam.is_position_behind(pos):
			continue
		var sp = cam.unproject_position(pos)
		var l = _label(str(m["name"]), 14)
		l.position = sp + Vector2(8, -8)
		l.mouse_filter = Control.MOUSE_FILTER_IGNORE
		_scan.add_child(l)

func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("pause"):
		if open != "":
			close()
		else:
			open_pause()
		get_viewport().set_input_as_handled()
	elif event.is_action_pressed("inventory"):
		if open == "inv":
			close()
		else:
			open_inventory()
		get_viewport().set_input_as_handled()
	elif event.is_action_pressed("autopilot") and bool(Game.state.get("player", {}).get("seated", false)):
		open_autopilot()
		get_viewport().set_input_as_handled()
	elif event.is_action_pressed("debug_console"):
		if open == "debug":
			close()
		else:
			open_debug()
		get_viewport().set_input_as_handled()

func close() -> void:
	open = ""
	blocks = false
	panel.visible = false
	_clear(panel_box)
	Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _clear(n: Node) -> void:
	for c in n.get_children():
		c.queue_free()

func _show(title: String) -> void:
	blocks = true
	panel.visible = true
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	_clear(panel_box)
	panel_box.add_child(_label(title, 22))

func _btn(text: String, cb: Callable) -> Button:
	var b = Button.new()
	b.text = text
	b.pressed.connect(cb)
	return b

func open_panel(which: String) -> void:
	open = which
	_show("KITE GRID")
	var st: Dictionary = Game.state["station"]
	panel_box.add_child(_label("Generation %.0f kW    Draw %.0f kW    Strain %.0f%%    Eff %.0f%%" % [
		float(st["power_gen"]), float(st["power_draw"]), Game.grid_strain() * 100.0, Game.grid_eff() * 100.0
	], 16))
	panel_box.add_child(_label("Sewage %.0f / 100     Food vat %.0f / 100" % [float(st["sewage"]), float(st["food"])], 16))
	panel_box.add_child(_label("Deck plates sag when strain passes 100. Vats slow with it. This is the station, not flavor.", 14))
	panel_box.add_child(_btn("Close", close))

func open_container(id: String, items: Array) -> void:
	open = "box"
	_show("CONTAINER  " + id)
	if items.is_empty():
		panel_box.add_child(_label("Empty.", 16))
	for it in items:
		var iid = str(it["id"])
		var count = int(it["count"])
		panel_box.add_child(_btn("%s x%d  — take" % [Catalog.display_name(iid), count], func():
			if Game.take_item(Game.state["station"]["containers"][id]["items"], iid, count):
				if iid == "credits_chit":
					Game.state["player"]["credits"] = int(Game.state["player"]["credits"]) + 15 * count
				else:
					Game.add_item(Game.player_inv(), iid, count)
				AudioFx.play("click")
				open_container(id, Game.state["station"]["containers"][id]["items"])
		))
	panel_box.add_child(_btn("Close", close))

func open_shop(kind: String) -> void:
	open = "shop"
	var title = {"general": "KITE COMMISSARY", "armorer": "ARMORER", "medical": "MEDICAL", "fuel": "FUEL PUMP"}.get(kind, kind)
	_show(title + "    credits %d" % int(Game.state["player"]["credits"]))
	var stock = Game.ensure_shop(kind)
	for i in stock.size():
		var row: Dictionary = stock[i]
		panel_box.add_child(_btn("Buy %s  (%d cr)  stock %d" % [Catalog.display_name(str(row["id"])), int(row["price"]), int(row["count"])], func():
			Game.buy(kind, i)
			open_shop(kind)
		))
	panel_box.add_child(_label("Sell from pockets", 16))
	var seen = {}
	for it in Game.player_inv():
		var iid = str(it["id"])
		if seen.has(iid):
			continue
		seen[iid] = true
		panel_box.add_child(_btn("Sell %s x%d" % [Catalog.display_name(iid), Game.count_item(Game.player_inv(), iid)], func():
			Game.sell(iid)
			open_shop(kind)
		))
	panel_box.add_child(_btn("Close", close))

func open_inventory() -> void:
	open = "inv"
	_show("POCKETS")
	var p: Dictionary = Game.state["player"]
	panel_box.add_child(_label("Weapon %s   Armor %s   Mag %s" % [
		Catalog.display_name(str(p["equipped_weapon"])),
		Catalog.display_name(str(p["equipped_armor"])),
		str(p["mag"].get(p["equipped_weapon"], 0))
	], 15))
	panel_box.add_child(_label("Ammo  pistol %d  rifle %d  cells %d  arrows %d  rockets %d" % [
		int(p["ammo"].get("pistol_rounds", 0)), int(p["ammo"].get("rifle_rounds", 0)),
		int(p["ammo"].get("ground_cells", 0)), int(p["ammo"].get("arrows", 0)), int(p["ammo"].get("rockets", 0))
	], 14))
	for it in p["inventory"]:
		var iid = str(it["id"])
		var d = Catalog.def_of(iid)
		var row = HBoxContainer.new()
		row.add_child(_label("%s x%d" % [Catalog.display_name(iid), int(it["count"])], 15))
		if str(d.get("category", "")) == "weapon_ground":
			row.add_child(_btn("Equip", func():
				p["equipped_weapon"] = iid
				world.player._refresh_gear()
				open_inventory()
			))
		if str(d.get("slot", "")) == "armor" or str(d.get("category", "")) == "armor":
			row.add_child(_btn("Wear", func():
				p["equipped_armor"] = iid
				world.player._refresh_gear()
				open_inventory()
			))
		var surfaces: Array = d.get("placement", {}).get("surfaces", [])
		if not surfaces.is_empty():
			row.add_child(_btn("Place", func():
				close()
				world.try_place(iid)
			))
		if iid == "ration":
			row.add_child(_btn("Eat", func():
				Game.eat()
				open_inventory()
			))
		if iid == "medkit":
			row.add_child(_btn("Use", func():
				Game.heal_self()
				open_inventory()
			))
		if iid == "buggy":
			row.add_child(_btn("Deploy", func():
				close()
				world.deploy_buggy()
			))
		panel_box.add_child(row)
	panel_box.add_child(_label("Skills — practice raises them. Master is rank 10. After that, gains shrink.", 14))
	for s in Catalog.SKILLS:
		var rank = Game.skill_rank(p, s)
		var lines = Catalog.perk_lines(s, rank)
		panel_box.add_child(_label("%s  rank %d  bonus %.0f%%  | %s" % [s, rank, Catalog.skill_bonus(rank) * 100.0, " ".join(lines)], 13))
	panel_box.add_child(_btn("Close", close))

func open_craft() -> void:
	open = "craft"
	_show("FABRICATOR   tier %d   grid %.0f%%" % [Game.bench_tier(), Game.grid_eff() * 100.0])
	panel_box.add_child(_label("Station services are tier 0. The skiff unlocks tier 1. A cutter frame unlocks tier 2.", 14))
	for rid in Catalog.recipes.keys():
		var r: Dictionary = Catalog.recipes[rid]
		if int(r["tier"]) > Game.bench_tier():
			continue
		var need = ""
		for k in r["in"].keys():
			need += "%s %d/%d  " % [Catalog.display_name(str(k)), Game.count_all(str(k)), int(r["in"][k])]
		panel_box.add_child(_btn("%s   [%s]" % [r["name"], need], func():
			Game.try_craft(rid)
			open_craft()
		))
	panel_box.add_child(_btn("Close", close))

func open_crew() -> void:
	open = "crew"
	_show("CREW DESK")
	for c in Game.state["crew"]:
		var status = "aboard" if bool(c.get("alive", true)) else ("QUIT" if c.get("quit", false) else "DEAD")
		panel_box.add_child(_label("%s  %s  morale %.0f  %s  wage %d" % [c["name"], c["role"], float(c["morale"]), status, int(c["wage"])], 15))
		if bool(c.get("alive", true)):
			panel_box.add_child(_btn("Toggle duty " + str(c["name"]), func():
				c["on_duty"] = not bool(c.get("on_duty", true))
				c["aboard"] = bool(c["on_duty"])
				open_crew()
			))
	panel_box.add_child(_label("Hire — same skill rules as you. What they do is what they learn.", 14))
	for entry in Catalog.hire_roster():
		panel_box.add_child(_btn("Hire %s (%s) retainer %d" % [entry["name"], entry["role"], int(entry["wage"]) * 2], func():
			Game.hire(entry)
			open_crew()
		))
	panel_box.add_child(_btn("Wake Tally if the astromech kit is crafted", func():
		if Game.count_all("crew_tally") > 0 or true:
			# craft path already hires; this is a fallback if the item remains
			if Game.take_anywhere("crew_tally", 1):
				Game.hire_tally()
			else:
				Game.notify("Craft Tally at the skiff bench first.")
		open_crew()
	))
	panel_box.add_child(_btn("Close", close))

func open_jobs() -> void:
	open = "jobs"
	_show("BOARD")
	var jobs = [
		{"id": "iron", "text": "Bring 4 Vesper iron", "need": {"iron_ore": 4}, "pay": 220},
		{"id": "wolf", "text": "Bring 2 pipewolf enzymes", "need": {"rat_enzyme": 2}, "pay": 150},
		{"id": "scan", "text": "Scan a surface site from orbit", "need": {}, "pay": 180, "scan": true},
	]
	for job in jobs:
		var done: Array = Game.state["world"].get("jobs_done", [])
		if done.has(job["id"]):
			panel_box.add_child(_label(job["text"] + "  — paid", 15))
			continue
		panel_box.add_child(_btn(job["text"] + "   pay %d" % int(job["pay"]), func():
			if job.get("scan", false):
				if Game.state["planet"]["scanned_sites"].is_empty():
					Game.notify("Scan from the seat first.")
					return
			else:
				for k in job["need"].keys():
					if Game.count_all(str(k)) < int(job["need"][k]):
						Game.notify("You do not have the goods.")
						return
				for k in job["need"].keys():
					Game.take_anywhere(str(k), int(job["need"][k]))
			Game.state["player"]["credits"] = int(Game.state["player"]["credits"]) + int(job["pay"])
			Game.state["world"]["jobs_done"].append(job["id"])
			Game.add_xp(Game.state["player"], "trading", 8)
			Game.notify("Paid %d." % int(job["pay"]))
			open_jobs()
		))
	panel_box.add_child(_btn("Close", close))

func open_autopilot() -> void:
	open = "ap"
	_show("AUTOPILOT")
	var ship: Dictionary = Game.state["ship"]
	var agg = Game.aggregated()
	panel_box.add_child(_label("Computer: %s    route %.2f  fuel %.2f  emergency %.2f  dock %.2f" % [
		agg["ap"]["name"], agg["ap"]["route"], agg["ap"]["fuel_eff"], agg["ap"]["emergency"], agg["ap"]["docking"]
	], 14))
	var pilot = Game.active_pilot()
	panel_box.add_child(_label("Pilot %s   piloting rank %d" % [str(pilot.get("name", "Keel")), Game.skill_rank(pilot, "piloting")], 15))
	var urg = HSlider.new()
	urg.min_value = 0.05
	urg.max_value = 1.0
	urg.step = 0.05
	urg.value = float(ship.get("urgency", 0.45))
	urg.custom_minimum_size = Vector2(400, 24)
	panel_box.add_child(_label("Urgency — fast burns fuel and wears the engines. Slow is cheap.", 14))
	panel_box.add_child(urg)
	var preview = _label("", 15)
	panel_box.add_child(preview)
	var refresh = func():
		ship["urgency"] = urg.value
		var dest_pos = Game.site_pos(0)
		var from = world.ship.global_position
		var prev = SimClock.transit_preview(from.distance_to(dest_pos), urg.value, agg["ap"], Game.skill_bonus_of(pilot, "piloting"), agg)
		preview.text = "Sample Cinder Shelf:  %.0f s   fuel %.1f   wear %.2f" % [prev["duration"], prev["fuel"], prev["wear"]]
	urg.value_changed.connect(func(_v): refresh.call())
	refresh.call()
	panel_box.add_child(_btn("Engage — Kite Anchorage", func():
		ship["urgency"] = urg.value
		Game.engage_transit("station")
		close()
	))
	for i in 8:
		panel_box.add_child(_btn("Engage — " + Game.site_name(i), func():
			ship["urgency"] = urg.value
			Game.engage_transit("site:%d" % i)
			close()
		))
	if not ship["transit"].is_empty():
		panel_box.add_child(_btn("Cancel route", func():
			Game.cancel_transit()
			close()
		))
	panel_box.add_child(_btn("Close", close))

func open_designer() -> void:
	open = "design"
	world.designer = true
	_show("SHIP DESIGNER  — schematic")
	_anchor_top(panel, 1.0, 0.0, -500, 24, -16, 680)
	panel.custom_minimum_size = Vector2(480, 640)
	var cats = ["engine", "weapon", "shield", "capacitor", "core", "conduit", "autopilot", "scanner", "bay", "frame"]
	var cat_list = ItemList.new()
	cat_list.custom_minimum_size = Vector2(200, 160)
	for c in cats:
		cat_list.add_item(c)
	panel_box.add_child(cat_list)
	var parts = ItemList.new()
	parts.custom_minimum_size = Vector2(440, 180)
	panel_box.add_child(parts)
	var fill = func():
		parts.clear()
		var idx = cat_list.get_selected_items()
		var cat = "engine"
		if idx.size() > 0:
			cat = cats[idx[0]]
		if cat == "frame":
			for id in ["lumen_frame_light", "voss_frame_light", "lumen_frame_medium", "voss_frame_medium"]:
				if id == Game.ship_frame_id() or Game.count_item(Game.player_inv(), id) > 0:
					parts.add_item(Catalog.display_name(id) + "  [" + id + "]")
					parts.set_item_metadata(parts.item_count - 1, id)
			return
		for it in Game.player_inv():
			var d = Catalog.part(str(it["id"]))
			if d.is_empty():
				continue
			if str(d.get("socket", "")) != cat:
				continue
			parts.add_item("%s x%d" % [d["name"], int(it["count"])])
			parts.set_item_metadata(parts.item_count - 1, str(it["id"]))
	cat_list.item_selected.connect(func(_i): fill.call())
	cat_list.select(0)
	fill.call()
	var tex_btn = _btn("Toggle textures", func():
		world.ship.textured = not world.ship.textured
		world.ship.rebuild()
	)
	panel_box.add_child(tex_btn)
	panel_box.add_child(_label("Select a part, then a socket. Invalid sockets stay dark.", 13))
	var sockets = VBoxContainer.new()
	panel_box.add_child(sockets)
	var selected = {"id": ""}
	parts.item_selected.connect(func(i):
		selected.id = str(parts.get_item_metadata(i))
		if world.ship:
			world.ship.set_socket_preview(selected.id if Catalog.parts.has(selected.id) else "")
		_clear(sockets)
		if Catalog.part(selected.id).get("socket", "") == "frame" or selected.id.begins_with("lumen_frame") or selected.id.begins_with("voss_frame"):
			sockets.add_child(_btn("Install frame " + Catalog.display_name(selected.id), func():
				Game.set_frame(selected.id)
				world.ship.rebuild()
				open_designer()
			))
			return
		for hp in Game.frame_def().get("hardpoints", []):
			var ok = Catalog.can_fit(Game.ship_frame_id(), selected.id, hp)
			var cur = Game.socket_part(str(hp["id"]))
			var label = "%s (%s) %s" % [hp["id"], hp["type"], "FIT" if ok else "NO"]
			if cur != "":
				label += " now " + Catalog.display_name(cur)
			var b = _btn(label, func():
				if Game.install_part(str(hp["id"]), selected.id):
					world.ship.rebuild()
					AudioFx.play("click")
					open_designer()
			)
			b.disabled = not ok
			sockets.add_child(b)
	)
	for hp in Game.frame_def().get("hardpoints", []):
		var cur = Game.socket_part(str(hp["id"]))
		if cur == "":
			continue
		panel_box.add_child(_btn("Strip %s (%s)" % [hp["id"], Catalog.display_name(cur)], func():
			Game.install_part(str(hp["id"]), "")
			world.ship.rebuild()
			open_designer()
		))
	panel_box.add_child(_btn("Close", func():
		world.designer = false
		if world.ship:
			world.ship.hide_socket_preview()
		close()
	))

func open_pause() -> void:
	open = "pause"
	_show("LUMEN KEEL")
	panel_box.add_child(_label("Scarcity 1 = you trip over quartz. 100 = you hunt.", 14))
	panel_box.add_child(_slider("scarcity", 1, 100, Game.scarcity(), func(v):
		Game.state["settings"]["scarcity"] = int(v)
		Game.refresh_shops()
	))
	panel_box.add_child(_label("Economy speed. Weekend-warrior is the right end.", 14))
	panel_box.add_child(_slider("eco", 1, 100, Game.economy_speed(), func(v):
		Game.state["settings"]["economy_speed"] = int(v)
	))
	panel_box.add_child(_slider("vol", 0, 100, int(float(Game.state["settings"].get("master_volume", 0.8)) * 100.0), func(v):
		Game.state["settings"]["master_volume"] = float(v) / 100.0
	))
	panel_box.add_child(_btn("Save", func(): SaveGame.save_current()))
	panel_box.add_child(_btn("Load clean slot", func():
		if SaveGame.load_slot(false):
			_reload_world()
	))
	panel_box.add_child(_btn("Load cheat slot", func():
		if SaveGame.load_slot(true):
			_reload_world()
	))
	panel_box.add_child(_btn("Display settings", func():
		_clear(panel_box)
		panel_box.add_child(DisplayCfg.build_controls(func():
			open_pause()
		))
	))
	panel_box.add_child(_btn("Resume", close))

func open_debug() -> void:
	open = "debug"
	_show("DEBUG  — grants set the cheat flag")
	panel_box.add_child(_label("Scarcity", 14))
	panel_box.add_child(_slider("sc", 1, 100, Game.scarcity(), func(v):
		Game.state["settings"]["scarcity"] = int(v)
	))
	panel_box.add_child(_label("Economy speed", 14))
	panel_box.add_child(_slider("es", 1, 100, Game.economy_speed(), func(v):
		Game.state["settings"]["economy_speed"] = int(v)
	))
	panel_box.add_child(_label("Brand availability", 14))
	panel_box.add_child(_slider("br", 0, 100, int(Game.state["settings"].get("brand_availability", 70)), func(v):
		Game.state["settings"]["brand_availability"] = int(v)
		Game.brand_availability = int(v)
		Game.refresh_shops()
	))
	panel_box.add_child(_label("Ammo fill (cheat)", 14))
	panel_box.add_child(_slider("am", 0, 100, 50, func(v):
		Game.mark_cheat()
		var n = int(v)
		for k in Game.state["player"]["ammo"].keys():
			Game.state["player"]["ammo"][k] = n
		for k in Game.state["ship"]["ammo"].keys():
			Game.state["ship"]["ammo"][k] = n
	))
	panel_box.add_child(_label("Materials fill (cheat)", 14))
	panel_box.add_child(_slider("mat", 0, 100, 0, func(v):
		Game.mark_cheat()
		for id in ["iron_ore", "copper_ore", "cinder_quartz", "moss_fiber", "rat_bone", "rat_enzyme", "hide", "veil_teak", "scrap"]:
			Game.add_item(Game.player_inv(), id, int(v))
	))
	panel_box.add_child(_btn("God mode + credits + master skills (cheat)", func():
		Game.mark_cheat()
		Game.god_mode = true
		Game.state["player"]["credits"] = 50000
		Game.state["ship"]["fuel"] = 80
		for s in Catalog.SKILLS:
			Game.state["player"]["skills"][s] = {"rank": 14, "xp": 0}
		Game.notify("Test character flagged.")
	))
	panel_box.add_child(_btn("Teleport to orbit (cheat)", func():
		Game.mark_cheat()
		Game.state["ship"]["loc"] = "space"
		var p = SimClock.station_pos(SimClock.now()) + Vector3(0, 40, 0)
		Game.state["ship"]["pos"] = [p.x, p.y, p.z]
		Game.state["ship"]["vel"] = [0, 0, 0]
		world._sync_bodies()
	))
	panel_box.add_child(_btn("Close", close))

func _slider(id: String, mn: float, mx: float, value: float, cb: Callable) -> HSlider:
	var s = HSlider.new()
	s.min_value = mn
	s.max_value = mx
	s.step = 1
	s.value = value
	s.custom_minimum_size = Vector2(360, 20)
	s.value_changed.connect(cb)
	_sliders[id] = s
	return s

func _reload_world() -> void:
	var tree = get_tree()
	close()
	tree.reload_current_scene()

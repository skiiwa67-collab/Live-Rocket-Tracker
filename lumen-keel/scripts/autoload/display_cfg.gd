extends Node
## Window, refresh cap, and look inversion. Stored outside the save slot
## so a new shift still opens on the monitor Chris picked.

const PATH := "user://lumen_keel_display.json"
const DESIGN := Vector2i(1280, 720)

var width := 1280
var height := 720
var refresh := 0
var window_mode := "windowed"
var invert_x := false
var invert_y := false
var mouse_sens := 0.0022

func _ready() -> void:
	load_file()
	get_tree().root.size_changed.connect(_keep_scale)
	call_deferred("apply")

func load_file() -> void:
	if not FileAccess.file_exists(PATH):
		return
	var parsed: Variant = JSON.parse_string(FileAccess.get_file_as_string(PATH))
	if typeof(parsed) != TYPE_DICTIONARY:
		return
	var d: Dictionary = parsed
	width = maxi(640, int(d.get("width", width)))
	height = maxi(480, int(d.get("height", height)))
	refresh = maxi(0, int(d.get("refresh", refresh)))
	window_mode = str(d.get("window_mode", window_mode))
	if window_mode not in ["windowed", "fullscreen", "borderless"]:
		window_mode = "windowed"
	invert_x = bool(d.get("invert_x", invert_x))
	invert_y = bool(d.get("invert_y", invert_y))
	mouse_sens = clampf(float(d.get("mouse_sens", mouse_sens)), 0.0004, 0.02)

func save_file() -> void:
	var d := {
		"width": width,
		"height": height,
		"refresh": refresh,
		"window_mode": window_mode,
		"invert_x": invert_x,
		"invert_y": invert_y,
		"mouse_sens": mouse_sens,
	}
	var f := FileAccess.open(PATH, FileAccess.WRITE)
	if f:
		f.store_string(JSON.stringify(d, "\t"))
		f.close()

func apply() -> void:
	if DisplayServer.get_name() == "headless":
		return
	var win := get_window()
	_apply_scale(win)
	match window_mode:
		"fullscreen":
			win.borderless = false
			win.mode = Window.MODE_EXCLUSIVE_FULLSCREEN
		"borderless":
			win.mode = Window.MODE_WINDOWED
			win.borderless = true
			var screen := DisplayServer.window_get_current_screen()
			win.position = DisplayServer.screen_get_position(screen)
			win.size = DisplayServer.screen_get_size(screen)
		_:
			win.borderless = false
			win.mode = Window.MODE_WINDOWED
			win.size = Vector2i(width, height)
			win.move_to_center()
	if refresh > 0:
		Engine.max_fps = refresh
	else:
		Engine.max_fps = 0
	DisplayServer.window_set_vsync_mode(DisplayServer.VSYNC_ENABLED)
	_apply_scale(win)

func _keep_scale() -> void:
	if DisplayServer.get_name() == "headless":
		return
	_apply_scale(get_window())

func _apply_scale(win: Window) -> void:
	win.content_scale_mode = Window.CONTENT_SCALE_MODE_CANVAS_ITEMS
	win.content_scale_aspect = Window.CONTENT_SCALE_ASPECT_EXPAND
	win.content_scale_stretch = Window.CONTENT_SCALE_STRETCH_FRACTIONAL
	win.content_scale_size = DESIGN
	# HiDPI was multiplying this above 1 and parking the 1280x720 canvas
	# in the corner of the real window. Stretch already fills the window.
	win.content_scale_factor = 1.0

func resolutions() -> Array:
	var found := {}
	var out: Array = []
	var add := func(w: int, h: int, label: String) -> void:
		if w < 640 or h < 480:
			return
		var key := "%d,%d" % [w, h]
		if found.has(key):
			return
		found[key] = true
		out.append({"w": w, "h": h, "label": label})
	var presets := [
		[1280, 720], [1600, 900], [1920, 1080], [1920, 1200],
		[2560, 1080], [2560, 1440], [3440, 1440],
		[3840, 1080], [3840, 1600], [3840, 2160],
		[5120, 1440], [5120, 2160],
	]
	for p in presets:
		add.call(int(p[0]), int(p[1]), "%d x %d" % [p[0], p[1]])
	var n := DisplayServer.get_screen_count()
	for i in n:
		var sz := DisplayServer.screen_get_size(i)
		add.call(sz.x, sz.y, "%d x %d  (screen %d)" % [sz.x, sz.y, i + 1])
	add.call(width, height, "%d x %d" % [width, height])
	return out

func refresh_choices() -> Array:
	var rates: Array = [0, 60, 75, 120, 144, 165, 240]
	var n := DisplayServer.get_screen_count()
	for i in n:
		var hz := int(round(DisplayServer.screen_get_refresh_rate(i)))
		if hz > 0 and not rates.has(hz):
			rates.append(hz)
	if refresh > 0 and not rates.has(refresh):
		rates.append(refresh)
	rates.sort()
	return rates

func look_scale() -> Vector2:
	return Vector2(-1.0 if invert_x else 1.0, -1.0 if invert_y else 1.0)

func build_controls(on_close: Callable) -> VBoxContainer:
	var box := VBoxContainer.new()
	box.add_theme_constant_override("separation", 8)
	var title := Label.new()
	title.text = "SETTINGS"
	title.add_theme_font_size_override("font_size", 28)
	title.add_theme_color_override("font_color", Color(0.95, 0.82, 0.55))
	box.add_child(title)
	box.add_child(_hint("Resolution, refresh cap, and window mode are saved on this machine."))
	var res := OptionButton.new()
	var list := resolutions()
	var sel := 0
	for i in list.size():
		var row: Dictionary = list[i]
		res.add_item(str(row["label"]))
		res.set_item_metadata(i, Vector2i(int(row["w"]), int(row["h"])))
		if int(row["w"]) == width and int(row["h"]) == height:
			sel = i
	res.selected = sel
	box.add_child(_hint("Resolution"))
	box.add_child(res)
	var hz_box := OptionButton.new()
	var rates := refresh_choices()
	var hsel := 0
	for i in rates.size():
		var hz := int(rates[i])
		hz_box.add_item("Monitor vsync" if hz == 0 else "%d Hz" % hz)
		hz_box.set_item_metadata(i, hz)
		if hz == refresh:
			hsel = i
	hz_box.selected = hsel
	box.add_child(_hint("Refresh cap. Vsync stays on. A number caps frames at that rate."))
	box.add_child(hz_box)
	var mode := OptionButton.new()
	mode.add_item("Windowed")
	mode.set_item_metadata(0, "windowed")
	mode.add_item("Fullscreen")
	mode.set_item_metadata(1, "fullscreen")
	mode.add_item("Borderless")
	mode.set_item_metadata(2, "borderless")
	mode.selected = ["windowed", "fullscreen", "borderless"].find(window_mode)
	if mode.selected < 0:
		mode.selected = 0
	box.add_child(_hint("Window"))
	box.add_child(mode)
	var ix := CheckBox.new()
	ix.text = "Invert mouse / trackball X"
	ix.button_pressed = invert_x
	var iy := CheckBox.new()
	iy.text = "Invert mouse / trackball Y"
	iy.button_pressed = invert_y
	box.add_child(ix)
	box.add_child(iy)
	var sens := HSlider.new()
	sens.min_value = 4
	sens.max_value = 80
	sens.step = 1
	sens.value = mouse_sens * 10000.0
	sens.custom_minimum_size = Vector2(420, 24)
	var sens_l := _hint("Look sensitivity  %d" % int(sens.value))
	sens.value_changed.connect(func(v: float) -> void:
		sens_l.text = "Look sensitivity  %d" % int(v)
	)
	box.add_child(sens_l)
	box.add_child(sens)
	var apply_btn := Button.new()
	apply_btn.text = "Apply and save"
	apply_btn.pressed.connect(func() -> void:
		var sz: Vector2i = res.get_item_metadata(res.selected)
		width = sz.x
		height = sz.y
		refresh = int(hz_box.get_item_metadata(hz_box.selected))
		window_mode = str(mode.get_item_metadata(mode.selected))
		invert_x = ix.button_pressed
		invert_y = iy.button_pressed
		mouse_sens = float(sens.value) / 10000.0
		save_file()
		apply()
		_mirror_game()
	)
	box.add_child(apply_btn)
	var back := Button.new()
	back.text = "Back"
	back.pressed.connect(on_close)
	box.add_child(back)
	return box

func _hint(text: String) -> Label:
	var l := Label.new()
	l.text = text
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	l.custom_minimum_size = Vector2(480, 0)
	l.add_theme_color_override("font_color", Color(0.78, 0.74, 0.62))
	return l

func _mirror_game() -> void:
	if not is_instance_valid(Game) or Game.state.is_empty():
		return
	var s: Dictionary = Game.state["settings"]
	s["invert_x"] = invert_x
	s["invert_y"] = invert_y
	s["mouse_sens"] = mouse_sens

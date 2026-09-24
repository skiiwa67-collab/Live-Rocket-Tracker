extends Node3D

var player: CharacterBody3D
var ship: CharacterBody3D
var buggy: CharacterBody3D
var station: Node3D
var planet: Node3D
var elevator: Node3D
var sun_light: DirectionalLight3D
var env: Environment
var ui
var designer = false
var placing = ""
var place_ghost: MeshInstance3D
var place_surface_marks: Array = []
var hostile: Node3D
var _acc = 0.0

func _ready() -> void:
	process_priority = -50
	_environment()
	_build_stars()
	_build_sun()
	_build_planet()
	_build_station()
	_build_ship()
	_build_player()
	_build_planet_life()
	_build_station_life()
	_restore_placed()
	ui = preload("res://scripts/ui/hud.gd").new()
	ui.world = self
	add_child(ui)
	_sync_bodies()

func _environment() -> void:
	var we = WorldEnvironment.new()
	env = Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color(0, 0, 0)
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.07, 0.08, 0.1)
	env.ambient_light_energy = 0.35
	env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
	env.glow_enabled = true
	env.glow_intensity = 0.45
	env.glow_bloom = 0.15
	env.fog_enabled = false
	env.fog_light_color = Color(0.45, 0.28, 0.16)
	env.fog_density = 0.0008
	we.environment = env
	add_child(we)
	var cam_far = Camera3D.new()
	cam_far.current = false

func _build_stars() -> void:
	var multi = MultiMeshInstance3D.new()
	var mm = MultiMesh.new()
	mm.transform_format = MultiMesh.TRANSFORM_3D
	var sm = SphereMesh.new()
	sm.radius = 40
	sm.height = 80
	sm.radial_segments = 4
	sm.rings = 2
	mm.mesh = sm
	mm.instance_count = 500
	var rng = RandomNumberGenerator.new()
	rng.seed = 42
	for i in 500:
		var dir = Vector3(rng.randf_range(-1, 1), rng.randf_range(-1, 1), rng.randf_range(-1, 1)).normalized()
		var xf = Transform3D(Basis.IDENTITY.scaled(Vector3.ONE * rng.randf_range(0.4, 1.4)), dir * rng.randf_range(80000, 140000))
		mm.set_instance_transform(i, xf)
	multi.multimesh = mm
	multi.material_override = Meshes.mat(Color(0.9, 0.92, 1.0), 1, 0, Color(1, 1, 1), false)
	add_child(multi)

func _build_sun() -> void:
	var sun = Meshes.sphere(900.0, Meshes.mat(Color(1, 0.72, 0.35), 0.4, 0, Color(1, 0.6, 0.2), false), 24)
	sun.position = SimClock.SUN_POS
	add_child(sun)
	sun_light = DirectionalLight3D.new()
	sun_light.light_energy = 1.35
	sun_light.shadow_enabled = true
	add_child(sun_light)
	_aim_sun()

func _aim_sun() -> void:
	sun_light.global_position = SimClock.SUN_POS.normalized() * 5000.0
	sun_light.look_at(Vector3.ZERO, Vector3.UP)

func _build_planet() -> void:
	planet = StaticBody3D.new()
	planet.name = "Vesper"
	add_child(planet)
	var mesh = SphereMesh.new()
	mesh.radius = SimClock.PLANET_R
	mesh.height = SimClock.PLANET_R * 2.0
	mesh.radial_segments = 64
	mesh.rings = 32
	var mi = MeshInstance3D.new()
	mi.mesh = mesh
	var shader = Shader.new()
	shader.code = """shader_type spatial;
uniform sampler2D noise_tex;
varying vec3 wp;
void vertex(){ wp = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz; }
void fragment(){
  vec3 n = normalize(wp);
  float h = texture(noise_tex, n.xz * 3.0).r;
  vec3 rust = vec3(0.45, 0.24, 0.14);
  vec3 dust = vec3(0.28, 0.22, 0.16);
  vec3 moss = vec3(0.18, 0.28, 0.16);
  vec3 c = mix(dust, rust, h);
  c = mix(c, moss, smoothstep(0.55, 0.8, h) * 0.45);
  float pole = abs(n.y);
  c = mix(c, vec3(0.55, 0.5, 0.45), smoothstep(0.78, 0.95, pole));
  ALBEDO = c;
  ROUGHNESS = 0.92;
}"""
	var mat = ShaderMaterial.new()
	mat.shader = shader
	var img = Image.create(64, 64, false, Image.FORMAT_RGB8)
	var rng = RandomNumberGenerator.new()
	rng.seed = 7
	for y in 64:
		for x in 64:
			var v = rng.randf()
			img.set_pixel(x, y, Color(v, v, v))
	var tex = ImageTexture.create_from_image(img)
	mat.set_shader_parameter("noise_tex", tex)
	mi.material_override = mat
	planet.add_child(mi)
	var col = CollisionShape3D.new()
	var sph = SphereShape3D.new()
	sph.radius = SimClock.PLANET_R
	col.shape = sph
	planet.add_child(col)

func _build_station() -> void:
	station = Node3D.new()
	station.name = "KiteAnchorage"
	add_child(station)
	var hull = Meshes.mat(Color(0.18, 0.19, 0.2), 0.55, 0.45, Color(0, 0, 0), true)
	var trim = Meshes.mat(Color(0.55, 0.32, 0.14), 0.4, 0.3, Color(0, 0, 0), true)
	var floor_mat = Meshes.mat(Color(0.16, 0.16, 0.17), 0.8, 0.2, Color(0, 0, 0), true)
	var drum = Meshes.cyl(22, 10, hull, 24)
	drum.position = Vector3(0, 6, 18)
	station.add_child(drum)
	var hangar_floor = _floor(Vector3(34, 0.4, 26), Vector3(0, 0, 0), floor_mat)
	station.add_child(hangar_floor)
	var prom = _floor(Vector3(28, 0.4, 22), Vector3(0, 0, 30), floor_mat)
	station.add_child(prom)
	var hab = _floor(Vector3(12, 0.4, 10), Vector3(-16, 0, 28), floor_mat)
	station.add_child(hab)
	var under = _floor(Vector3(26, 0.4, 18), Vector3(2, -6, 38), floor_mat)
	station.add_child(under)
	_wall(Vector3(0, 2, -13), Vector3(34, 4, 0.4), hull)
	_wall(Vector3(-17, 2, 0), Vector3(0.4, 4, 26), hull)
	_wall(Vector3(17, 2, 0), Vector3(0.4, 4, 26), hull)
	_wall(Vector3(-14, 2, 30), Vector3(0.4, 4, 22), hull)
	_wall(Vector3(14, 2, 30), Vector3(0.4, 4, 22), hull)
	_wall(Vector3(0, 2, 41), Vector3(28, 4, 0.4), hull)
	_wall(Vector3(-22, 2, 28), Vector3(0.4, 4, 10), hull)
	_wall(Vector3(-10, 2, 28), Vector3(0.4, 4, 10), hull)
	_wall(Vector3(-16, 2, 23), Vector3(12, 4, 0.4), hull)
	_wall(Vector3(-16, 2, 33), Vector3(12, 4, 0.4), hull)
	var stripe = Meshes.box(Vector3(20, 0.08, 0.4), trim)
	stripe.position = Vector3(0, 1.6, 19)
	station.add_child(stripe)
	elevator = Node3D.new()
	elevator.position = Vector3(0, 0.3, -2)
	station.add_child(elevator)
	var pad = Meshes.box(Vector3(8, 0.3, 10), Meshes.mat(Color(0.22, 0.2, 0.18), 0.5, 0.5, Color(0, 0, 0), true))
	elevator.add_child(pad)
	# railings around the pit
	for side in [-4.4, 4.4]:
		var rail = Meshes.box(Vector3(0.15, 1.0, 10), trim)
		rail.position = Vector3(side, 0.8, -2)
		station.add_child(rail)
	_light(Vector3(0, 3.2, 0), 1.1)
	_light(Vector3(0, 3.2, 30), 1.0)
	_light(Vector3(-16, 3.0, 28), 0.8)
	_light(Vector3(2, -3.2, 38), 0.45)
	_light(Vector3(12, 3.0, 24), 0.7)
	_interact_box("panel", "power", "Read wall panel", Vector3(-16, 1.4, 23.4), Vector3(1.2, 0.8, 0.2))
	_interact_box("locker", "hab_locker", "Open locker", Vector3(-20, 1.0, 28), Vector3(0.8, 1.6, 0.6))
	_interact_box("kiosk", "general", "Trade at Kite Commissary", Vector3(-6, 1.0, 34), Vector3(1.6, 2.0, 1.2))
	_interact_box("kiosk", "armorer", "Trade at the armorer", Vector3(6, 1.0, 34), Vector3(1.6, 2.0, 1.2))
	_interact_box("kiosk", "medical", "Trade at medical", Vector3(0, 1.0, 38), Vector3(1.8, 2.0, 1.0))
	_interact_box("kiosk", "fuel", "Buy fuel", Vector3(8, 1.0, 6), Vector3(1.4, 1.6, 1.0))
	_interact_box("terminal", "jobs", "Job board", Vector3(-8, 1.2, 24), Vector3(1.2, 1.4, 0.3))
	_interact_box("terminal", "designer", "Ship designer", Vector3(6, 1.2, 8), Vector3(1.4, 1.6, 0.4))
	_interact_box("terminal", "craft", "Hangar fabricator", Vector3(10, 1.0, 4), Vector3(1.8, 1.2, 0.8))
	_interact_box("terminal", "crew", "Crew desk", Vector3(-4, 1.2, 26), Vector3(1.2, 1.5, 0.4))
	_interact_box("launch", "launch", "Launch skiff", Vector3(3.5, 1.0, 2), Vector3(0.6, 1.2, 0.4))
	_interact_box("call", "call", "Call ship to elevator", Vector3(-3.5, 1.0, 6), Vector3(0.6, 1.2, 0.4))
	_sign("COMMISSARY", Vector3(-6, 2.4, 34))
	_sign("ARMORER", Vector3(6, 2.4, 34))
	_sign("MEDICAL", Vector3(0, 2.4, 38))
	_sign("HAB C-14", Vector3(-16, 2.5, 24))
	for i in 6:
		var ang = float(i)
		var p = Vector3(-8 + (i % 3) * 6.0, 0.6, 22 + int(i / 3) * 4.0)
		_container_box("trash_%d" % i, "Loot trash", p)
	_container_box("under_crate", "Loot crate", Vector3(6, -5.2, 42))
	# moss patches
	for i in 4:
		_harvest("moss", "moss", "Harvest filament moss", Vector3(-4 + i * 2.2, -4.6, 40), Color(0.2, 0.45, 0.22))
	# table in hab
	var table = _static_box(Vector3(1.4, 0.7, 0.8), Vector3(-14, 0.35, 30), Meshes.mat(Color(0.3, 0.24, 0.16), 0.7, 0.05, Color(0, 0, 0), true))
	table.set_meta("surface", "table")
	table.add_to_group("place_table")
	# floor tiles for placement
	_place_marker("floor", Vector3(-15, 0.25, 27))
	_place_marker("floor", Vector3(-18, 0.25, 30))
	_place_marker("wall", Vector3(-16, 1.6, 23.6))
	_place_marker("wall", Vector3(-21.6, 1.6, 28))
	_place_marker("table", Vector3(-14, 0.75, 30))
	# power room visual
	var gen = Meshes.cyl(1.2, 2.4, Meshes.mat(Color(0.3, 0.32, 0.28), 0.4, 0.6, Color(0.2, 0.35, 0.2), false), 12)
	gen.position = Vector3(12, 1.4, 22)
	station.add_child(gen)
	var vat = Meshes.box(Vector3(2.2, 1.4, 1.4), Meshes.mat(Color(0.2, 0.35, 0.22), 0.5, 0.1, Color(0.05, 0.2, 0.08), false))
	vat.position = Vector3(12, 0.9, 34)
	station.add_child(vat)
	var sewage = Meshes.cyl(0.6, 1.6, Meshes.mat(Color(0.25, 0.28, 0.18), 0.6, 0.2, Color(0, 0, 0), false), 10)
	sewage.position = Vector3(10, 0.9, 37)
	station.add_child(sewage)

func _floor(size: Vector3, at: Vector3, material: Material) -> StaticBody3D:
	var body = StaticBody3D.new()
	body.position = at
	body.add_child(Meshes.box(size, material))
	Meshes.add_collision_box(body, size)
	return body

func _wall(at: Vector3, size: Vector3, material: Material) -> void:
	station.add_child(_static_box(size, at + Vector3(0, 0, 0), material))

func _static_box(size: Vector3, at: Vector3, material: Material) -> StaticBody3D:
	var body = StaticBody3D.new()
	body.position = at
	body.add_child(Meshes.box(size, material))
	Meshes.add_collision_box(body, size)
	return body

func _light(at: Vector3, energy: float) -> void:
	var l = OmniLight3D.new()
	l.position = at
	l.light_energy = energy
	l.omni_range = 14
	l.set_meta("base_energy", energy)
	station.add_child(l)

func _sign(text: String, at: Vector3) -> void:
	var lab = Label3D.new()
	lab.text = text
	lab.font_size = 28
	lab.modulate = Color(1.0, 0.72, 0.35)
	lab.position = at
	lab.outline_modulate = Color(0, 0, 0)
	lab.outline_size = 8
	station.add_child(lab)

func _interact_box(kind: String, target: String, prompt: String, at: Vector3, size: Vector3) -> void:
	var body = StaticBody3D.new()
	body.set_script(preload("res://scripts/world/interactable.gd"))
	body.kind = kind
	body.target = target
	body.prompt = prompt
	body.position = at
	var col = Color(0.24, 0.26, 0.28)
	if kind == "kiosk":
		col = Color(0.28, 0.22, 0.16)
	body.add_child(Meshes.box(size, Meshes.mat(col, 0.55, 0.3, Color(0, 0, 0), true)))
	Meshes.add_collision_box(body, size)
	station.add_child(body)
	body.set_meta("marker", target)

func _container_box(id: String, prompt: String, at: Vector3) -> void:
	var body = StaticBody3D.new()
	body.set_script(preload("res://scripts/world/interactable.gd"))
	body.kind = "container"
	body.target = id
	body.prompt = prompt
	body.position = at
	body.add_child(Meshes.box(Vector3(0.7, 0.9, 0.6), Meshes.mat(Color(0.2, 0.22, 0.18), 0.7, 0.2, Color(0, 0, 0), true)))
	Meshes.add_collision_box(body, Vector3(0.7, 0.9, 0.6))
	station.add_child(body)

func _harvest(kind: String, table: String, prompt: String, at: Vector3, color: Color) -> void:
	var body = StaticBody3D.new()
	body.set_script(preload("res://scripts/world/interactable.gd"))
	body.kind = "harvest"
	body.target = table
	body.prompt = prompt
	body.position = at
	body.add_child(Meshes.box(Vector3(0.8, 0.25, 0.6), Meshes.mat(color, 0.9, 0.0, color * 0.2, false)))
	Meshes.add_collision_box(body, Vector3(0.8, 0.4, 0.6))
	body.add_to_group("scannable")
	body.set_meta("label", prompt)
	body.set_meta("kind", kind)
	station.add_child(body)

func _place_marker(surface: String, at: Vector3) -> void:
	var m = MeshInstance3D.new()
	var bm = BoxMesh.new()
	bm.size = Vector3(0.45, 0.08, 0.45)
	m.mesh = bm
	m.position = at
	m.visible = false
	m.set_meta("surface", surface)
	m.material_override = Meshes.mat(Color(0.3, 0.3, 0.3), 0.4, 0, Color(0, 0, 0), false)
	station.add_child(m)
	place_surface_marks.append(m)

func _build_ship() -> void:
	ship = CharacterBody3D.new()
	ship.set_script(preload("res://scripts/ship/ship.gd"))
	ship.world = self
	elevator.add_child(ship)
	ship.position = Vector3(0, 0.9, 0)
	ship.rebuild()
	var hatch = StaticBody3D.new()
	hatch.set_script(preload("res://scripts/world/interactable.gd"))
	hatch.kind = "hatch"
	hatch.target = "ship"
	hatch.prompt = "Board / leave the skiff"
	hatch.position = Vector3(-1.7, 0.4, 0.2)
	hatch.add_child(Meshes.box(Vector3(0.5, 0.9, 0.2), Meshes.mat(Color(0.3, 0.55, 0.52), 0.4, 0.4, Color(0, 0, 0), false)))
	Meshes.add_collision_box(hatch, Vector3(0.5, 0.9, 0.2))
	ship.add_child(hatch)
	for sid in Game.state["ship"]["parts"].keys():
		Game.state["ship"]["condition"][sid] = float(Game.state["ship"]["condition"].get(sid, 100.0))

func _build_player() -> void:
	player = CharacterBody3D.new()
	player.set_script(preload("res://scripts/player/pawn.gd"))
	player.world = self
	station.add_child(player)
	var lp: Array = Game.state["player"]["local"]
	player.position = Vector3(lp[0], lp[1], lp[2])

func _build_station_life() -> void:
	for i in 3:
		if i < Game.state["station"]["raiders"].size() and not bool(Game.state["station"]["raiders"][i]):
			continue
		var a = CharacterBody3D.new()
		a.set_script(preload("res://scripts/world/actors.gd"))
		station.add_child(a)
		a.position = Vector3(-2 + i * 3.0, -5.0, 36 + (i % 2))
		a.setup("raider", self)
		a.set_meta("slot", i)
	for i in 4:
		if i < Game.state["station"]["rats"].size() and not bool(Game.state["station"]["rats"][i]):
			continue
		var r = CharacterBody3D.new()
		r.set_script(preload("res://scripts/world/actors.gd"))
		station.add_child(r)
		r.position = Vector3(i * 1.5, -5.1, 40)
		r.setup("pipewolf", self)

func _build_planet_life() -> void:
	var rng = RandomNumberGenerator.new()
	rng.seed = int(Game.state["planet"].get("seed", 1))
	for i in 8:
		var pos = Game.site_pos(i)
		var kind = "mineral_iron"
		if i % 3 == 1:
			kind = "mineral_copper"
		elif i % 5 == 0:
			kind = "mineral_quartz"
		_planet_node(kind, pos + _tangent(pos) * rng.randf_range(4, 18), "Ore")
	_planet_node("teak", Game.site_pos(0) + _tangent(Game.site_pos(0)) * 8.0, "Veil teak")
	for i in 3:
		var g = CharacterBody3D.new()
		g.set_script(preload("res://scripts/world/actors.gd"))
		planet.add_child(g)
		var p = Game.site_pos(0) + _tangent(Game.site_pos(0)) * (6.0 + i * 4.0)
		g.global_position = p.normalized() * (SimClock.PLANET_R + 1.2)
		g.setup("grazer" if i < 2 else "lope", self)
		_align_node(g, g.global_position.normalized())
	hostile = CharacterBody3D.new()
	hostile.set_script(preload("res://scripts/world/actors.gd"))
	add_child(hostile)
	hostile.global_position = SimClock.station_pos(SimClock.now()) + Vector3(0, 120, 40)
	hostile.setup("hostile", self)
	if not bool(Game.state["world"].get("hostile_alive", true)):
		hostile.queue_free()
		hostile = null

func _planet_node(table: String, pos: Vector3, label: String) -> void:
	var radial = pos.normalized()
	var body = StaticBody3D.new()
	body.set_script(preload("res://scripts/world/interactable.gd"))
	body.kind = "harvest"
	body.target = table
	body.prompt = "Harvest " + label
	planet.add_child(body)
	body.global_position = radial * (SimClock.PLANET_R + 0.6)
	_align_node(body, radial)
	var col = Color(0.45, 0.3, 0.2)
	if "copper" in table:
		col = Color(0.55, 0.35, 0.22)
	elif "quartz" in table:
		col = Color(0.55, 0.3, 0.7)
	elif table == "teak":
		col = Color(0.25, 0.35, 0.18)
	body.add_child(Meshes.box(Vector3(1.2, 0.8, 1.0), Meshes.mat(col, 0.85, 0.05, Color(0, 0, 0), true)))
	Meshes.add_collision_box(body, Vector3(1.2, 0.8, 1.0))
	body.add_to_group("scannable")
	body.set_meta("label", label)
	body.set_meta("kind", table)

func _tangent(pos: Vector3) -> Vector3:
	var up = pos.normalized()
	var t = up.cross(Vector3.UP)
	if t.length() < 0.01:
		t = up.cross(Vector3.FORWARD)
	return t.normalized()

func _align_node(n: Node3D, up: Vector3) -> void:
	var fwd = up.cross(Vector3.FORWARD)
	if fwd.length() < 0.01:
		fwd = up.cross(Vector3.RIGHT)
	fwd = fwd.normalized()
	var right = fwd.cross(up).normalized()
	n.global_basis = Basis(right, up, -fwd)

func _restore_placed() -> void:
	for rec in Game.state["world"].get("placed", []):
		_spawn_placed(rec)

func _spawn_placed(rec: Dictionary) -> void:
	var parent: Node3D = station if str(rec.get("space", "station")) == "station" else planet
	var mi = MeshInstance3D.new()
	var id = str(rec.get("id", ""))
	if "plant" in id:
		mi = Meshes.sphere(0.18, Meshes.mat(Color(0.2, 0.5, 0.25), 0.8, 0, Color(0, 0, 0), false), 8)
		var pot = Meshes.cyl(0.12, 0.16, Meshes.mat(Color(0.4, 0.28, 0.18), 0.7, 0, Color(0, 0, 0), false), 8)
		mi.add_child(pot)
	elif "spike" in id:
		mi = Meshes.cyl(0.08, 0.8, Meshes.mat(Color(0.6, 0.5, 0.2), 0.4, 0.6, Color(0, 0, 0), false), 8)
	else:
		mi = Meshes.box(Vector3(0.7, 0.45, 0.04), Meshes.mat(Color(0.7, 0.55, 0.3), 0.5, 0, Color(0, 0, 0), false))
	parent.add_child(mi)
	var p: Array = rec.get("pos", [0, 0, 0])
	mi.position = Vector3(p[0], p[1], p[2])

func _process(delta: float) -> void:
	if Game.state.is_empty():
		return
	_acc += delta
	var t = SimClock.now()
	station.global_transform = Transform3D(SimClock.station_basis(t), SimClock.station_pos(t))
	planet.rotation.y = SimClock.planet_angle(t)
	_aim_sun()
	var elev_target = 0.3 if str(Game.state["ship"].get("loc", "")) == "hangar" else -6.0
	elevator.position.y = lerpf(elevator.position.y, elev_target, 1.0 - exp(-delta * 0.7 * Game.grid_eff()))
	for l in station.get_children():
		if l is OmniLight3D:
			l.light_energy = float(l.get_meta("base_energy", 1.0)) * Game.grid_eff()
	var alt = 9999.0
	if player:
		alt = SimClock.altitude(player.global_position)
	var in_atmo = alt < SimClock.ATMO_H and str(Game.state["player"].get("space", "")) in ["planet", "space", "ship"]
	env.fog_enabled = in_atmo
	if _acc > 1.0:
		_acc = 0.0
		Game.tick_needs(1.0)
		_check_space()
	if placing != "":
		_update_ghost()

func _physics_process(delta: float) -> void:
	if Game.state.is_empty() or ship == null:
		return
	var seated = bool(Game.state["player"].get("seated", false))
	AudioFx.in_ship = seated or str(Game.state["ship"].get("loc", "")) in ["space", "transit"]
	if str(Game.state["ship"].get("loc", "")) in ["space", "transit"]:
		if ship.get_parent() != self:
			var gt = ship.global_transform
			ship.reparent(self)
			ship.global_transform = gt
			if str(Game.state["ship"]["loc"]) == "space":
				var v: Array = Game.state["ship"]["vel"]
				ship.velocity = Vector3(v[0], v[1], v[2])
		ship.flight(delta, seated and not designer)
	elif str(Game.state["ship"].get("loc", "")) == "surface":
		if ship.get_parent() != planet:
			reparent_ship_surface()
	if buggy and is_instance_valid(buggy):
		buggy.drive(delta, str(Game.state["player"].get("mode", "")) == "buggy")
	if hostile and is_instance_valid(hostile) and bool(Game.state["world"].get("hostile_alive", true)):
		pass

func reparent_ship_surface() -> void:
	var gt = ship.global_transform
	ship.reparent(planet)
	ship.global_transform = gt

func _check_space() -> void:
	var p = player
	if p == null:
		return
	var ship_state = str(Game.state["ship"].get("loc", ""))
	if bool(Game.state["player"].get("seated", false)):
		Game.state["player"]["space"] = "ship" if ship_state != "hangar" else "station"
		return
	if str(Game.state["player"].get("mode", "")) == "buggy":
		Game.state["player"]["space"] = "planet"
		return
	if p.get_parent() == ship:
		Game.state["player"]["space"] = "ship"
		return
	if ship_state == "surface" and player.global_position.distance_to(ship.global_position) < 8.0:
		Game.state["player"]["space"] = "planet"
	elif player.global_position.length() > SimClock.PLANET_R + 30.0 and player.get_parent() == station:
		Game.state["player"]["space"] = "station"
	elif player.get_parent() == planet:
		Game.state["player"]["space"] = "planet"

func marker_pos(id: String) -> Vector3:
	match id:
		"medical":
			return station.to_global(Vector3(0, 1.5, 38))
		"store":
			return station.to_global(Vector3(-6, 1.5, 34))
		"armorer":
			return station.to_global(Vector3(6, 1.5, 34))
		"hangar":
			return station.to_global(Vector3(0, 1.5, 0))
	return station.global_position

func handle_interact(kind: String, target: String, node: Node) -> void:
	match kind:
		"panel":
			ui.open_panel("power")
			Game.flag("panel")
		"locker", "container":
			var items = Game.open_container(target)
			ui.open_container(target, items)
			if str(target).begins_with("trash"):
				Game.flag("trash")
		"kiosk":
			ui.open_shop(target)
		"terminal":
			if target == "designer":
				ui.open_designer()
			elif target == "craft":
				ui.open_craft()
			elif target == "crew":
				ui.open_crew()
			elif target == "jobs":
				ui.open_jobs()
		"launch":
			launch_ship()
		"call":
			call_ship()
		"harvest":
			_harvest_now(target, node)
		"hatch":
			board_ship()
		_:
			pass

func launch_ship() -> void:
	if str(Game.state["ship"].get("loc", "")) != "hangar":
		Game.notify("Ship is not on the elevator.")
		return
	if not bool(Game.state["player"].get("seated", false)):
		Game.notify("Take the seat first. E on the hatch, then G.")
		return
	var t = SimClock.now()
	var inherit = SimClock.station_vel(t)
	Game.state["ship"]["loc"] = "space"
	Game.state["ship"]["vel"] = [inherit.x, inherit.y + 6.0, inherit.z]
	Game.state["ship"]["pos"] = [ship.global_position.x, ship.global_position.y, ship.global_position.z]
	ship.velocity = inherit + station.global_basis.y * 6.0
	Game.flag("launch")
	Game.notify("Released. You have the station's orbital velocity plus a shove outward.")
	AudioFx.play("dock")

func call_ship() -> void:
	var loc = str(Game.state["ship"].get("loc", ""))
	if loc == "hangar":
		Game.notify("The skiff is already on the pad.")
		return
	if loc == "transit":
		Game.notify("It is already on a route.")
		return
	Game.state["ship"]["urgency"] = 0.35
	if Game.engage_transit("station"):
		Game.notify("Calling the ship home. The elevator will meet it.")

func board_ship() -> void:
	if player.get_parent() != ship:
		var hatch: Marker3D = ship.hatch
		player.reparent(ship)
		player.global_position = hatch.global_position
		Game.state["player"]["space"] = "ship"
		Game.state["player"]["seated"] = false
		Game.state["player"]["mode"] = "foot"
		Game.flag("board")
		Game.notify("Aboard. G sits you at the stick. E on the hatch leaves.")
	else:
		# exit
		if str(Game.state["ship"].get("loc", "")) == "hangar":
			player.reparent(station)
			player.global_position = station.to_global(Vector3(3, 1.0, 4))
			Game.state["player"]["space"] = "station"
		elif str(Game.state["ship"].get("loc", "")) == "surface":
			player.reparent(planet)
			player.global_position = ship.global_position + ship.global_basis.x * 2.2
			Game.state["player"]["space"] = "planet"
		else:
			player.reparent(self)
			player.global_position = ship.global_position + ship.global_basis.x * 2.5
			Game.state["player"]["space"] = "space"
			Game.notify("EVA. Oxygen is bleeding.")
		Game.state["player"]["seated"] = false
		Game.state["player"]["mode"] = "foot"

func toggle_seat() -> void:
	if player.get_parent() != ship and str(Game.state["ship"].get("loc", "")) == "hangar":
		board_ship()
	if player.get_parent() != ship:
		Game.notify("You are not aboard.")
		return
	var seated = not bool(Game.state["player"].get("seated", false))
	Game.state["player"]["seated"] = seated
	Game.state["player"]["mode"] = "seat" if seated else "foot"
	if seated:
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
		Game.notify("At the stick. WASD thrust, mouse to aim, X flight assist, Z autopilot, LMB fire.")
	else:
		Game.notify("Out of the seat.")

func _harvest_now(table: String, node: Node) -> void:
	if node.has_meta("depleted_until") and SimClock.now() < float(node.get_meta("depleted_until")):
		Game.notify("Already stripped. It will come back.")
		return
	var rng = RandomNumberGenerator.new()
	rng.randomize()
	var loot = Catalog.loot_table(table, Game.scarcity(), rng)
	var mult = Game.yield_mult()
	for it in loot:
		var n = maxi(1, int(round(float(it["count"]) * mult)))
		Game.add_item(Game.player_inv(), str(it["id"]), n)
	var scarce = clampf((float(Game.scarcity()) - 1.0) / 99.0, 0.0, 1.0)
	node.set_meta("depleted_until", SimClock.now() + lerpf(40.0, 360.0, scarce))
	if node is Node3D:
		node.visible = false
		get_tree().create_timer(lerpf(40.0, 360.0, scarce)).timeout.connect(func():
			if is_instance_valid(node):
				node.visible = true
		)
	Game.add_xp(Game.state["player"], "survival", 4)
	if table == "moss":
		Game.flag("moss")
	if "mineral" in table or table == "teak":
		Game.flag("mine")
	Game.notify("Harvested " + table.replace("_", " ") + ".")

func try_place(item_id: String) -> void:
	placing = item_id
	if place_ghost:
		place_ghost.queue_free()
	place_ghost = Meshes.box(Vector3(0.6, 0.4, 0.06), Meshes.mat(Color(0.2, 0.8, 0.3, 0.5), 0.4, 0, Color(0.2, 0.8, 0.3), false))
	add_child(place_ghost)
	for m in place_surface_marks:
		m.visible = true
		var surface = str(m.get_meta("surface", ""))
		var ok = Catalog.placement_ok(item_id, surface, {})
		var col = Color(0.2, 0.85, 0.35) if ok else Color(0.35, 0.35, 0.35)
		m.material_override = Meshes.mat(col, 0.4, 0, col * 0.3, false)
	if ship:
		ship.set_socket_preview(item_id if Catalog.parts.has(item_id) else "")
	Game.notify("Placement. Green holds. Gray does not. Click to set, Esc to cancel.")

func _update_ghost() -> void:
	if player == null or place_ghost == null:
		return
	var from = player.cam.global_position
	var to = from + -player.cam.global_basis.z * 6.0
	var q = PhysicsRayQueryParameters3D.create(from, to, 1 | 2)
	q.exclude = [player]
	var hit = get_world_3d().direct_space_state.intersect_ray(q)
	if hit.is_empty():
		place_ghost.visible = false
		return
	place_ghost.visible = true
	place_ghost.global_position = hit.position
	var surface = _surface_at(hit)
	var ok = Catalog.placement_ok(placing, surface, {})
	var col = Color(0.2, 0.85, 0.35) if ok else Color(0.4, 0.4, 0.4)
	place_ghost.material_override = Meshes.mat(col, 0.4, 0, col * 0.4, false)

func _surface_at(hit: Dictionary) -> String:
	var n: Vector3 = hit.normal
	if player and str(Game.state["player"].get("space", "")) == "planet":
		return "ground"
	if absf(n.dot(Vector3.UP)) < 0.4 and station:
		return "wall"
	return "floor"

func confirm_place() -> void:
	if placing == "":
		return
	if Game.count_item(Game.player_inv(), placing) <= 0 and placing != "seismic_spike":
		cancel_place()
		return
	var from = player.cam.global_position
	var to = from + -player.cam.global_basis.z * 6.0
	var q = PhysicsRayQueryParameters3D.create(from, to, 1 | 2)
	q.exclude = [player]
	var hit = get_world_3d().direct_space_state.intersect_ray(q)
	if hit.is_empty():
		Game.notify("Nothing to set it on.")
		return
	var surface = _surface_at(hit)
	if not Catalog.placement_ok(placing, surface, {}):
		Game.notify("Invalid surface. Gray means no.")
		AudioFx.play("error")
		return
	Game.take_item(Game.player_inv(), placing, 1)
	var space = "planet" if surface == "ground" else "station"
	var parent: Node3D = planet if space == "planet" else station
	var local = parent.to_local(hit.position)
	var rec = {"id": placing, "space": space, "pos": [local.x, local.y, local.z], "surface": surface}
	Game.state["world"]["placed"].append(rec)
	_spawn_placed(rec)
	Game.notify("Set.")
	cancel_place()

func cancel_place() -> void:
	placing = ""
	if place_ghost:
		place_ghost.queue_free()
		place_ghost = null
	for m in place_surface_marks:
		m.visible = false
	if ship:
		ship.hide_socket_preview()

func deploy_buggy() -> void:
	if not Game.aggregated()["bay"]:
		Game.notify("No vehicle bay on this frame.")
		return
	if str(Game.state["ship"].get("loc", "")) != "surface":
		Game.notify("Deploy on the surface, beside the ship.")
		return
	if not Game.take_anywhere("buggy", 1):
		Game.notify("No buggy in cargo or pockets. Buy one at the commissary.")
		return
	if buggy and is_instance_valid(buggy):
		buggy.queue_free()
	buggy = CharacterBody3D.new()
	buggy.set_script(preload("res://scripts/world/buggy.gd"))
	planet.add_child(buggy)
	buggy.global_position = ship.global_position + ship.global_basis.x * 4.0
	Game.state["planet"]["buggy_deployed"] = true
	Game.flag("buggy")
	Game.notify("Buggy down. E near it to drive. E again to step off. Recall from the ship hatch.")

func mount_buggy() -> void:
	if buggy == null or not is_instance_valid(buggy):
		return
	if player.global_position.distance_to(buggy.global_position) > 3.5:
		return
	if str(Game.state["player"].get("mode", "")) == "buggy":
		Game.state["player"]["mode"] = "foot"
		player.reparent(planet)
		player.global_position = buggy.global_position + buggy.global_basis.x * 1.8
		Game.notify("Off the buggy.")
	else:
		Game.state["player"]["mode"] = "buggy"
		Game.state["player"]["seated"] = false
		player.reparent(buggy)
		Game.notify("Driving. WASD, space brakes a little, V for the view.")

func _sync_bodies() -> void:
	var loc = str(Game.state["ship"].get("loc", "hangar"))
	if loc == "space" or loc == "transit":
		ship.reparent(self)
		var p: Array = Game.state["ship"]["pos"]
		ship.global_position = Vector3(p[0], p[1], p[2])
		var r: Array = Game.state["ship"].get("rot", [0, 0, 0, 1])
		if r.size() == 4:
			ship.global_basis = Basis(Quaternion(r[0], r[1], r[2], r[3]))
		var v: Array = Game.state["ship"]["vel"]
		ship.velocity = Vector3(v[0], v[1], v[2])
	elif loc == "surface":
		reparent_ship_surface()
		var p2: Array = Game.state["ship"]["pos"]
		ship.global_position = Vector3(p2[0], p2[1], p2[2])

func _unhandled_input(event: InputEvent) -> void:
	if event.is_action_pressed("interact") and buggy and is_instance_valid(buggy):
		if player.global_position.distance_to(buggy.global_position) < 3.2 and not bool(Game.state["player"].get("seated", false)):
			mount_buggy()
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT and placing != "":
		confirm_place()
	if event.is_action_pressed("pause") and placing != "":
		cancel_place()
		get_viewport().set_input_as_handled()

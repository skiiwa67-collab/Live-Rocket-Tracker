extends CharacterBody3D
## First-person body, third-person orbit, station gravity, planetary gravity, buggy seat.

var world: Node
var cam: Camera3D
var pivot: Node3D
var body: Node3D
var head: MeshInstance3D
var flashlight: SpotLight3D
var yaw = 0.0
var pitch = 0.0
var third = false
var mode = "foot" # foot, seat, buggy
var weapon_mesh: Node3D
var bob = 0.0
var recoil = 0.0
var fire_cd = 0.0
var reload_t = 0.0
var interact_text = ""
var scan_marks: Array = []
var scan_time = 0.0
var aim = false

func _ready() -> void:
	collision_layer = 2
	collision_mask = 1
	floor_snap_length = 0.3
	pivot = Node3D.new()
	pivot.position = Vector3(0, 1.55, 0)
	add_child(pivot)
	cam = Camera3D.new()
	cam.current = true
	cam.fov = 72
	pivot.add_child(cam)
	flashlight = SpotLight3D.new()
	flashlight.spot_range = 18
	flashlight.light_energy = 2.2
	flashlight.visible = false
	cam.add_child(flashlight)
	_build_body()
	_refresh_gear()

func _build_body() -> void:
	body = Node3D.new()
	add_child(body)
	var suit = Meshes.mat(Color(0.22, 0.28, 0.32), 0.7, 0.15, Color(0, 0, 0), true)
	var skin = Meshes.mat(Color(0.55, 0.4, 0.32), 0.85, 0.0, Color(0, 0, 0), false)
	var torso = Meshes.capsule(0.28, 0.85, suit)
	torso.position = Vector3(0, 1.05, 0)
	body.add_child(torso)
	head = Meshes.sphere(0.16, skin, 12)
	head.position = Vector3(0, 1.62, 0)
	body.add_child(head)
	var arm_l = Meshes.capsule(0.08, 0.55, suit)
	arm_l.position = Vector3(-0.38, 1.15, -0.05)
	arm_l.rotation.z = 0.4
	body.add_child(arm_l)
	var arm_r = Meshes.capsule(0.08, 0.55, suit)
	arm_r.position = Vector3(0.34, 1.05, -0.25)
	arm_r.rotation.x = 1.2
	body.add_child(arm_r)
	var leg_l = Meshes.capsule(0.09, 0.7, suit)
	leg_l.position = Vector3(-0.14, 0.4, 0)
	body.add_child(leg_l)
	var leg_r = Meshes.capsule(0.09, 0.7, suit)
	leg_r.position = Vector3(0.14, 0.4, 0)
	body.add_child(leg_r)
	weapon_mesh = Node3D.new()
	weapon_mesh.position = Vector3(0.32, 1.15, -0.45)
	body.add_child(weapon_mesh)
	var shape = CollisionShape3D.new()
	var cap = CapsuleShape3D.new()
	cap.radius = 0.32
	cap.height = 1.5
	shape.shape = cap
	shape.position = Vector3(0, 0.85, 0)
	add_child(shape)

func _refresh_gear() -> void:
	if weapon_mesh == null:
		return
	for c in weapon_mesh.get_children():
		c.queue_free()
	var wid = str(Game.state.get("player", {}).get("equipped_weapon", ""))
	var w = Catalog.item(wid)
	var col = Color(0.15, 0.15, 0.16)
	match str(w.get("mesh", "pistol")):
		"pistol", "laser":
			weapon_mesh.add_child(Meshes.box(Vector3(0.08, 0.12, 0.28), Meshes.mat(col, 0.4, 0.6, Color(0, 0, 0), false)))
		"sniper":
			weapon_mesh.add_child(Meshes.box(Vector3(0.06, 0.08, 0.85), Meshes.mat(col, 0.35, 0.7, Color(0, 0, 0), false)))
		"rocket":
			weapon_mesh.add_child(Meshes.box(Vector3(0.12, 0.12, 0.7), Meshes.mat(Color(0.35, 0.2, 0.12), 0.5, 0.4, Color(0, 0, 0), false)))
		"bow":
			weapon_mesh.add_child(Meshes.box(Vector3(0.04, 0.55, 0.04), Meshes.mat(Color(0.35, 0.28, 0.16), 0.7, 0.0, Color(0, 0, 0), false)))
		"knife":
			weapon_mesh.add_child(Meshes.box(Vector3(0.02, 0.04, 0.28), Meshes.mat(Color(0.8, 0.82, 0.85), 0.2, 0.8, Color(0, 0, 0), false)))
		_:
			weapon_mesh.add_child(Meshes.box(Vector3(0.07, 0.09, 0.62), Meshes.mat(col, 0.4, 0.55, Color(0, 0, 0), false)))
	var armor_id = str(Game.state["player"].get("equipped_armor", "undersuit"))
	var armor = float(Catalog.item(armor_id).get("armor", 2))
	Game.state["player"]["armor"] = armor
	var tint = Color(0.25, 0.32, 0.3)
	if armor_id == "vest_voss":
		tint = Color(0.35, 0.16, 0.14)
	elif armor_id == "vest_scout":
		tint = Color(0.2, 0.4, 0.38)
	if body and body.get_child_count() > 0 and body.get_child(0) is MeshInstance3D:
		body.get_child(0).material_override = Meshes.mat(tint, 0.65, 0.2, Color(0, 0, 0), true)

func _physics_process(delta: float) -> void:
	if Game.state.is_empty():
		return
	mode = "seat" if bool(Game.state["player"].get("seated", false)) else str(Game.state["player"].get("mode", "foot"))
	if mode == "buggy":
		mode = "buggy"
	fire_cd = maxf(0.0, fire_cd - delta)
	recoil = lerpf(recoil, 0.0, 1.0 - exp(-delta * 8.0))
	if mode == "seat":
		_ride_seat()
		return
	if mode == "buggy":
		_ride_buggy(delta)
		return
	_on_foot(delta)
	if scan_time > 0.0:
		scan_time -= delta

func _ride_seat() -> void:
	velocity = Vector3.ZERO
	if world and world.ship:
		var seat: Marker3D = world.ship.seat
		global_transform = seat.global_transform
	head.visible = third
	cam.position = Vector3(0, 0.15, 0.4) if not third else Vector3(0, 1.2, 4.5)
	cam.rotation = Vector3.ZERO
	if third:
		cam.rotation.x = deg_to_rad(-12)

func _ride_buggy(delta: float) -> void:
	if world and world.buggy:
		global_position = world.buggy.global_position + world.buggy.global_basis.y * 1.1 + world.buggy.global_basis.z * 0.2
		var up = world.buggy.global_basis.y
		up_direction = up
	head.visible = third
	_capture_look(delta)
	cam.position = Vector3(0, 0.3, 0.2) if not third else Vector3(0, 1.6, 4.2)

func _on_foot(delta: float) -> void:
	var space = str(Game.state["player"].get("space", "station"))
	var up = Vector3.UP
	if space == "planet" and world:
		up = global_position.normalized()
	elif space == "ship" and world and world.ship:
		up = world.ship.global_basis.y
	elif space == "station" and world and world.station:
		up = world.station.global_basis.y
	up_direction = up
	_align_up(up)
	_capture_look(delta)
	var speed = 4.4
	if Input.is_action_pressed("sprint"):
		speed = 7.2
	if Input.is_action_pressed("crouch"):
		speed = 2.2
	var input = Vector3.ZERO
	if Input.is_action_pressed("move_forward"):
		input -= global_basis.z
	if Input.is_action_pressed("move_back"):
		input += global_basis.z
	if Input.is_action_pressed("move_left"):
		input -= global_basis.x
	if Input.is_action_pressed("move_right"):
		input += global_basis.x
	if Input.get_connected_joypads().size() > 0:
		var j = Input.get_connected_joypads()[0]
		input += global_basis.x * Input.get_joy_axis(j, JOY_AXIS_LEFT_X)
		input += global_basis.z * Input.get_joy_axis(j, JOY_AXIS_LEFT_Y)
	input = input - up * input.dot(up)
	if input.length() > 1.0:
		input = input.normalized()
	var grav_strength = 9.8
	if space == "planet":
		grav_strength = SimClock.gravity_at(global_position).length()
	elif space == "station":
		grav_strength = 9.8 * Game.grid_eff()
		if Game.grid_strain() > 1.05 and fmod(Time.get_ticks_msec() / 400.0, 1.0) > 0.7:
			grav_strength *= 0.25
	var v_up = up * velocity.dot(up)
	var wish = input * speed
	velocity = v_up + wish
	if is_on_floor():
		if velocity.dot(up) < 0.0:
			velocity -= up * velocity.dot(up)
		if Input.is_action_just_pressed("jump"):
			velocity += up * 4.6
	else:
		velocity -= up * grav_strength * delta
	# Sewage drag in the underbelly.
	if space == "station" and global_position.y < world.station.global_position.y - 2.0 and float(Game.state["station"]["sewage"]) > 85.0:
		velocity *= 0.92
	move_and_slide()
	bob += delta * velocity.length()
	var heavy = float(Catalog.item(str(Game.state["player"]["equipped_weapon"])).get("mass", 1.0))
	cam.position = Vector3(0, sin(bob * 2.0) * 0.03 * heavy, 0)
	cam.rotation.x = pitch - recoil * 0.15
	head.visible = third
	if third:
		cam.position = Vector3(0, 0.4, 2.8)
	_weapons(delta)
	_eva(delta)
	Game.state["player"]["local"] = [position.x, position.y, position.z]
	Game.state["player"]["yaw"] = yaw

func _align_up(up: Vector3) -> void:
	var fwd = -global_basis.z
	fwd = (fwd - up * fwd.dot(up))
	if fwd.length() < 0.001:
		fwd = global_basis.x.cross(up)
	fwd = fwd.normalized()
	var right = fwd.cross(up).normalized()
	global_basis = Basis(right, up, -fwd).orthonormalized()

func _capture_look(delta: float) -> void:
	if Input.get_mouse_mode() != Input.MOUSE_MODE_CAPTURED:
		return
	var sens = float(Game.state["settings"].get("mouse_sens", 0.0022))
	var md = Input.get_last_mouse_velocity()
	# get_last_mouse_velocity is pixels per second; use event in _unhandled_input instead.
	delta = delta

func _unhandled_input(event: InputEvent) -> void:
	if Game.state.is_empty():
		return
	if event is InputEventMouseMotion and Input.get_mouse_mode() == Input.MOUSE_MODE_CAPTURED:
		if world and world.ui and world.ui.blocks_look():
			return
		var sens = float(Game.state["settings"].get("mouse_sens", 0.0022))
		var mot: InputEventMouseMotion = event
		var invert = 1.0 if not bool(Game.state["settings"].get("invert_y", false)) else -1.0
		if mode == "seat":
			return
		yaw -= mot.relative.x * sens
		pitch -= mot.relative.y * sens * invert
		pitch = clampf(pitch, deg_to_rad(-80), deg_to_rad(75))
		rotate_object_local(Vector3(0, 1, 0), -mot.relative.x * sens)
		if mode != "seat":
			pivot.rotation.x = pitch
	if event.is_action_pressed("view_toggle"):
		third = not third
		Game.state["player"]["third_person"] = third
	if event.is_action_pressed("flashlight"):
		flashlight.visible = not flashlight.visible
	if event.is_action_pressed("scanner"):
		_scan()
	if event.is_action_pressed("interact"):
		_interact()
	if event.is_action_pressed("seat_toggle"):
		if world:
			world.toggle_seat()

func _weapons(delta: float) -> void:
	if reload_t > 0.0:
		reload_t -= delta
		return
	aim = Input.is_action_pressed("aim")
	cam.fov = lerpf(cam.fov, 52.0 if aim else 72.0, 1.0 - exp(-delta * 10.0))
	var wid = str(Game.state["player"]["equipped_weapon"])
	var w = Catalog.item(wid)
	if w.is_empty():
		return
	if Input.is_action_just_pressed("weapon_next"):
		_cycle_weapon()
	if Input.is_action_just_pressed("reload"):
		_reload(w)
	var kind = str(w.get("kind", "kinetic"))
	var want = Input.is_action_pressed("fire") if kind != "bow" else Input.is_action_just_released("fire")
	if kind == "bow" and Input.is_action_pressed("fire"):
		recoil = minf(1.0, recoil + delta)
		return
	if not want or fire_cd > 0.0:
		return
	if kind == "melee":
		_melee(float(w.get("damage", 12)))
		fire_cd = float(w.get("rate", 0.4))
		AudioFx.play("hit")
		return
	var ammo_id = str(w.get("ammo", "none"))
	var mag = int(Game.state["player"]["mag"].get(wid, 0))
	if ammo_id != "none":
		if mag <= 0:
			_reload(w)
			return
		Game.state["player"]["mag"][wid] = mag - 1
	var dmg = float(w.get("damage", 8))
	dmg *= 1.0 + Game.skill_bonus_of(Game.state["player"], "gunnery") * 0.8
	recoil = clampf(0.4 + float(w.get("mass", 1.0)) * 0.25, 0.2, 1.4)
	recoil *= 1.0 - minf(0.4, Game.skill_bonus_of(Game.state["player"], "gunnery"))
	if aim:
		recoil *= 0.65
	fire_cd = float(w.get("rate", 0.2))
	AudioFx.play("rocket" if kind == "rocket" else ("laser" if kind == "laser" else "shot"))
	if kind in ["kinetic", "laser"]:
		_hitscan(dmg, kind == "laser")
	else:
		_projectile(dmg, kind)
	Game.add_xp(Game.state["player"], "gunnery", 0.8)

func _hitscan(dmg: float, laser: bool) -> void:
	var from = cam.global_position
	var to = from + -cam.global_basis.z * 180.0
	var q = PhysicsRayQueryParameters3D.create(from, to, 1 | 4)
	q.exclude = [self]
	var hit = get_world_3d().direct_space_state.intersect_ray(q)
	var end = to
	if not hit.is_empty():
		end = hit.position
		var n = hit.collider
		if n and n.has_method("hurt"):
			n.hurt(dmg)
			AudioFx.play("hit")
	_tracer(from, end, Color(0.6, 0.95, 1) if laser else Color(1, 0.85, 0.4))

func _projectile(dmg: float, kind: String) -> void:
	var bolt = Area3D.new()
	var col = Color(0.4, 1, 0.7) if kind == "plasma" else Color(1, 0.5, 0.2)
	bolt.add_child(Meshes.sphere(0.08 if kind != "rocket" else 0.16, Meshes.mat(col, 0.3, 0.2, col, false), 6))
	var shape = CollisionShape3D.new()
	var s = SphereShape3D.new()
	s.radius = 0.15
	shape.shape = s
	bolt.add_child(shape)
	bolt.global_position = cam.global_position + -cam.global_basis.z * 0.8
	var speed = 40.0 if kind == "rocket" else (28.0 if kind == "plasma" else 36.0)
	if kind == "bow":
		speed = 22.0 + recoil * 30.0
	bolt.set_meta("vel", -cam.global_basis.z * speed)
	bolt.set_meta("life", 4.0)
	bolt.set_meta("damage", dmg)
	bolt.set_meta("ballistic", true)
	bolt.collision_mask = 1 | 4
	get_tree().current_scene.add_child(bolt)
	bolt.body_entered.connect(func(body):
		if body == self:
			return
		if body.has_method("hurt"):
			body.hurt(dmg)
		bolt.queue_free()
	)
	bolt.set_script(preload("res://scripts/ship/bolt.gd"))

func _melee(dmg: float) -> void:
	var from = cam.global_position
	var to = from + -cam.global_basis.z * 2.0
	var q = PhysicsRayQueryParameters3D.create(from, to, 1 | 4)
	q.exclude = [self]
	var hit = get_world_3d().direct_space_state.intersect_ray(q)
	if not hit.is_empty() and hit.collider and hit.collider.has_method("hurt"):
		hit.collider.hurt(dmg * 1.2)

func _tracer(a: Vector3, b: Vector3, color: Color) -> void:
	var mi = MeshInstance3D.new()
	var mesh = ImmediateMesh.new()
	var mat = StandardMaterial3D.new()
	mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mat.albedo_color = color
	mesh.surface_begin(Mesh.PRIMITIVE_LINES, mat)
	mesh.surface_add_vertex(a)
	mesh.surface_add_vertex(b)
	mesh.surface_end()
	mi.mesh = mesh
	get_tree().current_scene.add_child(mi)
	get_tree().create_timer(0.05).timeout.connect(mi.queue_free)

func _reload(w: Dictionary) -> void:
	var ammo_id = str(w.get("ammo", "none"))
	if ammo_id == "none":
		return
	var wid = str(w.get("id", ""))
	var mag_max = int(w.get("mag", 1))
	var have = int(Game.state["player"]["ammo"].get(ammo_id, 0))
	var cur = int(Game.state["player"]["mag"].get(wid, 0))
	if have <= 0 or cur >= mag_max:
		return
	var need = mag_max - cur
	var take = mini(need, have)
	Game.state["player"]["ammo"][ammo_id] = have - take
	Game.state["player"]["mag"][wid] = cur + take
	reload_t = 0.7 - minf(0.3, Game.skill_bonus_of(Game.state["player"], "gunnery"))
	AudioFx.play("click")

func _cycle_weapon() -> void:
	var ids: Array = []
	for it in Game.player_inv():
		var d = Catalog.item(str(it["id"]))
		if str(d.get("category", "")) == "weapon_ground":
			ids.append(str(it["id"]))
	if ids.is_empty():
		return
	var cur = str(Game.state["player"]["equipped_weapon"])
	var idx = ids.find(cur)
	Game.state["player"]["equipped_weapon"] = ids[(idx + 1) % ids.size()]
	_refresh_gear()

func _scan() -> void:
	scan_time = 8.0
	scan_marks.clear()
	AudioFx.play("blip")
	var space = str(Game.state["player"].get("space", "station"))
	var agg = Game.aggregated()
	var op = Game.crew_by_role("scanner")
	var bonus = 1.0 + Game.skill_bonus_of(Game.state["player"], "scanning")
	if not op.is_empty():
		bonus += Game.skill_bonus_of(op, "scanning")
		Game.add_xp(op, "scanning", 6)
	Game.add_xp(Game.state["player"], "scanning", 4)
	if mode == "seat":
		Game.flag("scan_orbit")
		for i in 8:
			scan_marks.append({"name": Game.site_name(i), "pos": Game.site_pos(i), "kind": "site", "idx": i})
		Game.notify("Orbit scan. %d surface signatures." % scan_marks.size())
		if not Game.state["planet"]["scanned_sites"].has(0):
			Game.state["planet"]["scanned_sites"].append(0)
		return
	if space == "station":
		Game.flag("scan_station")
		scan_marks.append({"name": "Medical", "pos": world.marker_pos("medical"), "kind": "fac"})
		scan_marks.append({"name": "Store", "pos": world.marker_pos("store"), "kind": "fac"})
		scan_marks.append({"name": "Armorer", "pos": world.marker_pos("armorer"), "kind": "fac"})
		scan_marks.append({"name": "Hangar", "pos": world.marker_pos("hangar"), "kind": "fac"})
		Game.notify("Station scan. Facilities marked.")
		return
	Game.flag("mine")
	var reach = float(agg["scan_ground"]) * bonus
	for n in get_tree().get_nodes_in_group("scannable"):
		if global_position.distance_to(n.global_position) <= reach:
			scan_marks.append({"name": str(n.get_meta("label", "contact")), "pos": n.global_position, "kind": str(n.get_meta("kind", "poi"))})
	Game.notify("Ground scan. %d contacts." % scan_marks.size())

func _interact() -> void:
	if world and world.ui and world.ui.blocks_look():
		return
	var from = cam.global_position
	var to = from + -cam.global_basis.z * 3.2
	var q = PhysicsRayQueryParameters3D.create(from, to, 1 | 2 | 4)
	q.exclude = [self]
	var hit = get_world_3d().direct_space_state.intersect_ray(q)
	if hit.is_empty():
		return
	var col = hit.collider
	if col and col.has_method("on_interact"):
		col.on_interact(world, self)
		AudioFx.play("click")

func _eva(delta: float) -> void:
	var space = str(Game.state["player"].get("space", ""))
	if space == "space":
		var bonus = 1.0 + Game.skill_bonus_of(Game.state["player"], "survival")
		Game.state["player"]["o2"] = float(Game.state["player"]["o2"]) - delta * (3.5 / bonus)
		if float(Game.state["player"]["o2"]) <= 0.0:
			Game.damage_player(12.0 * delta)
	else:
		Game.state["player"]["o2"] = minf(100.0, float(Game.state["player"]["o2"]) + delta * 8.0)

func probe_interact() -> String:
	if cam == null:
		return ""
	var from = cam.global_position
	var to = from + -cam.global_basis.z * 3.2
	var q = PhysicsRayQueryParameters3D.create(from, to, 1 | 2 | 4)
	q.exclude = [self]
	if get_world_3d() == null:
		return ""
	var hit = get_world_3d().direct_space_state.intersect_ray(q)
	if hit.is_empty():
		return ""
	var col = hit.collider
	if col and col.has_meta("prompt"):
		return str(col.get_meta("prompt"))
	return ""

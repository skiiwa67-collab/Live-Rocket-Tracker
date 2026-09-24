extends CharacterBody3D
## Modular ship. Hardpoints, brand meshes, Newtonian flight, autopilot arc.

var world: Node
var mesh_root: Node3D
var socket_markers = {}
var plume_l: CPUParticles3D
var plume_r: CPUParticles3D
var cam_anchor: Marker3D
var seat: Marker3D
var hatch: Marker3D
var textured = true
var throttle = 0.0
var ang_vel = Vector3.ZERO
var prev_vel = Vector3.ZERO
var landed = false

func _ready() -> void:
	motion_mode = MOTION_MODE_FLOATING
	collision_layer = 1
	collision_mask = 1
	mesh_root = Node3D.new()
	add_child(mesh_root)
	cam_anchor = Marker3D.new()
	cam_anchor.position = Vector3(0, 1.15, -0.2)
	add_child(cam_anchor)
	seat = Marker3D.new()
	seat.position = Vector3(0, 0.55, -0.35)
	add_child(seat)
	hatch = Marker3D.new()
	hatch.position = Vector3(-1.6, 0.2, 0.2)
	add_child(hatch)
	floor_constant_speed = false

func rebuild() -> void:
	for c in mesh_root.get_children():
		c.queue_free()
	socket_markers.clear()
	var frame = Game.frame_def()
	var brand = Catalog.brand(str(frame.get("brand", "lumen")))
	var hull_mat = Meshes.mat(brand["hull"], 0.62, 0.45, Color(0, 0, 0), textured)
	var trim_mat = Meshes.mat(brand["trim"], 0.4, 0.55, Color(0, 0, 0), textured)
	var glow_mat = Meshes.mat(brand["glow"], 0.3, 0.2, brand["glow"], false)
	var medium = str(frame.get("size", "light")) == "medium"
	var scale = 1.22 if medium else 1.0
	_build_frame(str(frame.get("mesh", "")), hull_mat, trim_mat, glow_mat, brand, scale)
	for hp in frame.get("hardpoints", []):
		var marker = MeshInstance3D.new()
		var bm = BoxMesh.new()
		bm.size = Vector3(0.28, 0.28, 0.28)
		marker.mesh = bm
		marker.position = hp["pos"]
		marker.visible = false
		marker.material_override = Meshes.mat(Color(0.4, 0.4, 0.4), 0.5, 0.1, Color(0, 0, 0), false)
		mesh_root.add_child(marker)
		socket_markers[hp["id"]] = {"mesh": marker, "socket": hp}
		var pid = Game.socket_part(str(hp["id"]))
		if pid != "":
			_attach_part(pid, hp["pos"], brand)
	_ensure_collision(scale)
	_ensure_plumes(brand)

func _build_frame(kind: String, hull: Material, trim: Material, glow: Material, brand: Dictionary, scale: float) -> void:
	var voss: bool = str(brand.get("id")) == "voss"
	var body = Meshes.box(Vector3(2.4, 0.85, 4.6) * Vector3(1, 1, scale), hull)
	mesh_root.add_child(body)
	var nose = Meshes.prism(Vector3(2.1, 0.7, 1.8), hull)
	nose.position = Vector3(0, 0.05, -2.7 - (0.4 if scale > 1.0 else 0.0))
	nose.rotation.x = PI
	if voss:
		nose.rotation.z = 0.0
	mesh_root.add_child(nose)
	var canopy = Meshes.box(Vector3(0.9, 0.42, 1.1), Meshes.glass())
	canopy.position = Vector3(0, 0.62, -0.7)
	mesh_root.add_child(canopy)
	var wing_l = Meshes.box(Vector3(1.7, 0.08, 1.3), trim)
	wing_l.position = Vector3(-1.9, 0.05, 0.4)
	mesh_root.add_child(wing_l)
	var wing_r = wing_l.duplicate()
	wing_r.position.x = 1.9
	mesh_root.add_child(wing_r)
	if voss:
		var fin = Meshes.box(Vector3(0.08, 0.9, 0.8), trim)
		fin.position = Vector3(0, 0.85, 1.5)
		mesh_root.add_child(fin)
		var spike = Meshes.box(Vector3(0.12, 0.12, 1.4), trim)
		spike.position = Vector3(0, 0.15, -3.5)
		mesh_root.add_child(spike)
	else:
		var ring = Meshes.cyl(0.55, 0.12, trim, 20)
		ring.position = Vector3(0, 0.15, -2.55)
		ring.rotation.x = PI / 2.0
		mesh_root.add_child(ring)
	var logo = Meshes.box(Vector3(0.7, 0.45, 0.02), Meshes.mat(Color(1, 1, 1), 0.5, 0.1, Color(0, 0, 0), false))
	logo.position = Vector3(0, 0.15, 1.35)
	var lm: StandardMaterial3D = logo.material_override
	lm.albedo_texture = Meshes.logo("voss" if voss else "lumen")
	lm.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	mesh_root.add_child(logo)
	if scale > 1.0:
		var belly = Meshes.box(Vector3(1.6, 0.35, 2.2), hull)
		belly.position = Vector3(0, -0.45, 0.6)
		mesh_root.add_child(belly)

func _attach_part(pid: String, pos: Vector3, _frame_brand: Dictionary) -> void:
	var p = Catalog.part(pid)
	var b = Catalog.brand(str(p.get("brand", "lumen")))
	var hull = Meshes.mat(b["hull"], 0.5, 0.5, Color(0, 0, 0), textured)
	var trim = Meshes.mat(b["trim"], 0.35, 0.6, Color(0, 0, 0), textured)
	var glow = Meshes.mat(b["glow"], 0.2, 0.2, b["glow"], false)
	var root = Node3D.new()
	root.position = pos
	mesh_root.add_child(root)
	match str(p.get("socket", "")):
		"engine":
			if str(b["id"]) == "voss":
				var bell = Meshes.cone(0.42, 1.15, trim)
				bell.rotation.x = PI / 2.0
				bell.position = Vector3(0, 0, 0.55)
				root.add_child(bell)
				var core = Meshes.cyl(0.16, 0.7, glow, 10)
				core.rotation.x = PI / 2.0
				root.add_child(core)
			else:
				var fan = Meshes.cyl(0.48, 0.28, trim, 18)
				fan.rotation.x = PI / 2.0
				fan.position = Vector3(0, 0, 0.35)
				root.add_child(fan)
				var hub = Meshes.cyl(0.16, 0.5, glow, 10)
				hub.rotation.x = PI / 2.0
				root.add_child(hub)
		"weapon":
			if str(p.get("kind", "")) == "laser":
				var barrel = Meshes.box(Vector3(0.12, 0.12, 1.3), trim)
				barrel.position = Vector3(0, 0, -0.6)
				root.add_child(barrel)
				var tip = Meshes.sphere(0.1, glow, 8)
				tip.position = Vector3(0, 0, -1.3)
				root.add_child(tip)
			else:
				var gun = Meshes.box(Vector3(0.22, 0.22, 1.1), hull)
				gun.position = Vector3(0, 0, -0.4)
				root.add_child(gun)
				var muzzle = Meshes.cyl(0.1, 0.4, trim, 8)
				muzzle.rotation.x = PI / 2.0
				muzzle.position = Vector3(0, 0, -1.0)
				root.add_child(muzzle)
		"scanner":
			var vane = Meshes.box(Vector3(0.9, 0.06, 0.2), trim)
			root.add_child(vane)
			var eye = Meshes.sphere(0.12, glow, 8)
			root.add_child(eye)
		"shield":
			var blk = Meshes.box(Vector3(0.35, 0.22, 0.35), trim)
			root.add_child(blk)
		"core", "capacitor", "conduit", "autopilot", "bay":
			var blk2 = Meshes.box(Vector3(0.32, 0.18, 0.4), hull if str(p.get("socket")) != "core" else glow)
			root.add_child(blk2)
		_:
			var blk3 = Meshes.box(Vector3(0.3, 0.2, 0.3), hull)
			root.add_child(blk3)

func _ensure_collision(scale: float) -> void:
	for c in get_children():
		if c is CollisionShape3D:
			c.queue_free()
	var shape = CollisionShape3D.new()
	var box = BoxShape3D.new()
	box.size = Vector3(3.6, 1.3, 6.2 * scale)
	shape.shape = box
	add_child(shape)

func _ensure_plumes(brand: Dictionary) -> void:
	if plume_l == null:
		plume_l = _make_plume(brand["glow"])
		plume_r = _make_plume(brand["glow"])
		add_child(plume_l)
		add_child(plume_r)
	plume_l.position = Vector3(-1.2, 0.12, 2.8)
	plume_r.position = Vector3(1.2, 0.12, 2.8)
	plume_l.color = brand["glow"]
	plume_r.color = brand["glow"]

func _make_plume(color: Color) -> CPUParticles3D:
	var p = CPUParticles3D.new()
	p.amount = 32
	p.lifetime = 0.35
	p.local_coords = false
	p.direction = Vector3(0, 0, 1)
	p.spread = 8
	p.initial_velocity_min = 6
	p.initial_velocity_max = 14
	p.gravity = Vector3.ZERO
	p.color = color
	p.emitting = false
	var mesh = SphereMesh.new()
	mesh.radius = 0.08
	mesh.height = 0.16
	p.mesh = mesh
	return p

func set_socket_preview(part_id: String) -> void:
	for sid in socket_markers.keys():
		var entry: Dictionary = socket_markers[sid]
		var mesh: MeshInstance3D = entry["mesh"]
		mesh.visible = part_id != ""
		var ok = part_id != "" and Catalog.can_fit(Game.ship_frame_id(), part_id, entry["socket"])
		var col = Color(0.25, 0.85, 0.35) if ok else Color(0.35, 0.35, 0.35)
		mesh.material_override = Meshes.mat(col, 0.4, 0.0, col * 0.4, false)

func hide_socket_preview() -> void:
	for sid in socket_markers.keys():
		socket_markers[sid]["mesh"].visible = false

func flight(delta: float, piloting: bool) -> void:
	var ship: Dictionary = Game.state["ship"]
	if str(ship.get("loc", "")) == "transit":
		Game._apply_transit_progress(false)
		if Game.transit_u() >= 1.0:
			return
		var p = ship["pos"]
		global_position = Vector3(p[0], p[1], p[2])
		var v = ship["vel"]
		velocity = Vector3(v[0], v[1], v[2])
		if velocity.length() > 0.1:
			look_at(global_position + velocity.normalized(), global_position.normalized() if global_position.length() > 10 else Vector3.UP)
		throttle = 0.65
		_plumes(true)
		return
	if str(ship.get("loc", "")) != "space":
		throttle = 0.0
		_plumes(false)
		return
	var agg = Game.aggregated()
	var mass = maxf(float(agg["mass"]), 400.0)
	var g = SimClock.gravity_at(global_position)
	var density = SimClock.atmo_density(global_position)
	var thrust_dir = Vector3.ZERO
	if piloting:
		if bool(ship.get("flight_assist", true)):
			if Input.is_action_pressed("move_forward"):
				throttle = minf(1.0, throttle + delta * 0.45)
			elif Input.is_action_pressed("move_back"):
				throttle = maxf(-0.35, throttle - delta * 0.45)
		else:
			throttle = 0.0
			if Input.is_action_pressed("move_forward"):
				thrust_dir += -global_basis.z
			if Input.is_action_pressed("move_back"):
				thrust_dir += global_basis.z
		if Input.is_action_pressed("move_left"):
			thrust_dir += -global_basis.x
		if Input.is_action_pressed("move_right"):
			thrust_dir += global_basis.x
		if Input.is_action_pressed("thrust_up"):
			thrust_dir += global_basis.y
		if Input.is_action_pressed("thrust_down"):
			thrust_dir -= global_basis.y
		var joy = _joy_axes()
		thrust_dir += -global_basis.x * joy.x
		thrust_dir += -global_basis.z * (-joy.y)
		var look = _look_input()
		var pitch = look.y
		var yaw = -look.x
		var roll = 0.0
		if Input.is_action_pressed("roll_left"):
			roll += 1.0
		if Input.is_action_pressed("roll_right"):
			roll -= 1.0
		roll += joy.z
		ang_vel.x = lerpf(ang_vel.x, pitch * 1.4, 1.0 - exp(-delta * 6.0))
		ang_vel.y = lerpf(ang_vel.y, yaw * 1.1, 1.0 - exp(-delta * 6.0))
		ang_vel.z = lerpf(ang_vel.z, roll * 1.6, 1.0 - exp(-delta * 4.0))
		rotate_object_local(Vector3(1, 0, 0), ang_vel.x * delta)
		rotate_object_local(Vector3(0, 1, 0), ang_vel.y * delta)
		rotate_object_local(Vector3(0, 0, 1), ang_vel.z * delta)
	var boost = Input.is_action_pressed("boost") and piloting and float(ship["fuel"]) > 0.0
	var main = -global_basis.z * throttle
	if bool(ship.get("flight_assist", true)):
		thrust_dir += main
	var thrust_mag = float(agg["thrust"]) * (1.55 if boost else 1.0)
	if float(ship["fuel"]) <= 0.0:
		thrust_mag = 0.0
	var accel = Vector3.ZERO
	if thrust_dir.length() > 0.01 and thrust_mag > 0.0:
		accel += thrust_dir.normalized() * (thrust_mag / mass)
	accel += g
	var drag = density * 0.08
	accel += -velocity * drag * velocity.length()
	if bool(ship.get("flight_assist", true)) and piloting:
		var forward_speed = (-global_basis.z).dot(velocity)
		var target = throttle * 160.0 * (1.4 if boost else 1.0)
		var along = -global_basis.z * target
		var lateral = velocity - (-global_basis.z) * forward_speed
		accel += -lateral * 1.8
		accel += (-global_basis.z) * (target - forward_speed) * 0.8
		if not boost:
			along = along
	velocity += accel * delta
	if velocity.length() > 420.0:
		velocity = velocity.normalized() * 420.0
	var burned = (absf(throttle) + thrust_dir.length() * 0.25) * float(agg["fuel_factor"]) * delta * (2.2 if boost else 0.85)
	burned += float(agg["fuel_draw"]) * delta * 8.0
	ship["fuel"] = maxf(0.0, float(ship["fuel"]) - burned)
	var wear = burned * float(agg["wear_factor"]) * 0.15
	if density > 0.2 and velocity.length() > 80.0:
		wear += delta * density * velocity.length() * 0.002 * float(agg["wear_factor"])
	for sid in ship["parts"].keys():
		if str(Catalog.part(str(ship["parts"][sid])).get("socket", "")) == "engine":
			ship["condition"][sid] = maxf(1.0, float(ship["condition"].get(sid, 100.0)) - wear)
	var cap_max = float(agg["cap_max"])
	if cap_max <= 0.0:
		cap_max = 40.0
	var regen = float(agg["cap_regen"]) * float(agg["throughput"]) * (1.0 - float(agg["loss"]))
	ship["capacitor"] = minf(cap_max, float(ship.get("capacitor", 0)) + regen * delta)
	ship["shield"] = minf(float(agg["shield_max"]), float(ship.get("shield", 0)) + float(agg["shield_regen"]) * delta * float(agg["throughput"]))
	prev_vel = velocity
	var collided = move_and_collide(velocity * delta)
	if collided:
		var n: Vector3 = collided.get_normal()
		velocity = velocity.slide(n) * 0.4
		var impact = collided.get_remainder().length() / maxf(delta, 0.001)
		if impact > 18.0:
			Game.damage_ship(impact * 0.15)
			AudioFx.play("creak")
		if SimClock.altitude(global_position) < 12.0 and velocity.length() < 12.0:
			_touchdown()
	if SimClock.altitude(global_position) < 4.0 and velocity.length() < 9.0 and g.dot(global_position.normalized()) < 0.0:
		_touchdown()
	ship["pos"] = [global_position.x, global_position.y, global_position.z]
	ship["vel"] = [velocity.x, velocity.y, velocity.z]
	var q = global_transform.basis.get_rotation_quaternion()
	ship["rot"] = [q.x, q.y, q.z, q.w]
	var gnow = (velocity - prev_vel).length() / maxf(delta, 0.001) / 9.8
	AudioFx.g_force = gnow
	AudioFx.throttle = clampf(absf(throttle) + (0.3 if boost else 0.0), 0.0, 1.0)
	_plumes(thrust_mag > 0.0 and float(ship["fuel"]) > 0.0 and (absf(throttle) > 0.05 or thrust_dir.length() > 0.1))
	if piloting and Input.is_action_just_pressed("flight_assist"):
		ship["flight_assist"] = not bool(ship["flight_assist"])
		Game.notify("Flight assist %s" % ("on" if ship["flight_assist"] else "off"))
	if piloting and Input.is_action_just_pressed("fire"):
		_fire()

func _touchdown() -> void:
	var ship: Dictionary = Game.state["ship"]
	if str(ship.get("loc", "")) != "space":
		return
	if velocity.length() > 22.0:
		Game.damage_ship(velocity.length() * 0.4)
		Game.notify("Hard landing.")
	else:
		Game.notify("Gear down on Vesper.")
	ship["loc"] = "surface"
	var p = global_position.normalized() * (SimClock.PLANET_R + 1.5)
	global_position = p
	velocity = Vector3.ZERO
	ship["pos"] = [p.x, p.y, p.z]
	ship["vel"] = [0, 0, 0]
	var up = p.normalized()
	var fwd = (-global_basis.z - up * (-global_basis.z).dot(up)).normalized()
	if fwd.length() < 0.1:
		fwd = up.cross(Vector3.FORWARD).normalized()
	global_transform = Transform3D(Basis(fwd.cross(up).normalized(), up, -fwd), p)
	Game.flag("land")
	if world and world.has_method("reparent_ship_surface"):
		world.reparent_ship_surface()

func _fire() -> void:
	var agg = Game.aggregated()
	if agg["weapons"].is_empty():
		return
	var w: Dictionary = agg["weapons"][0]["part"]
	var kind = str(w.get("kind", "laser"))
	var ship: Dictionary = Game.state["ship"]
	if kind == "laser":
		var cost = 8.0
		if float(ship["capacitor"]) < cost:
			AudioFx.play("error")
			return
		ship["capacitor"] = float(ship["capacitor"]) - cost
		AudioFx.play("laser")
		_spawn_bolt(28.0, Color(0.5, 1.0, 0.9), 220.0, false)
	else:
		if int(ship["ammo"].get("shells", 0)) <= 0:
			AudioFx.play("error")
			return
		ship["ammo"]["shells"] = int(ship["ammo"]["shells"]) - 1
		AudioFx.play("shot")
		_spawn_bolt(float(w.get("damage", 18)), Color(1.0, 0.65, 0.3), 160.0, true)
	var gunner = Game.state["player"]
	Game.add_xp(gunner, "gunnery", 1.5)

func _spawn_bolt(damage: float, color: Color, speed: float, ballistic: bool) -> void:
	var bolt = Area3D.new()
	bolt.collision_layer = 8
	bolt.collision_mask = 1 | 4
	var mi = Meshes.sphere(0.12, Meshes.mat(color, 0.2, 0.1, color, false), 6)
	bolt.add_child(mi)
	var shape = CollisionShape3D.new()
	var sph = SphereShape3D.new()
	sph.radius = 0.2
	shape.shape = sph
	bolt.add_child(shape)
	bolt.global_position = global_position + (-global_basis.z) * 3.0
	var vel = -global_basis.z * speed
	var life = 3.0
	bolt.set_meta("vel", vel)
	bolt.set_meta("life", life)
	bolt.set_meta("damage", damage)
	bolt.set_meta("ballistic", ballistic)
	get_tree().current_scene.add_child(bolt)
	bolt.body_entered.connect(func(body):
		if body == self:
			return
		if body.has_method("hurt"):
			body.hurt(damage)
		elif body.is_in_group("hostile_ship"):
			body.hurt(damage)
		bolt.queue_free()
	)
	bolt.set_script(preload("res://scripts/ship/bolt.gd"))

func _plumes(on: bool) -> void:
	if plume_l:
		plume_l.emitting = on
		plume_r.emitting = on

func _look_input() -> Vector2:
	var sens = DisplayCfg.mouse_sens
	var look = DisplayCfg.look_scale()
	var v = Vector2.ZERO
	if Input.get_mouse_mode() == Input.MOUSE_MODE_CAPTURED:
		v = Input.get_last_mouse_velocity() * sens * 0.06
		v.x *= look.x
		v.y *= look.y
	if Input.get_connected_joypads().size() > 0:
		var j = Input.get_connected_joypads()[0]
		v.x += Input.get_joy_axis(j, JOY_AXIS_RIGHT_X) * 0.03
		v.y += Input.get_joy_axis(j, JOY_AXIS_RIGHT_Y) * 0.03
	return v

func _joy_axes() -> Vector3:
	if Input.get_connected_joypads().is_empty():
		return Vector3.ZERO
	var j = Input.get_connected_joypads()[0]
	return Vector3(
		Input.get_joy_axis(j, JOY_AXIS_LEFT_X),
		Input.get_joy_axis(j, JOY_AXIS_LEFT_Y),
		Input.get_joy_axis(j, JOY_AXIS_TRIGGER_RIGHT) - Input.get_joy_axis(j, JOY_AXIS_TRIGGER_LEFT)
	)

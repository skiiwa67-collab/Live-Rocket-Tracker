extends CharacterBody3D

var throttle = 0.0
var steer = 0.0

func _ready() -> void:
	motion_mode = MOTION_MODE_FLOATING
	collision_layer = 1
	collision_mask = 1
	var body = Meshes.box(Vector3(1.6, 0.6, 2.6), Meshes.mat(Color(0.72, 0.55, 0.28), 0.6, 0.3, Color(0, 0, 0), true))
	body.position = Vector3(0, 0.55, 0)
	add_child(body)
	var cage = Meshes.box(Vector3(1.3, 0.5, 1.2), Meshes.mat(Color(0.2, 0.22, 0.2), 0.5, 0.4, Color(0, 0, 0), false))
	cage.position = Vector3(0, 1.05, -0.1)
	add_child(cage)
	for x in [-0.7, 0.7]:
		for z in [-0.9, 0.9]:
			var w = Meshes.cyl(0.28, 0.2, Meshes.mat(Color(0.08, 0.08, 0.08), 0.9, 0.0, Color(0, 0, 0), false), 12)
			w.rotation.z = PI / 2.0
			w.position = Vector3(x, 0.28, z)
			add_child(w)
	var shape = CollisionShape3D.new()
	var box = BoxShape3D.new()
	box.size = Vector3(1.7, 1.0, 2.8)
	shape.shape = box
	shape.position = Vector3(0, 0.6, 0)
	add_child(shape)

func drive(delta: float, active: bool) -> void:
	var up = global_position.normalized()
	up_direction = up
	var grav = SimClock.gravity_at(global_position)
	if not active:
		velocity += grav * delta
		move_and_slide()
		return
	throttle = 0.0
	if Input.is_action_pressed("move_forward"):
		throttle = 1.0
	if Input.is_action_pressed("move_back"):
		throttle = -0.6
	steer = 0.0
	if Input.is_action_pressed("move_left"):
		steer = 1.0
	if Input.is_action_pressed("move_right"):
		steer = -1.0
	if Input.get_connected_joypads().size() > 0:
		var j = Input.get_connected_joypads()[0]
		steer -= Input.get_joy_axis(j, JOY_AXIS_LEFT_X)
		throttle -= Input.get_joy_axis(j, JOY_AXIS_LEFT_Y)
	rotate_object_local(Vector3(0, 1, 0), steer * delta * 1.4)
	var fwd = -global_basis.z
	fwd = (fwd - up * fwd.dot(up)).normalized()
	var right = fwd.cross(up).normalized()
	global_basis = Basis(right, up, -fwd)
	velocity += fwd * throttle * 18.0 * delta
	velocity += grav * delta
	var lateral = velocity - up * velocity.dot(up) - fwd * fwd.dot(velocity)
	velocity -= lateral * 0.15
	velocity -= fwd * fwd.dot(velocity) * 0.02
	move_and_slide()
	if Input.is_action_pressed("jump"):
		velocity -= fwd * fwd.dot(velocity) * 0.08

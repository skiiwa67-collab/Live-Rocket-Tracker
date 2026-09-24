extends Area3D

func _physics_process(delta: float) -> void:
	var vel: Vector3 = get_meta("vel", Vector3.ZERO)
	if bool(get_meta("ballistic", false)):
		vel += SimClock.gravity_at(global_position) * delta
		set_meta("vel", vel)
	global_position += vel * delta
	var life = float(get_meta("life", 0.0)) - delta
	set_meta("life", life)
	if life <= 0.0:
		queue_free()

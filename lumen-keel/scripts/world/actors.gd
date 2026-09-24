extends CharacterBody3D

var kind = "raider"
var hp = 40.0
var dead = false
var skinned = false
var world: Node
var home = Vector3.ZERO
var shoot_cd = 0.0
var label = "Raider"

func setup(k: String, w: Node) -> void:
	kind = k
	world = w
	collision_layer = 4
	collision_mask = 1
	add_to_group("scannable")
	var shape = CollisionShape3D.new()
	var cap = CapsuleShape3D.new()
	match kind:
		"raider":
			hp = 45
			label = "Underbelly raider"
			cap.radius = 0.35
			cap.height = 1.5
			_visual(Color(0.35, 0.18, 0.16), 1.7)
		"pipewolf":
			hp = 18
			label = "Pipewolf"
			cap.radius = 0.28
			cap.height = 0.6
			_visual(Color(0.28, 0.26, 0.24), 0.55)
		"grazer":
			hp = 30
			label = "Cinder grazer"
			cap.radius = 0.45
			cap.height = 1.0
			_visual(Color(0.45, 0.38, 0.28), 1.1)
		"lope":
			hp = 50
			label = "Ridge lope"
			cap.radius = 0.4
			cap.height = 1.2
			_visual(Color(0.4, 0.18, 0.12), 1.3)
		"hostile":
			hp = 80
			label = "Rustcutter drone"
			cap.radius = 1.2
			cap.height = 2.0
			_visual(Color(0.42, 0.16, 0.12), 2.4)
			add_to_group("hostile_ship")
	shape.shape = cap
	add_child(shape)
	set_meta("label", label)
	set_meta("kind", kind)
	set_meta("prompt", "Skin" if dead else label)
	home = global_position

func _visual(color: Color, h: float) -> void:
	var mi = Meshes.capsule(0.25 * h / 1.5, h, Meshes.mat(color, 0.8, 0.1, Color(0, 0, 0), true))
	mi.position = Vector3(0, h * 0.45, 0)
	add_child(mi)

func _physics_process(delta: float) -> void:
	if dead or Game.state.is_empty():
		return
	shoot_cd = maxf(0.0, shoot_cd - delta)
	var player = world.player if world else null
	if player == null:
		return
	var dist = global_position.distance_to(player.global_position)
	if kind == "hostile":
		_drone(delta, player, dist)
		return
	var aggro = kind in ["raider", "pipewolf", "lope"]
	if aggro and dist < (18.0 if kind != "pipewolf" else 10.0):
		var dir = player.global_position - global_position
		dir.y = 0
		if dir.length() > 0.1:
			look_at(global_position + dir.normalized(), Vector3.UP)
			velocity = dir.normalized() * (3.2 if kind != "lope" else 5.5)
		else:
			velocity = Vector3.ZERO
		velocity.y -= 9.8 * delta
		move_and_slide()
		if kind == "raider" and dist < 16.0 and shoot_cd <= 0.0:
			shoot_cd = 1.3
			if dist < 14.0:
				player_hurt(6.0)
				AudioFx.play("shot")
		if kind in ["pipewolf", "lope"] and dist < 1.6 and shoot_cd <= 0.0:
			shoot_cd = 0.9
			player_hurt(8.0 if kind == "pipewolf" else 14.0)
	else:
		velocity.y -= 9.8 * delta
		velocity.x = 0
		velocity.z = 0
		move_and_slide()

func _drone(delta: float, player, dist: float) -> void:
	var t = SimClock.now()
	var orbit = SimClock.station_pos(t + 40.0) + Vector3(0, 80, 0)
	var want = orbit
	if dist < 500.0 and player.mode == "seat":
		want = player.global_position
		if shoot_cd <= 0.0 and dist < 380.0:
			shoot_cd = 1.6
			if world and world.ship:
				Game.damage_ship(7.0)
				AudioFx.play("laser")
	global_position = global_position.lerp(want, 1.0 - exp(-delta * 0.4))

func player_hurt(amount: float) -> void:
	Game.damage_player(amount)

func hurt(amount: float) -> void:
	if dead:
		return
	hp -= amount
	if hp <= 0.0:
		die()

func die() -> void:
	dead = true
	set_meta("prompt", "Skin")
	rotation.z = PI * 0.5
	Game.state["world"]["kills"] = int(Game.state["world"].get("kills", 0)) + 1
	if kind == "raider":
		Game.flag("under")
		var loot = Catalog.loot_table("raider", Game.scarcity(), _rng())
		for it in loot:
			Game.add_item(Game.player_inv(), str(it["id"]), int(it["count"]))
		Game.notify("Raider down. Pockets emptied.")
	elif kind == "hostile":
		Game.state["world"]["hostile_alive"] = false
		Game.add_item(Game.player_inv(), "scrap", 4)
		Game.add_item(Game.player_inv(), "cinder_quartz", 1)
		Game.notify("Rustcutter breaks apart. Salvage in your pockets.")
	Game.add_xp(Game.state["player"], "gunnery", 6)

func on_interact(world_ref, _pawn) -> void:
	if not dead or skinned:
		if not dead and kind == "grazer":
			Game.notify("The grazer watches you and does not run.")
		return
	skinned = true
	set_meta("prompt", "")
	var table = "pipewolf" if kind in ["pipewolf", "lope"] else ("grazer" if kind == "grazer" else "raider")
	var rng = _rng()
	var loot = Catalog.loot_table(table, Game.scarcity(), rng)
	var bonus = 1 if Game.skill_bonus_of(Game.state["player"], "survival") > 0.15 else 0
	for it in loot:
		Game.add_item(Game.player_inv(), str(it["id"]), int(it["count"]) + bonus)
	Game.add_xp(Game.state["player"], "survival", 7)
	Game.flag("under")
	Game.notify("Skinned. Bones and enzymes go in the bag.")
	queue_free()

func _rng() -> RandomNumberGenerator:
	var rng = RandomNumberGenerator.new()
	rng.randomize()
	return rng

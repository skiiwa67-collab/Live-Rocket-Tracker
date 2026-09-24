extends SceneTree

func _init() -> void:
	var a = load("res://scripts/world/meshes.gd")
	print("meshes ", a)
	var b = load("res://scripts/ui/hud.gd")
	print("hud ", b)
	var c = load("res://scripts/ship/ship.gd")
	print("ship ", c)
	var d = load("res://scripts/player/pawn.gd")
	print("pawn ", d)
	quit()

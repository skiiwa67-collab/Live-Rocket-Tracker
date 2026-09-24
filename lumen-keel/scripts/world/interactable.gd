extends StaticBody3D

@export var kind = "panel"
@export var target = ""
@export var prompt = "Interact"

func _ready() -> void:
	collision_layer = 2
	collision_mask = 0
	set_meta("prompt", prompt)
	add_to_group("interact")

func on_interact(world, _pawn) -> void:
	if world:
		world.handle_interact(kind, target, self)

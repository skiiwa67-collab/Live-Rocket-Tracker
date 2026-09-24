extends RefCounted
class_name Meshes

static var _mats = {}
static var _panel: Texture2D
static var _logo_voss: Texture2D
static var _logo_lumen: Texture2D

static func panel() -> Texture2D:
	if _panel:
		return _panel
	var img = Image.create(64, 64, false, Image.FORMAT_RGB8)
	img.fill(Color(0.22, 0.21, 0.2))
	for y in 64:
		for x in 64:
			if x % 16 == 0 or y % 16 == 0:
				img.set_pixel(x, y, Color(0.12, 0.12, 0.12))
			elif (x / 8 + y / 8) % 2 == 0 and x % 16 > 2 and y % 16 > 2:
				img.set_pixel(x, y, Color(0.28, 0.27, 0.25))
	_panel = ImageTexture.create_from_image(img)
	return _panel

static func logo(kind: String) -> Texture2D:
	if kind == "voss" and _logo_voss:
		return _logo_voss
	if kind != "voss" and _logo_lumen:
		return _logo_lumen
	var img = Image.create(32, 32, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	for y in 32:
		for x in 32:
			if kind == "voss":
				if abs(x - 16) < 3 and y > 6 and y < 26:
					img.set_pixel(x, y, Color(0.8, 0.15, 0.1, 1))
				if y > 8 and y < 14 and x > 6 and x < 26:
					img.set_pixel(x, y, Color(0.8, 0.15, 0.1, 1))
			else:
				var d = Vector2(x - 15.5, y - 15.5).length()
				if d > 8 and d < 12:
					img.set_pixel(x, y, Color(0.2, 0.7, 0.65, 1))
				if d < 3:
					img.set_pixel(x, y, Color(0.9, 0.85, 0.7, 1))
	var tex = ImageTexture.create_from_image(img)
	if kind == "voss":
		_logo_voss = tex
	else:
		_logo_lumen = tex
	return tex

static func mat(color: Color, rough = 0.72, metal = 0.35, emit = Color(0, 0, 0), textured = true) -> StandardMaterial3D:
	var key = "%s|%s|%s|%s" % [color, rough, metal, emit]
	if _mats.has(key):
		return _mats[key]
	var m = StandardMaterial3D.new()
	m.albedo_color = color
	m.roughness = rough
	m.metallic = metal
	if emit.r + emit.g + emit.b > 0.01:
		m.emission_enabled = true
		m.emission = emit
		m.emission_energy_multiplier = 1.4
	if textured:
		m.albedo_texture = panel()
		m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
	_mats[key] = m
	return m

static func glass() -> StandardMaterial3D:
	return mat(Color(0.45, 0.7, 0.75, 0.55), 0.08, 0.1, Color(0.2, 0.35, 0.4), false)

static func box(size: Vector3, material: Material) -> MeshInstance3D:
	var mi = MeshInstance3D.new()
	var mesh = BoxMesh.new()
	mesh.size = size
	mi.mesh = mesh
	mi.material_override = material
	return mi

static func cyl(r: float, h: float, material: Material, radial = 16) -> MeshInstance3D:
	var mi = MeshInstance3D.new()
	var mesh = CylinderMesh.new()
	mesh.top_radius = r
	mesh.bottom_radius = r
	mesh.height = h
	mesh.radial_segments = radial
	mi.mesh = mesh
	mi.material_override = material
	return mi

static func cone(r: float, h: float, material: Material) -> MeshInstance3D:
	var mi = MeshInstance3D.new()
	var mesh = CylinderMesh.new()
	mesh.top_radius = 0.04
	mesh.bottom_radius = r
	mesh.height = h
	mesh.radial_segments = 14
	mi.mesh = mesh
	mi.material_override = material
	return mi

static func sphere(r: float, material: Material, seg = 24) -> MeshInstance3D:
	var mi = MeshInstance3D.new()
	var mesh = SphereMesh.new()
	mesh.radius = r
	mesh.height = r * 2.0
	mesh.radial_segments = seg
	mesh.rings = maxi(8, seg / 2)
	mi.mesh = mesh
	mi.material_override = material
	return mi

static func prism(size: Vector3, material: Material) -> MeshInstance3D:
	var mi = MeshInstance3D.new()
	var mesh = PrismMesh.new()
	mesh.size = size
	mi.mesh = mesh
	mi.material_override = material
	return mi

static func capsule(r: float, h: float, material: Material) -> MeshInstance3D:
	var mi = MeshInstance3D.new()
	var mesh = CapsuleMesh.new()
	mesh.radius = r
	mesh.height = h
	mi.mesh = mesh
	mi.material_override = material
	return mi

static func add_collision_box(body: CollisionObject3D, size: Vector3, at: Vector3 = Vector3.ZERO) -> void:
	var shape = CollisionShape3D.new()
	var box = BoxShape3D.new()
	box.size = size
	shape.shape = box
	shape.position = at
	body.add_child(shape)

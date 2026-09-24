extends Node
## Procedural audio. Engine hum, G creak, weapon punch, UI ticks.
## No sample library — the mix is synthesized so the build stays self-contained.

var _engine: AudioStreamPlayer
var _engine_pb: AudioStreamGeneratorPlayback
var _one: AudioStreamPlayer
var _ambient: AudioStreamPlayer
var _phase = 0.0
var _phase2 = 0.0
var throttle = 0.0
var g_force = 0.0
var in_ship = false
var alarm = 0.0
var _cache = {}
var _mix = 22050

func _ready() -> void:
	_one = AudioStreamPlayer.new()
	_one.bus = "Master"
	add_child(_one)
	_ambient = AudioStreamPlayer.new()
	add_child(_ambient)
	_ambient.stream = _wav_loop(70.0, 0.08, 1.6)
	_ambient.volume_db = -22.0
	_ambient.play()
	var gen = AudioStreamGenerator.new()
	gen.mix_rate = _mix
	gen.buffer_length = 0.12
	_engine = AudioStreamPlayer.new()
	_engine.stream = gen
	_engine.volume_db = -8.0
	add_child(_engine)
	_engine.play()
	_engine_pb = _engine.get_stream_playback()

func _process(delta: float) -> void:
	if _engine_pb == null:
		return
	var vol = 0.0
	if Game.state.has("settings"):
		vol = float(Game.state["settings"].get("master_volume", 0.8))
	_engine.volume_db = linear_to_db(maxf(0.001, vol)) - 6.0
	var frames = _engine_pb.get_frames_available()
	var freq = lerpf(42.0, 118.0, clampf(throttle, 0.0, 1.0))
	if not in_ship:
		freq = 36.0
	var amp = (0.04 + throttle * 0.22) if in_ship else 0.015
	if alarm > 0.0:
		alarm -= delta
		amp += 0.05
	for _i in frames:
		var s = sin(_phase) * amp
		s += sin(_phase2) * amp * 0.35
		if g_force > 2.2 and in_ship:
			s += (randf() - 0.5) * 0.04 * clampf(g_force / 8.0, 0.0, 1.0)
		if alarm > 0.0:
			s += sin(_phase * 4.5) * 0.08
		_phase += TAU * freq / float(_mix)
		_phase2 += TAU * (freq * 1.5 + 12.0) / float(_mix)
		_engine_pb.push_frame(Vector2(s, s))
	if g_force > 4.5 and in_ship and randf() < delta * 0.8:
		play("creak")

func play(id: String) -> void:
	if _one == null:
		return
	_one.stream = _clip(id)
	var vol = 0.8
	if Game.state.has("settings"):
		vol = float(Game.state["settings"].get("master_volume", 0.8))
	_one.volume_db = linear_to_db(maxf(0.001, vol))
	_one.play()

func _clip(id: String) -> AudioStream:
	if _cache.has(id):
		return _cache[id]
	var stream: AudioStream
	match id:
		"click":
			stream = _tone(920, 0.045, 0.4, "square")
		"blip":
			stream = _tone(640, 0.07, 0.35, "sine")
		"error":
			stream = _tone(180, 0.14, 0.4, "square")
		"shot":
			stream = _noise(0.09, 0.7, 1800)
		"laser":
			stream = _tone(740, 0.09, 0.35, "sine")
		"rocket":
			stream = _noise(0.25, 0.8, 400)
		"creak":
			stream = _noise(0.18, 0.25, 300)
		"hit":
			stream = _noise(0.06, 0.5, 900)
		"dock":
			stream = _tone(220, 0.3, 0.4, "sine")
		_:
			stream = _tone(440, 0.05, 0.2, "sine")
	_cache[id] = stream
	return stream

func _tone(freq: float, dur: float, amp: float, wave: String) -> AudioStreamWAV:
	var n = int(_mix * dur)
	var data = PackedByteArray()
	data.resize(n * 2)
	for i in n:
		var t = float(i) / float(_mix)
		var env = 1.0 - (t / dur)
		var s = sin(TAU * freq * t)
		if wave == "square":
			s = 1.0 if s > 0.0 else -1.0
		s *= env * amp
		_write_sample(data, i, s)
	return _wav(data, false)

func _noise(dur: float, amp: float, fall: float) -> AudioStreamWAV:
	var n = int(_mix * dur)
	var data = PackedByteArray()
	data.resize(n * 2)
	var hp = 0.0
	for i in n:
		var t = float(i) / float(_mix)
		var env = exp(-t * fall * 0.01)
		var white = randf() * 2.0 - 1.0
		hp = hp * 0.86 + white * 0.14
		_write_sample(data, i, hp * env * amp)
	return _wav(data, false)

func _wav_loop(freq: float, amp: float, dur: float) -> AudioStreamWAV:
	var stream = _tone(freq, dur, amp, "sine")
	stream.loop_mode = AudioStreamWAV.LOOP_FORWARD
	stream.loop_end = int(_mix * dur)
	return stream

func _wav(data: PackedByteArray, stereo: bool) -> AudioStreamWAV:
	var stream = AudioStreamWAV.new()
	stream.format = AudioStreamWAV.FORMAT_16_BITS
	stream.mix_rate = _mix
	stream.stereo = stereo
	stream.data = data
	return stream

func _write_sample(data: PackedByteArray, i: int, s: float) -> void:
	var v = int(clampf(s, -1.0, 1.0) * 32000.0)
	data[i * 2] = v & 255
	data[i * 2 + 1] = (v >> 8) & 255

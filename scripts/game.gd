extends Node2D

const MUSIC_PATH := "res://assets/music/CrushTekk.mp3"
const FLOOR_Y := 610.0
const PLAYER_SIZE := Vector2(54, 54)
const PLAYER_X := 250.0
const GRAVITY := 1900.0
const JUMP_VELOCITY := -720.0
const OBSTACLE_SPEED := 520.0

var player_y := FLOOR_Y - PLAYER_SIZE.y
var player_velocity := 0.0
var obstacles: Array[Rect2] = []
var spawn_timer := 0.0
var score := 0.0
var best_score := 0
var started := false
var game_over := false
var music_player: AudioStreamPlayer
var font: Font

func _ready() -> void:
	font = ThemeDB.fallback_font
	music_player = AudioStreamPlayer.new()
	music_player.stream = load(MUSIC_PATH)
	music_player.volume_db = -8.0
	music_player.finished.connect(_on_music_finished)
	add_child(music_player)
	queue_redraw()

func _process(delta: float) -> void:
	if not started or game_over:
		queue_redraw()
		return

	score += delta * 10.0
	player_velocity += GRAVITY * delta
	player_y += player_velocity * delta
	if player_y >= FLOOR_Y - PLAYER_SIZE.y:
		player_y = FLOOR_Y - PLAYER_SIZE.y
		player_velocity = 0.0

	spawn_timer -= delta
	if spawn_timer <= 0.0:
		var height := randf_range(48.0, 76.0)
		obstacles.append(Rect2(1380.0, FLOOR_Y - height, 42.0, height))
		spawn_timer = randf_range(1.25, 1.85)

	for i in range(obstacles.size() - 1, -1, -1):
		obstacles[i].position.x -= OBSTACLE_SPEED * delta
		if obstacles[i].end.x < -20.0:
			obstacles.remove_at(i)

	var player_rect := Rect2(Vector2(PLAYER_X, player_y), PLAYER_SIZE)
	for obstacle in obstacles:
		if player_rect.intersects(obstacle):
			_finish_game()
			break
	queue_redraw()

func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch and event.pressed:
		_handle_action()
	elif event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		_handle_action()
	elif event is InputEventKey and event.pressed and not event.echo:
		if event.keycode == KEY_SPACE or event.keycode == KEY_UP:
			_handle_action()

func _handle_action() -> void:
	if game_over:
		_reset_game()
		return
	if not started:
		started = true
		if music_player.stream != null and music_player.playing == false:
			music_player.play()
		player_velocity = JUMP_VELOCITY
		return
	if player_y >= FLOOR_Y - PLAYER_SIZE.y - 2.0:
		player_velocity = JUMP_VELOCITY

func _finish_game() -> void:
	game_over = true
	best_score = maxi(best_score, int(score))
	if music_player.playing:
		music_player.stop()

func _reset_game() -> void:
	player_y = FLOOR_Y - PLAYER_SIZE.y
	player_velocity = 0.0
	obstacles.clear()
	spawn_timer = 0.0
	score = 0.0
	started = false
	game_over = false

func _on_music_finished() -> void:
	if started and not game_over:
		music_player.play()

func _draw() -> void:
	var screen := get_viewport_rect().size
	draw_rect(Rect2(Vector2.ZERO, screen), Color("#080b16"), true)

	for x in range(0, int(screen.x) + 80, 80):
		draw_line(Vector2(x, 0), Vector2(x, screen.y), Color(0.18, 0.22, 0.34, 0.22), 1.0)
	for y in range(0, int(screen.y) + 80, 80):
		draw_line(Vector2(0, y), Vector2(screen.x, y), Color(0.18, 0.22, 0.34, 0.22), 1.0)

	draw_rect(Rect2(0, FLOOR_Y, screen.x, screen.y - FLOOR_Y), Color("#171b2b"), true)
	draw_line(Vector2(0, FLOOR_Y), Vector2(screen.x, FLOOR_Y), Color("#ff3f9b"), 4.0)

	for obstacle in obstacles:
		var points := PackedVector2Array([
			obstacle.position,
			obstacle.position + Vector2(obstacle.size.x * 0.5, -obstacle.size.y * 0.35),
			obstacle.position + Vector2(obstacle.size.x, 0)
		])
		draw_colored_polygon(points, Color("#ff3f9b"))
		draw_polyline(PackedVector2Array([points[0], points[1], points[2]]), Color("#ffd0e7"), 3.0)

	var player_rect := Rect2(Vector2(PLAYER_X, player_y), PLAYER_SIZE)
	draw_rect(player_rect, Color("#ffe34d"), true)
	draw_rect(player_rect, Color("#fff4ad"), false, 3.0)

	draw_string(font, Vector2(42, 62), "DASH-BLOCK", HORIZONTAL_ALIGNMENT_LEFT, -1, 34, Color("#ffffff"))
	draw_string(font, Vector2(42, 104), "SCORE  %06d" % int(score), HORIZONTAL_ALIGNMENT_LEFT, -1, 22, Color("#ff9acb"))

	if not started:
		_draw_center_text("TOQUE PARA COMEÇAR", 350, 34, Color("#ffffff"))
		_draw_center_text("Toque ou pressione ESPAÇO para pular", 397, 20, Color("#aeb8d6"))
	elif game_over:
		_draw_center_text("FIM DE JOGO", 315, 48, Color("#ff5ca8"))
		_draw_center_text("Pontuação: %d   •   Recorde: %d" % [int(score), best_score], 365, 24, Color("#ffffff"))
		_draw_center_text("Toque para tentar novamente", 415, 21, Color("#aeb8d6"))

func _draw_center_text(message: String, y: float, size: int, color: Color) -> void:
	var width := get_viewport_rect().size.x
	draw_string(font, Vector2(0, y), message, HORIZONTAL_ALIGNMENT_CENTER, width, size, color)

package com.dashblock.game

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.media.MediaPlayer
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
    private var music: MediaPlayer? = null
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        gameView = GameView(this) { startMusic() }
        setContentView(gameView)
    }

    private fun startMusic() {
        if (music != null) {
            if (music?.isPlaying != true) music?.start()
            return
        }
        try {
            val descriptor = assets.openFd("CrushTekk.mp3")
            music = MediaPlayer().apply {
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                isLooping = true
                setOnPreparedListener { player -> player.start() }
                setOnErrorListener { player, _, _ ->
                    player.release()
                    music = null
                    true
                }
                prepareAsync()
            }
            descriptor.close()
        } catch (_: Exception) {
            music = null
        }
    }

    override fun onPause() {
        music?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        music?.release()
        music = null
        super.onDestroy()
    }
}

private class GameView(
    context: Context,
    private val onStartMusic: () -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var started = false
    private var gameOver = false
    private var playerY = 0f
    private var velocityY = 0f
    private var lastFrame = 0L
    private var spawnTimer = 0f
    private var score = 0
    private var best = 0
    private val obstacles = mutableListOf<Obstacle>()

    private data class Obstacle(var x: Float, val width: Float, val height: Float, var counted: Boolean = false)

    private val background = android.graphics.Color.rgb(12, 14, 27)
    private val groundColor = android.graphics.Color.rgb(42, 46, 68)
    private val playerColor = android.graphics.Color.rgb(255, 210, 90)
    private val obstacleColor = android.graphics.Color.rgb(255, 91, 117)
    private val textColor = android.graphics.Color.rgb(242, 244, 255)

    private fun floorY() = height * 0.82f
    private fun playerSize() = min(width, height) * 0.075f
    private fun playerX() = width * 0.18f
    private fun onGround() = playerY >= floorY() - playerSize() - 1f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = if (lastFrame == 0L) 0.016f else min(0.04f, (now - lastFrame) / 1_000_000_000f)
        lastFrame = now

        canvas.drawColor(background)
        drawBackground(canvas)

        if (started && !gameOver) updateGame(dt)

        drawGround(canvas)
        drawPlayer(canvas)
        drawObstacles(canvas)
        drawHud(canvas)

        if (!started) drawCenterMessage(canvas, "DASH BLOCK", "TOQUE PARA COMEÇAR")
        if (gameOver) drawCenterMessage(canvas, "FIM DE JOGO", "TOQUE PARA TENTAR DE NOVO")

        postInvalidateDelayed(16)
    }

    private fun drawBackground(canvas: Canvas) {
        paint.color = android.graphics.Color.rgb(24, 28, 48)
        paint.strokeWidth = 2f
        val gap = max(36f, min(width, height) * 0.09f)
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, floorY(), paint)
            x += gap
        }
        var y = 0f
        while (y < floorY()) {
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
            y += gap
        }
    }

    private fun drawGround(canvas: Canvas) {
        paint.color = groundColor
        canvas.drawRect(0f, floorY(), width.toFloat(), height.toFloat(), paint)
        paint.color = android.graphics.Color.rgb(104, 111, 151)
        canvas.drawRect(0f, floorY(), width.toFloat(), floorY() + 5f, paint)
    }

    private fun drawPlayer(canvas: Canvas) {
        val size = playerSize()
        val top = if (started) playerY else floorY() - size
        paint.color = playerColor
        canvas.drawRoundRect(playerX(), top, playerX() + size, top + size, 8f, 8f, paint)
        paint.color = android.graphics.Color.rgb(35, 36, 48)
        canvas.drawCircle(playerX() + size * 0.68f, top + size * 0.34f, size * 0.055f, paint)
    }

    private fun drawObstacles(canvas: Canvas) {
        paint.color = obstacleColor
        for (obstacle in obstacles) {
            val top = floorY() - obstacle.height
            canvas.drawRoundRect(obstacle.x, top, obstacle.x + obstacle.width, floorY(), 5f, 5f, paint)
        }
    }

    private fun drawHud(canvas: Canvas) {
        paint.color = textColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = min(height * 0.075f, 42f)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("PONTOS: $score", width * 0.045f, height * 0.10f, paint)
        paint.textSize = min(height * 0.045f, 25f)
        canvas.drawText("RECORDE: $best", width * 0.045f, height * 0.16f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = min(height * 0.045f, 25f)
        canvas.drawText("TOQUE = PULAR", width * 0.95f, height * 0.10f, paint)
    }

    private fun drawCenterMessage(canvas: Canvas, title: String, subtitle: String) {
        paint.color = textColor
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = min(height * 0.13f, 64f)
        canvas.drawText(title, width / 2f, height * 0.39f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = min(height * 0.055f, 30f)
        canvas.drawText(subtitle, width / 2f, height * 0.49f, paint)
    }

    private fun updateGame(dt: Float) {
        val size = playerSize()
        val gravity = height * 3.6f
        velocityY += gravity * dt
        playerY += velocityY * dt
        val groundTop = floorY() - size
        if (playerY >= groundTop) {
            playerY = groundTop
            velocityY = 0f
        }

        spawnTimer += dt
        if (spawnTimer >= 1.45f) {
            spawnTimer = 0f
            val obstacleHeight = size * (0.85f + (score % 3) * 0.12f)
            obstacles.add(Obstacle(width.toFloat() + size, size * 0.72f, obstacleHeight))
        }

        val speed = width * 0.62f
        val playerLeft = playerX() + size * 0.12f
        val playerRight = playerX() + size * 0.88f
        val playerTop = playerY + size * 0.10f
        val playerBottom = playerY + size * 0.95f

        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.x -= speed * dt

            if (!obstacle.counted && obstacle.x + obstacle.width < playerLeft) {
                obstacle.counted = true
                score++
                best = max(best, score)
            }

            val obstacleTop = floorY() - obstacle.height
            val overlap = playerRight > obstacle.x &&
                playerLeft < obstacle.x + obstacle.width &&
                playerBottom > obstacleTop &&
                playerTop < floorY()

            if (overlap) {
                gameOver = true
                break
            }
            if (obstacle.x + obstacle.width < 0f) iterator.remove()
        }
    }

    private fun resetGame() {
        score = 0
        spawnTimer = 0f
        obstacles.clear()
        playerY = floorY() - playerSize()
        velocityY = 0f
        gameOver = false
        started = true
        onStartMusic()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            when {
                !started -> resetGame()
                gameOver -> resetGame()
                onGround() -> velocityY = -height * 1.28f
            }
            return true
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!started) playerY = floorY() - playerSize()
    }
}

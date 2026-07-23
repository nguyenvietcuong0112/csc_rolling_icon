package com.iconchanger.wallpaper.rolling.icons.render

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLES20
import android.opengl.GLUtils
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.QueryCallback
import com.badlogic.gdx.physics.box2d.joints.MouseJoint
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef
import com.iconchanger.wallpaper.rolling.icons.data.AppRepository
import com.iconchanger.wallpaper.rolling.icons.data.IconLoader
import com.iconchanger.wallpaper.rolling.icons.data.PreferenceRepository
import com.iconchanger.wallpaper.rolling.icons.model.AppInfo
import com.iconchanger.wallpaper.rolling.icons.physics.PhysicsWorld
import com.iconchanger.wallpaper.rolling.icons.render.mode.RollingModeStrategy
import com.iconchanger.wallpaper.rolling.icons.render.mode.ShapePathModeStrategy
import com.iconchanger.wallpaper.rolling.icons.render.mode.SpinningModeStrategy
import com.iconchanger.wallpaper.rolling.icons.render.mode.WallpaperModeStrategy
import com.iconchanger.wallpaper.rolling.icons.ui.PhotoShowActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameRenderer(private val context: Context) : ApplicationListener, AndroidWallpaperListener, SensorEventListener {

    data class IconData(
        val texture: Texture,
        val size: Float,
        val appInfo: AppInfo
    )

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val color: Color,
        val size: Float,
        var life: Float,
        val maxLife: Float
    )

    private lateinit var spriteBatch: SpriteBatch
    private lateinit var camera: OrthographicCamera
    private lateinit var physicsWorld: PhysicsWorld
    private var debugRenderer: Box2DDebugRenderer? = null
    private lateinit var shapeRenderer: ShapeRenderer

    private val particles = ArrayList<Particle>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Trọng lực & Cảm biến
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gravityX = 0f
    private var gravityY = -9.8f
    private var smoothX = 0f
    private var smoothY = -9.8f
    private val sensorAlpha = 0.15f

    // Tương tác vật lý
    private var mouseJoint: MouseJoint? = null
    private var selectedBody: Body? = null
    private val touchPoint = Vector3()

    // Cấu hình vật lý từ DataStore
    private var isSensorEnabled = true
    private var isWallpaperTouchEnabled = true

    // Chế độ Wallpaper Chiến lược (Strategy Pattern)
    private var wallpaperMode = "rolling"
    private var activeStrategy: WallpaperModeStrategy? = null

    // Cấu hình Nền
    private var bgType = 0 // 0: Màu đơn, 1: Gradient, 2: Ảnh nền
    private var bgColorStr = "#121212"
    private var bgGradientStartStr = "#1e272e"
    private var bgGradientEndStr = "#0f0f10"
    private var bgImagePathStr = ""
    private var backgroundTexture: Texture? = null
    private var currentLoadedBgPath = ""

    private val viewportWidthInMeters = 10f

    override fun create() {
        spriteBatch = SpriteBatch()
        camera = OrthographicCamera()
        physicsWorld = PhysicsWorld()
        debugRenderer = Box2DDebugRenderer()
        shapeRenderer = ShapeRenderer()

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val gestureListener = object : GestureDetector.GestureAdapter() {
            override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
                destroyMouseJoint()
                selectedBody = null

                if (wallpaperMode != "rolling") return true

                touchPoint.set(x, y, 0f)
                camera.unproject(touchPoint)

                try {
                    if (!physicsWorld.world.isLocked) {
                        physicsWorld.world.QueryAABB(object : QueryCallback {
                            override fun reportFixture(fixture: Fixture): Boolean {
                                if (fixture.testPoint(touchPoint.x, touchPoint.y)) {
                                    val body = fixture.body
                                    if (body.userData is IconData) {
                                        selectedBody = body
                                        return false
                                    }
                                }
                                return true
                            }
                        }, touchPoint.x - 0.1f, touchPoint.y - 0.1f, touchPoint.x + 0.1f, touchPoint.y + 0.1f)

                        val targetBody = selectedBody
                        val ground = physicsWorld.groundBody
                        if (targetBody != null && ground != null) {
                            val jointDef = MouseJointDef().apply {
                                bodyA = ground
                                bodyB = targetBody
                                target.set(touchPoint.x, touchPoint.y)
                                maxForce = 1000f * targetBody.mass
                                frequencyHz = 5.0f
                                dampingRatio = 0.7f
                            }
                            mouseJoint = physicsWorld.world.createJoint(jointDef) as MouseJoint
                            targetBody.isAwake = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }

            override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                if (wallpaperMode == "rolling" && mouseJoint != null) {
                    touchPoint.set(x, y, 0f)
                    camera.unproject(touchPoint)
                    try {
                        mouseJoint?.target = Vector2(touchPoint.x, touchPoint.y)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return true
            }

            override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean {
                destroyMouseJoint()
                return true
            }

            override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
                if (wallpaperMode == "rolling") {
                    val body = selectedBody
                    if (body != null && mouseJoint == null) {
                        val vx = (velocityX / Gdx.graphics.width) * camera.viewportWidth
                        val vy = -(velocityY / Gdx.graphics.height) * camera.viewportHeight
                        body.linearVelocity = Vector2(vx * 1.2f, vy * 1.2f)
                    }
                }
                return true
            }

            override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
                if (!isWallpaperTouchEnabled) {
                    return true
                }

                touchPoint.set(x, y, 0f)
                camera.unproject(touchPoint)

                val iconData = activeStrategy?.checkTouch(touchPoint, camera)
                if (iconData != null) {
                    spawnExplosion(touchPoint.x, touchPoint.y)
                    scope.launch {
                        delay(250)
                        launchBoundApp(iconData)
                    }
                }
                return true
            }
        }

        Gdx.input.inputProcessor = GestureDetector(gestureListener)
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        reloadPhysicsSettings()
    }

    private suspend fun launchBoundApp(iconData: IconData) {
        destroyMouseJoint()

        if (iconData.appInfo.type == AppInfo.TYPE_PHOTO) {
            val intent = Intent(context, PhotoShowActivity::class.java).apply {
                putExtra("photo_uri", iconData.appInfo.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (iconData.appInfo.type == AppInfo.TYPE_APP) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(iconData.appInfo.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (iconData.appInfo.type == AppInfo.TYPE_EMOJI) {
            try {
                val prefs = PreferenceRepository(context)
                val bindings = prefs.getEmojiAppBindings()
                val rawPkg = iconData.appInfo.packageName
                val emojiKey = if (bindings.containsKey(rawPkg)) rawPkg else rawPkg.removePrefix("emoji_")
                val boundPackage = bindings[emojiKey] ?: bindings[rawPkg]
                if (!boundPackage.isNullOrEmpty()) {
                    val intent = context.packageManager.getLaunchIntentForPackage(boundPackage)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun destroyMouseJoint() {
        val joint = mouseJoint
        mouseJoint = null
        selectedBody = null
        if (joint != null) {
            try {
                if (!physicsWorld.world.isLocked) {
                    physicsWorld.world.destroyJoint(joint)
                }
            } catch (t: Throwable) {
                // Catch native SIGSEGV or double free exception
            }
        }
    }

    private suspend fun loadBackgroundSettings(prefs: PreferenceRepository) {
        bgType = prefs.getBgType()
        bgColorStr = prefs.getBgColor()
        bgGradientStartStr = prefs.getBgGradientStart()
        bgGradientEndStr = prefs.getBgGradientEnd()
        bgImagePathStr = prefs.getBgImagePath()

        if (bgType == 2 && bgImagePathStr.isNotEmpty() && bgImagePathStr != currentLoadedBgPath) {
            currentLoadedBgPath = bgImagePathStr
            loadBackgroundImageAsync(bgImagePathStr)
        }
    }

    private fun bitmapToTexture(bitmap: Bitmap): Texture {
        return IconLoader(context).bitmapToTexture(bitmap)
    }

    private fun loadBackgroundImageAsync(uriString: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val inputStream = if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                    java.net.URL(uriString).openStream()
                } else {
                    val uri = android.net.Uri.parse(uriString)
                    context.contentResolver.openInputStream(uri)
                }
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val screenW = Gdx.graphics.width
                    val screenH = Gdx.graphics.height
                    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, screenW, screenH, true)
                    if (scaledBitmap != originalBitmap) originalBitmap.recycle()

                    Gdx.app.postRunnable {
                        backgroundTexture?.dispose()
                        backgroundTexture = bitmapToTexture(scaledBitmap)
                        scaledBitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun reloadPhysicsSettings() {
        scope.launch(Dispatchers.IO) {
            val prefs = PreferenceRepository(context)
            val newMode = prefs.getWallpaperMode()
            isWallpaperTouchEnabled = prefs.isWallpaperTouchEnabled()
            isSensorEnabled = prefs.isSensorEnabled()
            loadBackgroundSettings(prefs)

            Gdx.app.postRunnable {
                if (wallpaperMode != newMode || activeStrategy == null) {
                    wallpaperMode = newMode
                    activeStrategy?.dispose()
                    activeStrategy = when (newMode) {
                        "spinning" -> SpinningModeStrategy(context, scope)
                        "shape_path" -> ShapePathModeStrategy(context, scope)
                        else -> RollingModeStrategy(context, scope, physicsWorld)
                    }
                }
                activeStrategy?.reload()
            }
        }
    }

    private fun spawnExplosion(x: Float, y: Float) {
        val count = 25
        val colors = arrayOf(
            Color.WHITE, Color.YELLOW, Color.GOLD, Color.ORANGE,
            Color.CYAN, Color.MAGENTA, Color.LIME
        )

        for (i in 0 until count) {
            val angle = MathUtils.random(0f, 2f * MathUtils.PI)
            val speed = MathUtils.random(3.0f, 10.0f)
            val vx = MathUtils.cos(angle) * speed
            val vy = MathUtils.sin(angle) * speed

            val color = colors[MathUtils.random(0, colors.size - 1)]
            val size = MathUtils.random(0.08f, 0.22f)
            val life = MathUtils.random(0.4f, 0.8f)

            particles.add(Particle(x, y, vx, vy, color, size, life, life))
        }
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (wallpaperMode == "rolling") {
            if (isSensorEnabled) {
                physicsWorld.world.gravity = Vector2(gravityX, gravityY)
            } else {
                physicsWorld.world.gravity = Vector2(0f, -9.8f)
            }
            physicsWorld.step(Gdx.graphics.deltaTime)
        }

        renderBackground()

        spriteBatch.projectionMatrix = camera.combined
        spriteBatch.begin()
        activeStrategy?.render(spriteBatch, camera, Gdx.graphics.deltaTime)
        spriteBatch.end()

        renderParticles()
    }

    private fun renderBackground() {
        if (bgType == 2 && backgroundTexture != null) {
            spriteBatch.projectionMatrix = camera.combined
            spriteBatch.begin()
            val hw = camera.viewportWidth / 2f
            val hh = camera.viewportHeight / 2f
            spriteBatch.draw(backgroundTexture!!, -hw, -hh, camera.viewportWidth, camera.viewportHeight)
            spriteBatch.end()
            return
        }

        shapeRenderer.projectionMatrix = camera.combined
        val hw = camera.viewportWidth / 2f
        val hh = camera.viewportHeight / 2f

        if (bgType == 1) {
            try {
                val colorStart = Color.valueOf(bgGradientStartStr)
                val colorEnd = Color.valueOf(bgGradientEndStr)

                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                shapeRenderer.rect(-hw, -hh, camera.viewportWidth, camera.viewportHeight, colorEnd, colorEnd, colorStart, colorStart)
                shapeRenderer.end()
            } catch (e: Exception) {
                Gdx.gl.glClearColor(0.07f, 0.07f, 0.07f, 1f)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            }
        } else {
            try {
                val color = Color.valueOf(bgColorStr)
                Gdx.gl.glClearColor(color.r, color.g, color.b, color.a)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            } catch (e: Exception) {
                Gdx.gl.glClearColor(0.07f, 0.07f, 0.07f, 1f)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            }
        }
    }

    private fun renderParticles() {
        if (particles.isNotEmpty()) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            shapeRenderer.projectionMatrix = camera.combined
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

            val iterator = particles.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.life -= Gdx.graphics.deltaTime
                if (p.life <= 0) {
                    iterator.remove()
                    continue
                }

                p.x += p.vx * Gdx.graphics.deltaTime
                p.y += p.vy * Gdx.graphics.deltaTime
                p.vy -= 4.0f * Gdx.graphics.deltaTime

                val alpha = p.life / p.maxLife
                shapeRenderer.color = Color(p.color.r, p.color.g, p.color.b, alpha)
                shapeRenderer.circle(p.x, p.y, p.size)
            }
            shapeRenderer.end()
        }
    }

    override fun resize(width: Int, height: Int) {
        val aspect = height.toFloat() / width.toFloat()
        camera.viewportWidth = viewportWidthInMeters
        camera.viewportHeight = viewportWidthInMeters * aspect
        camera.position.set(0f, 0f, 0f)
        camera.update()

        physicsWorld.setupBoundaries(camera.viewportWidth, camera.viewportHeight)
        (activeStrategy as? RollingModeStrategy)?.resize(camera.viewportWidth, camera.viewportHeight)
    }

    override fun pause() {
        sensorManager?.unregisterListener(this)
        destroyMouseJoint()
    }

    override fun resume() {
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        reloadPhysicsSettings()
    }

    override fun dispose() {
        sensorManager?.unregisterListener(this)
        destroyMouseJoint()
        spriteBatch.dispose()
        shapeRenderer.dispose()
        backgroundTexture?.dispose()
        activeStrategy?.dispose()
        debugRenderer?.dispose()
        physicsWorld.dispose()
    }

    override fun offsetChange(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {}

    override fun previewStateChange(isPreview: Boolean) {}

    override fun iconDropped(x: Int, y: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isSensorEnabled || event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val (rawX, rawY) = when (Gdx.input.rotation) {
            90 -> Pair(event.values[1] * 1.5f, -event.values[0] * 1.5f)
            180 -> Pair(event.values[0] * 1.5f, event.values[1] * 1.5f)
            270 -> Pair(-event.values[1] * 1.5f, event.values[0] * 1.5f)
            else -> Pair(-event.values[0] * 1.5f, -event.values[1] * 1.5f)
        }

        smoothX += sensorAlpha * (rawX - smoothX)
        smoothY += sensorAlpha * (rawY - smoothY)

        gravityX = smoothX
        gravityY = smoothY
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

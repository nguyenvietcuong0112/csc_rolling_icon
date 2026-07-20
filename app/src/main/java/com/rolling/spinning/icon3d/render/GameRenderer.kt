package com.rolling.spinning.icon3d.render

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
import com.rolling.spinning.icon3d.data.AppRepository
import com.rolling.spinning.icon3d.data.IconLoader
import com.rolling.spinning.icon3d.data.PreferenceRepository
import com.rolling.spinning.icon3d.model.AppInfo
import com.rolling.spinning.icon3d.physics.PhysicsWorld
import com.rolling.spinning.icon3d.ui.PhotoShowActivity
import kotlinx.coroutines.*
import kotlin.math.abs

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

    private val activeBodies = ArrayList<Body>()
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
    private var currentIconSize = 1.0f
    private var isWallpaperTouchEnabled = true

    // Chế độ Wallpaper mới (Spinning)
    private var wallpaperMode = "rolling"
    private var spinningPattern = "dual_circle"
    private val spinningIcons = ArrayList<IconData>()
    private var spinningAngle = 0f

    // Chế độ Shape Path mới
    private var shapePathType = "heart"
    private val shapePathIcons = ArrayList<IconData>()
    private var shapePathAngle = 0f

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

                touchPoint.set(x, y, 0f)
                camera.unproject(touchPoint)

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
                return true
            }

            override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                if (mouseJoint != null) {
                    touchPoint.set(x, y, 0f)
                    camera.unproject(touchPoint)
                    mouseJoint?.target = Vector2(touchPoint.x, touchPoint.y)
                }
                return true
            }

            override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean {
                destroyMouseJoint()
                return true
            }

            override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
                val body = selectedBody
                if (body != null && mouseJoint == null) {
                    val vx = (velocityX / Gdx.graphics.width) * camera.viewportWidth
                    val vy = -(velocityY / Gdx.graphics.height) * camera.viewportHeight
                    body.linearVelocity = Vector2(vx * 1.2f, vy * 1.2f)
                }
                return true
            }

            override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
                if (!isWallpaperTouchEnabled) {
                    return true
                }

                touchPoint.set(x, y, 0f)
                camera.unproject(touchPoint)

                var clickedIcon: IconData? = null
                var clickPosX = 0f
                var clickPosY = 0f

                if (wallpaperMode == "spinning") {
                    val cx = 0f
                    val cy = 0f
                    val iconCount = synchronized(spinningIcons) { spinningIcons.size }
                    if (iconCount > 0) {
                        synchronized(spinningIcons) {
                            when (spinningPattern) {
                                "single_circle" -> {
                                    val radius = camera.viewportWidth * 0.28f
                                    val angleStep = 360f / iconCount
                                    for (i in 0 until iconCount) {
                                        val angle = Math.toRadians((spinningAngle + i * angleStep).toDouble())
                                        val ix = cx + radius * kotlin.math.cos(angle)
                                        val iy = cy + radius * kotlin.math.sin(angle)
                                        val size = spinningIcons[i].size
                                        if (touchPoint.x >= ix - size/2f && touchPoint.x <= ix + size/2f &&
                                            touchPoint.y >= iy - size/2f && touchPoint.y <= iy + size/2f) {
                                            clickedIcon = spinningIcons[i]
                                            clickPosX = ix.toFloat()
                                            clickPosY = iy.toFloat()
                                            break
                                        }
                                    }
                                }
                                "dual_circle" -> {
                                    val innerList = ArrayList<IconData>()
                                    val outerList = ArrayList<IconData>()
                                    for (i in 0 until iconCount) {
                                        if (i % 2 == 0) innerList.add(spinningIcons[i]) else outerList.add(spinningIcons[i])
                                    }
                                    
                                    if (innerList.isNotEmpty()) {
                                        val innerRadius = camera.viewportWidth * 0.18f
                                        val innerCount = innerList.size
                                        val innerStep = 360f / innerCount
                                        for (i in 0 until innerCount) {
                                            val angle = Math.toRadians((spinningAngle + i * innerStep).toDouble())
                                            val ix = cx + innerRadius * kotlin.math.cos(angle)
                                            val iy = cy + innerRadius * kotlin.math.sin(angle)
                                            val size = innerList[i].size
                                            if (touchPoint.x >= ix - size/2f && touchPoint.x <= ix + size/2f &&
                                                touchPoint.y >= iy - size/2f && touchPoint.y <= iy + size/2f) {
                                                clickedIcon = innerList[i]
                                                clickPosX = ix.toFloat()
                                                clickPosY = iy.toFloat()
                                                break
                                            }
                                        }
                                    }
                                    
                                    if (clickedIcon == null && outerList.isNotEmpty()) {
                                        val outerRadius = camera.viewportWidth * 0.33f
                                        val outerCount = outerList.size
                                        val outerStep = 360f / outerCount
                                        for (j in 0 until outerCount) {
                                            val angle = Math.toRadians((-spinningAngle + j * outerStep).toDouble())
                                            val ix = cx + outerRadius * kotlin.math.cos(angle)
                                            val iy = cy + outerRadius * kotlin.math.sin(angle)
                                            val size = outerList[j].size
                                            if (touchPoint.x >= ix - size/2f && touchPoint.x <= ix + size/2f &&
                                                touchPoint.y >= iy - size/2f && touchPoint.y <= iy + size/2f) {
                                                clickedIcon = outerList[j]
                                                clickPosX = ix.toFloat()
                                                clickPosY = iy.toFloat()
                                                break
                                            }
                                        }
                                    }
                                }
                                "vortex" -> {
                                    val baseRadius = camera.viewportWidth * 0.08f
                                    val radiusOffset = camera.viewportWidth * 0.015f
                                    val angleOffset = 25f
                                    for (i in 0 until iconCount) {
                                        val radius = baseRadius + i * radiusOffset
                                        val angle = Math.toRadians((spinningAngle + i * angleOffset).toDouble())
                                        val ix = cx + radius * kotlin.math.cos(angle)
                                        val iy = cy + radius * kotlin.math.sin(angle)
                                        val size = spinningIcons[i].size
                                        if (touchPoint.x >= ix - size/2f && touchPoint.x <= ix + size/2f &&
                                            touchPoint.y >= iy - size/2f && touchPoint.y <= iy + size/2f) {
                                            clickedIcon = spinningIcons[i]
                                            clickPosX = ix.toFloat()
                                            clickPosY = iy.toFloat()
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (wallpaperMode == "shape_path") {
                    val iconCount = synchronized(shapePathIcons) { shapePathIcons.size }
                    if (iconCount > 0) {
                        synchronized(shapePathIcons) {
                            val step = (2f * MathUtils.PI) / iconCount
                            val R = camera.viewportWidth * 0.35f
                            val cx = 0f
                            val cy = 0f
                            for (i in 0 until iconCount) {
                                val t = shapePathAngle + i * step
                                var ix = 0f
                                var iy = 0f
                                when (shapePathType) {
                                    "heart" -> {
                                        val sinT = kotlin.math.sin(t)
                                        ix = (R * 16f * sinT * sinT * sinT) / 16f
                                        iy = (R * (13f * kotlin.math.cos(t) - 5f * kotlin.math.cos(2f * t) - 2f * kotlin.math.cos(3f * t) - kotlin.math.cos(4f * t))) / 16f
                                    }
                                    "infinity" -> {
                                        val cosT = kotlin.math.cos(t)
                                        val sinT = kotlin.math.sin(t)
                                        val denom = 1f + sinT * sinT
                                        ix = (R * 1.5f * cosT) / denom
                                        iy = (R * 1.5f * sinT * cosT) / denom
                                    }
                                    "star" -> {
                                        val r = R * (0.65f + 0.35f * kotlin.math.cos(5f * t))
                                        ix = r * kotlin.math.cos(t)
                                        iy = r * kotlin.math.sin(t)
                                    }
                                    "flower" -> {
                                        val r = R * kotlin.math.cos(5f * t)
                                        ix = r * kotlin.math.cos(t)
                                        iy = r * kotlin.math.sin(t)
                                    }
                                    "clover" -> {
                                        val r = R * kotlin.math.cos(2f * t)
                                        ix = r * kotlin.math.cos(t)
                                        iy = r * kotlin.math.sin(t)
                                    }
                                    "butterfly" -> {
                                        val sinT = kotlin.math.sin(t)
                                        val cosT = kotlin.math.cos(t)
                                        val expCos = kotlin.math.exp(cosT.toDouble()).toFloat()
                                        val sin12Double = kotlin.math.sin(t / 12f).toDouble()
                                        val r = R * (expCos - 2f * kotlin.math.cos(4f * t) - java.lang.Math.pow(sin12Double, 5.0).toFloat()) / 4f
                                        ix = r * sinT
                                        iy = r * cosT
                                    }
                                }
                                val actualX = cx + ix
                                val actualY = cy + iy
                                val size = shapePathIcons[i].size
                                if (touchPoint.x >= actualX - size/2f && touchPoint.x <= actualX + size/2f &&
                                    touchPoint.y >= actualY - size/2f && touchPoint.y <= actualY + size/2f) {
                                    clickedIcon = shapePathIcons[i]
                                    clickPosX = actualX
                                    clickPosY = actualY
                                    break
                                }
                            }
                        }
                    }
                } else {
                    var clickedBody: Body? = null
                    physicsWorld.world.QueryAABB(object : QueryCallback {
                        override fun reportFixture(fixture: Fixture): Boolean {
                            if (fixture.testPoint(touchPoint.x, touchPoint.y)) {
                                val body = fixture.body
                                if (body.userData is IconData) {
                                    clickedBody = body
                                    return false
                                }
                            }
                            return true
                        }
                    }, touchPoint.x - 0.1f, touchPoint.y - 0.1f, touchPoint.x + 0.1f, touchPoint.y + 0.1f)
                    val body = clickedBody
                    if (body != null) {
                        val iconData = body.userData as? IconData
                        if (iconData != null) {
                            clickedIcon = iconData
                            clickPosX = body.position.x
                            clickPosY = body.position.y
                        }
                    }
                }

                val iconData = clickedIcon
                if (iconData != null) {
                    spawnExplosion(clickPosX, clickPosY)
                    scope.launch {
                        delay(250)
                        if (iconData.appInfo.type == AppInfo.TYPE_PHOTO) {
                            val intent = Intent(context, PhotoShowActivity::class.java).apply {
                                putExtra("photo_uri", iconData.appInfo.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
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
                        }
                    }
                }
                return true
            }
        }

        Gdx.input.inputProcessor = GestureDetector(gestureListener)

        val prefs = com.rolling.spinning.icon3d.data.PreferenceRepository(context)
        loadBackgroundSettings(prefs)
        loadSelectedAppsIcons()

        // Đăng ký lắng nghe cảm biến gia tốc ngay khi tạo để hoạt động mượt mà từ lần đầu tiên
        scope.launch(Dispatchers.IO) {
            isSensorEnabled = prefs.isSensorEnabled()
            if (isSensorEnabled) {
                Gdx.app.postRunnable {
                    sensorManager?.unregisterListener(this@GameRenderer)
                    sensorManager?.registerListener(
                        this@GameRenderer,
                        accelerometer,
                        SensorManager.SENSOR_DELAY_GAME
                    )
                }
            }
        }
    }

    private fun destroyMouseJoint() {
        mouseJoint?.let {
            physicsWorld.world.destroyJoint(it)
        }
        mouseJoint = null
    }

    private fun spawnExplosion(x: Float, y: Float) {
        // Danh sách các tông màu neon rực rỡ
        val colors = arrayOf(
            Color.valueOf("3498db"), // Neon Blue
            Color.valueOf("e74c3c"), // Neon Red
            Color.valueOf("2ecc71"), // Neon Green
            Color.valueOf("9b59b6"), // Neon Purple
            Color.valueOf("f1c40f"), // Neon Yellow
            Color.valueOf("e67e22")  // Neon Orange
        )
        val baseColor = colors.random()

        val count = 40 // Sinh 40 hạt
        for (i in 0 until count) {
            val angle = MathUtils.random(0f, MathUtils.PI2)
            val speed = MathUtils.random(2f, 6f)
            val vx = MathUtils.cos(angle) * speed
            val vy = MathUtils.sin(angle) * speed
            val size = MathUtils.random(0.04f, 0.12f)
            val maxLife = MathUtils.random(0.4f, 0.9f)

            // Ngẫu nhiên hóa nhẹ sắc độ để hiệu ứng chân thực hơn
            val r = MathUtils.clamp(baseColor.r + MathUtils.random(-0.08f, 0.08f), 0f, 1f)
            val g = MathUtils.clamp(baseColor.g + MathUtils.random(-0.08f, 0.08f), 0f, 1f)
            val b = MathUtils.clamp(baseColor.b + MathUtils.random(-0.08f, 0.08f), 0f, 1f)

            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = vx,
                    vy = vy,
                    color = Color(r, g, b, 1f),
                    size = size,
                    life = maxLife,
                    maxLife = maxLife
                )
            )
        }
    }

    private fun loadSelectedAppsIcons() {
        scope.launch(Dispatchers.IO) {
            val repository = AppRepository(context)
            val loader = IconLoader(context)
            val prefs = PreferenceRepository(context)

            currentIconSize = prefs.getIconSize()
            val density = prefs.getDensity()
            val friction = prefs.getFriction()
            val restitution = prefs.getRestitution()
            isSensorEnabled = prefs.isSensorEnabled()
            isWallpaperTouchEnabled = prefs.isWallpaperTouchEnabled()
            
            wallpaperMode = prefs.getWallpaperMode()
            if (wallpaperMode == "spinning") {
                spinningPattern = prefs.getSpinningPattern()
                synchronized(spinningIcons) {
                    spinningIcons.clear()
                }
                val selectedAppsSpinning = prefs.getSelectedAppsSpinning()
                for (pkg in selectedAppsSpinning) {
                    val bitmap = loader.loadAppIcon(pkg) ?: continue
                    Gdx.app.postRunnable {
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()
                        if (texture != null) {
                            val app = AppInfo(packageName = pkg, appName = "", iconCacheKey = pkg, type = AppInfo.TYPE_APP)
                            synchronized(spinningIcons) {
                                spinningIcons.add(IconData(texture, currentIconSize, app))
                            }
                        }
                    }
                }
                val selectedEmojisSpinning = prefs.getSelectedEmojisSpinning()
                for (emoji in selectedEmojisSpinning) {
                    val bitmap = loader.loadEmojiIcon(emoji) ?: continue
                    Gdx.app.postRunnable {
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()
                        if (texture != null) {
                            val app = AppInfo(packageName = "emoji_$emoji", appName = emoji, iconCacheKey = "emoji_${emoji.hashCode()}", type = AppInfo.TYPE_EMOJI)
                            synchronized(spinningIcons) {
                                spinningIcons.add(IconData(texture, currentIconSize, app))
                            }
                        }
                    }
                }
                val selectedPhotosSpinning = prefs.getSelectedPhotosSpinning()
                for (photoUri in selectedPhotosSpinning) {
                    val bitmap = loader.loadCustomPhotoIcon(photoUri) ?: continue
                    Gdx.app.postRunnable {
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()
                        if (texture != null) {
                            val app = AppInfo(packageName = photoUri, appName = "Custom Photo", iconCacheKey = "custom_photo_${photoUri.hashCode()}", type = AppInfo.TYPE_PHOTO)
                            synchronized(spinningIcons) {
                                spinningIcons.add(IconData(texture, currentIconSize, app))
                            }
                        }
                    }
                }
                return@launch
            }
            if (wallpaperMode == "shape_path") {
                shapePathType = prefs.getShapePathType()
                synchronized(shapePathIcons) {
                    shapePathIcons.clear()
                }
                var selectedAppsShape = prefs.getSelectedAppsShape()
                if (selectedAppsShape.isEmpty()) {
                    val allApps = repository.getInstalledApps()
                    selectedAppsShape = allApps.take(24).map { it.packageName }.toSet()
                }
                for (pkg in selectedAppsShape) {
                    val bitmap = loader.loadAppIcon(pkg) ?: continue
                    Gdx.app.postRunnable {
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()
                        if (texture != null) {
                            val app = AppInfo(packageName = pkg, appName = "", iconCacheKey = pkg, type = AppInfo.TYPE_APP)
                            synchronized(shapePathIcons) {
                                shapePathIcons.add(IconData(texture, currentIconSize, app))
                            }
                        }
                    }
                }
                val selectedEmojisShape = prefs.getSelectedEmojisShape()
                for (emoji in selectedEmojisShape) {
                    val bitmap = loader.loadEmojiIcon(emoji) ?: continue
                    Gdx.app.postRunnable {
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()
                        if (texture != null) {
                            val app = AppInfo(packageName = "emoji_$emoji", appName = emoji, iconCacheKey = "emoji_${emoji.hashCode()}", type = AppInfo.TYPE_EMOJI)
                            synchronized(shapePathIcons) {
                                shapePathIcons.add(IconData(texture, currentIconSize, app))
                            }
                        }
                    }
                }
                val selectedPhotosShape = prefs.getSelectedPhotosShape()
                for (photoUri in selectedPhotosShape) {
                    val bitmap = loader.loadCustomPhotoIcon(photoUri) ?: continue
                    Gdx.app.postRunnable {
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()
                        if (texture != null) {
                            val app = AppInfo(packageName = photoUri, appName = "Custom Photo", iconCacheKey = "custom_photo_${photoUri.hashCode()}", type = AppInfo.TYPE_PHOTO)
                            synchronized(shapePathIcons) {
                                shapePathIcons.add(IconData(texture, currentIconSize, app))
                            }
                        }
                    }
                }
                return@launch
            }



            val selectedApps = repository.getSelectedApps()

            for (app in selectedApps) {
                val bitmap = loader.loadAppIcon(app.packageName) ?: continue
                Gdx.app.postRunnable {
                    val texture = bitmapToTexture(bitmap)
                    bitmap.recycle()
                    if (texture != null) {
                        val rangeX = (camera.viewportWidth / 2f) - currentIconSize
                        val startX = MathUtils.random(-rangeX, rangeX)
                        val startY = MathUtils.random(-camera.viewportHeight / 3f, camera.viewportHeight / 2f)

                        val body = physicsWorld.createIconBody(
                            x = startX,
                            y = startY,
                            size = currentIconSize,
                            userData = IconData(texture, currentIconSize, app),
                            density = density,
                            friction = friction,
                            restitution = restitution
                        )
                        synchronized(activeBodies) { activeBodies.add(body) }
                    }
                }
            }

            val selectedPhotos = prefs.getSelectedPhotos()
            val selectedEmojis = prefs.getSelectedEmojis()

            for (photoUri in selectedPhotos) {
                val bitmap = loader.loadCustomPhotoIcon(photoUri) ?: continue
                Gdx.app.postRunnable {
                    val texture = bitmapToTexture(bitmap)
                    bitmap.recycle()
                    if (texture != null) {
                        val rangeX = (camera.viewportWidth / 2f) - currentIconSize
                        val startX = MathUtils.random(-rangeX, rangeX)
                        val startY = MathUtils.random(-camera.viewportHeight / 3f, camera.viewportHeight / 2f)

                        val app = AppInfo(
                            packageName = photoUri,
                            appName = "Custom Photo",
                            iconCacheKey = "custom_photo_${photoUri.hashCode()}",
                            type = AppInfo.TYPE_PHOTO
                        )

                        val body = physicsWorld.createIconBody(
                            x = startX,
                            y = startY,
                            size = currentIconSize,
                            userData = IconData(texture, currentIconSize, app),
                            density = density,
                            friction = friction,
                            restitution = restitution
                        )
                        synchronized(activeBodies) { activeBodies.add(body) }
                    }
                }
            }

            for (emoji in selectedEmojis) {
                val bitmap = loader.loadEmojiIcon(emoji) ?: continue
                Gdx.app.postRunnable {
                    val texture = bitmapToTexture(bitmap)
                    bitmap.recycle()
                    if (texture != null) {
                        val rangeX = (camera.viewportWidth / 2f) - currentIconSize
                        val startX = MathUtils.random(-rangeX, rangeX)
                        val startY = MathUtils.random(-camera.viewportHeight / 3f, camera.viewportHeight / 2f)

                        val app = AppInfo(
                            packageName = "emoji_${emoji}",
                            appName = emoji,
                            iconCacheKey = "emoji_${emoji.hashCode()}",
                            type = AppInfo.TYPE_EMOJI
                        )

                        val body = physicsWorld.createEmojiBody(
                            x = startX,
                            y = startY,
                            radius = currentIconSize / 2f,
                            userData = IconData(texture, currentIconSize, app),
                            density = density,
                            friction = friction,
                            restitution = restitution
                        )
                        synchronized(activeBodies) { activeBodies.add(body) }
                    }
                }
            }
        }
    }

    private fun loadBackgroundSettings(prefs: PreferenceRepository) {
        scope.launch(Dispatchers.IO) {
            val mode = prefs.getWallpaperMode()
            val type: Int
            val solidColor: String
            val gradStart: String
            val gradEnd: String
            val imgPath: String

            if (mode == "spinning") {
                type = 2
                solidColor = "#121212"
                gradStart = "#1e272e"
                gradEnd = "#0f0f10"
                imgPath = prefs.getSpinningBgImagePath()
            } else if (mode == "shape_path") {
                type = 2
                solidColor = "#121212"
                gradStart = "#1e272e"
                gradEnd = "#0f0f10"
                imgPath = prefs.getShapeBgImagePath()
            } else {
                type = prefs.getBgType()
                solidColor = prefs.getBgColor()
                gradStart = prefs.getBgGradientStart()
                gradEnd = prefs.getBgGradientEnd()
                imgPath = prefs.getBgImagePath()
            }

            Gdx.app.postRunnable {
                bgType = type
                bgColorStr = solidColor
                bgGradientStartStr = gradStart
                bgGradientEndStr = gradEnd
                bgImagePathStr = imgPath

                if (bgType == 2 && bgImagePathStr.isNotEmpty() && bgImagePathStr != currentLoadedBgPath) {
                    currentLoadedBgPath = bgImagePathStr
                    loadBackgroundImageAsync(bgImagePathStr)
                }
            }
        }
    }

    private fun loadBackgroundImageAsync(uriString: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
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
            wallpaperMode = prefs.getWallpaperMode()
            if (wallpaperMode == "spinning") {
                spinningPattern = prefs.getSpinningPattern()
                isWallpaperTouchEnabled = prefs.isWallpaperTouchEnabled()
                loadBackgroundSettings(prefs)
                reloadSpinningIcons()
                return@launch
            }
            if (wallpaperMode == "shape_path") {
                shapePathType = prefs.getShapePathType()
                isWallpaperTouchEnabled = prefs.isWallpaperTouchEnabled()
                loadBackgroundSettings(prefs)
                reloadShapePathIcons()
                return@launch
            }

            val repository = AppRepository(context)
            val newSize = prefs.getIconSize()
            val newDensity = prefs.getDensity()
            val newFriction = prefs.getFriction()
            val newRestitution = prefs.getRestitution()
            val isSensor = prefs.isSensorEnabled()
            val isTouch = prefs.isWallpaperTouchEnabled()

            val selectedApps = repository.getSelectedApps()
            val selectedPhotos = prefs.getSelectedPhotos()
            val selectedEmojis = prefs.getSelectedEmojis()
            val newKeys = selectedApps.map { it.packageName }.toSet() + selectedPhotos + selectedEmojis.map { "emoji_${it}" }

            loadBackgroundSettings(prefs)

            Gdx.app.postRunnable {
                isSensorEnabled = isSensor
                isWallpaperTouchEnabled = isTouch
                if (!isSensor) {
                    gravityX = 0f
                    gravityY = -9.8f
                    smoothX = 0f
                    smoothY = -9.8f
                }

                val currentKeys = synchronized(activeBodies) {
                    activeBodies.mapNotNull { (it.userData as? IconData)?.appInfo?.packageName }.toSet()
                }

                val listChanged = currentKeys != newKeys
                val sizeChanged = abs(currentIconSize - newSize) > 0.01f

                if (sizeChanged || listChanged) {
                    currentIconSize = newSize
                    reloadAllIcons(newDensity, newFriction, newRestitution)
                } else {
                    synchronized(activeBodies) {
                        for (body in activeBodies) {
                            val fixtures = body.fixtureList
                            for (fixture in fixtures) {
                                fixture.density = newDensity
                                fixture.friction = newFriction
                                fixture.restitution = newRestitution
                            }
                            body.resetMassData()
                        }
                    }
                }
            }
        }
    }

    private fun reloadAllIcons(density: Float, friction: Float, restitution: Float) {
        scope.launch(Dispatchers.IO) {
            val repository = AppRepository(context)
            val loader = IconLoader(context)
            val prefs = PreferenceRepository(context)

            val selectedApps = repository.getSelectedApps()
            val selectedPhotos = prefs.getSelectedPhotos()
            val selectedEmojis = prefs.getSelectedEmojis()

            val appBitmaps = selectedApps.mapNotNull { app ->
                val bitmap = loader.loadAppIcon(app.packageName)
                if (bitmap != null) Pair(app, bitmap) else null
            }

            val photoBitmaps = selectedPhotos.mapNotNull { photoUri ->
                val bitmap = loader.loadCustomPhotoIcon(photoUri)
                if (bitmap != null) Pair(photoUri, bitmap) else null
            }

            val emojiBitmaps = selectedEmojis.mapNotNull { emoji ->
                val bitmap = loader.loadEmojiIcon(emoji)
                if (bitmap != null) Pair(emoji, bitmap) else null
            }

            Gdx.app.postRunnable {
                synchronized(activeBodies) {
                    for (body in activeBodies) {
                        val iconData = body.userData as? IconData
                        iconData?.texture?.dispose()
                        physicsWorld.world.destroyBody(body)
                    }
                    activeBodies.clear()

                    for (pair in appBitmaps) {
                        val app = pair.first
                        val bitmap = pair.second
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()

                        if (texture != null) {
                            val rangeX = (camera.viewportWidth / 2f) - currentIconSize
                            val startX = MathUtils.random(-rangeX, rangeX)
                            val startY = MathUtils.random(-camera.viewportHeight / 3f, camera.viewportHeight / 2f)

                            val body = physicsWorld.createIconBody(
                                x = startX,
                                y = startY,
                                size = currentIconSize,
                                userData = IconData(texture, currentIconSize, app),
                                density = density,
                                friction = friction,
                                restitution = restitution
                            )
                            activeBodies.add(body)
                        }
                    }

                    for (pair in photoBitmaps) {
                        val photoUri = pair.first
                        val bitmap = pair.second
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()

                        if (texture != null) {
                            val rangeX = (camera.viewportWidth / 2f) - currentIconSize
                            val startX = MathUtils.random(-rangeX, rangeX)
                            val startY = MathUtils.random(-camera.viewportHeight / 3f, camera.viewportHeight / 2f)

                            val app = AppInfo(
                                packageName = photoUri,
                                appName = "Custom Photo",
                                iconCacheKey = "custom_photo_${photoUri.hashCode()}",
                                type = AppInfo.TYPE_PHOTO
                            )

                            val body = physicsWorld.createIconBody(
                                x = startX,
                                y = startY,
                                size = currentIconSize,
                                userData = IconData(texture, currentIconSize, app),
                                density = density,
                                friction = friction,
                                restitution = restitution
                            )
                            activeBodies.add(body)
                        }
                    }

                    for (pair in emojiBitmaps) {
                        val emoji = pair.first
                        val bitmap = pair.second
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()

                        if (texture != null) {
                            val rangeX = (camera.viewportWidth / 2f) - currentIconSize
                            val startX = MathUtils.random(-rangeX, rangeX)
                            val startY = MathUtils.random(-camera.viewportHeight / 3f, camera.viewportHeight / 2f)

                            val app = AppInfo(
                                packageName = "emoji_${emoji}",
                                appName = emoji,
                                iconCacheKey = "emoji_${emoji.hashCode()}",
                                type = AppInfo.TYPE_EMOJI
                            )

                            val body = physicsWorld.createEmojiBody(
                                x = startX,
                                y = startY,
                                radius = currentIconSize / 2f,
                                userData = IconData(texture, currentIconSize, app),
                                density = density,
                                friction = friction,
                                restitution = restitution
                            )
                            activeBodies.add(body)
                        }
                    }
                }
            }
        }
    }

    private fun bitmapToTexture(bitmap: Bitmap): Texture? {
        if (bitmap.isRecycled) return null
        return try {
            val texture = Texture(bitmap.width, bitmap.height, Pixmap.Format.RGBA8888)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.textureObjectHandle)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            texture
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun render() {
        // 1. Vẽ nền
        when (bgType) {
            0 -> {
                val color = try {
                    Color.valueOf(bgColorStr.replace("#", ""))
                } catch (e: Exception) {
                    Color.DARK_GRAY
                }
                Gdx.gl.glClearColor(color.r, color.g, color.b, 1f)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            }
            1 -> {
                Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

                val cStart = try {
                    Color.valueOf(bgGradientStartStr.replace("#", ""))
                } catch (e: Exception) {
                    Color.valueOf("1e272e")
                }
                val cEnd = try {
                    Color.valueOf(bgGradientEndStr.replace("#", ""))
                } catch (e: Exception) {
                    Color.valueOf("0f0f10")
                }

                shapeRenderer.projectionMatrix = camera.combined
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                val halfW = camera.viewportWidth / 2f
                val halfH = camera.viewportHeight / 2f
                shapeRenderer.rect(
                    -halfW, -halfH, camera.viewportWidth, camera.viewportHeight,
                    cEnd, cEnd, cStart, cStart
                )
                shapeRenderer.end()
            }
            2 -> {
                Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

                val bgTex = backgroundTexture
                if (bgTex != null) {
                    spriteBatch.projectionMatrix = camera.combined
                    spriteBatch.begin()
                    val halfW = camera.viewportWidth / 2f
                    val halfH = camera.viewportHeight / 2f
                    spriteBatch.draw(bgTex, -halfW, -halfH, camera.viewportWidth, camera.viewportHeight)
                    spriteBatch.end()
                }
            }
        }

        // 2. Tính vật lý
        if (wallpaperMode == "rolling") {
            if (isSensorEnabled) {
                physicsWorld.setGravity(gravityX, gravityY)
            } else {
                physicsWorld.setGravity(0f, -9.8f)
            }
            physicsWorld.step(Gdx.graphics.deltaTime)
        }

        camera.update()
        spriteBatch.projectionMatrix = camera.combined

        // 3. Vẽ icon app, ảnh & Emoji
        if (wallpaperMode == "spinning") {
            spriteBatch.begin()
            
            // Tăng góc xoay
            spinningAngle = (spinningAngle + 45f * Gdx.graphics.deltaTime) % 360f
            
            val cx = 0f
            val cy = 0f
            val count = synchronized(spinningIcons) { spinningIcons.size }
            
            if (count > 0) {
                synchronized(spinningIcons) {
                    when (spinningPattern) {
                        "single_circle" -> {
                            val radius = camera.viewportWidth * 0.28f
                            val angleStep = 360f / count
                            for (i in 0 until count) {
                                val angle = Math.toRadians((spinningAngle + i * angleStep).toDouble())
                                val x = cx + radius * kotlin.math.cos(angle)
                                val y = cy + radius * kotlin.math.sin(angle)
                                drawSpinningIcon(spriteBatch, spinningIcons[i], x.toFloat(), y.toFloat())
                            }
                        }
                        "dual_circle" -> {
                            val innerList = ArrayList<IconData>()
                            val outerList = ArrayList<IconData>()
                            for (i in 0 until count) {
                                if (i % 2 == 0) innerList.add(spinningIcons[i]) else outerList.add(spinningIcons[i])
                            }
                            
                            if (innerList.isNotEmpty()) {
                                val innerRadius = camera.viewportWidth * 0.18f
                                val innerCount = innerList.size
                                val innerStep = 360f / innerCount
                                for (i in 0 until innerCount) {
                                    val angle = Math.toRadians((spinningAngle + i * innerStep).toDouble())
                                    val x = cx + innerRadius * kotlin.math.cos(angle)
                                    val y = cy + innerRadius * kotlin.math.sin(angle)
                                    drawSpinningIcon(spriteBatch, innerList[i], x.toFloat(), y.toFloat())
                                }
                            }
                            
                            if (outerList.isNotEmpty()) {
                                val outerRadius = camera.viewportWidth * 0.33f
                                val outerCount = outerList.size
                                val outerStep = 360f / outerCount
                                for (j in 0 until outerCount) {
                                    val angle = Math.toRadians((-spinningAngle + j * outerStep).toDouble())
                                    val x = cx + outerRadius * kotlin.math.cos(angle)
                                    val y = cy + outerRadius * kotlin.math.sin(angle)
                                    drawSpinningIcon(spriteBatch, outerList[j], x.toFloat(), y.toFloat())
                                }
                            }
                        }
                        "vortex" -> {
                            val baseRadius = camera.viewportWidth * 0.08f
                            val radiusOffset = camera.viewportWidth * 0.015f
                            val angleOffset = 25f
                            for (i in 0 until count) {
                                val radius = baseRadius + i * radiusOffset
                                val angle = Math.toRadians((spinningAngle + i * angleOffset).toDouble())
                                val x = cx + radius * kotlin.math.cos(angle)
                                val y = cy + radius * kotlin.math.sin(angle)
                                drawSpinningIcon(spriteBatch, spinningIcons[i], x.toFloat(), y.toFloat())
                            }
                        }
                    }
                }
            }
            spriteBatch.end()
        } else if (wallpaperMode == "shape_path") {
            spriteBatch.begin()
            shapePathAngle = (shapePathAngle + 0.35f * Gdx.graphics.deltaTime) % (2f * MathUtils.PI)
            val count = synchronized(shapePathIcons) { shapePathIcons.size }
            if (count > 0) {
                synchronized(shapePathIcons) {
                    val step = (2f * MathUtils.PI) / count
                    val R = camera.viewportWidth * 0.35f
                    val cx = 0f
                    val cy = 0f

                    for (i in 0 until count) {
                        val t = shapePathAngle + i * step
                        var x = 0f
                        var y = 0f
                        when (shapePathType) {
                            "heart" -> {
                                val sinT = kotlin.math.sin(t)
                                x = (R * 16f * sinT * sinT * sinT) / 16f
                                y = (R * (13f * kotlin.math.cos(t) - 5f * kotlin.math.cos(2f * t) - 2f * kotlin.math.cos(3f * t) - kotlin.math.cos(4f * t))) / 16f
                            }
                            "infinity" -> {
                                val cosT = kotlin.math.cos(t)
                                val sinT = kotlin.math.sin(t)
                                val denom = 1f + sinT * sinT
                                x = (R * 1.5f * cosT) / denom
                                y = (R * 1.5f * sinT * cosT) / denom
                            }
                            "star" -> {
                                 val r = R * (0.65f + 0.35f * kotlin.math.cos(5f * t))
                                 x = r * kotlin.math.cos(t)
                                 y = r * kotlin.math.sin(t)
                             }
                            "flower" -> {
                                val r = R * kotlin.math.cos(5f * t)
                                x = r * kotlin.math.cos(t)
                                y = r * kotlin.math.sin(t)
                            }
                            "clover" -> {
                                val r = R * kotlin.math.cos(2f * t)
                                x = r * kotlin.math.cos(t)
                                y = r * kotlin.math.sin(t)
                            }
                            "butterfly" -> {
                                val sinT = kotlin.math.sin(t)
                                val cosT = kotlin.math.cos(t)
                                val expCos = kotlin.math.exp(cosT.toDouble()).toFloat()
                                val sin12Double = kotlin.math.sin(t / 12f).toDouble()
                                val r = R * (expCos - 2f * kotlin.math.cos(4f * t) - java.lang.Math.pow(sin12Double, 5.0).toFloat()) / 4f
                                x = r * sinT
                                y = r * cosT
                            }
                        }
                        drawSpinningIcon(spriteBatch, shapePathIcons[i], cx + x, cy + y)
                    }
                }
            }
            spriteBatch.end()
        } else {
            spriteBatch.begin()
            synchronized(activeBodies) {
                val iterator = activeBodies.iterator()
                while (iterator.hasNext()) {
                    val body = iterator.next()
                    val iconData = body.userData as? IconData ?: continue
                    val size = iconData.size
                    val pos = body.position
                    val angleInDegrees = body.angle * MathUtils.radiansToDegrees

                    spriteBatch.draw(
                        iconData.texture,
                        pos.x - size / 2f,
                        pos.y - size / 2f,
                        size / 2f,
                        size / 2f,
                        size,
                        size,
                        1f,
                        1f,
                        angleInDegrees,
                        0,
                        0,
                        iconData.texture.width,
                        iconData.texture.height,
                        false,
                        false
                    )
                }
            }
            spriteBatch.end()
        }

        // 4. Vẽ các hạt nổ bụi neon lung linh bay tản ra ngoài (Alpha blended)
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

                // Cập nhật tọa độ hạt rơi
                p.x += p.vx * Gdx.graphics.deltaTime
                p.y += p.vy * Gdx.graphics.deltaTime
                p.vy -= 4.0f * Gdx.graphics.deltaTime // Trọng lực hạt rơi tự do

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

        // Giới hạn lại tọa độ các icon động nằm trong biên mới để tránh bị kẹt hoặc rơi ra ngoài, đồng thời đánh thức chúng
        val halfW = camera.viewportWidth / 2f
        val halfH = camera.viewportHeight / 2f
        synchronized(activeBodies) {
            for (body in activeBodies) {
                val pos = body.position
                val iconData = body.userData as? IconData
                val size = iconData?.size ?: 1.0f
                val padding = size / 2f + 0.2f
                val clampX = pos.x.coerceIn(-halfW + padding, halfW - padding)
                val clampY = pos.y.coerceIn(-halfH + padding, halfH - padding)
                body.setTransform(clampX, clampY, body.angle)
                body.isAwake = true
            }
        }
    }

    override fun pause() {
        sensorManager?.unregisterListener(this)
        destroyMouseJoint()
    }

    override fun resume() {
        if (isSensorEnabled) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        reloadPhysicsSettings()
    }

    override fun dispose() {
        scope.cancel()
        sensorManager?.unregisterListener(this)
        destroyMouseJoint()
        spriteBatch.dispose()
        shapeRenderer.dispose()
        backgroundTexture?.dispose()
        particles.clear()
        
        synchronized(activeBodies) {
            for (body in activeBodies) {
                val iconData = body.userData as? IconData
                iconData?.texture?.dispose()
            }
            activeBodies.clear()
        }

        synchronized(spinningIcons) {
            for (icon in spinningIcons) {
                icon.texture.dispose()
            }
            spinningIcons.clear()
        }
        
        debugRenderer?.dispose()
        physicsWorld.dispose()
    }

    override fun offsetChange(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {}

    override fun previewStateChange(isPreview: Boolean) {}

    override fun iconDropped(x: Int, y: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isSensorEnabled || event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        
        // Điều chỉnh chiều gia tốc theo góc quay màn hình hiện tại (Portrait, Landscape Left, Reverse Portrait, Landscape Right)
        val rotation = Gdx.input.rotation
        var rawX = 0f
        var rawY = 0f
        when (rotation) {
            90 -> {
                rawX = event.values[1] * 1.5f
                rawY = -event.values[0] * 1.5f
            }
            180 -> {
                rawX = event.values[0] * 1.5f
                rawY = event.values[1] * 1.5f
            }
            270 -> {
                rawX = -event.values[1] * 1.5f
                rawY = event.values[0] * 1.5f
            }
            else -> { // 0 (Dọc)
                rawX = -event.values[0] * 1.5f
                rawY = -event.values[1] * 1.5f
            }
        }

        smoothX += sensorAlpha * (rawX - smoothX)
        smoothY += sensorAlpha * (rawY - smoothY)

        gravityX = smoothX
        gravityY = smoothY
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun drawSpinningIcon(batch: SpriteBatch, icon: IconData, x: Float, y: Float) {
        val size = icon.size
        batch.draw(
            icon.texture,
            x - size / 2f,
            y - size / 2f,
            size / 2f,
            size / 2f,
            size,
            size,
            1f,
            1f,
            0f,
            0,
            0,
            icon.texture.width,
            icon.texture.height,
            false,
            false
        )
    }

    private fun reloadSpinningIcons() {
        scope.launch(Dispatchers.IO) {
            val prefs = PreferenceRepository(context)
            val loader = IconLoader(context)
            val newSize = prefs.getIconSize()
            val selectedApps = prefs.getSelectedAppsSpinning()

            val appBitmaps = selectedApps.mapNotNull { pkg ->
                val bitmap = loader.loadAppIcon(pkg)
                if (bitmap != null) Pair(pkg, bitmap) else null
            }

            val selectedEmojis = prefs.getSelectedEmojisSpinning()
            val emojiBitmaps = selectedEmojis.mapNotNull { emoji ->
                val bitmap = loader.loadEmojiIcon(emoji)
                if (bitmap != null) Pair(emoji, bitmap) else null
            }

            val selectedPhotos = prefs.getSelectedPhotosSpinning()
            val photoBitmaps = selectedPhotos.mapNotNull { uri ->
                val bitmap = loader.loadCustomPhotoIcon(uri)
                if (bitmap != null) Pair(uri, bitmap) else null
            }

            Gdx.app.postRunnable {
                synchronized(spinningIcons) {
                    for (icon in spinningIcons) {
                        icon.texture.dispose()
                    }
                    spinningIcons.clear()

                    for (pair in appBitmaps) {
                        val pkg = pair.first
                        val bitmap = pair.second
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()

                        if (texture != null) {
                            val app = AppInfo(packageName = pkg, appName = "", iconCacheKey = pkg, type = AppInfo.TYPE_APP)
                            spinningIcons.add(IconData(texture, newSize, app))
                        }
                    }

                    for (pair in emojiBitmaps) {
                        val emoji = pair.first
                        val bitmap = pair.second
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()

                        if (texture != null) {
                            val app = AppInfo(packageName = "emoji_$emoji", appName = emoji, iconCacheKey = "emoji_${emoji.hashCode()}", type = AppInfo.TYPE_EMOJI)
                            spinningIcons.add(IconData(texture, newSize, app))
                        }
                    }

                    for (pair in photoBitmaps) {
                        val uri = pair.first
                        val bitmap = pair.second
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()

                        if (texture != null) {
                            val app = AppInfo(packageName = uri, appName = "Custom Photo", iconCacheKey = "custom_photo_${uri.hashCode()}", type = AppInfo.TYPE_PHOTO)
                            spinningIcons.add(IconData(texture, newSize, app))
                        }
                    }
                }
            }
        }
    }

    private fun reloadShapePathIcons() {
        scope.launch(Dispatchers.IO) {
            val prefs = PreferenceRepository(context)
            val loader = IconLoader(context)
            val repository = AppRepository(context)
            val newSize = prefs.getIconSize()

            var selectedAppsShape = prefs.getSelectedAppsShape()
            if (selectedAppsShape.isEmpty()) {
                val allApps = repository.getInstalledApps()
                selectedAppsShape = allApps.take(24).map { it.packageName }.toSet()
            }
            val appBitmaps = selectedAppsShape.mapNotNull { pkg ->
                val bitmap = loader.loadAppIcon(pkg)
                if (bitmap != null) Pair(pkg, bitmap) else null
            }

            val selectedEmojisShape = prefs.getSelectedEmojisShape()
            val emojiBitmaps = selectedEmojisShape.mapNotNull { emoji ->
                val bitmap = loader.loadEmojiIcon(emoji)
                if (bitmap != null) Pair(emoji, bitmap) else null
            }

            val selectedPhotosShape = prefs.getSelectedPhotosShape()
            val photoBitmaps = selectedPhotosShape.mapNotNull { uri ->
                val bitmap = loader.loadCustomPhotoIcon(uri)
                if (bitmap != null) Pair(uri, bitmap) else null
            }

            Gdx.app.postRunnable {
                synchronized(shapePathIcons) {
                    for (icon in shapePathIcons) {
                        icon.texture.dispose()
                    }
                    shapePathIcons.clear()

                    // Load Apps
                    for (pair in appBitmaps) {
                        val pkg = pair.first
                        val bitmap = pair.second
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()
                        if (texture != null) {
                            val app = AppInfo(packageName = pkg, appName = "", iconCacheKey = pkg, type = AppInfo.TYPE_APP)
                            shapePathIcons.add(IconData(texture, newSize, app))
                        }
                    }

                    // Load Emojis
                    for (pair in emojiBitmaps) {
                        val emoji = pair.first
                        val bitmap = pair.second
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()
                        if (texture != null) {
                            val app = AppInfo(packageName = "emoji_$emoji", appName = emoji, iconCacheKey = "emoji_${emoji.hashCode()}", type = AppInfo.TYPE_EMOJI)
                            shapePathIcons.add(IconData(texture, newSize, app))
                        }
                    }

                    // Load Photos
                    for (pair in photoBitmaps) {
                        val uri = pair.first
                        val bitmap = pair.second
                        val texture = bitmapToTexture(bitmap)
                        bitmap.recycle()
                        if (texture != null) {
                            val app = AppInfo(packageName = uri, appName = "Custom Photo", iconCacheKey = "custom_photo_${uri.hashCode()}", type = AppInfo.TYPE_PHOTO)
                            shapePathIcons.add(IconData(texture, newSize, app))
                        }
                    }
                }
            }
        }
    }
}

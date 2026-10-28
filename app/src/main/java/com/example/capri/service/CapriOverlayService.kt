package com.example.capri.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.capri.ai.AiService
import com.example.capri.ai.CapriBrain
import com.example.capri.audio.TtsManager
import com.example.capri.audio.VoiceManager
import com.example.capri.device.DeviceControlManager
import com.example.capri.model.AvatarManager
import com.example.capri.model.ChatMessage
import com.example.capri.model.MessageType
import com.example.capri.tool.CapriToolRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot

class CapriOverlayService : Service() {

    companion object {
        const val ACTION_STOP_OVERLAY = "com.example.capri.ACTION_STOP_OVERLAY"
        const val NOTIFICATION_ID = 2077
        const val CHANNEL_ID = "capri_overlay_hud_channel"

        // Physics constants
        private const val MAX_TILT_DEGREES = 14.0f
        private const val MIN_IDLE_SWAY_DEGREES = 1.6f
        private const val DAMPING_COEFFICIENT = 5.2f
        private const val OSCILLATION_FREQUENCY = 14.5f
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var rootOverlayView: FrameLayout? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    // Shared Capri Subsystems
    private lateinit var deviceControlManager: DeviceControlManager
    private lateinit var ttsManager: TtsManager
    private lateinit var toolRegistry: CapriToolRegistry
    private lateinit var brain: CapriBrain
    private lateinit var aiService: AiService
    private var voiceManager: VoiceManager? = null

    private val recentMessages = mutableListOf<ChatMessage>()

    // Overlay State & Touch Tracking
    private var isExpanded = false
    private var touchSlop = 0
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var touchDownTime = 0L
    private var isDragging = false

    // Natural Swinging Physics Variables
    private var velocityTracker: VelocityTracker? = null
    private var currentTilt = 0f
    private var currentScaleY = 1.0f
    private var idleSwayAnimator: ValueAnimator? = null
    private var springWobbleAnimator: ValueAnimator? = null
    private var avatarHolderView: FrameLayout? = null
    private var avatarImageView: ImageView? = null
    private var miniAvatarImageView: ImageView? = null

    // UI Views
    private var collapsedAvatarView: View? = null
    private var expandedPanelView: View? = null
    private var transcriptTextView: TextView? = null
    private var statusBadgeView: TextView? = null
    private var stopSpeakingBtn: TextView? = null
    private var micButton: FrameLayout? = null
    private var micIconText: TextView? = null
    private var inputEditText: EditText? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            Log.w("CapriOverlayService", "Overlay permission not granted. Stopping service.")
            stopSelf()
            return
        }

        touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        AvatarManager.init(applicationContext)

        // Initialize subsystems
        deviceControlManager = DeviceControlManager(applicationContext)
        ttsManager = TtsManager(applicationContext)
        toolRegistry = CapriToolRegistry(deviceControlManager, ttsManager)
        brain = CapriBrain(toolRegistry)
        aiService = AiService(applicationContext)

        initVoiceManager()
        startAsForeground()
        setupOverlayWindow()
        observeTtsSpeakingState()
        observeAvatarChanges()
    }

    private fun initVoiceManager() {
        voiceManager = VoiceManager(applicationContext) { recognizedText ->
            if (recognizedText.isNotBlank()) {
                handleCommand(recognizedText)
            }
        }

        serviceScope.launch {
            voiceManager?.isListening?.collectLatest { listening ->
                updateMicVisualState(listening)
            }
        }

        serviceScope.launch {
            voiceManager?.partialText?.collectLatest { partial ->
                if (partial.isNotBlank()) {
                    transcriptTextView?.text = "» \"$partial\""
                }
            }
        }
    }

    private fun observeAvatarChanges() {
        serviceScope.launch {
            AvatarManager.customAvatarBitmap.collectLatest { bitmap ->
                val bm = bitmap ?: AvatarManager.getAvatarBitmap(applicationContext)
                if (bm != null) {
                    avatarImageView?.setImageBitmap(bm)
                    miniAvatarImageView?.setImageBitmap(bm)
                } else {
                    avatarImageView?.setImageResource(R.drawable.capri_user)
                    miniAvatarImageView?.setImageResource(R.drawable.capri_user)
                }
            }
        }
    }

    private fun startAsForeground() {
        createNotificationChannel()

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CapriOverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Capri Cyberpunk Companion • Seluist")
            .setContentText("Capri is hovering on screen with natural swinging physics. Tap avatar to open panel.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(0xFF00F0FF.toInt())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Open App", openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } catch (e: Exception) {
                Log.e("CapriOverlayService", "Failed to start with SPECIAL_USE: ${e.message}")
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Capri Cyberpunk HUD Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Capri's floating assistant companion active on screen"
                enableLights(true)
                lightColor = Color.CYAN
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_OVERLAY) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val wlp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 280
        }
        windowLayoutParams = wlp

        val root = FrameLayout(this)
        rootOverlayView = root

        // Build Collapsed Floating View
        val collapsed = buildCollapsedView()
        collapsedAvatarView = collapsed
        root.addView(collapsed)

        // Build Expanded Panel View (Initially Hidden)
        val expanded = buildExpandedPanelView()
        expanded.visibility = View.GONE
        expandedPanelView = expanded
        root.addView(expanded)

        // Attach Natural Physics Drag & Tap Listener to Collapsed Avatar
        setupAvatarTouchPhysics(collapsed)

        windowManager?.addView(root, wlp)

        // Start subtle stationary idle sway
        startIdleSway()
    }

    private fun buildCollapsedView(): View {
        val rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(12, 10, 12, 10)
        }

        // Avatar Holder Box: pivot is configured at top center (near head/charm hook)
        val holder = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(150, 175).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            // Set pivot at top center so rotation swings naturally like a hanging avatar charm
            post {
                pivotX = width / 2.0f
                pivotY = height * 0.12f
            }
        }
        avatarHolderView = holder

        // Neon Glow Backdrop behind character
        val glowBackdrop = View(this).apply {
            val glow = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x3300F0FF.toInt())
            }
            background = glow
            layoutParams = FrameLayout.LayoutParams(110, 110).apply {
                gravity = Gravity.CENTER
            }
        }
        holder.addView(glowBackdrop)

        // Capri Character Artwork (Preserving original transparent colors without stretching)
        val avatar = ImageView(this).apply {
            val customBitmap = AvatarManager.getAvatarBitmap(this@CapriOverlayService)
            if (customBitmap != null) {
                setImageBitmap(customBitmap)
            } else {
                setImageResource(R.drawable.capri_user)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = null // Transparent so PNG/WebP alpha renders perfectly
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        avatarImageView = avatar
        holder.addView(avatar)

        // Sleek Cyber Pedestal / Tag below character
        val tagContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val bg = GradientDrawable().apply {
                setColor(0xE60A0D18.toInt())
                cornerRadius = 14f
                setStroke(2, 0xFF00F0FF.toInt())
            }
            background = bg
            setPadding(10, 3, 10, 3)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 2
            }
        }

        val badge = TextView(this).apply {
            text = "● TALK TO CAPRI"
            setTextColor(0xFF00F0FF.toInt())
            textSize = 8.5f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }

        val subBadge = TextView(this).apply {
            text = "TAP • DRAG // SELUIST"
            setTextColor(0xFFFF007F.toInt())
            textSize = 7.5f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }

        tagContainer.addView(badge)
        tagContainer.addView(subBadge)

        rootContainer.addView(holder)
        rootContainer.addView(tagContainer)
        return rootContainer
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupAvatarTouchPhysics(view: View) {
        view.setOnTouchListener { _, event ->
            val params = windowLayoutParams ?: return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Stop idle sway and spring wobble immediately
                    stopIdleSway()
                    stopSpringWobble()

                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchDownTime = System.currentTimeMillis()
                    isDragging = false

                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(event)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000)

                    val vx = velocityTracker?.xVelocity ?: 0f
                    val vy = velocityTracker?.yVelocity ?: 0f

                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    val distance = hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()

                    if (!isDragging && distance > touchSlop) {
                        isDragging = true
                    }

                    if (isDragging) {
                        // 1. Screen Boundary Clamping: Never get stuck or lost off screen
                        val screen = getScreenDimensions()
                        val viewW = view.width.coerceAtLeast(140)
                        val viewH = view.height.coerceAtLeast(180)

                        val clampedX = (initialX + deltaX.toInt()).coerceIn(0, (screen.x - viewW).coerceAtLeast(0))
                        val clampedY = (initialY + deltaY.toInt()).coerceIn(50, (screen.y - viewH - 60).coerceAtLeast(50))

                        params.x = clampedX
                        params.y = clampedY
                        windowManager?.updateViewLayout(rootOverlayView, params)

                        // 2. Natural Horizontal Drag Tilt Physics:
                        // Tilt in direction of movement, scaled dynamically by velocity.
                        // Slow drag -> subtle 2-4 deg. Fast drag -> up to 12-14 deg.
                        val targetTilt = (vx / 160f).coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
                        // Smooth filter (lerp) to make physics buttery smooth
                        currentTilt += (targetTilt - currentTilt) * 0.40f
                        avatarHolderView?.rotation = currentTilt.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)

                        // 3. Natural Vertical Bounce:
                        // Slight vertical compression / stretch based on vertical velocity
                        val targetScale = (1.0f - (abs(vy) / 16000f)).coerceIn(0.96f, 1.04f)
                        currentScaleY += (targetScale - currentScaleY) * 0.35f
                        avatarHolderView?.scaleY = currentScaleY
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000)
                    val releaseVx = velocityTracker?.xVelocity ?: 0f
                    velocityTracker?.recycle()
                    velocityTracker = null

                    val elapsed = System.currentTimeMillis() - touchDownTime

                    if (!isDragging && elapsed < 350) {
                        // Tapped: Open expanded conversation HUD
                        expandPanel()
                        startIdleSway()
                    } else if (isDragging) {
                        // Drag released: Launch natural damped spring wobble with overshoot!
                        startSpringWobble(releaseTilt = currentTilt, releaseVx = releaseVx)
                    } else {
                        startIdleSway()
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.recycle()
                    velocityTracker = null
                    isDragging = false
                    startSpringWobble(releaseTilt = currentTilt, releaseVx = 0f)
                    true
                }

                else -> false
            }
        }
    }

    private fun startSpringWobble(releaseTilt: Float, releaseVx: Float) {
        stopSpringWobble()
        stopIdleSway()

        // Include subtle velocity flick kick into initial overshoot amplitude
        val velocityOvershoot = (releaseVx / 500f).coerceIn(-3.5f, 3.5f)
        val initialAmplitude = (releaseTilt + velocityOvershoot).coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)

        springWobbleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 750L
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedFraction
                // Damped harmonic oscillation equation: theta(t) = A * e^(-damping*t) * cos(omega*t)
                val decay = exp(-DAMPING_COEFFICIENT * progress)
                val oscillation = cos(OSCILLATION_FREQUENCY * progress)
                val wobbleAngle = (initialAmplitude * decay * oscillation).toFloat()

                currentTilt = wobbleAngle
                avatarHolderView?.rotation = wobbleAngle

                // Restore vertical scale to 1.0 smoothly
                currentScaleY += (1.0f - currentScaleY) * 0.2f
                avatarHolderView?.scaleY = currentScaleY
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentTilt = 0f
                    currentScaleY = 1.0f
                    avatarHolderView?.rotation = 0f
                    avatarHolderView?.scaleY = 1.0f
                    // Smoothly transition into stationary idle sway
                    startIdleSway()
                }
            })
            start()
        }
    }

    private fun startIdleSway() {
        stopIdleSway()
        // Subtle living avatar breathing sway (+/- 1.6 degrees)
        idleSwayAnimator = ValueAnimator.ofFloat(-MIN_IDLE_SWAY_DEGREES, MIN_IDLE_SWAY_DEGREES).apply {
            duration = 2400L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val angle = animator.animatedValue as Float
                if (!isDragging) {
                    avatarHolderView?.rotation = angle
                }
            }
            start()
        }
    }

    private fun stopIdleSway() {
        idleSwayAnimator?.cancel()
        idleSwayAnimator = null
    }

    private fun stopSpringWobble() {
        springWobbleAnimator?.cancel()
        springWobbleAnimator = null
    }

    private fun getScreenDimensions(): Point {
        val wm = windowManager ?: return Point(1080, 2400)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            val point = Point()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getSize(point)
            point
        }
    }

    private fun buildExpandedPanelView(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(0xF5080A12.toInt())
                cornerRadius = 28f
                setStroke(3, 0xFF00F0FF.toInt())
            }
            background = bg
            setPadding(16, 14, 16, 14)
            layoutParams = FrameLayout.LayoutParams(680, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        // 1. Top Header: Title + Minimize + Close
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val title = TextView(this).apply {
            text = "⚡ CAPRI // CREATED BY SELUIST"
            setTextColor(0xFF00F0FF.toInt())
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val minBtn = TextView(this).apply {
            text = "—"
            setTextColor(0xFFFFE600.toInt())
            textSize = 16f
            setPadding(12, 4, 12, 4)
            setOnClickListener { collapsePanel() }
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(0xFFFF007F.toInt())
            textSize = 16f
            setPadding(12, 4, 12, 4)
            setOnClickListener { stopSelf() }
        }

        topBar.addView(title)
        topBar.addView(minBtn)
        topBar.addView(closeBtn)
        panel.addView(topBar)

        // Mini Avatar & 3 Interactive Options (Say, Send Message, Just Talk)
        val talkModesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 8)
            }
        }

        // Mini Capri Avatar (User imported image or bundled character)
        val miniAvatar = ImageView(this).apply {
            val customBitmap = AvatarManager.getAvatarBitmap(this@CapriOverlayService)
            if (customBitmap != null) {
                setImageBitmap(customBitmap)
            } else {
                setImageResource(R.drawable.capri_user)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = null
            layoutParams = LinearLayout.LayoutParams(64, 80).apply {
                setMargins(0, 0, 8, 0)
            }
        }
        miniAvatarImageView = miniAvatar
        talkModesRow.addView(miniAvatar)

        // 3 Options Container
        val buttonsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Option 1: SAY (Voice Command)
        val sayBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                setColor(0xEE0B0D17.toInt())
                cornerRadius = 10f
                setStroke(2, 0xFF00F0FF.toInt())
            }
            background = bg
            setPadding(6, 8, 6, 8)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 5, 0)
            }
            setOnClickListener {
                transcriptTextView?.text = "Listening for voice command, Seluist... Speak now!"
                startVoiceListening()
            }
        }
        val sayTitle = TextView(this).apply {
            text = "🎙️ SAY"
            setTextColor(0xFF00F0FF.toInt())
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val saySub = TextView(this).apply {
            text = "COMMAND"
            setTextColor(0xFF708090.toInt())
            textSize = 7f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        sayBtn.addView(sayTitle)
        sayBtn.addView(saySub)
        buttonsRow.addView(sayBtn)

        // Option 2: SEND MESSAGE (Type Text)
        val msgBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                setColor(0xEE0B0D17.toInt())
                cornerRadius = 10f
                setStroke(2, 0xFFFF007F.toInt())
            }
            background = bg
            setPadding(6, 8, 6, 8)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 5, 0)
            }
            setOnClickListener {
                transcriptTextView?.text = "Type your command or message below, Seluist:"
                inputEditText?.requestFocus()
            }
        }
        val msgTitle = TextView(this).apply {
            text = "💬 MESSAGE"
            setTextColor(0xFFFF007F.toInt())
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val msgSub = TextView(this).apply {
            text = "SEND TEXT"
            setTextColor(0xFF708090.toInt())
            textSize = 7f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        msgBtn.addView(msgTitle)
        msgBtn.addView(msgSub)
        buttonsRow.addView(msgBtn)

        // Option 3: JUST TALK (Free AI Banter & Dialogue)
        val talkBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                setColor(0xEE0B0D17.toInt())
                cornerRadius = 10f
                setStroke(2, 0xFFFFE600.toInt())
            }
            background = bg
            setPadding(6, 8, 6, 8)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val greeting = "Hey Seluist! I'm right here hovering on your screen. What's the plan, choom?"
                transcriptTextView?.text = greeting
                ttsManager.speak(greeting)
                recentMessages.add(ChatMessage(text = greeting, type = MessageType.CAPRI_AI))
                serviceScope.launch {
                    kotlinx.coroutines.delay(2200)
                    startVoiceListening()
                }
            }
        }
        val talkTitle = TextView(this).apply {
            text = "⚡ JUST TALK"
            setTextColor(0xFFFFE600.toInt())
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val talkSub = TextView(this).apply {
            text = "AI CHAT"
            setTextColor(0xFF708090.toInt())
            textSize = 7f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        talkBtn.addView(talkTitle)
        talkBtn.addView(talkSub)
        buttonsRow.addView(talkBtn)

        talkModesRow.addView(buttonsRow)
        panel.addView(talkModesRow)

        // 2. Status Badge / Mode
        val statusBadge = TextView(this).apply {
            text = "ONLINE // READY FOR VOICE COMMANDS"
            setTextColor(0xFF708090.toInt())
            textSize = 9f
            typeface = Typeface.MONOSPACE
            setPadding(0, 2, 0, 6)
        }
        statusBadgeView = statusBadge
        panel.addView(statusBadge)

        // 3. Transcript Display Area
        val transcriptBox = ScrollView(this).apply {
            val bg = GradientDrawable().apply {
                setColor(0xCC05060A.toInt())
                cornerRadius = 14f
                setStroke(1, 0xFF1C2237.toInt())
            }
            background = bg
            setPadding(12, 10, 12, 10)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                140
            )
        }

        val transcript = TextView(this).apply {
            text = "Capri floating companion active, Seluist! Tap mic, drag me around, or type any command."
            setTextColor(0xFFE2E8F0.toInt())
            textSize = 11.5f
            typeface = Typeface.DEFAULT
            setLineSpacing(0f, 1.15f)
        }
        transcriptTextView = transcript
        transcriptBox.addView(transcript)
        panel.addView(transcriptBox)

        // 4. Stop Speaking Interruption Button
        val stopBtn = TextView(this).apply {
            text = "■ STOP SPEAKING"
            setTextColor(0xFF05060A.toInt())
            textSize = 10f
            typeface = Typeface.MONOSPACE
            val btnBg = GradientDrawable().apply {
                setColor(0xFFFF007F.toInt())
                cornerRadius = 10f
            }
            background = btnBg
            setPadding(16, 6, 16, 6)
            gravity = Gravity.CENTER
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 4)
            }
            layoutParams = lp
            setOnClickListener {
                ttsManager.stop()
                transcriptTextView?.text = "Muted voice output."
                visibility = View.GONE
            }
        }
        stopSpeakingBtn = stopBtn
        panel.addView(stopBtn)

        // 5. Quick Action Chips Row
        val scrollActions = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        val chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        fun createChip(label: String, color: Int, onClick: () -> Unit) {
            val chip = TextView(this).apply {
                text = label
                setTextColor(color)
                textSize = 10f
                typeface = Typeface.MONOSPACE
                val bg = GradientDrawable().apply {
                    setColor(0xCC0E1220.toInt())
                    cornerRadius = 10f
                    setStroke(1, color)
                }
                background = bg
                setPadding(14, 6, 14, 6)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 10, 0)
                }
                layoutParams = lp
                setOnClickListener { onClick() }
            }
            chipsRow.addView(chip)
        }

        createChip("⚡ Torch", 0xFFFFE600.toInt()) {
            handleCommand("toggle flashlight")
        }
        createChip("🔊 Vol +", 0xFF00F0FF.toInt()) {
            handleCommand("volume up")
        }
        createChip("🔉 Vol -", 0xFF00F0FF.toInt()) {
            handleCommand("volume down")
        }
        createChip("▶ YouTube", 0xFFFF007F.toInt()) {
            handleCommand("launch youtube")
        }
        createChip("💬 Discord", 0xFF8A2BE2.toInt()) {
            handleCommand("open discord")
        }
        createChip("⚙ Settings", 0xFF00F0FF.toInt()) {
            handleCommand("open settings")
        }

        scrollActions.addView(chipsRow)
        panel.addView(scrollActions)

        // 6. Dock: Mic Button + EditText + Send
        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Mic Button
        val micFrame = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xCC0E1220.toInt())
                setStroke(2, 0xFF00F0FF.toInt())
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                setMargins(0, 0, 10, 0)
            }
        }

        val micIcon = TextView(this).apply {
            text = "🎤"
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        micIconText = micIcon
        micFrame.addView(micIcon)

        micFrame.setOnClickListener {
            toggleVoiceListening()
        }
        micButton = micFrame
        dock.addView(micFrame)

        // Text input
        val editText = EditText(this).apply {
            hint = "Type command or chat..."
            setHintTextColor(0xFF708090.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 12f
            typeface = Typeface.DEFAULT
            maxLines = 1
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            val editBg = GradientDrawable().apply {
                setColor(0xCC05060A.toInt())
                cornerRadius = 14f
                setStroke(1, 0xFF1C2237.toInt())
            }
            background = editBg
            setPadding(16, 10, 16, 10)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 8, 0)
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    val txt = text.toString().trim()
                    if (txt.isNotEmpty()) {
                        handleCommand(txt)
                        setText("")
                    }
                    true
                } else false
            }
        }
        inputEditText = editText
        dock.addView(editText)

        // Send Button
        val sendBtn = TextView(this).apply {
            text = "➤"
            setTextColor(0xFF05060A.toInt())
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF00F0FF.toInt())
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(70, 70)
            setOnClickListener {
                val txt = inputEditText?.text?.toString()?.trim() ?: ""
                if (txt.isNotEmpty()) {
                    handleCommand(txt)
                    inputEditText?.setText("")
                }
            }
        }
        dock.addView(sendBtn)

        panel.addView(dock)
        return panel
    }

    private fun expandPanel() {
        isExpanded = true
        collapsedAvatarView?.visibility = View.GONE
        expandedPanelView?.visibility = View.VISIBLE

        windowLayoutParams?.let { params ->
            // Make focusable so the EditText can receive keyboard input
            params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            windowManager?.updateViewLayout(rootOverlayView, params)
        }
    }

    private fun collapsePanel() {
        isExpanded = false
        voiceManager?.stopListening()
        expandedPanelView?.visibility = View.GONE
        collapsedAvatarView?.visibility = View.VISIBLE

        windowLayoutParams?.let { params ->
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            windowManager?.updateViewLayout(rootOverlayView, params)
        }
        startIdleSway()
    }

    private fun toggleVoiceListening() {
        val vm = voiceManager ?: return
        if (vm.isListening.value) {
            vm.stopListening()
        } else {
            ttsManager.stop()
            transcriptTextView?.text = "Listening for speech command..."
            vm.startListening()
        }
    }

    private fun startVoiceListening() {
        val vm = voiceManager ?: return
        ttsManager.stop()
        if (!vm.isListening.value) {
            vm.startListening()
        }
    }

    private fun updateMicVisualState(listening: Boolean) {
        val micFrame = micButton ?: return
        val micText = micIconText ?: return

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (listening) {
                setColor(0xFFFF007F.toInt())
                setStroke(3, 0xFF00F0FF.toInt())
            } else {
                setColor(0xCC0E1220.toInt())
                setStroke(2, 0xFF00F0FF.toInt())
            }
        }
        micFrame.background = bg
        micText.text = if (listening) "🔴" else "🎤"
        statusBadgeView?.text = if (listening) "● LISTENING... SPEAK NOW" else "ONLINE // READY FOR VOICE COMMANDS"
    }

    private fun observeTtsSpeakingState() {
        serviceScope.launch {
            ttsManager.isSpeaking.collectLatest { speaking ->
                stopSpeakingBtn?.visibility = if (speaking) View.VISIBLE else View.GONE
            }
        }
    }

    private fun handleCommand(rawText: String) {
        val input = rawText.trim()
        if (input.isEmpty()) return

        voiceManager?.stopListening()
        recentMessages.add(ChatMessage(text = input, type = MessageType.USER))

        // 1. Process with CapriBrain local rules
        val result = brain.processInput(input, null)

        if (result.handledLocally) {
            val reply = result.responseText
            transcriptTextView?.text = reply
            ttsManager.speak(reply)
            recentMessages.add(
                ChatMessage(
                    text = reply,
                    type = MessageType.CAPRI_COMMAND,
                    commandExecuted = result.commandType,
                    isSuccess = result.isSuccess
                )
            )
        } else if (result.shouldPassToOnlineAi) {
            transcriptTextView?.text = "Connecting to Gemini netlink..."
            serviceScope.launch {
                val aiResult = if (aiService.hasValidApiKey()) {
                    aiService.queryOnlineAi(
                        userPrompt = input,
                        recentHistory = recentMessages,
                        toolRegistry = toolRegistry,
                        activity = null
                    )
                } else {
                    com.example.capri.ai.AiQueryResult(
                        replyText = brain.getOfflineFallbackResponse(input)
                    )
                }

                transcriptTextView?.text = aiResult.replyText
                ttsManager.speak(aiResult.replyText)
                recentMessages.add(
                    ChatMessage(
                        text = aiResult.replyText,
                        type = MessageType.CAPRI_AI
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopIdleSway()
        stopSpringWobble()
        serviceScope.cancel()
        voiceManager?.destroy()
        ttsManager.shutdown()

        rootOverlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.e("CapriOverlayService", "Error removing overlay view: ${e.message}")
            }
            rootOverlayView = null
        }
    }
}

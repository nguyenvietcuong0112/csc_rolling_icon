package com.rolling.spinning.icon3d.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.rolling.spinning.icon3d.R
import com.rolling.spinning.icon3d.data.PreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShapeSelectorActivity : BaseActivity() {

    private lateinit var preferenceRepository: PreferenceRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var cardHeart: MaterialCardView
    private lateinit var cardInfinity: MaterialCardView
    private lateinit var cardStar: MaterialCardView
    private lateinit var cardFlower: MaterialCardView
    private lateinit var cardClover: MaterialCardView
    private lateinit var cardButterfly: MaterialCardView
    private lateinit var btnNext: Button

    private var selectedShape = "heart" // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shape_selector)

        preferenceRepository = PreferenceRepository(this)

        cardHeart = findViewById(R.id.cardHeart)
        cardInfinity = findViewById(R.id.cardInfinity)
        cardStar = findViewById(R.id.cardStar)
        cardFlower = findViewById(R.id.cardFlower)
        cardClover = findViewById(R.id.cardClover)
        cardButterfly = findViewById(R.id.cardButterfly)
        btnNext = findViewById(R.id.btnNext)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Add selection click listeners
        cardHeart.setOnClickListener { selectShape("heart") }
        cardInfinity.setOnClickListener { selectShape("infinity") }
        cardStar.setOnClickListener { selectShape("star") }
        cardFlower.setOnClickListener { selectShape("flower") }
        cardClover.setOnClickListener { selectShape("clover") }
        cardButterfly.setOnClickListener { selectShape("butterfly") }

        // Load saved selection
        scope.launch {
            val savedShape = preferenceRepository.getShapePathType()
            selectShape(savedShape)
        }

        btnNext.setOnClickListener {
            scope.launch {
                preferenceRepository.setShapePathType(selectedShape)
                val intent = Intent(this@ShapeSelectorActivity, ShapeSelectionActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun selectShape(shape: String) {
        selectedShape = shape

        val bgSelected = 0xFFEED4B6.toInt()
        val bgUnselected = 0xFFF5D8B3.toInt()
        val strokeSelected = 0xFFC85C32.toInt()
        val strokeUnselected = 0xFF2E1C0C.toInt()
        val widthSelected = (4f * resources.displayMetrics.density).toInt()
        val widthUnselected = (2f * resources.displayMetrics.density).toInt()

        // Reset backgrounds
        cardHeart.setCardBackgroundColor(if (shape == "heart") bgSelected else bgUnselected)
        cardInfinity.setCardBackgroundColor(if (shape == "infinity") bgSelected else bgUnselected)
        cardStar.setCardBackgroundColor(if (shape == "star") bgSelected else bgUnselected)
        cardFlower.setCardBackgroundColor(if (shape == "flower") bgSelected else bgUnselected)
        cardClover.setCardBackgroundColor(if (shape == "clover") bgSelected else bgUnselected)
        cardButterfly.setCardBackgroundColor(if (shape == "butterfly") bgSelected else bgUnselected)

        // Add borders/elevations for premium look
        cardHeart.strokeColor = if (shape == "heart") strokeSelected else strokeUnselected
        cardHeart.strokeWidth = if (shape == "heart") widthSelected else widthUnselected

        cardInfinity.strokeColor = if (shape == "infinity") strokeSelected else strokeUnselected
        cardInfinity.strokeWidth = if (shape == "infinity") widthSelected else widthUnselected

        cardStar.strokeColor = if (shape == "star") strokeSelected else strokeUnselected
        cardStar.strokeWidth = if (shape == "star") widthSelected else widthUnselected

        cardFlower.strokeColor = if (shape == "flower") strokeSelected else strokeUnselected
        cardFlower.strokeWidth = if (shape == "flower") widthSelected else widthUnselected

        cardClover.strokeColor = if (shape == "clover") strokeSelected else strokeUnselected
        cardClover.strokeWidth = if (shape == "clover") widthSelected else widthUnselected

        cardButterfly.strokeColor = if (shape == "butterfly") strokeSelected else strokeUnselected
        cardButterfly.strokeWidth = if (shape == "butterfly") widthSelected else widthUnselected
    }
}

package com.rolling.spinning.icon3d.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.rolling.spinning.icon3d.R

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Nút Settings góc trên bên phải trên header
        findViewById<ImageView>(R.id.btnHeaderSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val txtInsertCoin = findViewById<TextView>(R.id.txtInsertCoin)
        val blinkAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.anim_blink)
        txtInsertCoin.startAnimation(blinkAnim)

        // 2. Click vào thẻ Rolling Icon hoặc nút Get Started của Rolling Icon
        val rollingClickAction = {
            val intent = Intent(this, RollingSelectionActivity::class.java)
            startActivity(intent)
        }
        findViewById<View>(R.id.cardRollingIcon).setOnClickListener { rollingClickAction() }
        findViewById<TextView>(R.id.btnGetStartedRolling).setOnClickListener { rollingClickAction() }

        // 3. Click vào thẻ Spinning Icon hoặc nút Get Started của Spinning Icon
        val spinningClickAction = {
            val intent = Intent(this, SpinningIconActivity::class.java)
            startActivity(intent)
        }
        findViewById<View>(R.id.cardSpinningIcon).setOnClickListener { spinningClickAction() }
        findViewById<TextView>(R.id.btnGetStartedSpinning).setOnClickListener { spinningClickAction() }

        // 4. Click vào thẻ Shape Path Icon hoặc nút Get Started của Shape Path Icon
        val shapePathClickAction = {
            val intent = Intent(this, ShapeSelectorActivity::class.java)
            startActivity(intent)
        }
        findViewById<View>(R.id.cardShapePathIcon).setOnClickListener { shapePathClickAction() }
        findViewById<TextView>(R.id.btnGetStartedShapePath).setOnClickListener { shapePathClickAction() }
    }
}

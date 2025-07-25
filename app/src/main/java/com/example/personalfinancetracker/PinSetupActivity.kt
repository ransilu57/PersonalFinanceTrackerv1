package com.example.personalfinancetracker

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinancetracker.databinding.ActivityPinSetupBinding
import com.example.personalfinancetracker.R

class PinSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinSetupBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val pinLength = 4
    private val pinBuilder = StringBuilder()
    private val pinDots = mutableListOf<View>()
    private var firstPin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)

        // Initialize PIN dots
        pinDots.addAll(listOf(binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4))

        // Setup num Numpad buttons
        setupNumpad()

        // Set initial subtitle
        binding.tvSubtitle.text = "Enter a 4-digit PIN"
    }

    private fun setupNumpad() {
        val numberButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )

        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (pinBuilder.length < pinLength) {
                    pinBuilder.append(index.toString().last())
                    updatePinDots()
                    if (pinBuilder.length == pinLength) {
                        handlePinEntry()
                    }
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            if (pinBuilder.isNotEmpty()) {
                pinBuilder.deleteCharAt(pinBuilder.length - 1)
                updatePinDots()
            }
        }
    }

    private fun updatePinDots() {
        for (i in pinDots.indices) {
            if (i < pinBuilder.length) {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled)
            } else {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot)
            }
        }
    }

    private fun handlePinEntry() {
        if (firstPin == null) {
            // First PIN entry
            firstPin = pinBuilder.toString()
            pinBuilder.clear()
            updatePinDots()
            binding.tvSubtitle.text = "Confirm your PIN"
        } else {
            // Confirm PIN
            val secondPin = pinBuilder.toString()
            if (firstPin == secondPin) {
                // PINs match, save to SharedPreferences
                with(sharedPreferences.edit()) {
                    putString("pin", firstPin)
                    apply()
                }
                Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show()

                // Redirect to PinAuthActivity
                startActivity(Intent(this, PinAuthActivity::class.java))
                finish()
            } else {
                // PINs don't match, reset and try again
                Toast.makeText(this, "PINs do not match, try again", Toast.LENGTH_SHORT).show()
                firstPin = null
                pinBuilder.clear()
                updatePinDots()
                binding.tvSubtitle.text = "Enter a 4-digit PIN"
            }
        }
    }
}
package com.example.personalfinancetracker

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.personalfinancetracker.databinding.ActivityPinAuthBinding
import com.example.personalfinancetracker.R
import java.util.concurrent.Executor

class PinAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinAuthBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val pinLength = 4
    private val pinBuilder = StringBuilder()
    private val pinDots = mutableListOf<View>()
    private var savedPin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)

        // Load saved PIN
        savedPin = sharedPreferences.getString("pin", null)

        // If no PIN is set, redirect to PinSetupActivity
        if (savedPin == null) {
            startActivity(Intent(this, PinSetupActivity::class.java))
            finish()
            return
        }

        // Initialize PIN dots
        pinDots.addAll(listOf(binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4))

        // Setup numpad buttons
        setupNumpad()

        // Setup fingerprint authentication
        setupFingerprintAuth()
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
                        verifyPin()
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

    private fun verifyPin() {
        val enteredPin = pinBuilder.toString()
        if (enteredPin == savedPin) {
            // PIN correct, proceed to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            pinBuilder.clear()
            updatePinDots()
        }
    }

    private fun setupFingerprintAuth() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@PinAuthActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Fingerprint authenticated, proceed to MainActivity
                startActivity(Intent(this@PinAuthActivity, MainActivity::class.java))
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@PinAuthActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Authentication")
            .setSubtitle("Use your fingerprint to access the app")
            .setNegativeButtonText("Use PIN")
            .build()

        // Trigger fingerprint prompt when the fingerprint area is clicked
        binding.fingerprintPrompt.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
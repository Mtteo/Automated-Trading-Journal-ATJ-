package com.example.atj

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.atj.data.AppDatabase
import com.example.atj.model.User
import com.example.atj.utils.SessionManager

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var createAccountButton: Button
    private lateinit var backToLoginButton: Button

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.isLoggedIn(this)) {
            openMain()
            return
        }

        setContentView(R.layout.activity_register)

        database = AppDatabase.getDatabase(this)

        usernameInput = findViewById(R.id.registerUsernameInput)
        passwordInput = findViewById(R.id.registerPasswordInput)
        confirmPasswordInput = findViewById(R.id.registerConfirmPasswordInput)
        createAccountButton = findViewById(R.id.createAccountButton)
        backToLoginButton = findViewById(R.id.backToLoginButton)

        createAccountButton.setOnClickListener {
            register()
        }

        backToLoginButton.setOnClickListener {
            finish()
        }
    }

    private fun register() {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()

        if (username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Complete all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (username.length < 4) {
            Toast.makeText(this, "Username must be at least 4 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return
        }

        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }

        if (!hasLetter || !hasDigit) {
            Toast.makeText(
                this,
                "Password must contain at least one letter and one number",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val existingUser = database.userDao().getUserByUsername(username)

        if (existingUser != null) {
            Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
            return
        }

        val newUser = User(
            username = username,
            password = password
        )

        val newUserId = database.userDao().insertUser(newUser)

        SessionManager.saveLogin(this, newUserId, username)

        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()

        openMain()
    }

    private fun openMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
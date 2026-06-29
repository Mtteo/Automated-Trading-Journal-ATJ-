package com.example.atj

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.atj.data.AppDatabase
import com.example.atj.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.isLoggedIn(this)) {
            openMain()
            return
        }

        setContentView(R.layout.activity_login)

        database = AppDatabase.getDatabase(this)

        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)

        loginButton.setOnClickListener {
            login()
        }

        registerButton.setOnClickListener {
            openRegister()
        }
    }

    private fun login() {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Insert username and password", Toast.LENGTH_SHORT).show()
            return
        }

        val user = database.userDao().login(username, password)

        if (user != null) {
            SessionManager.saveLogin(this, user.id, user.username)
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
            openMain()
        } else {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
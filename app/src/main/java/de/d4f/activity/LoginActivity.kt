package de.d4f.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import de.d4f.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var submitButton: Button
    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        submitButton = findViewById(R.id.dialog_submit)
        editText = findViewById(R.id.dialog_edit_text)

        submitButton.setOnClickListener { view -> run {
            val newToken = editText.text.toString()

            GlobalScope.launch {
                val testConnection = URL("https://api.mapbox.com/directions-matrix/v1/mapbox/walking/?access_token=$newToken")
                val http = testConnection.openConnection() as HttpURLConnection
                val statusCode = http.responseCode

                if (statusCode != 401) {
                    runOnUiThread {
                        intent = Intent()
                        intent.putExtra("token", newToken)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Token invalid!", Toast.LENGTH_LONG)
                            .show()
                    }
                }

            }


        } }
    }
}
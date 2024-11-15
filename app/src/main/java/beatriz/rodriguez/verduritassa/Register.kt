package beatriz.rodriguez.verduritassa

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val registerButton: Button = findViewById(R.id.register_btn)
        val nameEditText: EditText = findViewById(R.id.register_name)
        val emailEditText: EditText = findViewById(R.id.register_email)
        val passwordEditText: EditText = findViewById(R.id.register_pass)
        val genderSpinner: Spinner = findViewById(R.id.register_gender)
        val countrySpinner: Spinner = findViewById(R.id.register_country)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val gender = genderSpinner.selectedItem.toString()
            val country = countrySpinner.selectedItem.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && gender.isNotEmpty() && country.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // User registration successful
                            val user = auth.currentUser
                            val userId = user?.uid

                            // Create a new user document in Firestore
                            val userMap = hashMapOf(
                                "nombre" to name,
                                "email" to email,
                                "genero" to gender,
                                "pais" to country
                            )

                            if (userId != null) {
                                db.collection("usuarios").document(userId)
                                    .set(userMap)
                                    .addOnSuccessListener {
                                        val intent = Intent(this, Login::class.java)
                                        Toast.makeText(baseContext, "Registro exitoso, por favor inicie sesión.",
                                            Toast.LENGTH_SHORT).show()
                                        startActivity(intent)
                                        // Navigate to the next activity or update UI
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(baseContext, "Error al guardar los datos del usuario.",
                                            Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            // If registration fails, display a message to the user.
                            Toast.makeText(baseContext, "Registro fallido.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(baseContext, "Please fill in all fields.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}
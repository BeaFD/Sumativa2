package beatriz.rodriguez.verduritassa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val loginButton: Button = findViewById(R.id.login_btn)
        val emailEditText: EditText = findViewById(R.id.login_email)
        val passwordEditText: EditText = findViewById(R.id.login_pass)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, fetch user data from Firestore
                            val user = auth.currentUser
                            user?.let {
                                db.collection("usuarios")
                                    .whereEqualTo("email", user.email)
                                    .get()
                                    .addOnSuccessListener { documents ->
                                        if (!documents.isEmpty) {
                                            val document = documents.first()
                                            val userName = document.getString("nombre") ?: "Usuario"
                                            val userEmail = document.getString("email") ?: user.email

                                            // Pass the user data to MainActivity
                                            val intent = Intent(this, MainActivity::class.java)
                                            intent.putExtra("USER_NAME", userName)
                                            intent.putExtra("USER_EMAIL", userEmail)
                                            startActivity(intent)
                                        } else {
                                            Toast.makeText(baseContext, "No user data found.",
                                                Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(baseContext, "Failed to retrieve user data.",
                                            Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(baseContext, "Intento de inicio de sesión fallido.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(baseContext, "Please enter email and password.",
                    Toast.LENGTH_SHORT).show()
            }
        }

        val googleSignInButton: Button = findViewById(R.id.login_btn_google)
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        val registerButton: Button = findViewById(R.id.register_nav_btn)
        registerButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, fetch user data from Firestore
                    val user = auth.currentUser
                    user?.let {
                        db.collection("usuarios")
                            .whereEqualTo("email", user.email)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val document = documents.first()
                                    val userName = document.getString("nombre") ?: "Usuario"
                                    val userEmail = document.getString("email") ?: user.email

                                    // Pass the user data to MainActivity
                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.putExtra("USER_NAME", userName)
                                    intent.putExtra("USER_EMAIL", userEmail)
                                    startActivity(intent)
                                } else {
                                    // No user data found, create a new user document
                                    val newUser = hashMapOf(
                                        "nombre" to (user.displayName ?: "Usuario"),
                                        "email" to user.email
                                    )
                                    db.collection("usuarios").document(user.uid)
                                        .set(newUser)
                                        .addOnSuccessListener {
                                            val intent = Intent(this, MainActivity::class.java)
                                            intent.putExtra("USER_NAME", user.displayName)
                                            intent.putExtra("USER_EMAIL", user.email)
                                            startActivity(intent)
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(baseContext, "Failed to create user data: ${e.message}",
                                                Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(baseContext, "Failed to retrieve user data.",
                                    Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication Failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}
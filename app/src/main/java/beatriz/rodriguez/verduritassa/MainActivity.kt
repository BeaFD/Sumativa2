package beatriz.rodriguez.verduritassa

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tableLayout: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize the Toolbar
        val toolbar: Toolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        // Set the title of the ActionBar
        supportActionBar?.title = "Verduritas SA"
        toolbar.setTitleMarginStart(8) // Add padding to the title

        // Get the user's full name from the Intent
        val fullName = intent.getStringExtra("USER_NAME") ?: "Usuario"

        // Extract the first name
        val firstName = fullName.split(" ").firstOrNull() ?: fullName

        // Find the TextView and set the welcome message
        val welcomeTextView: TextView = findViewById(R.id.welcomeTextView)
        welcomeTextView.text = "¡Bienvenidx, $firstName!"

        // Initialize the logout button
        val logoutButton: ImageButton = findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        val newButton: Button = findViewById(R.id.newButton)
        newButton.setOnClickListener {
            intent = Intent(this, AddProduct::class.java)
            startActivity(intent)
        }

        // Initialize the TableLayout
        tableLayout = findViewById(R.id.tableLayout)
    }

    override fun onResume() {
        super.onResume()

        // Remove all rows except the header
        val childCount = tableLayout.childCount
        if (childCount > 1) {
            tableLayout.removeViews(1, childCount - 1)
        }

        loadCultivos(auth.currentUser?.email ?: "")
    }

    private fun loadCultivos(userEmail: String) {
        db.collection("cultivos")
            .whereEqualTo("email_usuario", userEmail)
            .get()
            .addOnSuccessListener { documents ->
                var rowIndex = 1
                for (document in documents) {
                    val alias = document.getString("alias") ?: "N/A"
                    val fechaCosecha = document.getTimestamp("fecha_cosecha")?.toDate()
                    val planta = document.getString("planta") ?: "N/A"
                    val documentId = document.id

                    val formattedDate = fechaCosecha?.let {
                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(it)
                    } ?: "N/A"

                    addTableRow(rowIndex, alias, formattedDate, planta, documentId)
                    rowIndex++
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors
            }
    }

    private fun addTableRow(rowIndex: Int, alias: String, fechaCosecha: String, planta: String, documentId: String) {
        val tableRow = TableRow(this).apply {
            id = View.generateViewId()
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 36.dpToPx())
            background = getDrawable(R.drawable.table_border)
        }

        val aliasTextView = createTextView(alias).apply { id = View.generateViewId() }
        val fechaCosechaTextView = createTextView(fechaCosecha).apply { id = View.generateViewId() }
        val plantaTextView = createTextView(planta).apply { id = View.generateViewId() }

        val settingsButton = ImageButton(this).apply {
            id = View.generateViewId()
            layoutParams = TableRow.LayoutParams(36.dpToPx(), 36.dpToPx())
            setPadding(4, 4, 4, 4)
            setBackgroundResource(R.drawable.ic_settings)
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.dark_green)
            setOnClickListener {
                showBottomSheetDialog(documentId, this)

            }
        }

        tableRow.addView(aliasTextView)
        tableRow.addView(fechaCosechaTextView)
        tableRow.addView(plantaTextView)
        tableRow.addView(settingsButton)

        tableLayout.addView(tableRow)
    }

    private fun createTextView(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            layoutParams = TableRow.LayoutParams(0, 36.dpToPx(), 1f)
            setPadding(8, 8, 8, 8)
            setTextColor(ContextCompat.getColor(context, R.color.dark_grey))
            background = getDrawable(R.drawable.table_border)
        }
    }

    private fun showBottomSheetDialog(documentId: String, settingsButton: ImageButton) {
        settingsButton.setBackgroundResource(R.drawable.sett)
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = LayoutInflater.from(this).inflate(
            R.layout.bottom_sheet_layout,
            findViewById(R.id.bottomSheetContainer)
        )

        val editButton: Button = bottomSheetView.findViewById(R.id.editButton)
        val deleteButton: Button = bottomSheetView.findViewById(R.id.deleteButton)

        editButton.setOnClickListener {
            // Handle edit button click
            intent=Intent(this,EditProduct::class.java)
            intent.putExtra("DOCUMENT_ID", documentId)
            startActivity(intent)
            bottomSheetDialog.dismiss()
        }

        deleteButton.setOnClickListener {
            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Confirmar")
                .setMessage("¿Estas segurx que quieres eliminar este cultivo?")
                .setPositiveButton("Eliminar") { dialog, _ ->
                    // Delete the document
                    db.collection("cultivos").document(documentId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cultivo eliminado", Toast.LENGTH_SHORT).show()
                            // Refresh the table
                            val childCount = tableLayout.childCount
                            if (childCount > 1) {
                                tableLayout.removeViews(1, childCount - 1)
                            }

                            loadCultivos(auth.currentUser?.email ?: "")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error eliminando: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                    bottomSheetDialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        bottomSheetDialog.setOnDismissListener {
            // Restore the original icon when the dialog is dismissed
            settingsButton.setBackgroundResource(R.drawable.ic_settings)
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
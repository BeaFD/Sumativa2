package beatriz.rodriguez.verduritassa

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import beatriz.rodriguez.verduritassa.adapter.PlantAdapter
import beatriz.rodriguez.verduritassa.model.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddProduct : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var selectedPlant: Plant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val toolbar: Toolbar = findViewById(R.id.edit_plant_toolbar)
        setSupportActionBar(toolbar)

        // Set the title of the ActionBar
        supportActionBar?.title = "Verduritas SA"
        toolbar.setTitleMarginStart(8) // Add padding to the title

        // Enable the back button in the ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize the Spinner
        val items = listOf(
            Plant("Tomates", 80),
            Plant("Cebollas", 120),
            Plant("Lechugas", 85),
            Plant("Apio", 150),
            Plant("Choclo", 90)
        )

        val spinner: Spinner = findViewById(R.id.edit_plant_spinner)
        val adapter = PlantAdapter(this, items)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPlant = items[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle case where no item is selected
            }
        }

        // Initialize the DatePickerDialog
        val dateInput: EditText = findViewById(R.id.edit_plant_date)
        dateInput.setOnClickListener {
            showDatePickerDialog(dateInput)
        }

        // Initialize the Save button
        val saveButton: Button = findViewById(R.id.edit_plant_save)
        saveButton.setOnClickListener {
            savePlant()
        }
    }

    private fun showDatePickerDialog(dateInput: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
                dateInput.setText(dateFormat.format(selectedDate.time))
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun savePlant() {
        val aliasInput: EditText = findViewById(R.id.edit_plant_alias)
        val dateInput: EditText = findViewById(R.id.edit_plant_date)

        val alias = aliasInput.text.toString()
        val fechaCultivoStr = dateInput.text.toString()

        if (alias.isEmpty() || fechaCultivoStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
        val fechaCultivo = dateFormat.parse(fechaCultivoStr)

        val calendar = Calendar.getInstance()
        calendar.time = fechaCultivo
        calendar.add(Calendar.DAY_OF_YEAR, selectedPlant.days)

        val fechaCosecha = calendar.time

        val userEmail = auth.currentUser?.email ?: return

        val cultivo = hashMapOf(
            "alias" to alias,
            "email_usuario" to userEmail,
            "fecha_cultivo" to fechaCultivo,
            "planta" to selectedPlant.name,
            "fecha_cosecha" to fechaCosecha
        )

        db.collection("cultivos")
            .add(cultivo)
            .addOnSuccessListener {
                Toast.makeText(this, "Plant saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving plant: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
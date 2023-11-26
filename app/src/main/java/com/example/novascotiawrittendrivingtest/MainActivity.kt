package com.example.novascotiawrittendrivingtest


import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import android.widget.TextView
import androidx.cardview.widget.CardView
import java.util.Locale
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.novascotiawrittendrivingtest.aiChat.AIchatActivity
import com.example.novascotiawrittendrivingtest.dataClass.User
import com.example.novascotiawrittendrivingtest.test.DrivingTestActivity
import com.example.novascotiawrittendrivingtest.test.EmptyWrongActivity
import com.example.novascotiawrittendrivingtest.test.WrongQuestionReviewActivity

class MainActivity : AppCompatActivity() {

    private  lateinit var practiceTestContainer : CardView
    private lateinit var questionReviewContainer: CardView
    private lateinit var aiAssistantContainer: CardView
    private lateinit var testLocation: CardView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private var questionCount = 0
    private lateinit var navToolbar: Toolbar
    private lateinit var database: DatabaseReference
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set language
        val sharedPref = getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE)
        val language = sharedPref.getString("SelectedLanguage", Locale.getDefault().language)
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Set content view
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth to check if user is logged in
        val user = Firebase.auth.currentUser

        // If user is not logged in, navigate to login activity, else get user id
        if (user == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else{
            user.let {
                userId = it.uid
            }
        }

        // Get the navigation
        practiceTestContainer = findViewById(R.id.practiceTestContainer)
        questionReviewContainer = findViewById(R.id.questionReviewContainer)
        aiAssistantContainer = findViewById(R.id.AI_assistant_container)
        testLocation=findViewById(R.id.testLocationContainer)

        // Get the progress bar
        progressText = findViewById(R.id.progressText)

        // Initialize toolbar
        initialToolBar()

        // Set navigation listeners
        setNavigationListener()

        // Set question
        database = Firebase.database.reference
        val incorrectQuestionsRef = database.child("users").child(userId).child("incorrectQuestions")
        incorrectQuestionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Data exists
                    questionReviewContainer.setOnClickListener {
                        // Navigate to question review activity
                        val questionReviewIntent = Intent(this@MainActivity, WrongQuestionReviewActivity::class.java)
                        startActivity(questionReviewIntent)
                        finish()
                    }
                } else {
                    // Data is null or the path doesn't exist
                    questionReviewContainer.setOnClickListener {
                        // Navigate to question review activity
                        val questionReviewIntent = Intent(this@MainActivity, EmptyWrongActivity::class.java)
                        startActivity(questionReviewIntent)
                        finish()
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(ContentValues.TAG, "Database error: $databaseError")
            }
        })

        // Set progress bar
        progress()

        // Notification implementation
        createNotificationChannel()

        // Schedule notifications
        val notificationManager =
            com.example.novascotiawrittendrivingtest.notification.NotificationManager(this)
        notificationManager.scheduleNotification()
    }

    /**
     * Set the language to the opposite of the current language
     */
    private fun setNavigationListener() {
        aiAssistantContainer.setOnClickListener() {
            // Navigate to practice test activity
            val practiceTestIntent = Intent(this, AIchatActivity::class.java)
            startActivity(practiceTestIntent)
            finish()
        }

        practiceTestContainer.setOnClickListener() {
            // Navigate to practice test activity
            val practiceTestIntent = Intent(this, DrivingTestActivity::class.java)
            startActivity(practiceTestIntent)
            finish()
        }

        testLocation.setOnClickListener() {
            // Navigate to question review activity
            val testLocation = Intent(this, MapActivity::class.java)
            startActivity(testLocation)
            finish()
        }
    }

    // Will be completed once question part is done
    private fun progress() {
        progressBar = findViewById<ProgressBar>(R.id.user_progress_bar)

        var currentPosition = 0

        // logic to set progress bar based on question count
        progressBar.progress = questionCount

        // logic to set progress text based on question count
        database = Firebase.database.reference
        database.child("users").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue<User>()
                // Do something with the user data
                if (user != null) {
                    currentPosition = user.currentQuestionPosition
                }

                progressText.text = "$currentPosition / ${progressBar.max}"
                progressBar.progress = currentPosition
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(ContentValues.TAG, "Failed to read user data.", error.toException())
            }
        })

    }

    /**
     * Initialize the toolbar
     */
    private fun initialToolBar()
    {
        navToolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(navToolbar)
        supportActionBar?.setTitle("")
    }

    /**
     * Set the language to the opposite of the current language
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.action_log_out -> {

                Firebase.auth.signOut()

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Creates the menu for the toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * Creates a notification channel for the app
     */
    private fun createNotificationChannel() {
        val channelId = "default_channel"
        val channelName = "Default Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Channel description" // Set your channel description here
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

}

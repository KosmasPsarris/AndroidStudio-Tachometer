package com.p17191.ergasies.exercise1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationListener {

    MyTts myTts; // For TextToSpeech
    SharedPreferences preferences; // To keep speed limit
    LocationManager locationManager; // For gps data
    Button mainButton, recordsButton, changeLimitButton;
    TextView tachometerTextView, speedLimitTextView;
    SQLiteDatabase db; // For our database to save records
    Boolean recordSaved; // Helper variable for record saving
    public static final int REC_RESULT = 123; // For result of voice recognition

    // When data is returned from speech recognition, this method is called. Used to handle the data
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REC_RESULT && resultCode == RESULT_OK){ // requestCode = our REC_RESULT | resultCode = if everything went okay ( user spoke and machine recognised the message)
            // Data returned from voice recognition is basically matches between what the user said and what the machine thinks the user said
            // Top of the list are the best matches and as we iterate through the ArrayList, the least probable matches are stored
            // If we have phrases we need to check each phrase with the one we want
            // Also we need to have one general letter case so we don't have match problems.
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            // If the message spoken from user is a float, we update the speed limit
            try{
                Float.parseFloat(matches.get(0)); // Try to convert message into float

                // If it is float, edit shared preferences ( speed limit )
                SharedPreferences.Editor editor = preferences.edit();
                editor.putFloat("Speed_Limit", Float.parseFloat(matches.get(0)));
                editor.apply();

                // Update speed limit textview
                speedLimitTextView.setText("Speed Limit: "+String.valueOf(preferences.getFloat("Speed_Limit", 0)));
                myTts.speak("Speed limit updated.");

            }catch(NumberFormatException e){
                myTts.speak("That is not a float, try again.");
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordSaved = false;

        myTts = new MyTts(this); // We put it onCreate so we give some time to the initListener to get ready

        // Get shared preferences of application, must be put in every activity to share
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE); // Get service required for gps

        // Create database or open it if it's already created
        db = openOrCreateDatabase("RecordsDB", Context.MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Records(Longitude TEXT, Latitude TEXT,Speed TEXT, Timestamp TEXT)");


        mainButton = findViewById(R.id.button); // Start/Stop button for tachometer
        mainButton.setOnClickListener((view) -> {

            // In order to collect gps data we need permission of user so we can access their location
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION} ,
                        123);
                return; // Permission denied
            }
            // Permission granted
            // Start requesting location updates/data
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

            // On button click change button's text from start to stop or stop to start accordingly
            if(mainButton.getText().toString().equals("Stop")) {
                mainButton.setText("Start");
                tachometerTextView.setText(null); // Reset tachometerTextView's text
                locationManager.removeUpdates(this); // Stop gps updates if user stops the tachometer
            }
            else
                mainButton.setText("Stop");

        });


        recordsButton = findViewById(R.id.button2); // Button used to move to activity 2 where we can see records of times the speed limit was broken
        // Press Records button to go to Activity2
        recordsButton.setOnClickListener((view) -> startActivity(new Intent(MainActivity.this, MainActivity2.class)));

        changeLimitButton = findViewById(R.id.button7); // Button used to start voice recognition and change speed limit
        // On click start listening for user voice. open -> recognise -> return data
        changeLimitButton.setOnClickListener((view) -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); // Start new intent
            // We prefer the extra model and the free form of speech
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Give new speed limit."); // Ask user for new speed limit
            startActivityForResult(intent, REC_RESULT); // We use ForResult because we need to wait fo a result to be returned
            // REC_RESULT is used so we know which return we are talking about
        });

        
        tachometerTextView = findViewById(R.id.textView); // TextView used to display current speed

        speedLimitTextView = findViewById(R.id.textView3); // TextView used to display speed limit
        speedLimitTextView.setText("Speed Limit: "+String.valueOf(preferences.getFloat("Speed_Limit", 0)));

    }

    // Imported when we our class implemented interface LocationListener, gets called every time our location is changed
    @Override
    public void onLocationChanged(@NonNull Location location) {

        // After permission is granted and we started the location updates, we get the information we need with each update
        double x = location.getLatitude();
        double y = location.getLongitude();
        long timestamp = location.getTime();
        float speed = location.getSpeed();

        // Start displaying current speed always (as long as we get updates)
        tachometerTextView.setText(String.valueOf(speed));

        // If user surpasses the given speed limit
        if(speed > preferences.getFloat("Speed_Limit", 0) && !recordSaved) {
            // Add data/record to Records table
            db.execSQL("INSERT INTO Records(Longitude, Latitude, Speed, Timestamp) " +
                    "VALUES('" + String.valueOf(y) + "','" + String.valueOf(x) + "','" + String.valueOf(speed) + "','" + String.valueOf(timestamp) + "')");

            // Change MainActivity background color
            getWindow().getDecorView().setBackgroundColor(Color.RED);

            // Speak a warning message for the user that they have broken the limit
            myTts.speak("Warning! Speed limit has been broken.");

            // We saved the limit breaking record, so we want to wait until the user breaks it again at a later date
            // And NOT keep saving the same record over and over again
            recordSaved = true;
        }
        else if(speed <= preferences.getFloat("Speed_Limit", 0) && recordSaved) { // Else if the user is under the speed limit, we change back the background color
            getWindow().getDecorView().setBackgroundColor(Color.WHITE);
            // When user is moving in an acceptable speed, we assume that everything is normal
            // And the limit breaking record is saved and done. The next time the user
            // breaks the speed limit, we will record it as a new record.
            recordSaved = false;
        }
    }
}
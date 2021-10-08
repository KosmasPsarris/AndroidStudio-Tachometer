package com.p17191.ergasies.exercise1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity2 extends AppCompatActivity {

    MyTts myTts; // For TextToSpeech
    Button backButton, clearButton, dbSearchButton;
    SQLiteDatabase db; // For our database

    // When data is returned from speech recognition, this method is called. Used to handle the data
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == MainActivity.REC_RESULT && resultCode == RESULT_OK){ // requestCode = our REC_RESULT | resultCode = if everything went okay ( user spoke and machine recognised the message)
            // Data returned from voice recognition is basically matches between what the user said and what the machine thinks the user said
            // Top of the list are the best matches and as we iterate through the ArrayList, the least probable matches are stored
            // If we have phrases we need to check each phrase with the one we want
            // Also we need to have one general letter case so we don't have match problems.
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            // Check if user gave a correct type of records
            if(matches.get(0).toLowerCase().equals("all")){
                myTts.speak("Showing all records.");

                // Show all records
                Cursor cursor = db.rawQuery("SELECT * FROM Records", null);
                if(cursor.getCount() > 0){ // If there is at least one record in records table

                    StringBuilder builder = new StringBuilder(); // To handle string manipulation in loop
                    while(cursor.moveToNext()){ // While there is data in table
                        builder.append("Longitude: "+ cursor.getString(0)+"\n");
                        builder.append("Latitude: "+ cursor.getString(1)+"\n");
                        builder.append("Speed: "+ cursor.getString(2)+"\n");
                        builder.append("Timestamp: "+ cursor.getString(3)+"\n");
                        builder.append("-------------- \n");
                    }
                    showMessage("All Records:", builder.toString()); // Use helper function to message the result to user
                    cursor.close();
                }
            }
            else if(matches.get(0).toLowerCase().equals("sort by speed")){
                myTts.speak("Showing records sorted by speed.");

                // Show all records sorted by speed
                Cursor cursor = db.rawQuery("SELECT * FROM Records ORDER BY Speed DESC", null);
                if(cursor.getCount() > 0){ // If there is at least one record in records table

                    StringBuilder builder = new StringBuilder(); // To handle string manipulation in loop
                    while(cursor.moveToNext()){ // While there is data in table

                        builder.append("Longitude: " + cursor.getString(0) + "\n");
                        builder.append("Latitude: " + cursor.getString(1) + "\n");
                        builder.append("Speed: " + cursor.getString(2) + "\n");
                        builder.append("Timestamp: " + cursor.getString(3) + "\n");
                        builder.append("-------------- \n");
                    }
                    showMessage("Records Sorted By Speed:", builder.toString()); // Use helper function to message the result to user
                    cursor.close();
                }
            }
            else if(matches.get(0).toLowerCase().equals("most recent")){
                myTts.speak("Showing most recent record.");

                // Show all records sorted by speed
                Cursor cursor = db.rawQuery("SELECT * FROM Records", null);
                if(cursor.getCount() > 0){ // If there is at least one record in records table

                    StringBuilder builder = new StringBuilder(); // To handle string manipulation in loop
                    cursor.moveToLast(); // Go to the last record ( most recent one)

                    builder.append("Longitude: " + cursor.getString(0) + "\n");
                    builder.append("Latitude: " + cursor.getString(1) + "\n");
                    builder.append("Speed: " + cursor.getString(2) + "\n");
                    builder.append("Timestamp: " + cursor.getString(3) + "\n");

                    showMessage("Most Recent Record", builder.toString()); // Use helper function to message the result to user
                    cursor.close();
                }
            }
            else if (matches.get(0).toLowerCase().equals("last week")){
                myTts.speak("Showing last week records.");

                // Get current week number
                LocalDate date1 = LocalDate.now();
                WeekFields weekFields1 = WeekFields.of(Locale.getDefault());
                int currentWeek = date1.get(weekFields1.weekOfWeekBasedYear());

                Cursor cursor = db.rawQuery("SELECT * FROM Records", null);
                if(cursor.getCount() > 0){ // If there is at least one record in records table

                    StringBuilder builder = new StringBuilder(); // To handle string manipulation in loop
                    while(cursor.moveToNext()) { // While there is data in table

                        // Get week number from timestamp
                        // Convert long timestamp to LoCalDateTime
                        LocalDateTime triggerTime =
                                LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(cursor.getString(3))),
                                        TimeZone.getDefault().toZoneId());

                        // Convert LocalDateTime to LocalDate and find week number
                        LocalDate date2 = triggerTime.toLocalDate();
                        WeekFields weekFields2 = WeekFields.of(Locale.getDefault());
                        int weekFromTimestamp = date2.get(weekFields2.weekOfWeekBasedYear());

                        // If the record was added to the database (speed limit broken) this week, we want to display it
                        if (weekFromTimestamp == currentWeek) {

                            builder.append("Longitude: " + cursor.getString(0) + "\n");
                            builder.append("Latitude: " + cursor.getString(1) + "\n");
                            builder.append("Speed: " + cursor.getString(2) + "\n");
                            builder.append("Timestamp: " + cursor.getString(3) + "\n");
                            builder.append("-------------- \n");
                        }
                    }
                    showMessage("Last Week Records:", builder.toString()); // Use helper function to message the result to user
                    cursor.close();
                }
            }
            else{
                myTts.speak("Wrong type of records.");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        myTts = new MyTts(this); // We put it onCreate so we give some time to the initListener to get ready

        // Create database or open it if it's already created
        db = openOrCreateDatabase("RecordsDB", Context.MODE_PRIVATE,null);

        backButton = findViewById(R.id.button3); // Get back to main Activity(1)
        // Press Back button to go to Activity1
        backButton.setOnClickListener((view) -> startActivity(new Intent(MainActivity2.this, MainActivity.class)));

        dbSearchButton = findViewById(R.id.button4); // Search for records on database based on verbal command of user
        // On click start listening for user voice. open -> recognise -> return data
        dbSearchButton.setOnClickListener((view) -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); // Start new intent
            // We prefer the extra model and the free form of speech
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What type of records? " +
                    "(all, last week, most recent, sort by speed)"); // Ask user for the type of records to be displayed
            startActivityForResult(intent, MainActivity.REC_RESULT); // We use ForResult because we need to wait fo a result to be returned
            // REC_RESULT is used so we know which return we are talking about
        });


        clearButton = findViewById(R.id.button6); // Clear all records
        // Clear all data from table Records
        clearButton.setOnClickListener((view)->{
            db.execSQL("DELETE FROM Records");
            Toast.makeText(getApplicationContext(),"Records deleted.",Toast.LENGTH_SHORT).show();
        });

    }

    // Helper function to show messages/records
    public void showMessage(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this) ;
        builder.setCancelable(true).setTitle(title).setMessage(message).show();
    }
}
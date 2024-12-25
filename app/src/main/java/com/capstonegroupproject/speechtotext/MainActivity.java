package com.capstonegroupproject.speechtotext;

import static com.capstonegroupproject.speechtotext.WishMeFunction.wishMe;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.Button;

import android.Manifest;
import android.content.pm.PackageManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.io.File;
import java.io.FileOutputStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.bumptech.glide.Glide;

import androidx.appcompat.app.AlertDialog;

import android.media.AudioManager;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private SpeechRecognizer recognizer;
    private TextView textView;
    private TextToSpeech tts;
    private boolean isTTSInitialized = false;
    private MediaPlayer mediaPlayer;
    private ImageView currentLetterImage;
    private static final int TTS_ENGINE_REQUEST = 101;
    private static final int PERMISSION_REQUEST_CODE = 102;
    private int currentPage = 0;
    private final int LETTERS_PER_PAGE = 5;
    
    // Add this alphabet array
    private final String[] alphabet = {
        "A for Apple",
        "B for Ball",
        "C for Cat",
        "D for Dog",
        "E for Elephant",
        "F for Fish",
        "G for Giraffe",
        "H for Horse",
        "I for Ice cream",
        "J for Jellyfish",
        "K for Kite",
        "L for Lion",
        "M for Monkey",
        "N for Nest",
        "O for Orange",
        "P for Penguin",
        "Q for Queen",
        "R for Rabbit",
        "S for Snake",
        "T for Tiger",
        "U for Umbrella",
        "V for Violin",
        "W for Whale",
        "X for X-ray",
        "Y for Yacht",
        "Z for Zebra"
    };

    private UnsplashApiService unsplashApi;
    private static final String TAG = "MainActivity"; // For logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Retrofit and UnsplashApiService
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.unsplash.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        unsplashApi = retrofit.create(UnsplashApiService.class);

        // Initialize SpeechRecognizer
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        
        // Check for permissions first
        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            };

            ArrayList<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                requestPermissions(
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
                );
            } else {
                // Permissions already granted, proceed with TTS check
                checkTTSAvailability();
            }
        } else {
            // For devices below Android M, proceed with TTS check
            checkTTSAvailability();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // All permissions granted, proceed with TTS check
                checkTTSAvailability();
            } else {
                showError("Required permissions not granted. App may not work properly.");
                // You might want to show a dialog explaining why permissions are needed
                showPermissionExplanationDialog();
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs audio and internet permissions to function properly. Please grant the permissions in Settings.")
            .setPositiveButton("Open Settings", (dialog, which) -> {
                // Open app settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
                showError("App will have limited functionality without permissions");
            })
            .setCancelable(false)
            .show();
    }

    private void checkTTSAvailability() {
        Intent checkTTS = new Intent();
        checkTTS.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        try {
            startActivityForResult(checkTTS, TTS_ENGINE_REQUEST);
        } catch (Exception e) {
            showError("Error checking TTS availability");
            Log.e("TTS", "Error checking TTS", e);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || 
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                showError("Language not supported");
                Log.e("TTS", "Language not supported");
            } else {
                isTTSInitialized = true;
            }
        } else {
            showError("Text-to-Speech initialization failed");
            Log.e("TTS", "TTS Initialization failed");
        }
    }

    private void speak(String text) {
        if (!isTTSInitialized) {
            showError("Text-to-Speech not initialized");
            return;
        }

        try {
            int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            if (result == TextToSpeech.ERROR) {
                showError("Error speaking text");
                Log.e("TTS", "Error speaking text: " + text);
            }
        } catch (Exception e) {
            showError("Error in Text-to-Speech");
            Log.e("TTS", "Error in TTS speak", e);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void findById() {
        textView = findViewById(R.id.textView);
        currentLetterImage = findViewById(R.id.currentLetterImage);
    }

    private void result() {
        if (recognizer == null) {
            Log.e("MainActivity", "SpeechRecognizer is null");
            return;
        }

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Log.d("MainActivity", "onReadyForSpeech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("MainActivity", "onBeginningOfSpeech");
            }

            @Override
            public void onRmsChanged(float v) {
                // Optional: Log volume changes
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
                Log.d("MainActivity", "onBufferReceived");
            }

            @Override
            public void onEndOfSpeech() {
                Log.d("MainActivity", "onEndOfSpeech");
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorText(error);
                Log.e("MainActivity", "onError: " + errorMessage);
                if (textView != null) {
                    textView.setText(errorMessage);
                }
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    if (textView != null) {
                        textView.setText(text);
                    }
                    // Process the recognized text
                    response(text);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                Log.d("MainActivity", "onPartialResults");
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                Log.d("MainActivity", "onEvent");
            }
        });
    }

    // Helper method to convert error codes to messages
    private String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match found";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "Server error";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Unknown error occurred";
                break;
        }
        return message;
    }

    public void response(String message){
        String messages = message.toLowerCase(Locale.ROOT);
        if(messages.indexOf("hi")!=-1){
            speak("Hello, How can I help you today?");
        }
        if(messages.indexOf("time")!=-1){
            Date date = new Date();
            String time = DateUtils.formatDateTime(this,date.getTime(),DateUtils.FORMAT_SHOW_TIME);
            speak(time);
        }
        if(messages.indexOf("date")!=-1){
            SimpleDateFormat date = new SimpleDateFormat("dd,mm,yyyy");
            Calendar calendar = Calendar.getInstance();
            String todaysDate = date.format(calendar.getTime());
            speak("The date is "+ todaysDate);
        }
        if(messages.indexOf("google")!=-1){
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
            startActivity(intent);
        }
        if(messages.indexOf("youtube")!=-1){
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"));
            startActivity(intent);
        }
        if(messages.indexOf("search")!=-1){
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse
                    ("https://www.google.com/search?q="+messages.replace("search"," ")));
            startActivity(intent);
        }
        if(messages.indexOf("remember!")!=-1){
            speak("Noted. I will reminder you that");
            writeToFile(messages.replace("do you remember anything", " "));
        }
        if(messages.indexOf("know")!=-1){
            String dataFromFile = readFromFile();
            speak("Reminder! "+ dataFromFile);
        }
        if(messages.indexOf("play")!=-1){
            playMusic();
        }
        if(messages.indexOf("pause")!=-1){
            pauseMusic();
        }
        if(messages.indexOf("stop")!=-1){
            stopMusic();
        }
        if (messages.indexOf("teach") != -1 && messages.length() > 6) {
            // Extract the letter after "teach" (assuming format like "teach a")
            String letter = messages.substring(messages.indexOf("teach") + 6, 
                                            messages.indexOf("teach") + 7);
            teachLetter(letter);
        }
    }

    private void stopMusic() {
        stopPlayer();
    }

    private void pauseMusic() {
        if(mediaPlayer != null){
            mediaPlayer.pause();
        }
    }

    private void playMusic() {
        if(mediaPlayer == null){
            mediaPlayer = MediaPlayer.create(this, R.raw.song);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopPlayer();
                }
            });
        }
        mediaPlayer.start();
    }

    private void stopPlayer() {
        if(mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
            Toast.makeText(this,"MediaPlayer Released",Toast.LENGTH_SHORT).show();
        }
    }

    private String readFromFile() {
        String ret = "";
        try{
            InputStream inputStream = openFileInput("data.txt");
            if(inputStream!=null){
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiver = "";
                StringBuilder stringBuilder = new StringBuilder();

                while((receiver=bufferedReader.readLine())!=null){
                    stringBuilder.append("\n").append(receiver);
                }
                inputStream.close();
                ret = stringBuilder.toString();
            }

        }catch (IOException e){
            Log.e("Exception", "File not found: " + e.toString());
        }
        return ret;
    }

    private void writeToFile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                    openFileOutput("output.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write Failed: " + e.toString());
            e.printStackTrace();
        }
    }

    public void startRecording(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        recognizer.startListening(intent);
    }

    private void teachLetter(String letter) {
        letter = letter.toUpperCase();
        for (String alphabetEntry : alphabet) {
            if (alphabetEntry.startsWith(letter)) {
                String word = alphabetEntry.split(" for ")[1];
                loadImageForLetter(letter, word);
                speak(letter + " for " + word);  // Use Android TTS
                break;
            }
        }
    }

    // Add this method to handle the ABC button click
    public void startAlphabet(View view) {
        // Start with 'A'
        teachLetter("A");
    }

    public void onLetterClick(View view) {
        Button button = (Button) view;
        String letter = button.getText().toString();
        speak(letter);
    }

    public void onNextPageClick(View view) {
        currentPage++;
        updateLetterButtons();
        
        // Reset to first page if we're past Z
        if (currentPage * LETTERS_PER_PAGE >= alphabet.length) {
            currentPage = 0;
        }
    }

    private void updateLetterButtons() {
        int startIndex = currentPage * LETTERS_PER_PAGE;
        Button[] buttons = new Button[]{
            findViewById(R.id.buttonA),
            findViewById(R.id.buttonB),
            findViewById(R.id.buttonC),
            findViewById(R.id.buttonD),
            findViewById(R.id.buttonE)
        };

        for (int i = 0; i < buttons.length; i++) {
            int letterIndex = startIndex + i;
            if (letterIndex < alphabet.length) {
                buttons[i].setText(alphabet[letterIndex].substring(0, 1));
                buttons[i].setVisibility(View.VISIBLE);
            } else {
                buttons[i].setVisibility(View.INVISIBLE);
            }
        }
    }

    private void loadImageForLetter(String letter, String word) {
        String query = word + " for children";
        Call<UnsplashSearchResponse> call = unsplashApi.searchPhotos(query, 1);
        
        call.enqueue(new Callback<UnsplashSearchResponse>() {
            @Override
            public void onResponse(Call<UnsplashSearchResponse> call, 
                                 retrofit2.Response<UnsplashSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null && 
                    !response.body().results.isEmpty()) {
                    String imageUrl = response.body().results.get(0).urls.regular;
                    runOnUiThread(() -> {
                        Glide.with(MainActivity.this)
                            .load(imageUrl)
                            .centerCrop()
                            .placeholder(R.drawable.ic_launcher_background) // Add a placeholder image
                            .error(R.drawable.ic_launcher_background) // Add an error image
                            .into(currentLetterImage);
                    });
                }
            }

            @Override
            public void onFailure(Call<UnsplashSearchResponse> call, Throwable t) {
                Log.e("Unsplash", "Error loading image: " + t.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, 
                        "Failed to load image", 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception e) {
                Log.e("TTS", "Error shutting down TTS", e);
            }
        }
        if (recognizer != null) {
            recognizer.destroy();
        }
        super.onDestroy();
    }
}
package com.capstonegroupproject.speechtotext;

import static com.capstonegroupproject.speechtotext.WishMeFunction.wishMe;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

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

public class MainActivity extends AppCompatActivity {
    private SpeechRecognizer recognizer;
    private TextView textView;
    private TextToSpeech textToSpeech;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Dexter.withContext(this)
                .withPermission(Manifest.permission.RECORD_AUDIO)
                .withListener(new PermissionListener() {
                    @Override public void onPermissionGranted(PermissionGrantedResponse response) {/* ... */}
                    @Override public void onPermissionDenied(PermissionDeniedResponse response) {/* ... */}
                    @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {/* ... */}
                }).check();

        initTextToSpeech();
        findById();
        result();
    }

    private void initTextToSpeech(){
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(textToSpeech.getEngines().size()==0){
                    Toast.makeText(MainActivity.this, "Engine is not Available", Toast.LENGTH_SHORT).show();
                }
                else{
                    String s = wishMe();
                    speak(s + "This is your AI virtual assistant how can I help you");
                }

            }
        });

    }


    private void speak(String message) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void findById() {
        textView = (TextView)findViewById(R.id.textView);
    }

    private void result() {
        if(SpeechRecognizer.isRecognitionAvailable(this)){
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {

                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float v) {

                }

                @Override
                public void onBufferReceived(byte[] bytes) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                @Override
                public void onError(int i) {

                }

                @Override
                public void onResults(Bundle bundle) {
                    ArrayList<String> result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    Toast.makeText(MainActivity.this, ""+result.get(0), Toast.LENGTH_SHORT).show();
                    textView.setText(result.get(0));
                    response(result.get(0));

                }

                @Override
                public void onPartialResults(Bundle bundle) {

                }

                @Override
                public void onEvent(int i, Bundle bundle) {

                }
            });
        }
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

    private void writeToFile(String data){


        try {
            OutputStreamWriter outputStreamWriter =  outputStreamWriter = new OutputStreamWriter(
                    openFileOutput("output.txt", Context.MODE_PRIVATE));
            BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
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
}
package com.example.chatbot;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.chatbot.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private int tapCount = 0;
    private final long DOUBLE_TAP_TIMEOUT = 500L; // 500 milliseconds for tap count reset
    private final Handler tapHandler = new Handler();
    private final Runnable resetTapCountRunnable = () -> tapCount = 0;

    private SupabaseService supabaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setLogo(R.drawable.ic_logo); // Replace with your logo resource
            getSupportActionBar().setDisplayUseLogoEnabled(true);
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            } else {
                Toast.makeText(MainActivity.this, "TextToSpeech Initialization Failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize Retrofit and SupabaseService
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://zluaskkhmcpllmcumgnl.supabase.co/rest/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        supabaseService = retrofit.create(SupabaseService.class);

        // Set touch listener to count taps
        binding.getRoot().setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                tapCount++;
                tapHandler.removeCallbacks(resetTapCountRunnable);
                tapHandler.postDelayed(resetTapCountRunnable, DOUBLE_TAP_TIMEOUT);

                if (tapCount == 3) {
                    startVoiceRecognition();
                    tapCount = 0; // Reset tap count after triggering voice recognition
                }
            }
            return true;
        });

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(MainActivity.this, "Listening...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                Toast.makeText(MainActivity.this, "Processing...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int error) {
                Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0);
                    processVoiceCommand(command);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something...");
        speechRecognizer.startListening(intent);
    }

    private void processVoiceCommand(String command) {
        if (command.toLowerCase(Locale.getDefault()).contains("bank balance")) {
            fetchBankBalance();
        } else if (command.toLowerCase(Locale.getDefault()).contains("transaction history")) {
            fetchTransactionHistory();
        } else {
            String response = "Command not recognized";
            textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void fetchBankBalance() {
        Call<List<YourDataModel>> call = supabaseService.getData();
        call.enqueue(new Callback<List<YourDataModel>>() {
            @Override
            public void onResponse(Call<List<YourDataModel>> call, Response<List<YourDataModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<YourDataModel> data = response.body();
                    String responseText = "Your bank balance is $" + data.get(0).getValue(); // Adjust based on your data structure
                    textToSpeech.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    textToSpeech.speak("Failed to fetch bank balance", TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }

            @Override
            public void onFailure(Call<List<YourDataModel>> call, Throwable t) {
                textToSpeech.speak("Error: " + t.getMessage(), TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    private void fetchTransactionHistory() {
        Call<List<TransactionDataModel>> call = supabaseService.getTransactionData();
        call.enqueue(new Callback<List<TransactionDataModel>>() {
            @Override
            public void onResponse(Call<List<TransactionDataModel>> call, Response<List<TransactionDataModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TransactionDataModel> data = response.body();
                    StringBuilder responseText = new StringBuilder("Your recent transactions are: ");
                    for (TransactionDataModel transaction : data) {
                        responseText.append("\n")
                                .append(transaction.getDate())
                                .append(": $")
                                .append(transaction.getAmount())
                                .append(" - ")
                                .append(transaction.getDescription());
                    }
                    textToSpeech.speak(responseText.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    textToSpeech.speak("Failed to fetch transaction history", TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }

            @Override
            public void onFailure(Call<List<TransactionDataModel>> call, Throwable t) {
                textToSpeech.speak("Error: " + t.getMessage(), TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}

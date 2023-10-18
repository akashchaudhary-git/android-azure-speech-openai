//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//
// <code>
package com.microsoft.cognitiveservices.speech.samples.quickstart;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.azure.android.core.http.HttpCallback;
import com.azure.android.core.http.HttpClient;
import com.azure.android.core.http.HttpMethod;
import com.azure.android.core.http.HttpRequest;
import com.azure.android.core.http.HttpResponse;
import com.azure.android.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.azure.android.core.util.CancellationToken;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;

import java.util.concurrent.Future;


import static android.Manifest.permission.*;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String AZURE_OPENAI_KEY = "Enter Your Key";
    private static final String speechSubscriptionKey = "Enter Your Speech Key";
    private static final String serviceRegion = "Enter speech service region";



    TextView bot;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Note: we need to request the permissions
        int requestCode = 5; // unique code for the permission request
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, requestCode);
        bot = (TextView) this.findViewById(R.id.openai_text);
    }

    public void onSpeechButtonClicked(View v) {
        TextView prompt = (TextView) this.findViewById(R.id.user_text); // 'user_text' is the ID of your text view

        String bot_response;

        try (SpeechConfig config = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
             SpeechRecognizer reco = new SpeechRecognizer(config);) {

            Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();

            // Note: this will block the UI thread, so eventually, you want to
            //       register for the event (see full samples)
            SpeechRecognitionResult result = task.get();

            if (result.getReason() == ResultReason.RecognizedSpeech) {
                prompt.setText(result.getText());
                makeAPIRequest(result.getText());

            }
            else {
                prompt.setText("Error recognizing. Did you update the subscription info?" + System.lineSeparator() + result.toString());
            }

        } catch (Exception ex) {
            Log.e("SpeechSDKDemo", "unexpected " + ex.getMessage());
//            assert(false);
//            }
        }

    }

    private void makeAPIRequest(String prompt) {

        HttpClient httpClient = new OkHttpAsyncHttpClientBuilder().build();

        String url = "Enter your endpoint here";

        String requestBody = "{\n" +
                "  \"messages\": [{\"role\":\"system\",\"content\":\"You are an AI assistant that helps people find information.\"}," +
                "{\"role\":\"user\",\"content\":\""+prompt+"\"}],\n" +
                "  \"max_tokens\": 800,\n" +
                "  \"temperature\": 0.7,\n" +
                "  \"frequency_penalty\": 0,\n" +
                "  \"presence_penalty\": 0,\n" +
                "  \"top_p\": 0.95,\n" +
                "  \"stop\": null\n" +
                "}";

        HttpRequest httpRequest = new HttpRequest(HttpMethod.POST, url);
        httpRequest.setHeader("Content-Type", "application/json");
        httpRequest.setHeader("api-key", AZURE_OPENAI_KEY);
        httpRequest.setBody(requestBody.getBytes());

        httpClient.send(httpRequest, CancellationToken.NONE, new HttpCallback() {
            @Override
            public void onSuccess(HttpResponse response) {
                try {
                    String responseBody = response.getBodyAsString();
                    Log.d(TAG, "Response: " + responseBody);

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    // Extract the necessary object
                    JSONObject message = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message");

                    String role = message.getString("role");
                    String content = message.getString("content");

                    runOnUiThread(() -> bot.setText(content));
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException: " + e.getMessage());
                    runOnUiThread(() -> bot.setText("An error occurred while processing the response."));
                }
            }

            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "Error: " + error.getMessage());
                runOnUiThread(() -> bot.setText("An error occurred: " + error.getMessage()));
            }
        });
    }

    private void setTextToTextView(String content) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bot.setText(content);
            }
        });
    }
}
// </code>

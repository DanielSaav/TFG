package com.example.tfg;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class SilencioActivity extends AppCompatActivity {

    private TextView countdownText;
    private Button startButton;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 0;
    private boolean timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_silencio);

        countdownText = findViewById(R.id.countdownText);
        startButton = findViewById(R.id.button);


        startButton.setOnClickListener(v -> {
            if (!timerRunning) {
                showTimePickerDialog();
            } else {
                cancelTimer();
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "CanalSilencio";
            String description = "Notificaciones del modo silencio";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("canal_silencio", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }

    private void showTimePickerDialog() {
        final String[] times = new String[24];
        final long[] minutes = new long[24];

        for (int i = 0; i < 24; i++) {
            long currentMinutes = (i + 1) * 5;
            minutes[i] = currentMinutes;

            if (currentMinutes < 60) {
                times[i] = currentMinutes + " minutos";
            } else {
                long hours = currentMinutes / 60;
                long remainingMinutes = currentMinutes % 60;
                if (remainingMinutes == 0) {
                    times[i] = hours + (hours == 1 ? " hora" : " horas");
                } else {
                    times[i] = hours + " h " + remainingMinutes + " min";
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Elige el tiempo (de 5 en 5 minutos)");
        builder.setItems(times, (dialog, which) -> {
            long selectedMinutes = minutes[which];
            startCountdown(selectedMinutes * 60 * 1000);
        });
        builder.show();
    }

    private void startCountdown(long millisInFuture) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        timerRunning = true;
        startButton.setText("Cancelar");

        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                countdownText.setText("¡Tiempo terminado!");
                startButton.setText("Comenzar");
            }
        }.start();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        countdownText.setText("00:00");
        startButton.setText("Comenzar");

        // Mostrar diálogo de cancelación
        new AlertDialog.Builder(this)
                .setTitle(":(")
                .setMessage("No completaste el tiempo establecido")
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private void updateCountdownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);
        countdownText.setText(timeFormatted);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (timerRunning) {
            showExitNotification();
        }
    }

    private void showExitNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_silencio")
                .setSmallIcon(R.drawable.logosilencioscuro) // Usa un icono válido en tu drawable
                .setContentTitle("Temporizador en pausa")
                .setContentText("Saliste antes de que terminara el tiempo.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }


}
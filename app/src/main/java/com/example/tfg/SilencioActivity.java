package com.example.tfg;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SilencioActivity extends AppCompatActivity {

    private TextView countdownText;
    private Button startButton;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 0;
    private long tiempoSeleccionado = 0; // <-- Tiempo total elegido por el usuario
    private boolean timerRunning = false;
    private ImageView salir;
    private boolean cancelledExternally = false;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_silencio);

        countdownText = findViewById(R.id.countdownText);
        startButton = findViewById(R.id.button);
        salir = findViewById(R.id.imageView3);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

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

        salir.setOnClickListener(v -> {
            if (timerRunning) {
                Toast.makeText(this, "No puedes salir cuando está en marcha el cronómetro", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, HomeActivity.class));
            }
        });
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
        builder.setTitle("Elige el tiempo");
        builder.setItems(times, (dialog, which) -> {
            tiempoSeleccionado = minutes[which]; // <-- Guardamos tiempo elegido
            startCountdown(tiempoSeleccionado * 60 * 1000);
        });
        builder.show();
    }

    private void startCountdown(long millisInFuture) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        timerRunning = true;
        cancelledExternally = false;
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
                sumarPuntosAFirebase(); // <-- Añadido
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

    private void sumarPuntosAFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        int puntosGanados = (int) ((tiempoSeleccionado / 5) * 3); // 3 puntos por cada 5 minutos

        db.collection("usuarios").document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long puntosActuales = documentSnapshot.getLong("puntos");
                int nuevosPuntos = (puntosActuales != null ? puntosActuales.intValue() : 0) + puntosGanados;

                db.collection("usuarios").document(user.getUid())
                        .update("puntos", nuevosPuntos)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "+" + puntosGanados + " puntos por completar el silencio", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Error al actualizar puntos", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (timerRunning) {
            cancelledExternally = true;
            cancelTimer();
            showExitNotification();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (cancelledExternally) {
            new AlertDialog.Builder(this)
                    .setTitle(":(")
                    .setMessage("No completaste el tiempo establecido")
                    .setPositiveButton("Aceptar", null)
                    .show();
            cancelledExternally = false;
        }
    }

    private void showExitNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_silencio")
                .setSmallIcon(R.drawable.logosilencioscuro)
                .setContentTitle("Temporizador cancelado")
                .setContentText("Saliste antes de que terminara el tiempo.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}

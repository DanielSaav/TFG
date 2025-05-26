package com.example.tfg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LogrosFragment extends Fragment {

    private TextView tareasCompletadasText;
    private TextView tareasPendientesText;
    private TextView tareasNoCompletadasYExcedidasText;
    private TextView fechaOrigenText;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logros, container, false);

        tareasCompletadasText = view.findViewById(R.id.textView5);
        tareasPendientesText = view.findViewById(R.id.textView6);
        tareasNoCompletadasYExcedidasText = view.findViewById(R.id.textView10);
        fechaOrigenText = view.findViewById(R.id.textView11);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            return view;
        }

        contarTareas();

        mostrarFechaCreacionCuenta();

        return view;
    }

    private void mostrarFechaCreacionCuenta() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getMetadata() != null) {
            long creationTimestamp = user.getMetadata().getCreationTimestamp();
            Date fechaCreacion = new Date(creationTimestamp);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String fechaFormateada = sdf.format(fechaCreacion);

            fechaOrigenText.setText("Fecha origen: " + fechaFormateada);
        } else {
            fechaOrigenText.setText("Fecha origen: desconocida");
        }
    }

    private void contarTareas() {
        String uid = mAuth.getCurrentUser().getUid();

        // Fecha actual como Timestamp
        Timestamp fechaActual = Timestamp.now();

        // Contar tareas completadas ("Sí")
        db.collection("usuarios")
                .document(uid)
                .collection("tareas")
                .whereEqualTo("completado", "Sí")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int completadas = queryDocumentSnapshots.size();
                    tareasCompletadasText.setText("Tareas Completas: " + completadas);
                });

        // Contar tareas pendientes ("No")
        db.collection("usuarios")
                .document(uid)
                .collection("tareas")
                .whereEqualTo("completado", "No")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int pendientes = queryDocumentSnapshots.size();
                    tareasPendientesText.setText("Tareas Pendientes: " + pendientes);
                });

        // Contar tareas no completadas que ya excedieron su fecha límite
        db.collection("usuarios")
                .document(uid)
                .collection("tareas")
                .whereEqualTo("completado", "No")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int excedidas = 0;
                    Timestamp ahora = Timestamp.now();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String fechaStr = doc.getString("fechaLimite"); // formato esperado: "dd/MM/yyyy"
                        String hora = doc.getString("horaLimite");       // formato esperado: "HH:mm"

                        if (fechaStr != null && hora != null) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                Date fechaDate = sdf.parse(fechaStr);

                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(fechaDate);

                                String[] partesHora = hora.split(":");
                                int horaNum = Integer.parseInt(partesHora[0]);
                                int minutoNum = Integer.parseInt(partesHora[1]);

                                calendar.set(Calendar.HOUR_OF_DAY, horaNum);
                                calendar.set(Calendar.MINUTE, minutoNum);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);

                                Timestamp fechaHoraCompleta = new Timestamp(calendar.getTime());

                                if (fechaHoraCompleta.compareTo(ahora) < 0) {
                                    excedidas++;
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    tareasNoCompletadasYExcedidasText.setText("Tareas No Completadas y Excedidas: " + excedidas);
                });

    }
}

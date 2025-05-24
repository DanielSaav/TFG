package com.example.tfg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class LogrosFragment extends Fragment {

    private TextView tareasCompletadasText;
    private TextView tareasPendientesText;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logros, container, false);

        tareasCompletadasText = view.findViewById(R.id.textView5);
        tareasPendientesText = view.findViewById(R.id.textView6);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            return view;
        }

        contarTareas();

        return view;
    }

    private void contarTareas() {
        String uid = mAuth.getCurrentUser().getUid();

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
    }
}
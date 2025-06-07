package com.example.tfg;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TareasListaFragment extends Fragment {

    public RecyclerView rv;
    public TareasAdapter adapter;
    private List<Tarea> listaTareas;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ImageView aniadirTarea;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FirebaseApp.initializeApp(requireContext());
        return inflater.inflate(R.layout.fragment_lista_tareas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Usar el mismo nombre de preferencias en toda la app
        sharedPreferences = requireActivity().getSharedPreferences("tareas_prefs", Context.MODE_PRIVATE);

        // Configurar RecyclerView
        rv = view.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        listaTareas = new ArrayList<>();
        adapter = new TareasAdapter(getContext(), listaTareas);
        rv.setAdapter(adapter);

        // Configurar botón de añadir tarea
        aniadirTarea = view.findViewById(R.id.aniadirTareaBoton);
        aniadirTarea.setOnClickListener(v -> mostrarDialogoNuevaTarea());

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "No hay usuario logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        cargarTareasDeFirestore();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            cargarTareasDeFirestore();
        }
    }

    protected void cargarTareasDeFirestore() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("usuarios")
                .document(uid)
                .collection("tareas")
                .get()
                .addOnSuccessListener(qsnap -> {
                    listaTareas.clear();
                    for (QueryDocumentSnapshot doc : qsnap) {
                        Tarea t = doc.toObject(Tarea.class);
                        t.setId(doc.getId());
                        listaTareas.add(t);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Error cargando tareas: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarDialogoNuevaTarea() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_nueva_tarea, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etTitulo = dialogView.findViewById(R.id.etTitulo);
        EditText etFecha = dialogView.findViewById(R.id.etFecha);
        EditText etHora = dialogView.findViewById(R.id.etHora);
        Button btnAceptar = dialogView.findViewById(R.id.btnAceptar);

        // Configurar selectores de fecha y hora
        etFecha.setOnClickListener(v -> mostrarSelectorFecha(etFecha));
        etHora.setOnClickListener(v -> mostrarSelectorHora(etHora));

        btnAceptar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            String fecha = etFecha.getText().toString().trim();
            String hora = etHora.getText().toString().trim();

            if (!validarCampos(titulo, fecha, hora) || !validarFechaHora(fecha, hora)) {
                return;
            }

            dialog.dismiss();
            guardarTareaEnFirestore(titulo, fecha, hora);
        });

        dialog.show();
    }

    private void mostrarSelectorFecha(EditText etFecha) {
        Calendar calendario = Calendar.getInstance();
        new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    String fecha = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    etFecha.setText(fecha);
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void mostrarSelectorHora(EditText etHora) {
        Calendar calendario = Calendar.getInstance();
        new TimePickerDialog(getContext(),
                (view, hourOfDay, minute) -> {
                    String hora = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    etHora.setText(hora);
                },
                calendario.get(Calendar.HOUR_OF_DAY),
                calendario.get(Calendar.MINUTE),
                true)
                .show();
    }

    private boolean validarCampos(String titulo, String fecha, String hora) {
        if (titulo.isEmpty() || fecha.isEmpty() || hora.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validarFechaHora(String fecha, String hora) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date fechaHoraTarea = sdf.parse(fecha + " " + hora);
            Date ahora = new Date();

            if (fechaHoraTarea.before(ahora)) {
                Toast.makeText(getContext(), "No puedes seleccionar una fecha/hora pasada", Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Formato de fecha/hora incorrecto", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void guardarTareaEnFirestore(String titulo, String fecha, String hora) {
        String uid = mAuth.getCurrentUser().getUid();
        Tarea nuevaTarea = new Tarea();
        nuevaTarea.setTitulo(titulo);
        nuevaTarea.setCorreoUsuario(mAuth.getCurrentUser().getEmail());
        nuevaTarea.setFechaLimite(fecha);
        nuevaTarea.setHoraLimite(hora);
        nuevaTarea.setCompletado("No");

        db.collection("usuarios")
                .document(uid)
                .collection("tareas")
                .add(nuevaTarea)
                .addOnSuccessListener(docRef -> {
                    nuevaTarea.setId(docRef.getId());
                    listaTareas.add(nuevaTarea);
                    adapter.notifyItemInserted(listaTareas.size() - 1);
                    Toast.makeText(getContext(), "Tarea añadida", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    public void actualizarListaTareas() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
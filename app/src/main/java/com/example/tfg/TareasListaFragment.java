package com.example.tfg;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

    private RecyclerView rv;
    private TareasAdapter adapter;
    private List<Tarea> listaTareas;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ImageView aniadirTarea;
    private ImageView eliminarTareasCompletadas;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FirebaseApp.initializeApp(requireContext());
        return inflater.inflate(R.layout.fragment_lista_tareas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv = view.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        listaTareas = new ArrayList<>();
        adapter = new TareasAdapter(getContext(), listaTareas);
        rv.setAdapter(adapter);

        aniadirTarea = view.findViewById(R.id.aniadirTareaBoton);
        eliminarTareasCompletadas = view.findViewById(R.id.eliminarTareasCompletadas);

        aniadirTarea.setOnClickListener(v -> mostrarDialogoNuevaTarea());
        eliminarTareasCompletadas.setOnClickListener(v -> mostrarDialogoConfirmacionEliminar());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "No hay usuario logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        cargarTareasDeFirestore();
    }

    private void mostrarDialogoConfirmacionEliminar() {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar eliminación")
                .setMessage("¿Quieres eliminar todas las tareas completadas de la lista?")
                .setPositiveButton("Sí", (dialog, which) -> eliminarTareasCompletadas())
                .setNegativeButton("No", null)
                .show();
    }

    private void eliminarTareasCompletadas() {
        List<Tarea> tareasAEliminar = new ArrayList<>();

        for (Tarea tarea : listaTareas) {
            if ("Sí".equalsIgnoreCase(tarea.getCompletado())) {
                tareasAEliminar.add(tarea);
            }
        }

        if (tareasAEliminar.isEmpty()) {
            Toast.makeText(getContext(), "No hay tareas completadas para ocultar", Toast.LENGTH_SHORT).show();
            return;
        }

        listaTareas.removeAll(tareasAEliminar);
        adapter.notifyDataSetChanged();

        Toast.makeText(getContext(),
                tareasAEliminar.size() + " tareas completadas ocultadas",
                Toast.LENGTH_SHORT).show();
    }

    private void cargarTareasDeFirestore() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("usuarios")
                .document(uid)
                .collection("tareas")
                .whereEqualTo("completado", "No")
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
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_nueva_tarea, null);

        EditText etTitulo = dialogView.findViewById(R.id.etTitulo);
        EditText etFecha = dialogView.findViewById(R.id.etFecha);
        EditText etHora = dialogView.findViewById(R.id.etHora);
        Button btnAceptar = dialogView.findViewById(R.id.btnAceptar);

        etFecha.setFocusable(false);
        etHora.setFocusable(false);

        etFecha.setOnClickListener(v -> {
            Calendar calendario = Calendar.getInstance();
            int anio = calendario.get(Calendar.YEAR);
            int mes = calendario.get(Calendar.MONTH);
            int dia = calendario.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        String fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        etFecha.setText(fechaSeleccionada);
                    },
                    anio, mes, dia
            ).show();
        });

        etHora.setOnClickListener(v -> {
            Calendar calendario = Calendar.getInstance();
            int hora = calendario.get(Calendar.HOUR_OF_DAY);
            int minuto = calendario.get(Calendar.MINUTE);

            new TimePickerDialog(
                    getContext(),
                    (view, hourOfDay, minute) -> {
                        String horaSeleccionada = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        etHora.setText(horaSeleccionada);
                    },
                    hora, minuto, true
            ).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Nueva tarea")
                .setView(dialogView)
                .create();

        btnAceptar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            String fecha = etFecha.getText().toString().trim();
            String hora = etHora.getText().toString().trim();

            if (titulo.isEmpty() || fecha.isEmpty() || hora.isEmpty()) {
                Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!esFechaValida(fecha)) {
                Toast.makeText(getContext(), "Formato de fecha incorrecto (dd/MM/yyyy)", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!esHoraValida(hora)) {
                Toast.makeText(getContext(), "Formato de hora incorrecto (HH:mm)", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!esFechaHoraValida(fecha, hora)) {
                Toast.makeText(getContext(), "No puedes poner una fecha y hora anterior a la actual", Toast.LENGTH_LONG).show();
                return;
            }

            dialog.dismiss();
            guardarTareaEnFirestore(titulo, fecha, hora);
        });

        dialog.show();
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
                    Toast.makeText(getContext(), "Tarea añadida correctamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Error guardando tarea: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean esFechaValida(String fecha) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false);
        try {
            sdf.parse(fecha);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean esHoraValida(String hora) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setLenient(false);
        try {
            sdf.parse(hora);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean esFechaHoraValida(String fecha, String hora) {
        SimpleDateFormat sdfFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat sdfCompleto = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        sdfFecha.setLenient(false);
        sdfHora.setLenient(false);
        sdfCompleto.setLenient(false);

        try {
            Date fechaSeleccionada = sdfFecha.parse(fecha);
            Date horaSeleccionada = sdfHora.parse(hora);
            Date fechaHoraSeleccionada = sdfCompleto.parse(fecha + " " + hora);
            Date ahora = new Date();

            // Comparar fechas sin tener en cuenta la hora
            String fechaHoyStr = sdfFecha.format(ahora);
            String fechaSeleccionadaStr = sdfFecha.format(fechaSeleccionada);

            if (fechaSeleccionadaStr.equals(fechaHoyStr)) {
                // Si es hoy, la hora debe ser mayor a la actual
                String horaActualStr = sdfHora.format(ahora);
                Date horaActual = sdfHora.parse(horaActualStr);
                return horaSeleccionada != null && horaSeleccionada.after(horaActual);
            } else {
                // Si es otro día, solo tiene que ser en el futuro
                return fechaHoraSeleccionada != null && fechaHoraSeleccionada.after(ahora);
            }
        } catch (ParseException e) {
            return false;
        }
    }

}

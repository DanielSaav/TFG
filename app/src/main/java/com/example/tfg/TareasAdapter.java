package com.example.tfg;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TareasAdapter extends RecyclerView.Adapter<TareasAdapter.TareaViewHolder> {

    private final List<Tarea> lista;
    private final Context ctx;

    private final Set<String> tareasNotificadas = new HashSet<>();

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;


    public TareasAdapter(Context ctx, List<Tarea> lista) {
        this.ctx = ctx;
        this.lista = lista;
        this.sharedPreferences = ctx.getSharedPreferences("tareas_notificadas", Context.MODE_PRIVATE);
        this.editor = sharedPreferences.edit();

    }

    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_tarea, parent, false);
        return new TareaViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int position) {
        Tarea t = lista.get(position);
        holder.tvTitulo.setText(t.getTitulo());
        holder.tvCorreo.setText("Correo: " + t.getCorreoUsuario());
        holder.tvFechaHora.setText("Límite: " + t.getFechaLimite() + " | " + t.getHoraLimite());

        try {
            String[] fechaPartes = t.getFechaLimite().split("/");
            String[] horaPartes = t.getHoraLimite().split(":");

            int dia = Integer.parseInt(fechaPartes[0]);
            int mes = Integer.parseInt(fechaPartes[1]) - 1; // Mes en 0-11
            int anio = Integer.parseInt(fechaPartes[2]);
            int hora = Integer.parseInt(horaPartes[0]);
            int minuto = Integer.parseInt(horaPartes[1]);

            Calendar fechaLimite = Calendar.getInstance();
            fechaLimite.set(anio, mes, dia, hora, minuto, 0);

            Calendar ahora = Calendar.getInstance();

            boolean fechaLimitePasada = false;

            Calendar hoyInicio = Calendar.getInstance();
            hoyInicio.set(Calendar.HOUR_OF_DAY, 0);
            hoyInicio.set(Calendar.MINUTE, 0);
            hoyInicio.set(Calendar.SECOND, 0);
            hoyInicio.set(Calendar.MILLISECOND, 0);

            Calendar hoyFin = Calendar.getInstance();
            hoyFin.set(Calendar.HOUR_OF_DAY, 23);
            hoyFin.set(Calendar.MINUTE, 59);
            hoyFin.set(Calendar.SECOND, 59);
            hoyFin.set(Calendar.MILLISECOND, 999);

            if (fechaLimite.before(hoyInicio)) {
                fechaLimitePasada = true;
            } else if (fechaLimite.after(hoyFin)) {
                fechaLimitePasada = false;
            } else {
                if (fechaLimite.before(ahora)) {
                    fechaLimitePasada = true;
                }
            }

            if (fechaLimitePasada) {
                // Cambiar fondo a rojo
                TypedValue typedValue = new TypedValue();
                Context context = holder.itemView.getContext();
                context.getTheme().resolveAttribute(R.attr.colorRojo, typedValue, true);
                holder.rootLayout.setBackgroundColor(typedValue.data);

                // Verificar si ya se notificó
                boolean yaNotificada = sharedPreferences.getBoolean(t.getTitulo(), false);

                if (!"Sí".equalsIgnoreCase(t.getCompletado()) && !yaNotificada) {
                    mostrarNotificacion("Tarea fuera de plazo", "La tarea \"" + t.getTitulo() + "\" ha pasado su fecha límite.");
                    editor.putBoolean(t.getTitulo(), true);  // Marcar como notificada
                    editor.apply();
                }
            } else {
                holder.rootLayout.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            }



        } catch (Exception e) {
            // Si hay error, fondo blanco
            holder.rootLayout.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        }

        // Botón completar
        holder.btnCompletar.setText("✓");
        if ("Sí".equalsIgnoreCase(t.getCompletado())) {
            holder.btnCompletar.setEnabled(false);
            holder.btnCompletar.setAlpha(0.5f);
        } else {
            holder.btnCompletar.setEnabled(true);
            holder.btnCompletar.setAlpha(1f);
            holder.btnCompletar.setOnClickListener(v -> marcarComoCompletada(t, position));
        }

        // Evento de clic
        holder.itemView.setOnClickListener(v -> mostrarDialogoEditar(t, position));
    }



    private void mostrarNotificacion(String titulo, String mensaje) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, "canal_tareas")
                .setSmallIcon(R.drawable.images__3_) // Usa un icono que tengas en res/drawable
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }


    private void marcarComoCompletada(Tarea tarea, int position) {
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("tareas")
                .document(tarea.getId())
                .update("completado", "Sí")
                .addOnSuccessListener(aVoid -> {
                    tarea.setCompletado("Sí");
                    notifyItemChanged(position);
                    Toast.makeText(ctx, "Tarea completada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ctx, "Error al completar tarea", Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarDialogoEditar(Tarea tarea, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_editar_tarea, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        EditText etTitulo = view.findViewById(R.id.etTitulo);
        EditText etFechaHora = view.findViewById(R.id.etFechaHora);
        TextView tvEliminar = view.findViewById(R.id.tvEliminar);
        Button btnAceptar = view.findViewById(R.id.btnAceptar);
        Button btnCancelar = view.findViewById(R.id.btnCancelar);

        etTitulo.setText(tarea.getTitulo());
        etFechaHora.setText(tarea.getFechaLimite() + " | " + tarea.getHoraLimite());

        etFechaHora.setOnClickListener(v -> {
            final Calendar ahora = Calendar.getInstance();

            DatePickerDialog datePicker = new DatePickerDialog(ctx, (view1, year, month, dayOfMonth) -> {
                boolean esHoy = ahora.get(Calendar.YEAR) == year &&
                        ahora.get(Calendar.MONTH) == month &&
                        ahora.get(Calendar.DAY_OF_MONTH) == dayOfMonth;

                int horaInicial = esHoy ? ahora.get(Calendar.HOUR_OF_DAY) : 0;
                int minutoInicial = esHoy ? ahora.get(Calendar.MINUTE) : 0;

                TimePickerDialog timePicker = new TimePickerDialog(ctx, (view2, hourOfDay, minute) -> {
                    if (esHoy && (hourOfDay < ahora.get(Calendar.HOUR_OF_DAY) ||
                            (hourOfDay == ahora.get(Calendar.HOUR_OF_DAY) && minute < ahora.get(Calendar.MINUTE)))) {
                        Toast.makeText(ctx, "No puedes seleccionar una hora anterior a la actual", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String fecha = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    String hora = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    etFechaHora.setText(fecha + " | " + hora);
                }, horaInicial, minutoInicial, true);

                timePicker.show();

            }, ahora.get(Calendar.YEAR), ahora.get(Calendar.MONTH), ahora.get(Calendar.DAY_OF_MONTH));

            datePicker.getDatePicker().setMinDate(ahora.getTimeInMillis()); // Impide fechas anteriores
            datePicker.show();
        });

        btnAceptar.setOnClickListener(v -> {
            String nuevoTitulo = etTitulo.getText().toString().trim();
            String[] partes = etFechaHora.getText().toString().split(" \\| ");
            String nuevaFecha = partes.length > 0 ? partes[0] : "";
            String nuevaHora = partes.length > 1 ? partes[1] : "";

            // Validar que la fecha y hora no sean anteriores a la actual
            try {
                String[] fechaPartes = nuevaFecha.split("/");
                String[] horaPartes = nuevaHora.split(":");

                int dia = Integer.parseInt(fechaPartes[0]);
                int mes = Integer.parseInt(fechaPartes[1]) - 1; // Mes empieza desde 0
                int anio = Integer.parseInt(fechaPartes[2]);
                int hora = Integer.parseInt(horaPartes[0]);
                int minuto = Integer.parseInt(horaPartes[1]);

                Calendar fechaSeleccionada = Calendar.getInstance();
                fechaSeleccionada.set(anio, mes, dia, hora, minuto, 0);

                Calendar fechaActual = Calendar.getInstance();

                if (fechaSeleccionada.before(fechaActual)) {
                    Toast.makeText(ctx, "La fecha y hora no pueden ser anteriores a la actual", Toast.LENGTH_LONG).show();
                    return;
                }

                // Actualizar tarea si la fecha es válida
                FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .collection("tareas")
                        .document(tarea.getId())
                        .update("titulo", nuevoTitulo, "fechaLimite", nuevaFecha, "horaLimite", nuevaHora)
                        .addOnSuccessListener(aVoid -> {
                            tarea.setTitulo(nuevoTitulo);
                            tarea.setFechaLimite(nuevaFecha);
                            tarea.setHoraLimite(nuevaHora);
                            notifyItemChanged(position);
                            Toast.makeText(ctx, "Tarea actualizada", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(ctx, "Error al actualizar", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                Toast.makeText(ctx, "Fecha u hora no válida", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        tvEliminar.setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("tareas")
                    .document(tarea.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        lista.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(ctx, "Tarea eliminada", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(ctx, "Error al eliminar", Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class TareaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvCorreo, tvFechaHora;
        Button btnCompletar;

        View rootLayout;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvCorreo = itemView.findViewById(R.id.tvCorreo);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            btnCompletar = itemView.findViewById(R.id.btnCompletar);
            rootLayout = itemView;  // Aquí usamos el itemView completo para cambiar fondo
        }

    }
}

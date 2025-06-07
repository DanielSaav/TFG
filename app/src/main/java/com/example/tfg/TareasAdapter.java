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
        holder.tvFechaHora.setText("Límite: " + t.getFechaLimite() + " | " + t.getHoraLimite());

        // Obtener preferencias
        boolean mostrarFueraDePlazo = sharedPreferences.getBoolean("mostrar_fuera_plazo", true);
        boolean mostrarCompletadas = sharedPreferences.getBoolean("mostrar_completadas", true);
        boolean fechaLimitePasada = false;
        boolean esCompletada = "Sí".equalsIgnoreCase(t.getCompletado());

        // Verificar si la tarea está fuera de plazo
        try {
            String[] fechaPartes = t.getFechaLimite().split("/");
            String[] horaPartes = t.getHoraLimite().split(":");

            int dia = Integer.parseInt(fechaPartes[0]);
            int mes = Integer.parseInt(fechaPartes[1]) - 1;
            int anio = Integer.parseInt(fechaPartes[2]);
            int hora = Integer.parseInt(horaPartes[0]);
            int minuto = Integer.parseInt(horaPartes[1]);

            Calendar fechaLimite = Calendar.getInstance();
            fechaLimite.set(anio, mes, dia, hora, minuto, 0);

            fechaLimitePasada = fechaLimite.before(Calendar.getInstance());
        } catch (Exception e) {
            fechaLimitePasada = false;
        }

        // Configurar visibilidad según preferencias
        boolean debeMostrarse = true;

        if (fechaLimitePasada && !esCompletada) {
            if (!mostrarFueraDePlazo) {
                debeMostrarse = false;
            } else {
                // Cambiar fondo a rojo si está fuera de plazo
                TypedValue typedValue = new TypedValue();
                Context context = holder.itemView.getContext();
                context.getTheme().resolveAttribute(R.attr.colorRojo, typedValue, true);
                holder.rootLayout.setBackgroundColor(typedValue.data);
            }
        } else if (esCompletada && !mostrarCompletadas) {
            debeMostrarse = false;
        } else {
            // Fondo normal (blanco) para tareas dentro de plazo
            holder.rootLayout.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        }

        // Aplicar visibilidad
        if (!debeMostrarse) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // Configurar botón de completar
        holder.btnCompletar.setText("✓");
        holder.btnCompletar.setEnabled(!esCompletada && !fechaLimitePasada);
        holder.btnCompletar.setAlpha((esCompletada || fechaLimitePasada) ? 0.5f : 1f);

        if (!esCompletada && !fechaLimitePasada) {
            holder.btnCompletar.setOnClickListener(v -> marcarComoCompletada(t, position));
        } else {
            holder.btnCompletar.setOnClickListener(null);
        }

        // Configurar clic para editar
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

                Calendar ahora = Calendar.getInstance();

                if (fechaSeleccionada.before(ahora)) {
                    Toast.makeText(ctx, "No puedes seleccionar una fecha/hora pasada", Toast.LENGTH_SHORT).show();
                    return;
                }

            } catch (Exception e) {
                Toast.makeText(ctx, "Formato de fecha/hora incorrecto", Toast.LENGTH_SHORT).show();
                return;
            }

            if (nuevoTitulo.isEmpty()) {
                Toast.makeText(ctx, "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

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
                    .addOnFailureListener(e -> {
                        Toast.makeText(ctx, "Error al actualizar tarea", Toast.LENGTH_SHORT).show();
                    });
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
                        notifyItemRangeChanged(position, lista.size());
                        Toast.makeText(ctx, "Tarea eliminada", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ctx, "Error al eliminar tarea", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }


    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class TareaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvFechaHora;
        Button btnCompletar;
        View rootLayout;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            btnCompletar = itemView.findViewById(R.id.btnCompletar);
            rootLayout = itemView.findViewById(R.id.rootLayout);
        }
    }
}

package com.example.tfg;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TareasAdapter extends RecyclerView.Adapter<TareasAdapter.TareaViewHolder> {

    private final List<Tarea> lista;
    private final Context ctx;

    public TareasAdapter(Context ctx, List<Tarea> lista) {
        this.ctx = ctx;
        this.lista = lista;
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

        // Configurar botón de completado
        holder.btnCompletar.setText("✓");
        if ("Sí".equalsIgnoreCase(t.getCompletado())) {
            holder.btnCompletar.setEnabled(false);
            holder.btnCompletar.setAlpha(0.5f);
        } else {
            holder.btnCompletar.setEnabled(true);
            holder.btnCompletar.setAlpha(1f);
            holder.btnCompletar.setOnClickListener(v -> marcarComoCompletada(t, position));
        }

        holder.itemView.setOnClickListener(v -> mostrarDialogoEditar(t, position));
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
            final Calendar c = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(ctx, (view1, year, month, day) -> {
                TimePickerDialog timePicker = new TimePickerDialog(ctx, (view2, hour, minute) -> {
                    String fecha = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
                    String hora = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    etFechaHora.setText(fecha + " | " + hora);
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
                timePicker.show();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        btnAceptar.setOnClickListener(v -> {
            String nuevoTitulo = etTitulo.getText().toString().trim();
            String[] partes = etFechaHora.getText().toString().split(" \\| ");
            String nuevaFecha = partes.length > 0 ? partes[0] : "";
            String nuevaHora = partes.length > 1 ? partes[1] : "";

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

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvCorreo = itemView.findViewById(R.id.tvCorreo);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            btnCompletar = itemView.findViewById(R.id.btnCompletar);
        }
    }
}
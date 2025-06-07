package com.example.tfg;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class PerfilFragment extends Fragment {

    private ImageView imageView;
    private TextView tvEmail, tvNombre;
    private Button cambiarDatos, eliminarCuenta;
    private Uri selectedImageUri;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private final androidx.activity.result.ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::onImagePicked
            );

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // Inflamos la vista aquí, sin usar aún imageView ni otras vistas
        return inflater.inflate(R.layout.activity_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializamos las vistas ya que la vista está inflada
        imageView = view.findViewById(R.id.imageView);
        tvEmail = view.findViewById(R.id.textView);
        tvNombre = view.findViewById(R.id.textView2);

        cambiarDatos = view.findViewById(R.id.cambiarDatos);
        eliminarCuenta = view.findViewById(R.id.eliminarCuenta);

        // Inicializamos Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("fotos_perfil");

        // Cargar URI guardada si existe y mostrar con Glide
        String savedUri = requireActivity()
                .getSharedPreferences("perfil_prefs", getContext().MODE_PRIVATE)
                .getString("foto_uri", null);

        if (savedUri != null) {
            Glide.with(this)
                    .load(Uri.parse(savedUri))
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.sin_imagen);
        }

        // Listeners
        imageView.setOnClickListener(v -> openGallery());
        cambiarDatos.setOnClickListener(v -> showChangeNameDialog());
        eliminarCuenta.setOnClickListener(v -> showDeleteAccountDialog());

        // Cargar datos usuario
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvEmail.setText("Correo: " + user.getEmail());

            db.collection("usuarios")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(this::onUserDocument)
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Error al cargar perfil: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void onUserDocument(DocumentSnapshot doc) {
        if (!doc.exists()) return;

        if (doc.contains("nombre")) {
            String nombre = doc.getString("nombre");
            tvNombre.setText(nombre);
        }

        // Mostrar imagen por defecto si no hay foto en Firestore
        if (!doc.contains("fotoPerfil")) {
            imageView.setImageResource(R.drawable.sin_imagen);
            return;
        }

        String url = doc.getString("fotoPerfil");
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.sin_imagen) // Esto muestra sin_imagen si hay error al cargar
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.sin_imagen);
        }
    }

    private void showChangeNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Cambiar nombre de usuario");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        input.setText(tvNombre.getText().toString());
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoNombre = input.getText().toString().trim();
            if (nuevoNombre.isEmpty()) {
                Toast.makeText(getContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            db.collection("usuarios")
                    .document(user.getUid())
                    .update("nombre", nuevoNombre)
                    .addOnSuccessListener(aVoid -> {
                        tvNombre.setText(nuevoNombre);
                        Toast.makeText(getContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Error al actualizar nombre: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro de que quieres eliminar tu cuenta? Esta acción es irreversible.")
                .setPositiveButton("Sí", (dialog, which) -> deleteAccount())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("usuarios")
                .document(user.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    user.delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(getContext(),
                                        "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getActivity(), LoginActivity.class));
                                getActivity().finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Error al eliminar cuenta: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Error al borrar datos en Firestore: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void onImagePicked(ActivityResult result) {
        if (result.getResultCode() != requireActivity().RESULT_OK || result.getData() == null) {
            imageView.setImageResource(R.drawable.sin_imagen); // Mostrar imagen por defecto si se cancela
            return;
        }

        selectedImageUri = result.getData().getData();
        if (selectedImageUri != null) {
            Glide.with(this)
                    .load(selectedImageUri)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.sin_imagen) // Muestra sin_imagen si hay error al cargar
                    .into(imageView);

            requireActivity().getSharedPreferences("perfil_prefs", getContext().MODE_PRIVATE)
                    .edit()
                    .putString("foto_uri", selectedImageUri.toString())
                    .apply();
        } else {
            imageView.setImageResource(R.drawable.sin_imagen);
        }
    }

}

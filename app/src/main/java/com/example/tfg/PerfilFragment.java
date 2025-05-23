package com.example.tfg;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    // Lanzador de selección de imagen
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
        return inflater.inflate(R.layout.activity_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializa Firebase
        mAuth      = FirebaseAuth.getInstance();
        db         = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance()
                .getReference("fotos_perfil");

        // Referencias a vistas
        imageView      = view.findViewById(R.id.imageView);
        tvEmail        = view.findViewById(R.id.textView);
        tvNombre       = view.findViewById(R.id.textView2);
        cambiarDatos   = view.findViewById(R.id.cambiarDatos);
        eliminarCuenta = view.findViewById(R.id.eliminarCuenta);

        imageView.setOnClickListener(v -> openGallery());

        cambiarDatos.setOnClickListener(v -> showChangeNameDialog());
        eliminarCuenta.setOnClickListener(v -> showDeleteAccountDialog());

        // Carga datos del usuario (foto, email y nombre)
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // email de Auth
            tvEmail.setText("Correo: " + user.getEmail());

            // resto de datos de Firestore
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
        if (doc.contains("fotoPerfil")) {
            String url = doc.getString("fotoPerfil");
            if (url != null && !url.isEmpty()) {
                Glide.with(this)
                        .load(url)
                        .circleCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageView);
            }
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

        // Primero borrar el documento en Firestore
        db.collection("usuarios")
                .document(user.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Luego borrar la cuenta de Auth
                    user.delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(getContext(),
                                        "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show();
                                // Vuelve a la pantalla de login
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
            return;
        }
        selectedImageUri = result.getData().getData();
        if (selectedImageUri != null) {
            Glide.with(this)
                    .load(selectedImageUri)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView);
            uploadImageToFirebase();
        }
    }

    private void uploadImageToFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference imgRef = storageRef.child(user.getUid() + ".jpg");
        imgRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Usar el storage del snapshot
                    taskSnapshot.getStorage().getDownloadUrl()
                            .addOnSuccessListener(uri -> saveUrlInFirestore(user.getUid(), uri.toString()))
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Error al obtener URL: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Error al subir imagen: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }


    private void saveUrlInFirestore(String userId, String url) {
        db.collection("usuarios")
                .document(userId)
                .update("fotoPerfil", url)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(),
                                "Foto de perfil guardada en Firebase",
                                Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Error al guardar URL en Firestore: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
}

package com.example.tfg;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextNombre, editTextClave, editTextRepetirClave;
    private Button buttonRegistro;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        editTextEmail         = findViewById(R.id.editTextEmail);
        editTextNombre        = findViewById(R.id.editTextNombre);
        editTextClave         = findViewById(R.id.editTextClave);
        editTextRepetirClave  = findViewById(R.id.editTextRepetirClave);
        buttonRegistro        = findViewById(R.id.buttonRegistro);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        buttonRegistro.setOnClickListener(v -> {
            String email         = editTextEmail.getText().toString().trim();
            String nombre        = editTextNombre.getText().toString().trim();
            String clave         = editTextClave.getText().toString().trim();
            String repetirClave  = editTextRepetirClave.getText().toString().trim();

            if (email.isEmpty() || nombre.isEmpty() || clave.isEmpty() || repetirClave.isEmpty()) {
                Toast.makeText(RegistroActivity.this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(RegistroActivity.this, "Email no válido", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!clave.equals(repetirClave)) {
                Toast.makeText(RegistroActivity.this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            buttonRegistro.setEnabled(false);

            // 1) Crear usuario en Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, clave)
                    .addOnCompleteListener(task -> {
                        buttonRegistro.setEnabled(true);

                        if (task.isSuccessful()) {
                            // 2) Enviar email de verificación
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.sendEmailVerification()
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(RegistroActivity.this,
                                                    "Te hemos enviado un email de verificación. Revisa tu bandeja.",
                                                    Toast.LENGTH_LONG).show();

                                            // 3) Guardar datos en Firestore
                                            String userId = user.getUid();
                                            Map<String, Object> userMap = new HashMap<>();
                                            userMap.put("email", email);
                                            userMap.put("nombre", nombre);

                                            db.collection("usuarios")
                                                    .document(userId)
                                                    .set(userMap)
                                                    .addOnSuccessListener(aVoid -> {
                                                        // opcional: puedes cerrar la Activity
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(RegistroActivity.this,
                                                                "Error al guardar datos: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(RegistroActivity.this,
                                                    "Error al enviar email de verificación: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            Exception ex = task.getException();
                            if (ex instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(RegistroActivity.this,
                                        "El usuario ya está registrado",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RegistroActivity.this,
                                        "Error: " + (ex != null ? ex.getMessage() : "desconocido"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
    }
}

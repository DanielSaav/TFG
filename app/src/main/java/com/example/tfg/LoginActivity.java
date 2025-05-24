package com.example.tfg;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextClave;
    private Button buttonLogin;
    private TextView registrarse, olvidarContrasenia;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextUsername);
        editTextClave = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        registrarse = findViewById(R.id.textViewRegister2);
        olvidarContrasenia = findViewById(R.id.olvidarContrasenia);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        olvidarContrasenia.setOnClickListener(v -> showPasswordResetDialog());

        registrarse.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class))
        );

        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String clave = editTextClave.getText().toString().trim();

            if (email.isEmpty() || clave.isEmpty()) {
                Toast.makeText(this, "Email y contraseña obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, clave)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            getSharedPreferences("modo_tema", Context.MODE_PRIVATE)
                                    .edit()
                                    .remove("fragment_actual")
                                    .apply();

                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException e) {
                                Toast.makeText(this, "Usuario no registrado", Toast.LENGTH_SHORT).show();
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
    }

    private void showPasswordResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Restablecer contraseña");

        final EditText emailInput = new EditText(this);
        emailInput.setHint("Ingresa tu email registrado");
        builder.setView(emailInput);

        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            if (!email.isEmpty()) {
                sendPasswordResetEmail(email);
            } else {
                Toast.makeText(this, "Debes ingresar un email", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Email de restablecimiento enviado a " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            Toast.makeText(this,
                                    "No existe una cuenta con este email",
                                    Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this,
                                    "Error al enviar el email: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
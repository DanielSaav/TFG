package com.example.tfg;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

   private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Aplicar el modo oscuro antes de cargar el layout
        SharedPreferences prefs = getSharedPreferences("modo_tema", Context.MODE_PRIVATE);
        boolean modoOscuro = prefs.getBoolean("modo_oscuro", false);
        if (modoOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Inicializar el Toolbar PRIMERO
        toolbar = findViewById(R.id.materialToolbar);
        setSupportActionBar(toolbar); // Esto es crucial para que funcione el menú

        // Resto del código...
        String fragmentActual = prefs.getString("fragment_actual", "");

        if (savedInstanceState == null) {
            Fragment fragmentInicial;
            if ("ajustes".equals(fragmentActual)) {
                fragmentInicial = new AjustesFragment();
                prefs.edit().remove("fragment_actual").apply();
            } else {
                fragmentInicial = new TareasListaFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView2, fragmentInicial)
                    .commit();
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.materialToolbar);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.home) {
                fragment = new TareasListaFragment();
            } else if (id == R.id.perfil) {
                fragment = new PerfilFragment();
            } else if (id == R.id.ajustes) {
                fragment = new AjustesFragment();
            } else if (id == R.id.logros) {
                fragment = new LogrosFragment();
            }

            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerView2, fragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.salir) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Saliendo...", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        } else if (id == R.id.modo_silencio) {
            Intent intent = new Intent(this, SilencioActivity.class);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

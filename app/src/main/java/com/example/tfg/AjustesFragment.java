package com.example.tfg;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class AjustesFragment extends Fragment {

    private Switch switchModoOscuro, switchMostrarCompletadas, switchTareasFueraPlazo;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ajustes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchModoOscuro = view.findViewById(R.id.modo_oscuro);
        switchMostrarCompletadas = view.findViewById(R.id.mostrarTareasCompletadas);
        switchTareasFueraPlazo = view.findViewById(R.id.mostrarTareasFueraPlazo);

        // Usamos el mismo nombre de preferencias que en TareasAdapter
        prefs = requireActivity().getSharedPreferences("tareas_prefs", Context.MODE_PRIVATE);
        editor = prefs.edit();

        setupModoOscuro();
        setupMostrarCompletadas();
        setupMostrarTareasFueraPlazo();
    }

    private void setupModoOscuro() {
        boolean modoOscuroActivado = prefs.getBoolean("modo_oscuro", false);
        switchModoOscuro.setChecked(modoOscuroActivado);
        switchModoOscuro.setText(modoOscuroActivado ? "Modo oscuro" : "Modo claro");

        switchModoOscuro.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("modo_oscuro", isChecked);
            editor.apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            switchModoOscuro.setText(isChecked ? "Modo oscuro" : "Modo claro");
        });
    }

    private void setupMostrarCompletadas() {
        boolean mostrarCompletadas = prefs.getBoolean("mostrar_completadas", true);
        switchMostrarCompletadas.setChecked(mostrarCompletadas);

        switchMostrarCompletadas.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("mostrar_completadas", isChecked);
            editor.apply();
            notifyTareasFragment();
        });
    }

    private void setupMostrarTareasFueraPlazo() {
        boolean mostrarFueraDePlazo = prefs.getBoolean("mostrar_fuera_plazo", true);
        switchTareasFueraPlazo.setChecked(mostrarFueraDePlazo);

        switchTareasFueraPlazo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("mostrar_fuera_plazo", isChecked);
            editor.apply();
            notifyTareasFragment();
        });
    }

    private void notifyTareasFragment() {
        FragmentManager fragmentManager = getParentFragmentManager();
        TareasListaFragment tareasFragment = (TareasListaFragment) fragmentManager.findFragmentByTag("tareas_fragment");

        if (tareasFragment != null) {
            // Notificar al adapter primero para cambios inmediatos
            if (tareasFragment.adapter != null) {
                tareasFragment.adapter.notifyDataSetChanged();
            }
            // Luego recargar desde Firestore para sincronización completa
            tareasFragment.cargarTareasDeFirestore();
        }
    }
}
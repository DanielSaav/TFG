package com.example.tfg;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

public class AjustesFragment extends Fragment {

    private Switch switchModoOscuro, switchMostrarCompletadas, switchTareasFueraPlazo;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_ajustes, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view, @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        switchModoOscuro = view.findViewById(R.id.modo_oscuro);
        switchMostrarCompletadas = view.findViewById(R.id.mostrarTareasCompletadas);

switchTareasFueraPlazo = view.findViewById(R.id.mostrarTareasFueraPlazo);
        // Obtener las preferencias
        SharedPreferences prefs = requireActivity().getSharedPreferences("modo_tema", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // ---------- Switch Modo Oscuro ----------
        boolean modoOscuroActivado = prefs.getBoolean("modo_oscuro", false);
        switchModoOscuro.setChecked(modoOscuroActivado);
        switchModoOscuro.setText(modoOscuroActivado ? "Modo oscuro" : "Modo claro");

        switchModoOscuro.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("modo_oscuro", isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // ---------- Switch Mostrar Tareas Completadas ----------
        boolean mostrarCompletadas = prefs.getBoolean("mostrar_completadas", true);
        switchMostrarCompletadas.setChecked(mostrarCompletadas);

        switchMostrarCompletadas.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("mostrar_completadas", isChecked);
            editor.apply();
        });

        // ---------- Spinner Fuentes ----------
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.lista_fuentes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        // Cargar la fuente guardada
        String fuenteSeleccionada = prefs.getString("fuente", "Arial");
        int posicion = adapter.getPosition(fuenteSeleccionada);


        // Guardar nueva selección

    }
}



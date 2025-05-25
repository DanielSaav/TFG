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

    private Switch switchModoOscuro;
    private Spinner fuenteSpinner;

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
        fuenteSpinner = view.findViewById(R.id.spinner);

        SharedPreferences prefs = requireActivity().getSharedPreferences("modo_tema", Context.MODE_PRIVATE);
        boolean modoOscuroActivado = prefs.getBoolean("modo_oscuro", false);

        switchModoOscuro.setChecked(modoOscuroActivado);
        switchModoOscuro.setText(modoOscuroActivado ? "Modo oscuro" : "Modo claro");

        switchModoOscuro.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("modo_oscuro", isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Configurar el Spinner con el array de fuentes
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.lista_fuentes,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fuenteSpinner.setAdapter(adapter);

        // Cargar selección guardada
        String fuenteSeleccionada = prefs.getString("fuente", "Arial");
        int posicion = adapter.getPosition(fuenteSeleccionada);
        fuenteSpinner.setSelection(posicion);

        // Guardar nueva selección
        fuenteSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String fuente = parent.getItemAtPosition(position).toString();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("fuente", fuente);
                editor.apply();


            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Nada
            }
        });
    }
}

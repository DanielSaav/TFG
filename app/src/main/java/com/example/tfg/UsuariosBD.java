package com.example.tfg;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

/**
 * Clase encargada de gestionar la base de datos de usuarios de la aplicación.
 * Extiende {@link SQLiteOpenHelper} para manejar la creación y actualización de la base de datos.
 */
public class UsuariosBD extends SQLiteOpenHelper {

    /** Nombre del archivo de la base de datos. */
    private static final String DATABASE_NAME = "Usuarios.db";

    /** Versión de la base de datos. Se incrementa cuando hay cambios en la estructura. */
    private static final int DATABASE_VERSION = 3;

    /**
     * Constructor de la base de datos.
     *
     * @param context Contexto de la aplicación.
     */
    public UsuariosBD(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Método llamado cuando la base de datos es creada por primera vez.
     * Se ejecuta la consulta SQL para crear la tabla de usuarios.
     *
     * @param db Base de datos en la que se ejecutará la consulta.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT, " +
                "clave TEXT, " +
                "email TEXT, " +  // Campo de correo electrónico añadido en la versión 3
                "imagen_perfil TEXT)");
    }

    /**
     * Método llamado cuando se actualiza la versión de la base de datos.
     * Elimina la tabla anterior y la vuelve a crear.
     *
     * @param db         Base de datos a modificar.
     * @param oldVersion Versión anterior de la base de datos.
     * @param newVersion Nueva versión de la base de datos.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        onCreate(db);
    }
}

package com.example.tfg;

public class Tarea {
    private String id;
    private String titulo;
    private String correoUsuario;
    private String fechaLimite;  // por ejemplo "2025-06-01"
    private String horaLimite;   // por ejemplo "14:30"
    private String completado = "No";



    // Constructor vacío (requerido por Firestore)
    public Tarea() {}

    public Tarea(String id, String titulo, String correoUsuario,
                 String fechaLimite, String horaLimite) {
        this.id            = id;
        this.titulo        = titulo;
        this.correoUsuario = correoUsuario;
        this.fechaLimite   = fechaLimite;
        this.horaLimite    = horaLimite;
    }

    // Getters / setters
    public String getCompletado() {
        return completado;
    }

    public void setCompletado(String completado) {
        this.completado = completado;
    }
    public String getId()               { return id; }
    public void   setId(String id)      { this.id = id; }

    public String getTitulo()           { return titulo; }
    public void   setTitulo(String t)   { this.titulo = t; }

    public String getCorreoUsuario()    { return correoUsuario; }
    public void   setCorreoUsuario(String e) { this.correoUsuario = e; }

    public String getFechaLimite()      { return fechaLimite; }
    public void   setFechaLimite(String f)   { this.fechaLimite = f; }

    public String getHoraLimite()       { return horaLimite; }
    public void   setHoraLimite(String h)    { this.horaLimite = h; }


}

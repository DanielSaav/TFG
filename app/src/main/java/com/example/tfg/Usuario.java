package com.example.tfg;

public class Usuario {

    //Atributos
    private int id;
    private String usuarioNombre;
    private String clave;
    private String imagenPerfil;  // Nuevo campo para la imagen de perfil
    private String email;

    //Constructor
    public Usuario(String email, String usuarioNombre, String clave, String imagenPerfil) {
        this.email = email;
        this.usuarioNombre = usuarioNombre;
        this.clave = clave;
        this.imagenPerfil = imagenPerfil;
    }

    // Getters y Setters
    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public String getClave() {
        return clave;
    }

    public String getImagenPerfil() {
        return imagenPerfil;
    }

    public void setImagenPerfil(String imagenPerfil) {
        this.imagenPerfil = imagenPerfil;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

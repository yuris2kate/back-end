package com.sloth.whatsapp.models;

import java.util.Map;

public class ConquistaResponse {

    private Map<String, Conquista> conquistas;

    public ConquistaResponse(Map<String, Conquista> conquistas) {
        this.conquistas = conquistas;
    }

    public Map<String, Conquista> getConquistas() {
        return conquistas;
    }

    public void setConquistas(Map<String, Conquista> conquistas) {
        this.conquistas = conquistas;
    }
}

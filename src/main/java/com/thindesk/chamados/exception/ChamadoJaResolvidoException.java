package com.thindesk.chamados.exception;

public class ChamadoJaResolvidoException extends RuntimeException {

    public ChamadoJaResolvidoException(String id) {
        super("O chamado " + id + " já está RESOLVIDO e não pode ser alterado novamente");
    }
}

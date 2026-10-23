package com.thindesk.chamados.exception;

public class ChamadoNaoEncontradoException extends RuntimeException {

    public ChamadoNaoEncontradoException(String id) {
        super("Chamado não encontrado com id: " + id);
    }
}

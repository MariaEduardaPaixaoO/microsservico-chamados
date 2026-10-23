package com.thindesk.chamados.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChamadoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(ChamadoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ChamadoJaResolvidoException.class)
    public ResponseEntity<ErroResponse> jaResolvido(ChamadoJaResolvidoException ex) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Erros de @Valid nos DTOs: devolve a mensagem de cada campo inválido. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        HttpStatus s = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(s)
                .body(ErroResponse.of(s.value(), s.getReasonPhrase(), "Dados inválidos", campos));
    }

    /** Requisição sem o cabeçalho de identificação do usuário autenticado. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErroResponse> semUsuario(MissingRequestHeaderException ex) {
        return resposta(HttpStatus.UNAUTHORIZED,
                "Usuário não autenticado: informe o cabeçalho " + ex.getHeaderName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> argumentoInvalido(IllegalArgumentException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou JSON inválido");
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus s, String mensagem) {
        return ResponseEntity.status(s).body(ErroResponse.of(s.value(), s.getReasonPhrase(), mensagem));
    }
}

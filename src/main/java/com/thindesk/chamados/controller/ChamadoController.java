package com.thindesk.chamados.controller;

import com.thindesk.chamados.dto.AbrirChamadoRequest;
import com.thindesk.chamados.dto.ResolverChamadoRequest;
import com.thindesk.chamados.model.Chamado;
import com.thindesk.chamados.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST de chamados técnicos.
 *
 * A autenticação é feita pelo ThinDesk (sistema de login). Ele atua como fachada:
 * depois que o usuário faz login, encaminha as requisições para este microserviço
 * informando quem é o usuário autenticado no cabeçalho "X-Usuario-Id".
 */
@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    public static final String HEADER_USUARIO = "X-Usuario-Id";

    private final ChamadoService service;

    public ChamadoController(ChamadoService service) {
        this.service = service;
    }

    /** POST /api/chamados - Abrir chamado. */
    @PostMapping
    public ResponseEntity<Chamado> abrir(@RequestHeader(HEADER_USUARIO) String usuarioId,
                                         @Valid @RequestBody AbrirChamadoRequest req) {
        Chamado criado = service.abrir(req, usuarioId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.getId()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    /** GET /api/chamados/usuario/{id} - Listar chamados do usuário. */
    @GetMapping("/usuario/{id}")
    public List<Chamado> listarPorUsuario(@PathVariable("id") String usuarioId) {
        return service.listarPorUsuario(usuarioId);
    }

    /** GET /api/chamados/{id} - Consultar chamado (detalhes e status). */
    @GetMapping("/{id}")
    public Chamado buscar(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    /** PUT /api/chamados/{id}/resolver - Técnico registra a solução. */
    @PutMapping("/{id}/resolver")
    @ResponseStatus(HttpStatus.OK)
    public Chamado resolver(@PathVariable String id,
                            @RequestHeader(HEADER_USUARIO) String tecnicoId,
                            @Valid @RequestBody ResolverChamadoRequest req) {
        return service.resolver(id, req, tecnicoId);
    }
}

package com.thindesk.chamados.service;

import com.thindesk.chamados.dto.AbrirChamadoRequest;
import com.thindesk.chamados.dto.ResolverChamadoRequest;
import com.thindesk.chamados.exception.ChamadoJaResolvidoException;
import com.thindesk.chamados.exception.ChamadoNaoEncontradoException;
import com.thindesk.chamados.model.Chamado;
import com.thindesk.chamados.repository.ChamadoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;

    public ChamadoService(ChamadoRepository repository) {
        this.repository = repository;
    }

    /** Abre o chamado com solicitante, data de abertura e status ABERTO definidos automaticamente. */
    public Chamado abrir(AbrirChamadoRequest req, String usuarioId) {
        exigirUsuario(usuarioId);
        Chamado chamado = Chamado.abrir(req.titulo().trim(), req.descricao().trim(), usuarioId.trim());
        return repository.save(chamado);
    }

    public List<Chamado> listarPorUsuario(String usuarioId) {
        exigirUsuario(usuarioId);
        return repository.findBySolicitanteIdOrderByDataAberturaDesc(usuarioId.trim());
    }

    public Chamado buscarPorId(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ChamadoNaoEncontradoException(id));
    }

    /** Registra a solução. Um chamado já RESOLVIDO não pode ser alterado novamente. */
    public Chamado resolver(String id, ResolverChamadoRequest req, String tecnicoId) {
        exigirUsuario(tecnicoId);
        Chamado chamado = buscarPorId(id);
        if (chamado.isResolvido()) {
            throw new ChamadoJaResolvidoException(id);
        }
        chamado.resolver(tecnicoId.trim(), req.solucao().trim());
        return repository.save(chamado);
    }

    private void exigirUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("O id do usuário é obrigatório");
        }
    }
}

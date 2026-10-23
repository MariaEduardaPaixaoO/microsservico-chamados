package com.thindesk.chamados.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chamados")
public class Chamado {

    @Id
    private String id;

    private String titulo;
    private String descricao;

    /** Id do usuário autenticado que abriu o chamado. */
    @Indexed
    private String solicitanteId;

    private StatusChamado status;
    private LocalDateTime dataAbertura;

    /** Preenchidos somente na resolução. */
    private String tecnicoId;
    private String solucao;
    private LocalDateTime dataResolucao;

    public Chamado() {
    }

    /** Cria um chamado novo já com solicitante, data de abertura e status ABERTO. */
    public static Chamado abrir(String titulo, String descricao, String solicitanteId) {
        Chamado c = new Chamado();
        c.titulo = titulo;
        c.descricao = descricao;
        c.solicitanteId = solicitanteId;
        c.status = StatusChamado.ABERTO;
        c.dataAbertura = LocalDateTime.now();
        return c;
    }

    /** Registra a solução. Quem chama deve garantir que o chamado ainda está ABERTO. */
    public void resolver(String tecnicoId, String solucao) {
        this.tecnicoId = tecnicoId;
        this.solucao = solucao;
        this.status = StatusChamado.RESOLVIDO;
        this.dataResolucao = LocalDateTime.now();
    }

    @JsonIgnore
    public boolean isResolvido() {
        return status == StatusChamado.RESOLVIDO;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(String solicitanteId) { this.solicitanteId = solicitanteId; }

    public StatusChamado getStatus() { return status; }
    public void setStatus(StatusChamado status) { this.status = status; }

    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDateTime dataAbertura) { this.dataAbertura = dataAbertura; }

    public String getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(String tecnicoId) { this.tecnicoId = tecnicoId; }

    public String getSolucao() { return solucao; }
    public void setSolucao(String solucao) { this.solucao = solucao; }

    public LocalDateTime getDataResolucao() { return dataResolucao; }
    public void setDataResolucao(LocalDateTime dataResolucao) { this.dataResolucao = dataResolucao; }
}

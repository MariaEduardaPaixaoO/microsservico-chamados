package com.thindesk.chamados;

import com.thindesk.chamados.controller.ChamadoController;
import com.thindesk.chamados.model.Chamado;
import com.thindesk.chamados.repository.ChamadoRepository;
import com.thindesk.chamados.service.ChamadoService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa os endpoints com o service real e um repositório simulado em memória
 * (para não depender do MongoDB Atlas durante o build).
 */
@WebMvcTest(ChamadoController.class)
@Import(ChamadoService.class)
class ChamadoControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ChamadoRepository repository;

    final Map<String, Chamado> banco = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        banco.clear();
        when(repository.save(any(Chamado.class))).thenAnswer(inv -> {
            Chamado c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID().toString());
            banco.put(c.getId(), c);
            return c;
        });
        when(repository.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(banco.get(inv.<String>getArgument(0))));
        when(repository.findBySolicitanteIdOrderByDataAberturaDesc(anyString()))
                .thenAnswer(inv -> banco.values().stream()
                        .filter(c -> c.getSolicitanteId().equals(inv.getArgument(0)))
                        .toList());
    }

    private String abrirChamado(String usuario) throws Exception {
        String body = mvc.perform(post("/api/chamados")
                        .header("X-Usuario-Id", usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Impressora não liga","descricao":"A impressora do 2º andar não liga desde ontem"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.solicitanteId").value(usuario))
                .andExpect(jsonPath("$.dataAbertura").exists())
                .andExpect(jsonPath("$.dataResolucao").value(org.hamcrest.Matchers.nullValue()))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void fluxoCompleto_abrirListarConsultarResolver() throws Exception {
        String id = abrirChamado("user-1");
        abrirChamado("user-2");

        mvc.perform(get("/api/chamados/usuario/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(get("/api/chamados/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Impressora não liga"))
                .andExpect(jsonPath("$.status").value("ABERTO"));

        mvc.perform(put("/api/chamados/" + id + "/resolver")
                        .header("X-Usuario-Id", "tecnico-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solucao\":\"Cabo de energia substituído\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.tecnicoId").value("tecnico-1"))
                .andExpect(jsonPath("$.solucao").value("Cabo de energia substituído"))
                .andExpect(jsonPath("$.dataResolucao").exists());
    }

    @Test
    void naoPermiteResolverChamadoJaResolvido() throws Exception {
        String id = abrirChamado("user-1");
        String resolver = "{\"solucao\":\"Cabo de energia substituído\"}";

        mvc.perform(put("/api/chamados/" + id + "/resolver").header("X-Usuario-Id", "tec")
                        .contentType(MediaType.APPLICATION_JSON).content(resolver))
                .andExpect(status().isOk());

        mvc.perform(put("/api/chamados/" + id + "/resolver").header("X-Usuario-Id", "tec")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solucao\":\"Outra solução qualquer\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("já está RESOLVIDO")));

        // a solução original foi mantida
        mvc.perform(get("/api/chamados/" + id))
                .andExpect(jsonPath("$.solucao").value("Cabo de energia substituído"));
    }

    @Test
    void ignoraStatusEnviadoPeloCliente() throws Exception {
        mvc.perform(post("/api/chamados").header("X-Usuario-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Teste de status","descricao":"Tentando já abrir resolvido","status":"RESOLVIDO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void validaCamposObrigatorios() throws Exception {
        mvc.perform(post("/api/chamados").header("X-Usuario-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"descricao\":\"curta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.titulo").exists())
                .andExpect(jsonPath("$.campos.descricao").exists());
    }

    @Test
    void exigeUsuarioAutenticado() throws Exception {
        mvc.perform(post("/api/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Sem usuário\",\"descricao\":\"Requisição sem cabeçalho\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chamadoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/chamados/nao-existe"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/chamados/nao-existe/resolver").header("X-Usuario-Id", "tec")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"solucao\":\"Qualquer solução aqui\"}"))
                .andExpect(status().isNotFound());
    }
}

package demo.govtech;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import demo.govtech.usuario.Papel;
import demo.govtech.usuario.Usuario;
import demo.govtech.usuario.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Fluxo completo contra um PostgreSQL real (Testcontainers), passando por seguranca, Flyway e JPA. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FluxoAlvaraTest {

    private static final String SENHA = "senha-de-teste-123";
    private static final String RASCUNHO = """
            {"nomeEstabelecimento":"Padaria Exemplo","atividade":"Comercio de alimentos",
             "endereco":"Rua Ficticia, 10","latitude":-11.30,"longitude":-41.85}""";
    private static final String TODOS_ATENDIDOS = """
            {"criterios":[
              {"criterio":"DOCUMENTACAO_COMPLETA","atendido":true},
              {"criterio":"ENDERECO_COMPATIVEL_COM_ZONEAMENTO","atendido":true},
              {"criterio":"SEM_PENDENCIAS_FISCAIS","atendido":true},
              {"criterio":"VISTORIA_APROVADA","atendido":true}]}""";
    private static final String UM_REPROVADO = """
            {"criterios":[
              {"criterio":"DOCUMENTACAO_COMPLETA","atendido":true},
              {"criterio":"ENDERECO_COMPATIVEL_COM_ZONEAMENTO","atendido":true},
              {"criterio":"SEM_PENDENCIAS_FISCAIS","atendido":false,"observacao":"Debito em aberto"},
              {"criterio":"VISTORIA_APROVADA","atendido":true}]}""";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    @Test
    void fluxoCompletoAteOMapa() throws Exception {
        MockHttpSession cidadao = login(novoUsuario(Papel.CIDADAO));
        long id = criarRascunho(cidadao);
        anexarPdf(cidadao, id).andExpect(status().isCreated());
        mvc.perform(post("/api/alvaras/{id}/enviar", id).with(csrf()).session(cidadao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENVIADO"));

        MockHttpSession analista = login(novoUsuario(Papel.ANALISTA));
        mvc.perform(get("/api/alvaras").param("status", "ENVIADO").session(analista))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id==" + id + ")]").exists());
        mvc.perform(post("/api/alvaras/{id}/analise", id).with(csrf()).session(analista)
                        .contentType(MediaType.APPLICATION_JSON).content(TODOS_ATENDIDOS))
                .andExpect(status().isOk()).andExpect(jsonPath("$.parecer").value("FAVORAVEL"));

        MockHttpSession gestor = login(novoUsuario(Papel.GESTOR));
        mvc.perform(post("/api/alvaras/{id}/homologacao", id).with(csrf()).session(gestor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decisao\":\"APROVAR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("HOMOLOGADO"));

        // o mapa e publico: nao precisa de sessao
        mvc.perform(get("/api/unidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features[?(@.properties.nome=='Padaria Exemplo')]").exists());
    }

    @Test
    void parecerDesfavoravelNaoPodeSerAprovadoMasPodeSerIndeferido() throws Exception {
        MockHttpSession cidadao = login(novoUsuario(Papel.CIDADAO));
        long id = criarRascunho(cidadao);
        anexarPdf(cidadao, id).andExpect(status().isCreated());
        mvc.perform(post("/api/alvaras/{id}/enviar", id).with(csrf()).session(cidadao)).andExpect(status().isOk());
        MockHttpSession analista = login(novoUsuario(Papel.ANALISTA));
        mvc.perform(post("/api/alvaras/{id}/analise", id).with(csrf()).session(analista)
                        .contentType(MediaType.APPLICATION_JSON).content(UM_REPROVADO))
                .andExpect(status().isOk()).andExpect(jsonPath("$.parecer").value("DESFAVORAVEL"));

        MockHttpSession gestor = login(novoUsuario(Papel.GESTOR));
        mvc.perform(post("/api/alvaras/{id}/homologacao", id).with(csrf()).session(gestor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decisao\":\"APROVAR\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/alvaras/{id}/homologacao", id).with(csrf()).session(gestor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisao\":\"INDEFERIR\",\"motivo\":\"Debito em aberto\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INDEFERIDO"));
    }

    @Test
    void semSessaoRetorna401() throws Exception {
        mvc.perform(get("/api/alvaras")).andExpect(status().isUnauthorized());
    }

    @Test
    void escritaSemTokenCsrfRetorna403() throws Exception {
        MockHttpSession cidadao = login(novoUsuario(Papel.CIDADAO));
        mvc.perform(post("/api/alvaras").session(cidadao)
                        .contentType(MediaType.APPLICATION_JSON).content(RASCUNHO))
                .andExpect(status().isForbidden());
    }

    @Test
    void cidadaoNaoEnxergaAlvaraDeOutroCidadao() throws Exception {
        MockHttpSession dono = login(novoUsuario(Papel.CIDADAO));
        long id = criarRascunho(dono);
        MockHttpSession intruso = login(novoUsuario(Papel.CIDADAO));
        mvc.perform(get("/api/alvaras/{id}", id).session(intruso)).andExpect(status().isNotFound());
        mvc.perform(post("/api/alvaras/{id}/enviar", id).with(csrf()).session(intruso))
                .andExpect(status().isNotFound());
    }

    @Test
    void cidadaoNaoPodeAnalisar() throws Exception {
        MockHttpSession cidadao = login(novoUsuario(Papel.CIDADAO));
        long id = criarRascunho(cidadao);
        mvc.perform(post("/api/alvaras/{id}/analise", id).with(csrf()).session(cidadao)
                        .contentType(MediaType.APPLICATION_JSON).content(TODOS_ATENDIDOS))
                .andExpect(status().isForbidden());
    }

    @Test
    void arquivoComAssinaturaInvalidaEhRejeitado() throws Exception {
        MockHttpSession cidadao = login(novoUsuario(Papel.CIDADAO));
        long id = criarRascunho(cidadao);
        MockMultipartFile falso = new MockMultipartFile("arquivo", "falso.pdf", "application/pdf", "nao sou pdf".getBytes());
        mvc.perform(multipart("/api/alvaras/{id}/documentos", id).file(falso).with(csrf()).session(cidadao))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cadastroValidaDadosEImpedeEmailDuplicado() throws Exception {
        mvc.perform(post("/api/auth/registrar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"A\",\"email\":\"invalido\",\"senha\":\"curta\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.email").exists());
        String email = "dup-" + UUID.randomUUID() + "@exemplo.test";
        String corpo = "{\"nome\":\"Fulano\",\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}";
        mvc.perform(post("/api/auth/registrar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/registrar").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail", containsString("e-mail")));
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        String email = novoUsuario(Papel.CIDADAO);
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"errada-errada\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- apoio ----

    private String novoUsuario(Papel papel) {
        String email = papel.name().toLowerCase() + "-" + UUID.randomUUID() + "@exemplo.test";
        usuarios.save(new Usuario("Usuario " + papel, email, encoder.encode(SENHA), papel));
        return email;
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    private long criarRascunho(MockHttpSession sessao) throws Exception {
        String corpo = mvc.perform(post("/api/alvaras").with(csrf()).session(sessao)
                        .contentType(MediaType.APPLICATION_JSON).content(RASCUNHO))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions anexarPdf(MockHttpSession sessao, long id) throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("arquivo", "contrato.pdf", "application/pdf",
                "%PDF-1.4 conteudo ficticio".getBytes());
        return mvc.perform(multipart("/api/alvaras/{id}/documentos", id).file(pdf).with(csrf()).session(sessao));
    }
}

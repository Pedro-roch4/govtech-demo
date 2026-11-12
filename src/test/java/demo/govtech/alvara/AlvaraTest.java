package demo.govtech.alvara;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import demo.govtech.erro.ErroDeRegra;
import demo.govtech.unidade.Unidade;
import demo.govtech.usuario.Papel;
import demo.govtech.usuario.Usuario;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regras do fluxo, sem Spring e sem banco. */
class AlvaraTest {

    private static Alvara rascunhoCompleto() {
        Alvara a = new Alvara(new Usuario("Fulana", "fulana@exemplo.test", "hash", Papel.CIDADAO));
        a.atualizarRascunho("Padaria Exemplo", "Comercio de alimentos", "Rua Ficticia, 10", -11.30, -41.85);
        return a;
    }

    private static Alvara enviado() {
        Alvara a = rascunhoCompleto();
        a.enviar(1);
        return a;
    }

    private static List<ResultadoCriterio> criterios(Criterio naoAtendido) {
        List<ResultadoCriterio> lista = new ArrayList<>();
        for (Criterio c : Criterio.values()) {
            boolean ok = c != naoAtendido;
            lista.add(new ResultadoCriterio(c, ok, ok ? null : "Pendente"));
        }
        return lista;
    }

    @Test
    void naoEnviaSemDocumento() {
        assertThrows(ErroDeRegra.class, () -> rascunhoCompleto().enviar(0));
    }

    @Test
    void naoEnviaComDadosIncompletos() {
        Alvara a = new Alvara(new Usuario("Fulana", "fulana@exemplo.test", "hash", Papel.CIDADAO));
        a.atualizarRascunho("Padaria Exemplo", null, null, null, null);
        assertThrows(ErroDeRegra.class, () -> a.enviar(1));
    }

    @Test
    void naoEditaDepoisDeEnviado() {
        Alvara a = enviado();
        assertThrows(ErroDeRegra.class, () -> a.atualizarRascunho("Outro", "x", "y", 0.0, 0.0));
    }

    @Test
    void analiseExigeTodosOsCriterios() {
        Alvara a = enviado();
        List<ResultadoCriterio> incompleta = criterios(null).subList(0, 2);
        assertThrows(ErroDeRegra.class, () -> a.registrarAnalise(incompleta));
    }

    @Test
    void criterioNaoAtendidoExigeObservacao() {
        Alvara a = enviado();
        List<ResultadoCriterio> lista = criterios(null);
        lista.set(0, new ResultadoCriterio(Criterio.DOCUMENTACAO_COMPLETA, false, " "));
        assertThrows(ErroDeRegra.class, () -> a.registrarAnalise(lista));
    }

    @Test
    void analiseFavoravelPermiteHomologarEGeraUnidade() {
        Alvara a = enviado();
        a.registrarAnalise(criterios(null));
        assertEquals(Parecer.FAVORAVEL, a.getParecer());
        Unidade u = a.homologar();
        assertEquals(StatusAlvara.HOMOLOGADO, a.getStatus());
        assertEquals("Padaria Exemplo", u.getNome());
        assertEquals(-41.85, u.getLongitude());
    }

    @Test
    void parecerDesfavoravelSoPermiteIndeferir() {
        Alvara a = enviado();
        a.registrarAnalise(criterios(Criterio.SEM_PENDENCIAS_FISCAIS));
        assertEquals(Parecer.DESFAVORAVEL, a.getParecer());
        assertThrows(ErroDeRegra.class, a::homologar);
        assertThrows(ErroDeRegra.class, () -> a.indeferir(" "));
        a.indeferir("Pendencias fiscais");
        assertEquals(StatusAlvara.INDEFERIDO, a.getStatus());
    }

    @Test
    void naoHomologaSemAnalise() {
        assertThrows(ErroDeRegra.class, () -> enviado().homologar());
    }
}

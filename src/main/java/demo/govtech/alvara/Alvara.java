package demo.govtech.alvara;

import demo.govtech.erro.ErroDeRegra;
import demo.govtech.unidade.Unidade;
import demo.govtech.usuario.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Solicitacao de alvara. As regras do fluxo vivem aqui (e nao no controller),
 * para poderem ser testadas sem Spring nem banco:
 * RASCUNHO, ENVIADO, ANALISADO e, por fim, HOMOLOGADO ou INDEFERIDO.
 */
@Entity
@Table(name = "alvara")
public class Alvara {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id")
    private Usuario solicitante;

    @Column(name = "nome_estabelecimento")
    private String nomeEstabelecimento;

    private String atividade;

    private String endereco;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAlvara status = StatusAlvara.RASCUNHO;

    @Enumerated(EnumType.STRING)
    private Parecer parecer;

    @Column(name = "motivo_decisao")
    private String motivoDecisao;

    @OneToMany(mappedBy = "alvara", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnaliseCriterio> analise = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Alvara() {
    }

    public Alvara(Usuario solicitante) {
        this.solicitante = solicitante;
    }

    @PrePersist
    void aoCriar() {
        criadoEm = atualizadoEm = Instant.now();
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }

    /** Autosave: aceita dados parciais, a validacao completa so acontece em enviar(). */
    public void atualizarRascunho(String nomeEstabelecimento, String atividade, String endereco,
            Double latitude, Double longitude) {
        exigir(StatusAlvara.RASCUNHO, "So e possivel editar um rascunho.");
        this.nomeEstabelecimento = nomeEstabelecimento;
        this.atividade = atividade;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void enviar(long totalDocumentos) {
        exigir(StatusAlvara.RASCUNHO, "Apenas rascunhos podem ser enviados.");
        if (vazio(nomeEstabelecimento) || vazio(atividade) || vazio(endereco) || latitude == null || longitude == null) {
            throw ErroDeRegra.regra("Preencha nome, atividade, endereco e localizacao antes de enviar.");
        }
        if (totalDocumentos == 0) {
            throw ErroDeRegra.regra("Anexe ao menos um documento antes de enviar.");
        }
        status = StatusAlvara.ENVIADO;
    }

    /** Registra o resultado de todos os criterios; qualquer criterio nao atendido torna o parecer desfavoravel. */
    public void registrarAnalise(List<ResultadoCriterio> resultados) {
        exigir(StatusAlvara.ENVIADO, "Apenas alvaras enviados podem ser analisados.");
        Set<Criterio> avaliados = EnumSet.noneOf(Criterio.class);
        for (ResultadoCriterio r : resultados) {
            if (!avaliados.add(r.criterio())) {
                throw ErroDeRegra.regra("Criterio repetido: " + r.criterio());
            }
            if (!r.atendido() && vazio(r.observacao())) {
                throw ErroDeRegra.regra("Informe a observacao do criterio nao atendido: " + r.criterio());
            }
        }
        if (avaliados.size() != Criterio.values().length) {
            throw ErroDeRegra.regra("Avalie todos os criterios.");
        }
        resultados.forEach(r -> analise.add(new AnaliseCriterio(this, r.criterio(), r.atendido(), r.observacao())));
        parecer = resultados.stream().allMatch(ResultadoCriterio::atendido) ? Parecer.FAVORAVEL : Parecer.DESFAVORAVEL;
        status = StatusAlvara.ANALISADO;
    }

    /** Homologa e devolve a unidade a ser cadastrada no mapa. */
    public Unidade homologar() {
        exigir(StatusAlvara.ANALISADO, "Apenas alvaras analisados podem ser homologados.");
        if (parecer != Parecer.FAVORAVEL) {
            throw ErroDeRegra.regra("Parecer desfavoravel: o alvara so pode ser indeferido.");
        }
        status = StatusAlvara.HOMOLOGADO;
        return new Unidade(this, nomeEstabelecimento, endereco, latitude, longitude);
    }

    public void indeferir(String motivo) {
        exigir(StatusAlvara.ANALISADO, "Apenas alvaras analisados podem ser indeferidos.");
        if (vazio(motivo)) {
            throw ErroDeRegra.regra("Informe o motivo do indeferimento.");
        }
        motivoDecisao = motivo;
        status = StatusAlvara.INDEFERIDO;
    }

    private void exigir(StatusAlvara esperado, String mensagem) {
        if (status != esperado) {
            throw ErroDeRegra.regra(mensagem + " Status atual: " + status + ".");
        }
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    List<ResultadoCriterio> resultadosDaAnalise() {
        return analise.stream().map(AnaliseCriterio::comoResultado).toList();
    }

    public Long getId() {
        return id;
    }

    public Long getVersao() {
        return versao;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public String getNomeEstabelecimento() {
        return nomeEstabelecimento;
    }

    public String getAtividade() {
        return atividade;
    }

    public String getEndereco() {
        return endereco;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public StatusAlvara getStatus() {
        return status;
    }

    public Parecer getParecer() {
        return parecer;
    }

    public String getMotivoDecisao() {
        return motivoDecisao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}

package demo.govtech.alvara;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** ponytail: o arquivo fica em BYTEA (transacional e sem path traversal); para volume real, mover para object storage. */
@Entity
@Table(name = "documento")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alvara_id")
    private Alvara alvara;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long tamanho;

    @Column(nullable = false)
    private byte[] conteudo;

    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm = Instant.now();

    protected Documento() {
    }

    Documento(Alvara alvara, String nomeArquivo, String contentType, byte[] conteudo) {
        this.alvara = alvara;
        this.nomeArquivo = nomeArquivo;
        this.contentType = contentType;
        this.conteudo = conteudo;
        this.tamanho = conteudo.length;
    }

    public Long getId() {
        return id;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getConteudo() {
        return conteudo;
    }
}

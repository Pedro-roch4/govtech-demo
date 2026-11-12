package demo.govtech.alvara;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "analise_criterio")
public class AnaliseCriterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alvara_id")
    private Alvara alvara;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Criterio criterio;

    @Column(nullable = false)
    private boolean atendido;

    private String observacao;

    protected AnaliseCriterio() {
    }

    AnaliseCriterio(Alvara alvara, Criterio criterio, boolean atendido, String observacao) {
        this.alvara = alvara;
        this.criterio = criterio;
        this.atendido = atendido;
        this.observacao = observacao;
    }

    ResultadoCriterio comoResultado() {
        return new ResultadoCriterio(criterio, atendido, observacao);
    }
}

package demo.govtech.alvara;

import java.time.Instant;
import java.util.List;

public record AlvaraDto(
        Long id,
        Long versao,
        StatusAlvara status,
        Parecer parecer,
        String nomeEstabelecimento,
        String atividade,
        String endereco,
        Double latitude,
        Double longitude,
        String motivoDecisao,
        List<ResultadoCriterio> analise,
        Instant criadoEm,
        Instant atualizadoEm) {

    static AlvaraDto de(Alvara a) {
        return new AlvaraDto(a.getId(), a.getVersao(), a.getStatus(), a.getParecer(), a.getNomeEstabelecimento(),
                a.getAtividade(), a.getEndereco(), a.getLatitude(), a.getLongitude(), a.getMotivoDecisao(),
                a.resultadosDaAnalise(), a.getCriadoEm(), a.getAtualizadoEm());
    }
}

package demo.govtech.alvara;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/** Todos os campos sao opcionais no rascunho (autosave); a completude e exigida ao enviar. */
public record AlvaraRequest(
        @Size(max = 160) String nomeEstabelecimento,
        @Size(max = 160) String atividade,
        @Size(max = 255) String endereco,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
}

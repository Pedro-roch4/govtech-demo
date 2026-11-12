package demo.govtech.alvara;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResultadoCriterio(
        @NotNull Criterio criterio,
        @NotNull Boolean atendido,
        @Size(max = 500) String observacao) {
}

package demo.govtech.alvara;

import java.time.Instant;

/** Metadados de um documento (o conteudo so sai no download). */
public record DocumentoDto(Long id, String nomeArquivo, String contentType, long tamanho, Instant enviadoEm) {
}

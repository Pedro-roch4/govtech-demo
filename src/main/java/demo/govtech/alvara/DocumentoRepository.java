package demo.govtech.alvara;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    @Query("""
            select new demo.govtech.alvara.DocumentoDto(d.id, d.nomeArquivo, d.contentType, d.tamanho, d.enviadoEm)
            from Documento d where d.alvara.id = :alvaraId order by d.id""")
    List<DocumentoDto> listarPorAlvara(Long alvaraId);

    long countByAlvaraId(Long alvaraId);

    Optional<Documento> findByIdAndAlvaraId(Long id, Long alvaraId);
}

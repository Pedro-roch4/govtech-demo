package demo.govtech.alvara;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlvaraRepository extends JpaRepository<Alvara, Long> {

    List<Alvara> findBySolicitanteIdOrderByIdDesc(Long solicitanteId);

    List<Alvara> findByStatusOrderByIdDesc(StatusAlvara status);

    List<Alvara> findByStatusNotOrderByIdDesc(StatusAlvara status);
}

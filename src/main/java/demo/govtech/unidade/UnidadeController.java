package demo.govtech.unidade;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publico: devolve as unidades em GeoJSON, pronto para um mapa (Leaflet, MapLibre etc.). */
@RestController
@RequestMapping("/api/unidades")
public class UnidadeController {

    private final UnidadeRepository unidades;

    UnidadeController(UnidadeRepository unidades) {
        this.unidades = unidades;
    }

    @GetMapping
    Map<String, Object> listar() {
        List<Map<String, Object>> features = unidades.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "type", "Feature",
                        "geometry", Map.of("type", "Point", "coordinates", List.of(u.getLongitude(), u.getLatitude())),
                        "properties", Map.of("id", u.getId(), "nome", u.getNome(), "endereco", u.getEndereco())))
                .toList();
        return Map.of("type", "FeatureCollection", "features", features);
    }
}

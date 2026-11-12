package demo.govtech.alvara;

import demo.govtech.auth.UsuarioLogado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Quem pode chamar cada rota esta definido em SecurityConfig; a regra de acesso a cada alvara, no AlvaraService. */
@RestController
@RequestMapping("/api/alvaras")
public class AlvaraController {

    public record AnaliseRequest(@NotNull List<@Valid ResultadoCriterio> criterios) {
    }

    public enum Decisao { APROVAR, INDEFERIR }

    public record HomologacaoRequest(@NotNull Decisao decisao, @Size(max = 500) String motivo) {
    }

    private final AlvaraService service;

    AlvaraController(AlvaraService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AlvaraDto criar(@AuthenticationPrincipal UsuarioLogado u, @Valid @RequestBody AlvaraRequest r) {
        return service.criar(u, r);
    }

    @GetMapping
    List<AlvaraDto> listar(@AuthenticationPrincipal UsuarioLogado u, @RequestParam(required = false) StatusAlvara status) {
        return service.listar(u, status);
    }

    @GetMapping("/{id}")
    AlvaraDto obter(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id) {
        return service.obter(u, id);
    }

    @PutMapping("/{id}")
    AlvaraDto atualizar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id,
            @Valid @RequestBody AlvaraRequest r) {
        return service.atualizar(u, id, r);
    }

    @PostMapping(path = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    DocumentoDto anexar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id,
            @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return service.anexar(u, id, arquivo);
    }

    @GetMapping("/{id}/documentos")
    List<DocumentoDto> listarDocumentos(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id) {
        return service.listarDocumentos(u, id);
    }

    @GetMapping("/{id}/documentos/{documentoId}")
    ResponseEntity<byte[]> baixar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id,
            @PathVariable Long documentoId) {
        Documento d = service.baixar(u, id, documentoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.getNomeArquivo()).build().toString())
                .body(d.getConteudo());
    }

    @PostMapping("/{id}/enviar")
    AlvaraDto enviar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id) {
        return service.enviar(u, id);
    }

    @PostMapping("/{id}/analise")
    AlvaraDto analisar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id,
            @Valid @RequestBody AnaliseRequest r) {
        return service.analisar(u, id, r.criterios());
    }

    @PostMapping("/{id}/homologacao")
    AlvaraDto homologar(@AuthenticationPrincipal UsuarioLogado u, @PathVariable Long id,
            @Valid @RequestBody HomologacaoRequest r) {
        return r.decisao() == Decisao.APROVAR ? service.homologar(u, id) : service.indeferir(u, id, r.motivo());
    }
}

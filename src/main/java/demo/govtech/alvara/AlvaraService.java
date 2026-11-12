package demo.govtech.alvara;

import demo.govtech.auth.UsuarioLogado;
import demo.govtech.erro.ErroDeRegra;
import demo.govtech.unidade.UnidadeRepository;
import demo.govtech.usuario.Papel;
import demo.govtech.usuario.UsuarioRepository;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orquestra o fluxo e aplica a regra de acesso: cidadao so enxerga os proprios alvaras,
 * a equipe so enxerga o que ja foi enviado. Acesso indevido responde 404 (nao revela que existe).
 */
@Service
@Transactional
public class AlvaraService {

    static final int MAX_DOCUMENTOS = 10;
    private static final Set<String> TIPOS = Set.of("application/pdf", "image/png", "image/jpeg");

    private final AlvaraRepository alvaras;
    private final DocumentoRepository documentos;
    private final UnidadeRepository unidades;
    private final UsuarioRepository usuarios;

    AlvaraService(AlvaraRepository alvaras, DocumentoRepository documentos, UnidadeRepository unidades,
            UsuarioRepository usuarios) {
        this.alvaras = alvaras;
        this.documentos = documentos;
        this.unidades = unidades;
        this.usuarios = usuarios;
    }

    public AlvaraDto criar(UsuarioLogado u, AlvaraRequest r) {
        Alvara a = new Alvara(usuarios.getReferenceById(u.id()));
        a.atualizarRascunho(r.nomeEstabelecimento(), r.atividade(), r.endereco(), r.latitude(), r.longitude());
        return AlvaraDto.de(alvaras.save(a));
    }

    @Transactional(readOnly = true)
    public List<AlvaraDto> listar(UsuarioLogado u, StatusAlvara filtro) {
        List<Alvara> lista;
        if (u.papel() == Papel.CIDADAO) {
            lista = alvaras.findBySolicitanteIdOrderByIdDesc(u.id());
        } else if (filtro == null) {
            lista = alvaras.findByStatusNotOrderByIdDesc(StatusAlvara.RASCUNHO);
        } else {
            lista = filtro == StatusAlvara.RASCUNHO ? List.of() : alvaras.findByStatusOrderByIdDesc(filtro);
        }
        return lista.stream().map(AlvaraDto::de).toList();
    }

    @Transactional(readOnly = true)
    public AlvaraDto obter(UsuarioLogado u, Long id) {
        return AlvaraDto.de(visivel(u, id));
    }

    public AlvaraDto atualizar(UsuarioLogado u, Long id, AlvaraRequest r) {
        Alvara a = doSolicitante(u, id);
        a.atualizarRascunho(r.nomeEstabelecimento(), r.atividade(), r.endereco(), r.latitude(), r.longitude());
        return AlvaraDto.de(a);
    }

    public DocumentoDto anexar(UsuarioLogado u, Long id, MultipartFile arquivo) throws IOException {
        Alvara a = doSolicitante(u, id);
        if (a.getStatus() != StatusAlvara.RASCUNHO) {
            throw ErroDeRegra.regra("So e possivel anexar documentos a um rascunho.");
        }
        if (documentos.countByAlvaraId(id) >= MAX_DOCUMENTOS) {
            throw ErroDeRegra.regra("Limite de " + MAX_DOCUMENTOS + " documentos por alvara.");
        }
        byte[] bytes = arquivo.getBytes();
        String tipo = TIPOS.contains(arquivo.getContentType()) ? arquivo.getContentType() : null;
        // Nao confiamos no Content-Type informado pelo cliente: conferimos a assinatura do arquivo.
        if (bytes.length == 0 || tipo == null || !assinaturaConfere(tipo, bytes)) {
            throw ErroDeRegra.regra("Envie um arquivo PDF, PNG ou JPEG valido.");
        }
        String nome = StringUtils.getFilename(StringUtils.cleanPath(String.valueOf(arquivo.getOriginalFilename())));
        if (!StringUtils.hasText(nome)) {
            nome = "documento";
        }
        Documento d = documentos.save(new Documento(a, nome.length() > 255 ? nome.substring(0, 255) : nome, tipo, bytes));
        return new DocumentoDto(d.getId(), d.getNomeArquivo(), d.getContentType(), bytes.length, null);
    }

    @Transactional(readOnly = true)
    public List<DocumentoDto> listarDocumentos(UsuarioLogado u, Long id) {
        visivel(u, id);
        return documentos.listarPorAlvara(id);
    }

    @Transactional(readOnly = true)
    public Documento baixar(UsuarioLogado u, Long id, Long documentoId) {
        visivel(u, id);
        return documentos.findByIdAndAlvaraId(documentoId, id)
                .orElseThrow(() -> ErroDeRegra.naoEncontrado("Documento nao encontrado."));
    }

    public AlvaraDto enviar(UsuarioLogado u, Long id) {
        Alvara a = doSolicitante(u, id);
        a.enviar(documentos.countByAlvaraId(id));
        return AlvaraDto.de(a);
    }

    public AlvaraDto analisar(UsuarioLogado u, Long id, List<ResultadoCriterio> resultados) {
        Alvara a = visivel(u, id);
        a.registrarAnalise(resultados);
        return AlvaraDto.de(a);
    }

    public AlvaraDto homologar(UsuarioLogado u, Long id) {
        Alvara a = visivel(u, id);
        unidades.save(a.homologar());
        return AlvaraDto.de(a);
    }

    public AlvaraDto indeferir(UsuarioLogado u, Long id, String motivo) {
        Alvara a = visivel(u, id);
        a.indeferir(motivo);
        return AlvaraDto.de(a);
    }

    private Alvara visivel(UsuarioLogado u, Long id) {
        Alvara a = alvaras.findById(id).orElseThrow(AlvaraService::naoEncontrado);
        boolean dono = a.getSolicitante().getId().equals(u.id());
        boolean equipe = u.papel() != Papel.CIDADAO && a.getStatus() != StatusAlvara.RASCUNHO;
        if (!dono && !equipe) {
            throw naoEncontrado();
        }
        return a;
    }

    private Alvara doSolicitante(UsuarioLogado u, Long id) {
        Alvara a = alvaras.findById(id).orElseThrow(AlvaraService::naoEncontrado);
        if (!a.getSolicitante().getId().equals(u.id())) {
            throw naoEncontrado();
        }
        return a;
    }

    private static ErroDeRegra naoEncontrado() {
        return ErroDeRegra.naoEncontrado("Alvara nao encontrado.");
    }

    private static boolean assinaturaConfere(String tipo, byte[] b) {
        return switch (tipo) {
            case "application/pdf" -> comeca(b, 0x25, 0x50, 0x44, 0x46); // %PDF
            case "image/png" -> comeca(b, 0x89, 0x50, 0x4E, 0x47);
            default -> comeca(b, 0xFF, 0xD8, 0xFF); // JPEG
        };
    }

    private static boolean comeca(byte[] b, int... prefixo) {
        if (b.length < prefixo.length) {
            return false;
        }
        for (int i = 0; i < prefixo.length; i++) {
            if ((b[i] & 0xFF) != prefixo[i]) {
                return false;
            }
        }
        return true;
    }
}

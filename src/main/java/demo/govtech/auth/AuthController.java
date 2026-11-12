package demo.govtech.auth;

import demo.govtech.erro.ErroDeRegra;
import demo.govtech.usuario.Papel;
import demo.govtech.usuario.Usuario;
import demo.govtech.usuario.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Autenticacao por sessao: o servidor guarda a sessao e envia so o cookie JSESSIONID (HttpOnly). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegistroRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String senha) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String senha) {
    }

    public record UsuarioDto(Long id, String nome, String email, Papel papel) {
        static UsuarioDto de(UsuarioLogado u) {
            return new UsuarioDto(u.id(), u.nome(), u.getUsername(), u.papel());
        }
    }

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    AuthController(AuthenticationManager authenticationManager, UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.authenticationManager = authenticationManager;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    /** Autocadastro: sempre cria CIDADAO. Analistas e gestores nao se cadastram por aqui. */
    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    UsuarioDto registrar(@Valid @RequestBody RegistroRequest req) {
        String email = req.email().toLowerCase();
        if (usuarios.existsByEmail(email)) {
            throw ErroDeRegra.conflito("Ja existe um usuario com este e-mail.");
        }
        Usuario u = usuarios.save(new Usuario(req.nome(), email, encoder.encode(req.senha()), Papel.CIDADAO));
        return new UsuarioDto(u.getId(), u.getNome(), u.getEmail(), u.getPapel());
    }

    @PostMapping("/login")
    UsuarioDto login(@Valid @RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(req.email().toLowerCase(), req.senha()));
        if (request.getSession(false) != null) {
            request.changeSessionId(); // evita fixacao de sessao
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(auth);
        SecurityContextHolder.setContext(contexto);
        contextRepository.saveContext(contexto, request, response);
        return UsuarioDto.de((UsuarioLogado) auth.getPrincipal());
    }

    @GetMapping("/eu")
    UsuarioDto eu(@AuthenticationPrincipal UsuarioLogado usuario) {
        return UsuarioDto.de(usuario);
    }
}

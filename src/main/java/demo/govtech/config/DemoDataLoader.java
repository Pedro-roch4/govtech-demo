package demo.govtech.config;

import demo.govtech.usuario.Papel;
import demo.govtech.usuario.Usuario;
import demo.govtech.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria um usuario de cada papel (dados ficticios) para experimentar o fluxo. Ativo so com o perfil "demo". */
@Component
@Profile("demo")
class DemoDataLoader implements ApplicationRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String senha;

    DemoDataLoader(UsuarioRepository usuarios, PasswordEncoder encoder, @Value("${demo.senha}") String senha) {
        if (senha.isBlank()) {
            throw new IllegalStateException("Defina DEMO_SENHA (veja .env.example) para usar o perfil demo.");
        }
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        criar("Cidadao Exemplo", "cidadao@exemplo.test", Papel.CIDADAO);
        criar("Analista Exemplo", "analista@exemplo.test", Papel.ANALISTA);
        criar("Gestor Exemplo", "gestor@exemplo.test", Papel.GESTOR);
    }

    private void criar(String nome, String email, Papel papel) {
        if (!usuarios.existsByEmail(email)) {
            usuarios.save(new Usuario(nome, email, encoder.encode(senha), papel));
        }
    }
}

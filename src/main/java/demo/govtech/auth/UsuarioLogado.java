package demo.govtech.auth;

import demo.govtech.usuario.Papel;
import demo.govtech.usuario.Usuario;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Principal guardado na sessao: carrega o id e o papel para evitar consulta a cada requisicao. */
public class UsuarioLogado extends User {

    private final Long id;
    private final String nome;
    private final Papel papel;

    UsuarioLogado(Usuario u) {
        super(u.getEmail(), u.getSenhaHash(), List.of(new SimpleGrantedAuthority("ROLE_" + u.getPapel())));
        this.id = u.getId();
        this.nome = u.getNome();
        this.papel = u.getPapel();
    }

    public Long id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public Papel papel() {
        return papel;
    }
}

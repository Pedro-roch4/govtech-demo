package demo.govtech.auth;

import demo.govtech.usuario.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    UsuarioDetailsService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarios.findByEmail(email.toLowerCase())
                .map(UsuarioLogado::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
    }
}

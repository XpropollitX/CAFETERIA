package pe.edu.cafeterialasrosas.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.edu.cafeterialasrosas.entity.Usuario;

public final class UserPrincipal implements UserDetails {

    private final Long id;
    private final String correo;
    private final String passwordHash;
    private final boolean activo;
    private final List<GrantedAuthority> authorities;

    private UserPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.correo = usuario.getCorreo();
        this.passwordHash = usuario.getPasswordHash();
        this.activo = usuario.isActivo();
        this.authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre())
        );
    }

    public static UserPrincipal from(Usuario usuario) {
        return new UserPrincipal(usuario);
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}

package pe.edu.cafeterialasrosas.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorHandler securityErrorHandler;
    private final UserDetailsServiceImpl userDetailsService;
    private final String frontendUrl;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        SecurityErrorHandler securityErrorHandler,
        UserDetailsServiceImpl userDetailsService,
        @Value("${app.frontend-url}") String frontendUrl,
        @Value("${app.jwt.cookie-secure:false}") boolean cookieSecure,
        @Value("${app.jwt.cookie-same-site:Lax}") String cookieSameSite
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityErrorHandler = securityErrorHandler;
        this.userDetailsService = userDetailsService;
        this.frontendUrl = frontendUrl;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = validateSameSite(cookieSameSite, cookieSecure);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CookieCsrfTokenRepository csrfTokenRepository,
        AuthenticationManager authenticationManager
    ) throws Exception {
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        return http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(csrfRequestHandler)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler)
            )
            .authenticationManager(authenticationManager)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/product-images/**").permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/csrf"
                ).permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login"
                ).permitAll()
                .requestMatchers("/api/admin/**", "/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/api/pedidos/operativos/**").hasAnyRole("PERSONAL", "ADMIN")
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/pedidos/*/estado",
                    "/api/pedidos/*/cancelar",
                    "/api/pagos/*/verificar",
                    "/api/pagos/*/anular"
                ).hasAnyRole("PERSONAL", "ADMIN")
                .requestMatchers("/api/pagos/**").hasAnyRole("PERSONAL", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/categorias/**", "/api/productos/**")
                    .hasAnyRole("CLIENTE", "PERSONAL", "ADMIN")
                .requestMatchers("/api/categorias/**", "/api/productos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/pagos").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/pedidos/*/pagos")
                    .hasAnyRole("CLIENTE", "PERSONAL", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/pedidos/mis-pedidos").hasRole("CLIENTE")
                .requestMatchers("/api/pedidos/**").hasAnyRole("CLIENTE", "PERSONAL", "ADMIN")
                .anyRequest().authenticated()
            )
            .logout(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie
            .path("/")
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
        );
        return repository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    private String validateSameSite(String sameSite, boolean secure) {
        if (!List.of("Lax", "Strict", "None").contains(sameSite)) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE debe ser Lax, Strict o None.");
        }
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalStateException("SameSite=None requiere AUTH_COOKIE_SECURE=true.");
        }
        return sameSite;
    }
}

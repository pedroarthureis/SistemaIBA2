package com.iba.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Proteção administrativa opt-in sem trocar a API ou o frontend existentes.
 * Ative IBA_AUTH_ENABLED=true e defina IBA_ADMIN_USER / IBA_ADMIN_PASSWORD no host.
 * Obrigatório usar HTTPS em produção. Contas por usuário/permissão continuam pendentes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BasicAdminAccessFilter extends OncePerRequestFilter {
    private final boolean enabled = Boolean.parseBoolean(System.getenv().getOrDefault("IBA_AUTH_ENABLED", "false"));
    private final String adminUser = System.getenv("IBA_ADMIN_USER");
    private final String adminPassword = System.getenv("IBA_ADMIN_PASSWORD");
    private final String publicOrigin = System.getenv("IBA_PUBLIC_ORIGIN");

    public BasicAdminAccessFilter() {
        if (enabled && (adminUser == null || adminUser.isBlank() || adminPassword == null || adminPassword.length() < 12)) {
            throw new IllegalStateException("IBA_AUTH_ENABLED exige IBA_ADMIN_USER e IBA_ADMIN_PASSWORD de no mínimo 12 caracteres");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.setHeader("Cache-Control", "no-store");
        }
        if (!enabled) { chain.doFilter(request, response); return; }
        if (!validCredentials(request.getHeader("Authorization"))) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Sistema IBA - administracao\", charset=\"UTF-8\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Autenticação necessária");
            return;
        }
        // Defende contra requisições cross-site com credenciais armazenadas pelo navegador.
        String method = request.getMethod();
        if (!("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method))) {
            String fetchSite = request.getHeader("Sec-Fetch-Site");
            if ("cross-site".equalsIgnoreCase(fetchSite)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origem não autorizada"); return;
            }
            String origin = request.getHeader("Origin");
            // Aceita chamadas do próprio frontend quando o proxy reescreve Origin.
            // Sec-Fetch-Site é definido pelo navegador; o cabeçalho personalizado
            // impede que formulários cross-site usem esta exceção.
            boolean chamadaDoFrontend = "same-origin".equalsIgnoreCase(fetchSite)
                && "1".equals(request.getHeader("X-IBA-Request"))
                && request.getRequestURI().startsWith(request.getContextPath() + "/api/");

            if (origin != null && !sameOrigin(origin, request) && !chamadaDoFrontend) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origem não autorizada"); return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean validCredentials(String header) {
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(header.substring(6).trim());
            String credentials = new String(decoded, StandardCharsets.UTF_8);
            int colon = credentials.indexOf(':');
            if (colon < 1) return false;
            String user = credentials.substring(0, colon);
            String password = credentials.substring(colon + 1);
            return MessageDigest.isEqual(user.getBytes(StandardCharsets.UTF_8), adminUser.getBytes(StandardCharsets.UTF_8))
                && MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8), adminPassword.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean sameOrigin(String origin, HttpServletRequest request) {
        try {
            URI incoming = URI.create(origin);
            if (incoming.getHost() == null || incoming.getUserInfo() != null) return false;
            if (publicOrigin != null && !publicOrigin.isBlank()) {
                URI canonical = URI.create(publicOrigin);
                return incoming.getScheme().equalsIgnoreCase(canonical.getScheme())
                    && incoming.getHost().equalsIgnoreCase(canonical.getHost())
                    && normalizedPort(incoming) == normalizedPort(canonical);
            }
            // Dev local, sem proxy. Em Render configure IBA_PUBLIC_ORIGIN com a URL HTTPS.
            return incoming.getHost().equalsIgnoreCase(request.getServerName())
                && incoming.getScheme().equalsIgnoreCase(request.getScheme())
                && normalizedPort(incoming) == request.getServerPort();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private int normalizedPort(URI uri) {
        if (uri.getPort() != -1) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}

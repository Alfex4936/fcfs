package csw.fcfs.config.ratelimit;

import java.security.Principal;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = getRateLimitKey(request);
        if (key != null) {
            Bucket bucket = rateLimitService.resolveBucket(key);
            if (bucket != null && !bucket.tryConsume(1)) {
                response.setStatus(429);
                response.getWriter().write("Too many requests");
                return false;
            }
        }
        return true;
    }

    private String getRateLimitKey(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/claims")) {
            Principal principal = request.getUserPrincipal();
            if (principal != null && principal.getName() != null && !principal.getName().isEmpty()) {
                return "claims-" + principal.getName();
            }
            // Fallback for unauthenticated users
            return "claims-" + getClientIp(request);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}

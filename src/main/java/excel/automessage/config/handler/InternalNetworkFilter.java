package excel.automessage.config.handler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Slf4j
public class InternalNetworkFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String remoteAddr = request.getRemoteAddr();
        String requestURI = request.getRequestURI();
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        // 로그인 URL 제외
        if (requestURI.contains("/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isInternalNetwork(remoteAddr)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "외부 접근 차단");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isInternalNetwork(String ip) {
        return ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.") || ip.startsWith("127.");
    }

}


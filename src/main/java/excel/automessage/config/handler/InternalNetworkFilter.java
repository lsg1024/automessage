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

        String remoteAdd2 = getClientIp(request);

        log.info("접속 ip {} {}", remoteAddr, remoteAdd2);

        // 로그인 URL 제외
        if (requestURI.contains("/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isInternalNetwork(remoteAddr)) {
            response.sendRedirect("/login");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isInternalNetwork(String ip) {
        return ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.") || ip.startsWith("127.") || ip.equals("0:0:0:0:0:0:0:1");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}


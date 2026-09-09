package com.creatorhub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Component
public class FooterInjectionFilter extends OncePerRequestFilter {

    private String footer;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        BufferedResponse wrapped = new BufferedResponse(response);
        filterChain.doFilter(request, wrapped);

        String contentType = wrapped.getContentType();
        byte[] body = wrapped.body();

        if (contentType != null && contentType.toLowerCase().contains("text/html")) {
            String html = new String(body, StandardCharsets.UTF_8);
            html = html.replaceAll("(?is)<footer\\b[^>]*>.*?</footer>", "");

            String sharedFooter = loadFooter();
            int bodyEnd = html.toLowerCase().lastIndexOf("</body>");
            if (bodyEnd >= 0) {
                html = html.substring(0, bodyEnd) + sharedFooter + "\n" + html.substring(bodyEnd);
            } else {
                html += sharedFooter;
            }

            byte[] output = html.getBytes(StandardCharsets.UTF_8);
            response.setContentType("text/html;charset=UTF-8");
            response.setContentLength(output.length);
            response.getOutputStream().write(output);
        } else {
            response.setContentType(contentType);
            response.setContentLength(body.length);
            response.getOutputStream().write(body);
        }
    }

    private String loadFooter() throws IOException {
        if (footer == null) {
            footer = new String(new ClassPathResource("static/footer.html").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
        return footer;
    }

    private static class BufferedResponse extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private PrintWriter writer;

        BufferedResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
                @Override public void write(int b) { buffer.write(b); }
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener listener) { }
            };
        }

        @Override
        public PrintWriter getWriter() {
            if (writer == null) {
                writer = new PrintWriter(buffer, true, StandardCharsets.UTF_8);
            }
            return writer;
        }

        byte[] body() {
            if (writer != null) writer.flush();
            return buffer.toByteArray();
        }
    }
}

package ru.rbpo.museum.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Пишет ответ об ошибке безопасности в формате application/problem+json.
 * Тексты фиксированные: в ответ не попадают ни детали исключений, ни данные из запроса.
 */
final class ProblemResponses {

    private ProblemResponses() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"title\":\"" + status.getReasonPhrase() + "\",\"status\":" + status.value()
                + ",\"code\":\"" + code + "\",\"detail\":\"" + detail + "\"}");
    }
}

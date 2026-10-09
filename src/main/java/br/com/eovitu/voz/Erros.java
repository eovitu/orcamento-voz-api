package br.com.eovitu.voz;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@RestControllerAdvice
public class Erros {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class, MissingServletRequestPartException.class,
        MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String,String>> entrada(Exception erro) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Entrada invalida"));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String,String>> tamanho(Exception erro) {
        return ResponseEntity.status(413).body(Map.of("erro", "Arquivo muito grande"));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,String>> indisponivel(Exception erro) {
        return ResponseEntity.status(503).body(Map.of("erro", "IA indisponivel: confira configuracao ou tente novamente"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,String>> interno(Exception erro) {
        return ResponseEntity.status(500).body(Map.of("erro", "Falha interna"));
    }
}

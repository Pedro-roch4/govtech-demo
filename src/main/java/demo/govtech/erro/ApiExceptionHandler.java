package demo.govtech.erro;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Traduz erros para ProblemDetail (RFC 9457), sem vazar detalhes internos. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ErroDeRegra.class)
    ProblemDetail regra(ErroDeRegra e) {
        HttpStatus status = switch (e.tipo()) {
            case NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CONFLITO -> HttpStatus.CONFLICT;
            case REGRA_DE_NEGOCIO -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ProblemDetail.forStatusAndDetail(status, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail credenciais(AuthenticationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais invalidas.");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concorrencia(ObjectOptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "O registro foi alterado por outra requisicao. Recarregue e tente de novo.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados invalidos.");
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        pd.setProperty("campos", campos);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }
}

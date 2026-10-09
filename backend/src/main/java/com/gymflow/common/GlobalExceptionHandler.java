package com.gymflow.common;

import com.gymflow.assistant.AssistantNotAllowedException;
import com.gymflow.assistant.ai.AssistantFailedException;
import com.gymflow.assistant.ai.AssistantUnavailableException;
import com.gymflow.auth.EmailAlreadyRegisteredException;
import com.gymflow.auth.InvalidCredentialsException;
import com.gymflow.booking.BookingNotAllowedException;
import com.gymflow.booking.BookingNotFoundException;
import com.gymflow.booking.SessionNotFoundException;
import com.gymflow.contact.TooManyContactAttemptsException;
import com.gymflow.plan.MembershipNotFoundException;
import com.gymflow.workout.WorkoutNotAllowedException;
import com.gymflow.workout.ai.TrainerFailedException;
import com.gymflow.workout.ai.TrainerUnavailableException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores con formato uniforme (RFC 9457 / ProblemDetail). CLAUDE.md §Convenciones. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Correo ya registrado");
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Credenciales inválidas");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem =
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los datos del formulario");
        problem.setTitle("Error de validación");
        problem.setProperty(
            "errors",
            ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, fe -> fe.getDefaultMessage(), (a, b) -> a)));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BookingNotAllowedException.class)
    public ProblemDetail handleBookingNotAllowed(BookingNotAllowedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("No se puede completar la reserva");
        return problem;
    }

    @ExceptionHandler({SessionNotFoundException.class, BookingNotFoundException.class, MembershipNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(WorkoutNotAllowedException.class)
    public ProblemDetail handleWorkoutNotAllowed(WorkoutNotAllowedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Entrenador con IA no disponible para tu tarifa");
        return problem;
    }

    @ExceptionHandler(TrainerUnavailableException.class)
    public ProblemDetail handleTrainerUnavailable(TrainerUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(TrainerFailedException.class)
    public ProblemDetail handleTrainerFailed(TrainerFailedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(AssistantNotAllowedException.class)
    public ProblemDetail handleAssistantNotAllowed(AssistantNotAllowedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Asistente con IA no disponible para tu tarifa");
        return problem;
    }

    @ExceptionHandler(AssistantUnavailableException.class)
    public ProblemDetail handleAssistantUnavailable(AssistantUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(AssistantFailedException.class)
    public ProblemDetail handleAssistantFailed(AssistantFailedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(TooManyContactAttemptsException.class)
    public ProblemDetail handleTooManyContactAttempts(TooManyContactAttemptsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setTitle("Demasiados envíos");
        return problem;
    }
}

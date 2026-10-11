package com.gymflow.workout;

import com.gymflow.common.TenantContext;
import com.gymflow.workout.dto.GenerateWorkoutRequest;
import com.gymflow.workout.dto.LogSetRequest;
import com.gymflow.workout.dto.StreakResponse;
import com.gymflow.workout.dto.TodayWorkoutResponse;
import com.gymflow.workout.dto.WeekWorkoutResponse;
import com.gymflow.workout.dto.WeeklyPlan;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping("/workout/today")
    public TodayWorkoutResponse today(Authentication authentication) {
        return workoutService.getToday(TenantContext.require(), userId(authentication));
    }

    @GetMapping("/workout/week")
    public WeekWorkoutResponse week(Authentication authentication) {
        return workoutService.getWeek(TenantContext.require(), userId(authentication));
    }

    @GetMapping("/streak")
    public StreakResponse streak(Authentication authentication) {
        return workoutService.getStreak(TenantContext.require(), userId(authentication));
    }

    @PostMapping("/workout/logs")
    public ResponseEntity<Void> logSet(Authentication authentication, @Valid @RequestBody LogSetRequest request) {
        workoutService.logSet(TenantContext.require(), userId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/workout/generate")
    public WeeklyPlan generate(Authentication authentication, @Valid @RequestBody GenerateWorkoutRequest request) {
        return workoutService.generate(TenantContext.require(), userId(authentication), request);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}

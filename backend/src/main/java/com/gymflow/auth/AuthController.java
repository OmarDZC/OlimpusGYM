package com.gymflow.auth;

import com.gymflow.auth.dto.AuthResponse;
import com.gymflow.auth.dto.DemoLoginRequest;
import com.gymflow.auth.dto.LoginRequest;
import com.gymflow.auth.dto.MeResponse;
import com.gymflow.auth.dto.RegisterRequest;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /** Trozo 9f: las dos cuentas públicas que usan los botones "entrar como demo" en
     * /acceso, solo disponibles con el perfil "demo" activo. El OWNER real de la demo
     * (DEMO_OWNER_EMAIL/DEMO_OWNER_PASSWORD) nunca aparece aquí ni en ningún botón. */
    private static final String DEMO_STAFF_EMAIL = "marta@olympusgym.test";
    private static final String DEMO_MEMBER_EMAIL = "carlos@olympusgym.test";

    private final TenantRepository tenants;
    private final UserRepository users;
    private final PlanRepository plans;
    private final MembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Environment environment;

    public AuthController(
            TenantRepository tenants,
            UserRepository users,
            PlanRepository plans,
            MembershipRepository memberships,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            Environment environment) {
        this.tenants = tenants;
        this.users = users;
        this.plans = plans;
        this.memberships = memberships;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.environment = environment;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        Tenant tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseThrow(() -> new IllegalStateException("No existe el tenant por defecto: revisa el seed"));

        String email = request.email().trim().toLowerCase();
        if (users.existsByTenantIdAndEmailIgnoreCase(tenant.getId(), email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        Plan plan = plans.findByTenantIdAndCode(tenant.getId(), request.planCode())
            .orElseThrow(() -> new IllegalArgumentException("Tarifa no disponible: " + request.planCode()));

        User user = new User(
            tenant.getId(),
            email,
            passwordEncoder.encode(request.password()),
            request.fullName().trim(),
            Role.MEMBER);
        users.save(user);

        // El cobro real llega en una fase posterior (LOGIC.md §10): de momento la membresía
        // queda activa desde el alta, sin pasarela de pago.
        memberships.save(new Membership(tenant.getId(), user.getId(), plan.getId(), MembershipStatus.ACTIVE, Instant.now()));

        String token = jwtService.issue(user.getId(), tenant.getId(), user.getRole().name(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.from(user, token));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.issue(user.getId(), user.getTenantId(), user.getRole().name(), user.getEmail());
        return AuthResponse.from(user, token);
    }

    @PostMapping("/demo-login")
    public ResponseEntity<AuthResponse> demoLogin(@Valid @RequestBody DemoLoginRequest request) {
        if (!environment.matchesProfiles("demo")) {
            // Ni 403 ni un mensaje de "no disponible": en producción este endpoint no debe
            // dar ninguna pista de que existe.
            return ResponseEntity.notFound().build();
        }
        String email = "STAFF".equals(request.role()) ? DEMO_STAFF_EMAIL : DEMO_MEMBER_EMAIL;
        User user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalStateException("La cuenta de demo " + email + " no existe: revisa el seed"));

        String token = jwtService.issue(user.getId(), user.getTenantId(), user.getRole().name(), user.getEmail());
        return ResponseEntity.ok(AuthResponse.from(user, token));
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        User user = users.findById(userId)
            .orElseThrow(() -> new IllegalStateException("El usuario del token ya no existe"));
        return MeResponse.from(user);
    }
}

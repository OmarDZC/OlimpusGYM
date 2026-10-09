package com.gymflow.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Reglas de reserva de LOGIC.md §5, incluido el test de concurrencia de aforo exigido:
 * 50 hilos reservando 20 plazas ⇒ exactamente 20 BOOKED. Se omite automáticamente sin Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class BookingServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // El test de concurrencia abre hasta 50 transacciones a la vez: con el pool por
        // defecto (10) se sigue serializando bien por el bloqueo de fila, solo más lento.
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "30");
    }

    @Autowired
    BookingService bookingService;

    @Autowired
    TenantRepository tenants;

    @Autowired
    UserRepository users;

    @Autowired
    PlanRepository plans;

    @Autowired
    MembershipRepository memberships;

    @Autowired
    ClassSessionRepository sessions;

    @Autowired
    BookingRepository bookings;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    Clock clock;

    private Tenant tenant;
    private Plan unlimitedPlan;
    private Plan limitedPlan;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        tenant = tenants.save(new Tenant("Tenant " + unique, "tenant-" + unique));
        unlimitedPlan = plans.save(new Plan(tenant.getId(), PlanCode.COMPLETA, "Completa", 5900, null, 14, true, true));
        limitedPlan = plans.save(new Plan(tenant.getId(), PlanCode.BASICA, "Basica", 3900, 4, 7, false, false));
        // Instantánea fija de "ahora": de sobra para todos los tests existentes (usan
        // desplazamientos de horas/días), y los tests con reloj controlado la sobrescriben.
        Mockito.when(clock.instant()).thenReturn(Instant.now());
    }

    private User createMember(Plan plan) {
        User user = new User(
                tenant.getId(), "user-" + UUID.randomUUID() + "@test.local",
                passwordEncoder.encode("ClaveDePrueba123!"), "Socio de prueba", Role.MEMBER);
        users.save(user);
        memberships.save(
                new Membership(tenant.getId(), user.getId(), plan.getId(), MembershipStatus.ACTIVE, Instant.now()));
        return user;
    }

    private ClassSession createSession(Instant startsAt, int capacity) {
        ClassSession session = new ClassSession(
                tenant.getId(), "Clase de prueba", "Test", null, null, "Sala 1",
                startsAt, startsAt.plus(60, ChronoUnit.MINUTES), capacity);
        return sessions.save(session);
    }

    @Test
    void fiftyThreadsTwentySeats_exactlyTwentyBooked() throws InterruptedException {
        ClassSession session = createSession(Instant.now().plus(2, ChronoUnit.DAYS), 20);
        List<User> members = IntStream.range(0, 50).mapToObj(i -> createMember(unlimitedPlan)).toList();

        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch ready = new CountDownLatch(50);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(50);

        for (User member : members) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingService.book(tenant.getId(), member.getId(), session.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException ignored) {
                    // Algunos hilos pueden chocar con una condición de negocio inesperada;
                    // lo que importa para este test es el recuento final de BOOKED/WAITLIST.
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        boolean finished = done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        org.junit.jupiter.api.Assertions.assertTrue(finished, "Los 50 hilos no terminaron a tiempo");

        long bookedCount = bookings.countBySessionIdAndStatus(session.getId(), BookingStatus.BOOKED);
        long waitlistCount = bookings.countBySessionIdAndStatus(session.getId(), BookingStatus.WAITLIST);

        assertEquals(20, bookedCount, "Deben quedar exactamente 20 reservas confirmadas, ni una más");
        assertEquals(30, waitlistCount, "Las otras 30 deben quedar en lista de espera, no perderse");
    }

    @Test
    void bookingTwiceReturnsTheSameBooking_idempotent() {
        ClassSession session = createSession(Instant.now().plus(1, ChronoUnit.DAYS), 10);
        User member = createMember(unlimitedPlan);

        Booking first = bookingService.book(tenant.getId(), member.getId(), session.getId());
        Booking second = bookingService.book(tenant.getId(), member.getId(), session.getId());

        assertEquals(first.getId(), second.getId());
        assertEquals(1, bookings.countBySessionIdAndStatus(session.getId(), BookingStatus.BOOKED));
    }

    @Test
    void cannotBookWithoutActiveMembership() {
        ClassSession session = createSession(Instant.now().plus(1, ChronoUnit.DAYS), 10);
        User user = new User(
                tenant.getId(), "sin-cuota@test.local", passwordEncoder.encode("x"), "Sin Cuota", Role.MEMBER);
        users.save(user);

        assertThrows(
                BookingNotAllowedException.class,
                () -> bookingService.book(tenant.getId(), user.getId(), session.getId()));
    }

    @Test
    void cannotBookOutsidePlanBookingWindow() {
        // limitedPlan tiene ventana de 7 días: una sesión a 10 días no se puede reservar aún.
        ClassSession farSession = createSession(Instant.now().plus(10, ChronoUnit.DAYS), 10);
        User member = createMember(limitedPlan);

        assertThrows(
                BookingNotAllowedException.class,
                () -> bookingService.book(tenant.getId(), member.getId(), farSession.getId()));
    }

    @Test
    void cannotBookTwoOverlappingSessions() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        ClassSession sessionA = createSession(start, 10);
        ClassSession sessionB = createSession(start.plus(30, ChronoUnit.MINUTES), 10); // se solapa
        User member = createMember(unlimitedPlan);

        bookingService.book(tenant.getId(), member.getId(), sessionA.getId());

        assertThrows(
                BookingNotAllowedException.class,
                () -> bookingService.book(tenant.getId(), member.getId(), sessionB.getId()));
    }

    @Test
    void cannotBookPastOrCancelledSession() {
        ClassSession past = createSession(Instant.now().minus(1, ChronoUnit.HOURS), 10);
        User member = createMember(unlimitedPlan);

        assertThrows(
                BookingNotAllowedException.class,
                () -> bookingService.book(tenant.getId(), member.getId(), past.getId()));
    }

    @Test
    void respectsMonthlyQuotaOfThePlan() {
        // Mismo día, horas distintas: evita cualquier problema de borde de fin de mes.
        User member = createMember(limitedPlan); // 4 clases/mes
        Instant base = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS).plus(2, ChronoUnit.HOURS);

        for (int i = 0; i < 4; i++) {
            ClassSession s = createSession(base.plus(i * 3L, ChronoUnit.HOURS), 10);
            bookingService.book(tenant.getId(), member.getId(), s.getId());
        }

        ClassSession fifth = createSession(base.plus(4 * 3L, ChronoUnit.HOURS), 10);
        assertThrows(
                BookingNotAllowedException.class,
                () -> bookingService.book(tenant.getId(), member.getId(), fifth.getId()));
    }

    @Test
    void cannotCancelWithinTwoHoursOfStart() {
        ClassSession soon = createSession(Instant.now().plus(1, ChronoUnit.HOURS), 10);
        User member = createMember(unlimitedPlan);
        Booking booking = bookingService.book(tenant.getId(), member.getId(), soon.getId());

        assertThrows(
                CancellationWindowClosedException.class,
                () -> bookingService.cancel(tenant.getId(), member.getId(), booking.getId()));
    }

    @Test
    void cancellingAConfirmedBookingPromotesTheFirstOnTheWaitlist() {
        ClassSession session = createSession(Instant.now().plus(1, ChronoUnit.DAYS), 1);
        User first = createMember(unlimitedPlan);
        User second = createMember(unlimitedPlan);

        Booking firstBooking = bookingService.book(tenant.getId(), first.getId(), session.getId());
        Booking secondBooking = bookingService.book(tenant.getId(), second.getId(), session.getId());

        assertEquals(BookingStatus.BOOKED, firstBooking.getStatus());
        assertEquals(BookingStatus.WAITLIST, secondBooking.getStatus());
        assertEquals(1, secondBooking.getWaitlistPosition());

        bookingService.cancel(tenant.getId(), first.getId(), firstBooking.getId());

        Booking promoted = bookings.findById(secondBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.BOOKED, promoted.getStatus());
        assertNull(promoted.getWaitlistPosition());
    }

    @Test
    void waitlistReordersWhenSomeoneInTheMiddleCancels() {
        ClassSession session = createSession(Instant.now().plus(1, ChronoUnit.DAYS), 1);
        User booked = createMember(unlimitedPlan);
        User waitA = createMember(unlimitedPlan);
        User waitB = createMember(unlimitedPlan);
        User waitC = createMember(unlimitedPlan);

        bookingService.book(tenant.getId(), booked.getId(), session.getId());
        Booking bookingA = bookingService.book(tenant.getId(), waitA.getId(), session.getId());
        Booking bookingB = bookingService.book(tenant.getId(), waitB.getId(), session.getId());
        Booking bookingC = bookingService.book(tenant.getId(), waitC.getId(), session.getId());

        assertEquals(1, bookingA.getWaitlistPosition());
        assertEquals(2, bookingB.getWaitlistPosition());
        assertEquals(3, bookingC.getWaitlistPosition());

        bookingService.cancel(tenant.getId(), waitA.getId(), bookingA.getId());

        assertEquals(1, bookings.findById(bookingB.getId()).orElseThrow().getWaitlistPosition());
        assertEquals(2, bookings.findById(bookingC.getId()).orElseThrow().getWaitlistPosition());
    }

    @Test
    void cannotCancelSomeoneElsesBooking() {
        ClassSession session = createSession(Instant.now().plus(1, ChronoUnit.DAYS), 10);
        User owner = createMember(unlimitedPlan);
        User intruder = createMember(unlimitedPlan);
        Booking booking = bookingService.book(tenant.getId(), owner.getId(), session.getId());

        assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.cancel(tenant.getId(), intruder.getId(), booking.getId()));
    }

    // ---- Clases pasadas, con reloj controlado (arreglo del fallo de "próxima clase") ----

    @Test
    void cannotCancelAClassThatAlreadyStarted_givesADifferentMessageThanTheTwoHourWindow() {
        Instant referenceNow = Instant.parse("2026-10-09T12:00:00Z");
        Instant sessionStart = referenceNow.minus(1, ChronoUnit.HOURS); // empezó hace una hora
        ClassSession session = createSession(sessionStart, 10);
        User member = createMember(unlimitedPlan);

        // La reserva se creó de verdad cuando la clase todavía era futura (igual que en el bug
        // real: nadie canceló a tiempo y la clase pasó sola). Se inserta directamente para no
        // depender de encadenar dos "ahora" distintos en el mismo test.
        Booking booking = bookings.save(new Booking(tenant.getId(), session.getId(), member.getId(), BookingStatus.BOOKED, null));

        Mockito.when(clock.instant()).thenReturn(referenceNow);

        SessionAlreadyStartedException ex = assertThrows(
                SessionAlreadyStartedException.class,
                () -> bookingService.cancel(tenant.getId(), member.getId(), booking.getId()));
        assertEquals("Esta clase ya ha empezado, no se puede cancelar.", ex.getMessage());
    }

    @Test
    void canCancelAClassThatStartsInThreeHours() {
        Instant referenceNow = Instant.parse("2026-10-09T12:00:00Z");
        ClassSession session = createSession(referenceNow.plus(3, ChronoUnit.HOURS), 10);
        User member = createMember(unlimitedPlan);
        Mockito.when(clock.instant()).thenReturn(referenceNow);
        Booking booking = bookingService.book(tenant.getId(), member.getId(), session.getId());

        bookingService.cancel(tenant.getId(), member.getId(), booking.getId());

        assertEquals(BookingStatus.CANCELLED, bookings.findById(booking.getId()).orElseThrow().getStatus());
    }

    @Test
    void cannotBookAClassThatAlreadyStarted_withControlledClock() {
        Instant referenceNow = Instant.parse("2026-10-09T12:00:00Z");
        ClassSession yesterday = createSession(referenceNow.minus(1, ChronoUnit.DAYS), 10);
        User member = createMember(unlimitedPlan);
        Mockito.when(clock.instant()).thenReturn(referenceNow);

        assertThrows(
                BookingNotAllowedException.class,
                () -> bookingService.book(tenant.getId(), member.getId(), yesterday.getId()));
    }

    @Test
    void staffCancellingAnAlreadyStartedSessionDoesNotPromoteTheWaitlist() {
        Instant referenceNow = Instant.parse("2026-10-09T12:00:00Z");
        Instant future = referenceNow.plus(1, ChronoUnit.DAYS);
        ClassSession session = createSession(future, 1);
        User booked = createMember(unlimitedPlan);
        User waiting = createMember(unlimitedPlan);

        Mockito.when(clock.instant()).thenReturn(referenceNow);
        Booking bookedBooking = bookingService.book(tenant.getId(), booked.getId(), session.getId());
        Booking waitlistBooking = bookingService.book(tenant.getId(), waiting.getId(), session.getId());
        assertEquals(BookingStatus.WAITLIST, waitlistBooking.getStatus());

        // La clase ya ha pasado cuando staff decide (tarde) cancelar la reserva confirmada.
        Mockito.when(clock.instant()).thenReturn(future.plus(2, ChronoUnit.HOURS));
        bookingService.cancelAsStaff(tenant.getId(), bookedBooking.getId());

        Booking stillWaiting = bookings.findById(waitlistBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.WAITLIST, stillWaiting.getStatus(), "no se debe promocionar a una clase ya empezada");
    }

    @Test
    void handlesTheEuropeMadridDaylightSavingChangeCorrectly() {
        // Último domingo de octubre de 2026: a las 03:00 hora de Madrid (CEST, UTC+2) los
        // relojes retroceden a las 02:00 (CET, UTC+1). Una sesión a las 02:30 hora local justo
        // ese día es ambigua en hora local, pero en UTC (lo que guarda y compara el backend)
        // sigue siendo un instante exacto sin ambigüedad: la comparación no se entera del cambio.
        Instant sessionStartUtc = Instant.parse("2026-10-25T01:30:00Z"); // 03:30 CEST / tras el cambio, 02:30 CET
        ClassSession session = createSession(sessionStartUtc, 10);
        User member = createMember(unlimitedPlan);

        Mockito.when(clock.instant()).thenReturn(sessionStartUtc.minus(3, ChronoUnit.HOURS));
        Booking booking = bookingService.book(tenant.getId(), member.getId(), session.getId());
        bookingService.cancel(tenant.getId(), member.getId(), booking.getId());
        assertEquals(BookingStatus.CANCELLED, bookings.findById(booking.getId()).orElseThrow().getStatus());

        User member2 = createMember(unlimitedPlan);
        Mockito.when(clock.instant()).thenReturn(sessionStartUtc.plus(10, ChronoUnit.MINUTES));
        assertThrows(
                BookingNotAllowedException.class,
                () -> bookingService.book(tenant.getId(), member2.getId(), session.getId()));
    }
}

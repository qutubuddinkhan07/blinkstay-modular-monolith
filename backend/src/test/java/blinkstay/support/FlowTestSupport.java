package blinkstay.support;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.repository.UserRepository;
import blinkstay.auth.util.JWTUtil;
import blinkstay.booking.entities.Booking;
import blinkstay.booking.repository.BookingRepository;
import blinkstay.earnings.entities.EarningsEntry;
import blinkstay.earnings.repository.EarningsRepository;
import blinkstay.listing.entities.Listing;
import blinkstay.listing.enums.ListingCategory;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.listing.repository.ListingRepository;
import blinkstay.payment.repository.PaymentRepository;
import blinkstay.room.entities.ListingRoom;
import blinkstay.room.enums.RoomCategory;
import blinkstay.room.repository.RoomRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BooleanSupplier;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helpers for whole-flow tests (@IntegrationTest): real users, listing and room in Postgres,
 * and HTTP calls made the way the React app makes them (login cookie + XSRF cookie and header).
 * Nothing is rolled back: every test creates its own users, so tests never share data.
 */
public abstract class FlowTestSupport {

    protected static final String AUTH_COOKIE = "BLINKSTAY_TOKEN";
    private static final String CSRF_TOKEN = "flow-test-csrf-token";

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected JWTUtil jwtUtil;
    @Autowired
    protected UserRepository userRepo;
    @Autowired
    protected ListingRepository listingRepo;
    @Autowired
    protected RoomRepository roomRepo;
    @Autowired
    protected BookingRepository bookingRepo;
    @Autowired
    protected PaymentRepository paymentRepo;
    @Autowired
    protected EarningsRepository earningsRepo;

    // ---------- test data ----------

    protected User newUser(UserRole role) {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        return userRepo.save(User.builder()
                .username(role.name().toLowerCase() + "-" + tag)
                .email(tag + "@flow.test")
                .password("not-used-in-flow-tests")
                .roles(new HashSet<>(Set.of(role)))
                .isActive(true)
                .build());
    }

    /**
     * A published listing owned by the host, with one DOUBLE room type: 2000.00 per night.
     */
    protected ListingRoom newPublishedListingWithRoom(User host, int totalRooms) {
        Listing listing = listingRepo.save(Listing.builder()
                .title("Flow Test Stay").location("Panaji, Goa").country("India")
                .description("A listing created by an automated flow test")
                .category(ListingCategory.HOTEL).status(ListingStatus.PUBLISHED)
                .managerId(host.getId()).amenities(new ArrayList<>())
                .build());
        return roomRepo.save(ListingRoom.builder()
                .listingId(listing.getId()).roomType(RoomCategory.DOUBLE)
                .price(new BigDecimal("2000.00")).totalRooms(totalRooms).maxGuests(2)
                .build());
    }

    protected static LocalDate inDays(int days) {
        return TestDates.today().plusDays(days);
    }

    // ---------- requests, like the browser sends them ----------

    /**
     * Logged in as this user, with a matching XSRF cookie and header.
     */
    protected RequestPostProcessor as(User user) {
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        Cookie login = new Cookie(AUTH_COOKIE, jwtUtil.generateToken(user.getId().toString(), roles));
        return request -> {
            request.setCookies(login, new Cookie("XSRF-TOKEN", CSRF_TOKEN));
            request.addHeader("X-XSRF-TOKEN", CSRF_TOKEN);
            return request;
        };
    }

    protected ResultActions createBooking(User guest, ListingRoom room, LocalDate checkIn, LocalDate checkOut)
            throws Exception {
        String body = """
                {"roomId":"%s","checkIn":"%s","checkOut":"%s","rooms":1,"guests":2}"""
                .formatted(room.getId(), checkIn, checkOut);
        return mvc.perform(post("/api/v5/bookings").with(as(guest))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions startPayment(User guest, UUID bookingId) throws Exception {
        return mvc.perform(post("/api/v5/bookings/{bookingId}/pay", bookingId).with(as(guest)));
    }

    protected ResultActions completeMock(User guest, String orderId, String outcome) throws Exception {
        return mvc.perform(post("/api/v5/payments/mock/{orderId}/complete", orderId).with(as(guest))
                .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"" + outcome + "\"}"));
    }

    protected ResultActions cancel(User guest, UUID bookingId) throws Exception {
        return mvc.perform(post("/api/v5/bookings/{bookingId}/cancel", bookingId).with(as(guest)));
    }

    protected ResultActions getBooking(User guest, UUID bookingId) throws Exception {
        return mvc.perform(get("/api/v5/bookings/{bookingId}", bookingId).with(as(guest)));
    }

    // ---------- reading results ----------

    protected static String read(ResultActions result, String jsonPath) throws Exception {
        Object value = JsonPath.read(result.andReturn().getResponse().getContentAsString(), jsonPath);
        return String.valueOf(value);
    }

    protected static UUID bookingIdOf(ResultActions created) throws Exception {
        return UUID.fromString(read(created, "$.data.id"));
    }

    /**
     * Book and pay successfully; returns the booking id.
     */
    protected UUID confirmedBooking(User guest, ListingRoom room, LocalDate checkIn, LocalDate checkOut)
            throws Exception {
        UUID bookingId = bookingIdOf(createBooking(guest, room, checkIn, checkOut).andExpect(status().isCreated()));
        String orderId = read(startPayment(guest, bookingId).andExpect(status().isOk()), "$.data.orderId");
        completeMock(guest, orderId, "SUCCESS").andExpect(status().isOk());
        return bookingId;
    }

    protected Booking booking(UUID bookingId) {
        return bookingRepo.findById(bookingId).orElseThrow();
    }

    protected EarningsEntry earningsFor(UUID bookingId) {
        return earningsRepo.findAll().stream().filter(e -> e.getBookingId().equals(bookingId)).findFirst()
                .orElseThrow(() -> new AssertionError("No earnings row for booking " + bookingId));
    }

    /**
     * Pretends the 15-minute hold ran out a minute ago (the expiry job still has to run).
     */
    protected void holdRanOut(UUID bookingId) {
        Booking b = booking(bookingId);
        b.setHoldExpiresAt(Instant.now().minus(Duration.ofMinutes(1)));
        bookingRepo.save(b);
    }

    /**
     * For work that runs on another thread (@Async listeners): check every 100 ms, give up after 10 s.
     */
    protected static void waitUntil(String what, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out after 10 s waiting until " + what);
            }
            Thread.sleep(100);
        }
    }
}
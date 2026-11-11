package blinkstay.notification.util;

import blinkstay.common.booking.BookingConfirmedEvent;
import blinkstay.common.booking.PaymentRefundedEvent;
import blinkstay.common.payment.BookingCancelledEvent;
import blinkstay.notification.util.EmailTemplate.Tone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

/**
 * Every BlinkStay email in one place. Each method returns an EmailContent
 * (subject + plain text + HTML). Pass it to the event with
 * SimpleMessageEvent.builder().receiverId(email).content(...).build().
 */
@Component
@Lazy(true)
public class EmailMessageBuilderUtil {

    // Frontend routes the buttons link to. Change these if your React routes differ.
    private static final String LOGIN = "/login";
    private static final String MY_BOOKINGS = "/bookings";
    private static final String HOST_RESERVATIONS = "/manager/reservations";
    private static final String MY_LISTINGS = "/manager/listings";
    private static final String BECOME_HOST = "/become-host";
    private static final String ADMIN_HOST_REQUESTS = "/admin/host-requests";
    private static final String EXPLORE = "/explore";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    @Value("${app.support.email:}")
    private String supportEmail;

    @Value("${app.mail.logo-url:}")
    private String logoUrl;

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    private EmailTemplate template(String subject, String preheader) {
        return new EmailTemplate(subject, preheader, logoUrl, supportEmail, frontendUrl);
    }

    /* ============================================================ ACCOUNT */

    public EmailContent registrationOtpMessage(String username, String otp, int validMinutes) {
        return template("Your BlinkStay verification code", "Use this code to finish creating your account.")
                .heading("Verify your email")
                .greeting(username)
                .paragraph("Enter this code on BlinkStay to finish creating your account:")
                .code(otp)
                .paragraph("The code expires in " + validMinutes + " minutes. For your security, don't share it with anyone.")
                .note("If you didn't try to sign up for BlinkStay, you can safely ignore this email.")
                .build();
    }

    public EmailContent passwordResetOtpMessage(String username, String otp, int validMinutes) {
        return template("Reset your BlinkStay password", "Use this code to reset your password.")
                .heading("Reset your password")
                .greeting(username)
                .paragraph("We received a request to reset your BlinkStay password. Enter this code to choose a new one:")
                .code(otp)
                .paragraph("The code expires in " + validMinutes + " minutes. BlinkStay will never ask you for this code.")
                .note("If you didn't ask for a reset, ignore this email. Your password has not been changed.")
                .build();
    }

    public EmailContent welcomeMessage(String username) {
        return template("Welcome to BlinkStay, " + username, "Your account is ready. Start exploring stays.")
                .heading("Welcome to BlinkStay")
                .greeting(username)
                .paragraph("Your account is ready. You can now explore stays, save your favourites and book in a few taps.")
                .button("Start exploring", EXPLORE)
                .paragraph("Have a place to share? You can also apply to become a host from your profile.")
                .build();
    }

    public EmailContent passwordChangedMessage(String username) {
        return template("Your BlinkStay password was changed", "If this was you, no action is needed.")
                .heading("Your password was changed")
                .greeting(username)
                .paragraph("The password for your BlinkStay account was just changed.")
                .callout(Tone.WARNING, "Wasn't you?",
                        "Reset your password right away using \"Forgot password\" on the login page, then contact support.")
                .button("Go to login", LOGIN)
                .note("If you made this change, no further action is needed.")
                .build();
    }

    public EmailContent accountDeletedMessage(String username) {
        return template("Your BlinkStay account has been deleted", "We're sorry to see you go.")
                .heading("Your account has been deleted")
                .greeting(username)
                .paragraph("Your BlinkStay account has been deleted as requested. Any listings you owned have been taken offline.")
                .paragraph("Thank you for being part of BlinkStay. You're always welcome back.")
                .note("If you didn't delete your account, contact support immediately.")
                .build();
    }

    /* ============================================================ MODERATION */

    public EmailContent accountBlockedMessage(String username, String reason) {
        return template("Your BlinkStay account has been blocked", "An administrator has blocked your account.")
                .heading("Your account has been blocked")
                .greeting(username)
                .paragraph("An administrator has blocked your BlinkStay account.")
                .callout(Tone.DANGER, "Reason", reason)
                .paragraph("While your account is blocked you can't log in, and any listings you own are offline.")
                .note("If you think this is a mistake, contact our support team at the address below.")
                .build();
    }

    public EmailContent accountUnblockedMessage(String username) {
        return template("Your BlinkStay account is active again", "You can log in again.")
                .heading("Your account is active again")
                .greeting(username)
                .paragraph("Good news: your BlinkStay account has been unblocked and you can log in again.")
                .paragraph("Listings that were taken offline because of the block are back to their previous status.")
                .button("Log in to BlinkStay", LOGIN)
                .build();
    }

    public EmailContent hostRequestSubmittedAdminMessage(String adminName, String applicantName) {
        return template("New host request from " + applicantName, applicantName + " wants to become a host.")
                .heading("New host request")
                .greeting(adminName)
                .paragraph(applicantName + " has applied to become a host. The request is waiting for review in the host requests queue.")
                .button("Review request", ADMIN_HOST_REQUESTS)
                .build();
    }

    public EmailContent hostRequestApprovedMessage(String username) {
        return template("You're now a BlinkStay host", "Your host request has been approved.")
                .heading("Welcome, host!")
                .greeting(username)
                .callout(Tone.SUCCESS, null, "Your request to become a host has been approved.")
                .paragraph("You can now create listings, add rooms and publish them for guests to book.")
                .button("Create your first listing", MY_LISTINGS)
                .build();
    }

    public EmailContent hostRequestRejectedMessage(String username, String reason) {
        return template("Update on your host request", "Your host request was not approved this time.")
                .heading("Your host request wasn't approved")
                .greeting(username)
                .paragraph("Thank you for applying to host on BlinkStay. After review, we weren't able to approve your request this time.")
                .callout(Tone.INFO, "Reason", reason)
                .paragraph("You're welcome to apply again with updated details.")
                .button("Apply again", BECOME_HOST)
                .build();
    }

    public EmailContent listingSuspendedMessage(String username, String title, String reason) {
        return template("Your listing \"" + title + "\" has been suspended", "It is no longer visible to guests.")
                .heading("Your listing has been suspended")
                .greeting(username)
                .paragraph("An administrator has suspended your listing \"" + title + "\". It is no longer visible to guests.")
                .callout(Tone.DANGER, "Reason", reason)
                .paragraph("If you'd like the suspension reviewed, contact our support team.")
                .button("View my listings", MY_LISTINGS)
                .build();
    }

    public EmailContent listingRestoredMessage(String username, String title, String restoredStatus) {
        String where = switch (restoredStatus) {
            case "PUBLISHED" -> "It's live again and guests can book it.";
            case "PAUSED" -> "It's paused, so you can resume it whenever you're ready.";
            default -> "It's back in your drafts.";
        };
        return template("Your listing \"" + title + "\" is back", "The suspension has been lifted.")
                .heading("Your listing is back")
                .greeting(username)
                .callout(Tone.SUCCESS, null, "The suspension on \"" + title + "\" has been lifted.")
                .paragraph(where)
                .button("View my listings", MY_LISTINGS)
                .build();
    }

    /* ============================================================ BOOKINGS */

    public EmailContent bookingConfirmedMessage(String username, BookingConfirmedEvent e) {
        return template("Booking confirmed: " + e.listingTitle() + ", " + SHORT_DAY.format(e.checkIn()),
                "Your stay at " + e.listingTitle() + " is confirmed. Reference " + e.reference() + ".")
                .heading("Your booking is confirmed")
                .greeting(username)
                .paragraph("Pack your bags! Your stay at " + e.listingTitle() + " is booked.")
                .details(
                        "Booking reference", e.reference(),
                        "Check-in", DAY.format(e.checkIn()),
                        "Check-out", DAY.format(e.checkOut()),
                        "Duration", plural(e.nights(), "night"),
                        "Room", e.roomType() + " × " + e.rooms(),
                        "Guests", String.valueOf(e.guests()))
                .total("Total paid", money(e.currency(), e.totalAmount()))
                .button("View booking", MY_BOOKINGS)
                .note("Free cancellation is available up to 48 hours before check-in. You can cancel from My bookings.")
                .build();
    }

    public EmailContent bookingReceivedMessage(String hostName, String guestName, BookingConfirmedEvent e) {
        return template("New booking: " + guestName + ", " + SHORT_DAY.format(e.checkIn()) + " – "
                + SHORT_DAY.format(e.checkOut()), guestName + " booked " + e.listingTitle() + ".")
                .heading("You have a new booking")
                .greeting(hostName)
                .paragraph(guestName + " has booked " + e.listingTitle() + ". The payment is confirmed.")
                .details(
                        "Booking reference", e.reference(),
                        "Guest", guestName,
                        "Check-in", DAY.format(e.checkIn()),
                        "Check-out", DAY.format(e.checkOut()),
                        "Duration", plural(e.nights(), "night"),
                        "Room", e.roomType() + " × " + e.rooms(),
                        "Guests", String.valueOf(e.guests()))
                .total("Booking total", money(e.currency(), e.totalAmount()))
                .button("View reservations", HOST_RESERVATIONS)
                .build();
    }

    public EmailContent paymentRefundedMessage(String username, PaymentRefundedEvent e) {
        String amount = money(e.currency(), e.amount());
        return template("Refund of " + amount + " for booking " + e.bookingReference(),
                "Your refund is on its way.")
                .heading("Your refund is on its way")
                .greeting(username)
                .paragraph("We've refunded your payment for booking " + e.bookingReference() + ".")
                .details(
                        "Booking reference", e.bookingReference(),
                        "Refund amount", amount,
                        "Reason", e.reason())
                .paragraph("Refunds usually take 5–7 working days to appear, depending on your bank.")
                .button("View my bookings", MY_BOOKINGS)
                .build();
    }

    public EmailContent bookingCancelledGuestMessage(String username, BookingCancelledEvent e) {
        String intro = switch (e.cancelledBy()) {
            case "HOST" -> "Unfortunately, the host had to cancel your booking at " + e.listingTitle() + ".";
            case "ADMIN" -> "BlinkStay support has cancelled your booking at " + e.listingTitle() + ".";
            default -> "Your booking at " + e.listingTitle() + " has been cancelled, as you requested.";
        };
        EmailTemplate t = template("Booking " + e.reference() + " has been cancelled",
                "Your booking at " + e.listingTitle() + " was cancelled.")
                .heading("Your booking has been cancelled")
                .greeting(username)
                .paragraph(intro)
                .details(
                        "Booking reference", e.reference(),
                        "Listing", e.listingTitle(),
                        "Check-in", DAY.format(e.checkIn()),
                        "Check-out", DAY.format(e.checkOut()));
        if (!"GUEST".equals(e.cancelledBy())) {
            t.callout(Tone.INFO, "Reason", e.reason());
        }
        if (e.wasPaid()) {
            if (e.refundAmount() == null || e.refundAmount().signum() == 0) {
                t.callout(Tone.WARNING, "No refund",
                        "This booking was cancelled inside the no-refund period, so no refund is due.");
            } else {
                t.callout(Tone.SUCCESS, "Refund of " + money(e.currency(), e.refundAmount()),
                        "Your refund has been issued. It usually takes 5–7 working days to appear, depending on your bank.");
            }
        }
        return t.button("Find another stay", EXPLORE).build();
    }

    public EmailContent bookingCancelledHostMessage(String hostName, String guestName, BookingCancelledEvent e) {
        String by = "ADMIN".equals(e.cancelledBy()) ? "BlinkStay support" : guestName;
        return template("Booking " + e.reference() + " was cancelled",
                by + " cancelled a booking at " + e.listingTitle() + ".")
                .heading("A booking was cancelled")
                .greeting(hostName)
                .paragraph(by + " cancelled booking " + e.reference() + " at " + e.listingTitle() + ".")
                .details(
                        "Booking reference", e.reference(),
                        "Guest", guestName,
                        "Check-in", DAY.format(e.checkIn()),
                        "Check-out", DAY.format(e.checkOut()))
                .callout(Tone.INFO, "Reason", e.reason())
                .paragraph("The rooms are available again for those dates.")
                .button("View reservations", HOST_RESERVATIONS)
                .build();
    }

    /* ============================================================ HELPERS */

    private static String plural(long n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    /**
     * ₹4,500.00 for INR; falls back to "USD 45.00" style for unknown codes.
     */
    private static String money(String currency, BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        try {
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"));
            nf.setCurrency(Currency.getInstance(currency));
            return nf.format(amount);
        } catch (RuntimeException ex) {
            return currency + " " + amount.toPlainString();
        }
    }
}
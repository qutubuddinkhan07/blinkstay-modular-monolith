package blinkstay.notification.util;

import java.time.Year;

/**
 * Builds one email in two forms at the same time: plain text and branded HTML.
 * <p>
 * The HTML uses tables and inline styles only, because Gmail, Outlook and most
 * mobile mail apps ignore <style> blocks and modern CSS layout. Every value
 * that can come from a user (names, titles, reasons) is HTML-escaped.
 * <p>
 * Usage: emailBuilder.template("subject", "preview text").greeting(name)
 * .paragraph("...").button("Open", "/path").build();
 */
public final class EmailTemplate {

    public enum Tone {
        INFO, SUCCESS, WARNING, DANGER
    }

    // BlinkStay brand (matches the favicon tile)
    private static final String BRAND = "#710019";
    private static final String BRAND_SOFT = "#f8eef0";
    private static final String TEXT = "#222222";
    private static final String MUTED = "#6a6a6a";
    private static final String BORDER = "#ebebeb";
    private static final String PAGE_BG = "#f5f5f5";
    private static final String FONT = "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif";

    private final String subject;
    private final String preheader;
    private final String logoUrl;
    private final String supportEmail;
    private final String appUrl;

    private final StringBuilder html = new StringBuilder();
    private final StringBuilder text = new StringBuilder();

    EmailTemplate(String subject, String preheader, String logoUrl, String supportEmail, String appUrl) {
        this.subject = subject;
        this.preheader = preheader == null ? "" : preheader;
        this.logoUrl = blankToNull(logoUrl);
        this.supportEmail = blankToNull(supportEmail);
        String url = blankToNull(appUrl);
        this.appUrl = url == null ? null : url.replaceAll("/+$", "");
    }

    /* ---------------------------------------------------------------- blocks */

    public EmailTemplate heading(String title) {
        html.append("<h1 style=\"margin:0 0 20px;font-size:24px;line-height:32px;font-weight:700;color:")
                .append(TEXT).append(";\">").append(esc(title)).append("</h1>");
        text.append(title).append("\n").append("=".repeat(Math.min(title.length(), 60))).append("\n\n");
        return this;
    }

    public EmailTemplate greeting(String name) {
        return paragraph("Hi " + (name == null || name.isBlank() ? "there" : name) + ",");
    }

    public EmailTemplate paragraph(String body) {
        html.append("<p style=\"margin:0 0 16px;\">").append(esc(body)).append("</p>");
        text.append(body).append("\n\n");
        return this;
    }

    /**
     * Small grey text, for "if this wasn't you" style notes.
     */
    public EmailTemplate note(String body) {
        html.append("<p style=\"margin:0 0 16px;font-size:13px;line-height:20px;color:").append(MUTED)
                .append(";\">").append(esc(body)).append("</p>");
        text.append(body).append("\n\n");
        return this;
    }

    /**
     * Big one-time code box.
     */
    public EmailTemplate code(String code) {
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"margin:4px 0 20px;\"><tr><td align=\"center\" bgcolor=\"").append(BRAND_SOFT)
                .append("\" style=\"background:").append(BRAND_SOFT).append(";border-radius:12px;padding:20px 12px;")
                .append("font-family:'Courier New',Courier,monospace;font-size:34px;line-height:40px;")
                .append("letter-spacing:10px;font-weight:700;color:").append(BRAND).append(";\">")
                .append(esc(code)).append("</td></tr></table>");
        text.append("    ").append(code).append("\n\n");
        return this;
    }

    /**
     * Label/value rows, passed as pairs: details("Check-in", "12 Dec", "Guests",
     * "2"). Rows with a null value are skipped.
     */
    public EmailTemplate details(String... labelValuePairs) {
        if (labelValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("details() needs label/value pairs");
        }
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"margin:4px 0 20px;border:1px solid ").append(BORDER)
                .append(";border-radius:12px;border-collapse:separate;\">");
        boolean first = true;
        for (int i = 0; i < labelValuePairs.length; i += 2) {
            String label = labelValuePairs[i];
            String value = labelValuePairs[i + 1];
            if (value == null) {
                continue;
            }
            String top = first ? "" : "border-top:1px solid " + BORDER + ";";
            html.append("<tr><td style=\"padding:12px 16px;").append(top).append("color:").append(MUTED)
                    .append(";font-size:14px;\">").append(esc(label)).append("</td>")
                    .append("<td align=\"right\" style=\"padding:12px 16px;").append(top)
                    .append("font-size:14px;font-weight:600;color:").append(TEXT).append(";\">").append(esc(value))
                    .append("</td></tr>");
            text.append(label).append(": ").append(value).append("\n");
            first = false;
        }
        html.append("</table>");
        text.append("\n");
        return this;
    }

    /**
     * A highlighted line, e.g. "Total paid ₹4,500.00".
     */
    public EmailTemplate total(String label, String value) {
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"margin:-8px 0 20px;\"><tr>")
                .append("<td style=\"padding:4px 16px;font-size:16px;font-weight:700;color:").append(TEXT)
                .append(";\">").append(esc(label)).append("</td>")
                .append("<td align=\"right\" style=\"padding:4px 16px;font-size:16px;font-weight:700;color:")
                .append(TEXT).append(";\">").append(esc(value)).append("</td></tr></table>");
        text.append(label).append(": ").append(value).append("\n\n");
        return this;
    }

    /**
     * Coloured box with a bold label, used for reasons and warnings.
     */
    public EmailTemplate callout(Tone tone, String label, String body) {
        String bg;
        String line;
        switch (tone) {
            case SUCCESS -> {
                bg = "#ecfdf3";
                line = "#067647";
            }
            case WARNING -> {
                bg = "#fffaeb";
                line = "#b54708";
            }
            case DANGER -> {
                bg = "#fef3f2";
                line = "#b42318";
            }
            default -> {
                bg = "#f2f4f7";
                line = "#475467";
            }
        }
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"margin:4px 0 20px;\"><tr><td bgcolor=\"").append(bg).append("\" style=\"background:")
                .append(bg).append(";border-left:4px solid ").append(line)
                .append(";border-radius:8px;padding:14px 16px;font-size:14px;line-height:22px;color:").append(TEXT)
                .append(";\">");
        if (label != null) {
            html.append("<strong style=\"color:").append(line).append(";\">").append(esc(label))
                    .append("</strong><br>");
            text.append(label).append(": ");
        }
        html.append(esc(body)).append("</td></tr></table>");
        text.append(body).append("\n\n");
        return this;
    }

    /**
     * Call-to-action button linking into the frontend. path is relative, e.g.
     * "/bookings". Skipped if app.frontend.url is not set.
     */
    public EmailTemplate button(String label, String path) {
        if (appUrl == null) {
            return this;
        }
        String href = appUrl + (path.startsWith("/") ? path : "/" + path);
        html.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"margin:8px 0 24px;\"><tr><td bgcolor=\"").append(BRAND)
                .append("\" style=\"border-radius:8px;background:").append(BRAND).append(";\">")
                .append("<a href=\"").append(esc(href)).append("\" target=\"_blank\" style=\"display:inline-block;")
                .append("padding:13px 26px;font-family:").append(FONT)
                .append(";font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:8px;\">")
                .append(esc(label)).append("</a></td></tr></table>");
        text.append(label).append(": ").append(href).append("\n\n");
        return this;
    }

    /* ---------------------------------------------------------------- build */

    public EmailContent build() {
        return new EmailContent(subject, buildText(), buildHtml());
    }

    private String buildText() {
        StringBuilder out = new StringBuilder("BlinkStay\n\n").append(text.toString().stripTrailing())
                .append("\n\n--\n");
        if (supportEmail != null) {
            out.append("Need help? Contact us at ").append(supportEmail).append("\n");
        }
        out.append("This is a service email about your BlinkStay account or bookings.\n")
                .append("© ").append(Year.now().getValue()).append(" BlinkStay");
        return out.toString();
    }

    private String buildHtml() {
        StringBuilder out = new StringBuilder(6000);
        out.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<meta name=\"color-scheme\" content=\"light\"><meta name=\"supported-color-schemes\" content=\"light\">")
                .append("<title>").append(esc(subject)).append("</title></head>")
                .append("<body style=\"margin:0;padding:0;background:").append(PAGE_BG).append(";\">")
                // preheader: the grey preview line in the inbox list, hidden in the email itself
                .append("<div style=\"display:none;max-height:0;overflow:hidden;mso-hide:all;font-size:1px;line-height:1px;color:")
                .append(PAGE_BG).append(";opacity:0;\">").append(esc(preheader))
                .append("&#8203;&nbsp;".repeat(40)).append("</div>")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"")
                .append(PAGE_BG).append("\" style=\"background:").append(PAGE_BG).append(";\">")
                .append("<tr><td align=\"center\" style=\"padding:32px 12px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
                .append("style=\"max-width:560px;\">");

        // header: logo + wordmark (the wordmark is text, so it shows even when images are blocked)
        out.append("<tr><td style=\"padding:0 4px 20px;\"><table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>");
        if (logoUrl != null) {
            out.append("<td style=\"padding-right:10px;\"><img src=\"").append(esc(logoUrl))
                    .append("\" width=\"36\" height=\"36\" alt=\"\" style=\"display:block;border:0;outline:none;\"></td>");
        } else {
            out.append("<td style=\"padding-right:10px;\"><table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>")
                    .append("<td width=\"36\" height=\"36\" align=\"center\" bgcolor=\"").append(BRAND)
                    .append("\" style=\"width:36px;height:36px;border-radius:9px;background:").append(BRAND)
                    .append(";font-family:").append(FONT)
                    .append(";font-size:20px;font-weight:800;color:#ffffff;\">B</td></tr></table></td>");
        }
        out.append("<td style=\"font-family:").append(FONT).append(";font-size:22px;font-weight:800;color:").append(BRAND)
                .append(";letter-spacing:-0.3px;\">BlinkStay</td></tr></table></td></tr>");

        // card
        out.append("<tr><td bgcolor=\"#ffffff\" style=\"background:#ffffff;border:1px solid ").append(BORDER)
                .append(";border-radius:16px;padding:36px 32px 16px;font-family:").append(FONT)
                .append(";font-size:15px;line-height:24px;color:").append(TEXT).append(";\">")
                .append(html).append("</td></tr>");

        // footer
        out.append("<tr><td align=\"center\" style=\"padding:24px 16px 8px;font-family:").append(FONT)
                .append(";font-size:12px;line-height:19px;color:").append(MUTED).append(";\">");
        if (supportEmail != null) {
            out.append("Need help? Contact us at <a href=\"mailto:").append(esc(supportEmail)).append("\" style=\"color:")
                    .append(MUTED).append(";text-decoration:underline;\">").append(esc(supportEmail)).append("</a><br>");
        }
        out.append("This is a service email about your BlinkStay account or bookings.<br>")
                .append("&copy; ").append(Year.now().getValue()).append(" BlinkStay")
                .append("</td></tr></table></td></tr></table></body></html>");
        return out.toString();
    }

    /* ---------------------------------------------------------------- helpers */

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /**
     * Escapes user-supplied text so it can never inject HTML into the email.
     */
    static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&#39;");
                case '\n' -> b.append("<br>");
                case '\r' -> {
                }
                default -> b.append(c);
            }
        }
        return b.toString();
    }
}
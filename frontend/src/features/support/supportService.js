import { SUPPORT_EMAIL } from "../../utils/support";

export const CONTACT_TOPICS = [
  { value: "BOOKING", label: "A booking" },
  { value: "PAYMENT", label: "Payment or refund" },
  { value: "HOSTING", label: "Hosting" },
  { value: "ACCOUNT", label: "My account" },
  { value: "OTHER", label: "Something else" },
];

export const CONTACT_LIMITS = { name: 80, messageMin: 20, messageMax: 2000 };

// For now this opens the visitor's email app with everything filled in.
// When the backend has POST /v1/support/contact, replace the body with:
//   return axiosInstance.post("/v1/support/contact", form).then((r) => r.data);
// and nothing else on the page needs to change.
export const sendContactMessage = async (form) => {
  const topic =
    CONTACT_TOPICS.find((t) => t.value === form.topic)?.label ?? form.topic;

  const subject = `[BlinkStay] ${topic}${
    form.bookingReference ? ` (${form.bookingReference})` : ""
  }`;

  const body = [
    form.message,
    "",
    "---",
    `Name: ${form.name}`,
    `Email: ${form.email}`,
    form.bookingReference ? `Booking: ${form.bookingReference}` : "",
  ]
    .filter((line) => line !== "")
    .join("\n");

  window.location.href = `mailto:${SUPPORT_EMAIL}?subject=${encodeURIComponent(
    subject,
  )}&body=${encodeURIComponent(body)}`;

  return { via: "email-app" };
};
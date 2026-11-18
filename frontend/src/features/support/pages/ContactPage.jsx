import { useEffect, useId, useState } from "react";
import { Link } from "react-router-dom";
import { FiMail } from "react-icons/fi";
import { useAuth } from "../../../context/AuthContext";
import { usePageTitle } from "../../../utils/usePageTitle";
import { SUPPORT_EMAIL } from "../../../utils/support";
import { handleApiError } from "../../../api/errors/handleApiError";
import {
  CONTACT_LIMITS,
  CONTACT_TOPICS,
  sendContactMessage,
} from "../supportService";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";
const fieldCls = (bad) =>
  `w-full rounded-lg border bg-input px-3 py-2.5 text-sm text-text placeholder:text-subtext outline-none focus:ring-2 focus:ring-primary/30 ${
    bad ? "border-danger" : "border-border"
  }`;

const EMPTY = {
  name: "",
  email: "",
  topic: "",
  bookingReference: "",
  message: "",
};

const validate = (f) => {
  const e = {};
  if (f.name.trim().length < 2) e.name = "Please enter your name.";
  if (!/\S+@\S+\.\S+/.test(f.email)) e.email = "Please enter a valid email.";
  if (!f.topic) e.topic = "Please choose a topic.";
  if (f.message.trim().length < CONTACT_LIMITS.messageMin)
    e.message = `Please write at least ${CONTACT_LIMITS.messageMin} characters.`;
  return e;
};

const ContactPage = () => {
  usePageTitle("Contact us");
  const id = useId();
  const { user } = useAuth();

  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [sent, setSent] = useState(false);

  // logged in: fill in who they are
  useEffect(() => {
    if (user) {
      setForm((f) => ({
        ...f,
        name: f.name || user.username || "",
        email: user.email || f.email,
      }));
    }
  }, [user]);

  const set = (name) => (e) => {
    setForm((f) => ({ ...f, [name]: e.target.value }));
    if (errors[name]) setErrors((x) => ({ ...x, [name]: "" }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (busy) return;

    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length) return;

    setBusy(true);
    try {
      await sendContactMessage({
        ...form,
        name: form.name.trim(),
        message: form.message.trim(),
        bookingReference: form.bookingReference.trim().toUpperCase(),
      });
      setSent(true);
    } catch (err) {
      setErrors({ api: handleApiError(err, "message") });
    } finally {
      setBusy(false);
    }
  };

  const count = form.message.length;

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-10 text-text">
      <div className="mx-auto grid max-w-5xl gap-10 lg:grid-cols-[1fr_1.4fr]">
        <div>
          <h1 className="font-display text-3xl font-semibold sm:text-4xl">
            Contact us
          </h1>
          <p className="mt-2 text-subtext">
            Questions about a booking, a payment or hosting? Send us a message
            and we'll reply by email.
          </p>
          <p className="mt-4 text-sm text-subtext">
            Many answers are already in the{" "}
            <Link to="/help" className="font-semibold text-primary underline">
              Help Center
            </Link>
            .
          </p>
          <div className="mt-6 flex items-center gap-3 rounded-2xl border border-border bg-surface p-4 text-sm">
            <FiMail
              className="shrink-0 text-primary"
              size={20}
              aria-hidden="true"
            />
            <div className="min-w-0">
              <p className="text-subtext">Or write to us directly</p>
              <a
                href={`mailto:${SUPPORT_EMAIL}`}
                className={`break-all font-semibold text-text hover:text-primary ${focusRing}`}
              >
                {SUPPORT_EMAIL}
              </a>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-surface p-6 sm:p-8">
          {sent ? (
            <div role="status" className="text-center">
              <h2 className="font-display text-2xl font-semibold">
                Almost done
              </h2>
              <p className="mt-2 text-sm text-subtext">
                Your email app should have opened with your message ready. Press
                Send there to reach us. Didn't open? Write to{" "}
                <a
                  href={`mailto:${SUPPORT_EMAIL}`}
                  className="font-semibold text-primary underline"
                >
                  {SUPPORT_EMAIL}
                </a>
                .
              </p>
              <button
                type="button"
                onClick={() => {
                  setSent(false);
                  setForm((f) => ({ ...EMPTY, name: f.name, email: f.email }));
                }}
                className={`mt-5 cursor-pointer rounded-lg border border-border px-5 py-2.5 text-sm font-semibold text-text hover:bg-bg ${focusRing}`}
              >
                Write another message
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} noValidate className="space-y-5">
              <div className="grid gap-5 sm:grid-cols-2">
                <div>
                  <label
                    htmlFor={`${id}-name`}
                    className="mb-1.5 block text-sm font-medium"
                  >
                    Name
                  </label>
                  <input
                    id={`${id}-name`}
                    value={form.name}
                    onChange={set("name")}
                    maxLength={CONTACT_LIMITS.name}
                    autoComplete="name"
                    aria-invalid={Boolean(errors.name)}
                    className={fieldCls(errors.name)}
                  />
                  {errors.name && (
                    <p className="mt-1.5 text-xs text-danger">{errors.name}</p>
                  )}
                </div>
                <div>
                  <label
                    htmlFor={`${id}-email`}
                    className="mb-1.5 block text-sm font-medium"
                  >
                    Email
                  </label>
                  <input
                    id={`${id}-email`}
                    type="email"
                    value={form.email}
                    onChange={set("email")}
                    readOnly={Boolean(user?.email)}
                    autoComplete="email"
                    aria-invalid={Boolean(errors.email)}
                    className={`${fieldCls(errors.email)} ${user?.email ? "opacity-70" : ""}`}
                  />
                  {errors.email && (
                    <p className="mt-1.5 text-xs text-danger">{errors.email}</p>
                  )}
                </div>
              </div>

              <div className="grid gap-5 sm:grid-cols-2">
                <div>
                  <label
                    htmlFor={`${id}-topic`}
                    className="mb-1.5 block text-sm font-medium"
                  >
                    Topic
                  </label>
                  <select
                    id={`${id}-topic`}
                    value={form.topic}
                    onChange={set("topic")}
                    aria-invalid={Boolean(errors.topic)}
                    className={fieldCls(errors.topic)}
                  >
                    <option value="">Choose a topic</option>
                    {CONTACT_TOPICS.map((t) => (
                      <option key={t.value} value={t.value}>
                        {t.label}
                      </option>
                    ))}
                  </select>
                  {errors.topic && (
                    <p className="mt-1.5 text-xs text-danger">{errors.topic}</p>
                  )}
                </div>
                <div>
                  <label
                    htmlFor={`${id}-ref`}
                    className="mb-1.5 block text-sm font-medium"
                  >
                    Booking reference{" "}
                    <span className="font-normal text-subtext">(optional)</span>
                  </label>
                  <input
                    id={`${id}-ref`}
                    value={form.bookingReference}
                    onChange={set("bookingReference")}
                    placeholder="BS-XXXXXXXX"
                    maxLength={20}
                    className={fieldCls(false)}
                  />
                </div>
              </div>

              <div>
                <label
                  htmlFor={`${id}-message`}
                  className="mb-1.5 block text-sm font-medium"
                >
                  Message
                </label>
                <textarea
                  id={`${id}-message`}
                  value={form.message}
                  onChange={set("message")}
                  rows={6}
                  maxLength={CONTACT_LIMITS.messageMax}
                  placeholder="Tell us what happened and how we can help."
                  aria-invalid={Boolean(errors.message)}
                  aria-describedby={`${id}-count`}
                  className={`${fieldCls(errors.message)} min-h-32 resize-y`}
                />
                <div className="mt-1 flex justify-between gap-3 text-xs">
                  <span className="text-danger">{errors.message}</span>
                  <span
                    id={`${id}-count`}
                    className="tabular-nums text-subtext"
                  >
                    {count}/{CONTACT_LIMITS.messageMax}
                  </span>
                </div>
              </div>

              {errors.api && (
                <p role="alert" className="text-sm text-danger">
                  {errors.api}
                </p>
              )}

              <button
                type="submit"
                disabled={busy}
                className={`w-full cursor-pointer rounded-full bg-primary py-3 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
              >
                {busy ? "Sending…" : "Send message"}
              </button>
              <p className="text-center text-xs text-subtext">
                This opens your email app with the message ready to send.
              </p>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default ContactPage;

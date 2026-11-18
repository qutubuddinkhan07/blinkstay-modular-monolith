import { Link } from "react-router-dom";
import { FiChevronDown } from "react-icons/fi";
import { usePageTitle } from "../../../utils/usePageTitle";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const TOPICS = [
  {
    id: "booking",
    title: "Booking",
    items: [
      {
        q: "How does booking work?",
        a: "Choose your dates, rooms and guests on a listing and press Book. Your rooms are held for 15 minutes while you pay. Once the payment goes through, the booking is confirmed straight away; hosts don't need to approve it.",
      },
      {
        q: "What happens if I don't pay within 15 minutes?",
        a: "The hold ends and the rooms are released to other guests. The booking shows as Expired, and you can book the same dates again if they are still free.",
      },
      {
        q: "How long can I stay?",
        a: "Up to 30 nights per booking. You can have at most 2 unpaid bookings at a time.",
      },
      {
        q: "Where do I find my bookings?",
        a: "Open My bookings from your profile menu. Upcoming, past and cancelled stays each have their own tab.",
      },
    ],
  },
  {
    id: "payments",
    title: "Payments and refunds",
    items: [
      {
        q: "Is real money charged?",
        a: "No. BlinkStay is a demo, and payments run in test mode. Use the test card or test UPI that the payment window offers; nothing is charged.",
      },
      {
        q: "What is the cancellation policy?",
        a: "Cancel for free until 48 hours before your check-in day and you get a full refund. After that you can still cancel, but there is no refund. Once your check-in day has started, the booking can't be cancelled. Unpaid bookings can be cancelled at any time for free.",
      },
      {
        q: "The host cancelled my booking. What now?",
        a: "You get a full refund automatically, and you'll see the host's reason on the booking.",
      },
      {
        q: "When will I see my refund?",
        a: "The refund is sent a few seconds after the cancellation. In live mode, banks usually take 5 to 7 working days to show it.",
      },
    ],
  },
  {
    id: "hosting",
    title: "Hosting",
    items: [
      {
        q: "How do I become a host?",
        a: "Choose Become a host in the menu and fill in your business details. An admin reviews your request; once it's approved, you can create listings.",
      },
      {
        q: "Why can't I publish my listing?",
        a: "A listing needs at least one room before it can be published. Add a room on the edit page, then publish.",
      },
      {
        q: "Can I pause a listing?",
        a: "Yes. Pausing stops new bookings but keeps the ones already made. Resume it whenever you're ready.",
      },
    ],
  },
  {
    id: "account",
    title: "Account and reviews",
    items: [
      {
        q: "When can I write a review?",
        a: "After your stay is completed (the day after check-out). Each booking can be reviewed once; you can edit or delete your review later.",
      },
      {
        q: "I forgot my password.",
        a: "Use Forgot password on the login page. We'll email you a code to set a new one.",
      },
      {
        q: "What happens when I delete my account?",
        a: "Your account is closed straight away. For 30 days you can contact us to restore it with all your details. After that, your photo and reviews are removed.",
      },
    ],
  },
];

const HelpCenter = () => {
  usePageTitle("Help Center");

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-10 text-text">
      <div className="mx-auto max-w-3xl">
        <h1 className="font-display text-3xl font-semibold sm:text-4xl">
          Help Center
        </h1>
        <p className="mt-2 text-subtext">
          Answers to the most common questions about BlinkStay.
        </p>

        <nav aria-label="Topics" className="mt-6 flex flex-wrap gap-2">
          {TOPICS.map((t) => (
            <a
              key={t.id}
              href={`#${t.id}`}
              className={`rounded-full border border-border bg-surface px-4 py-1.5 text-sm font-medium text-text hover:bg-input ${focusRing}`}
            >
              {t.title}
            </a>
          ))}
        </nav>

        {TOPICS.map((topic) => (
          <section
            key={topic.id}
            id={topic.id}
            aria-labelledby={`${topic.id}-heading`}
            className="mt-10 scroll-mt-24"
          >
            <h2
              id={`${topic.id}-heading`}
              className="font-display text-xl font-semibold"
            >
              {topic.title}
            </h2>
            <div className="mt-4 divide-y divide-border rounded-2xl border border-border bg-surface">
              {topic.items.map((item) => (
                <details key={item.q} className="group">
                  <summary
                    className={`flex cursor-pointer list-none items-center justify-between gap-4 px-5 py-4 font-medium text-text [&::-webkit-details-marker]:hidden ${focusRing}`}
                  >
                    {item.q}
                    <FiChevronDown
                      className="shrink-0 text-subtext transition-transform group-open:rotate-180 motion-reduce:transition-none"
                      aria-hidden="true"
                    />
                  </summary>
                  <p className="px-5 pb-4 text-sm leading-relaxed text-subtext">
                    {item.a}
                  </p>
                </details>
              ))}
            </div>
          </section>
        ))}

        <div className="mt-12 rounded-2xl border border-border bg-surface p-6 text-center">
          <h2 className="font-display text-xl font-semibold">
            Still need help?
          </h2>
          <p className="mt-1 text-sm text-subtext">
            Send us a message and we'll reply by email.
          </p>
          <Link
            to="/contact"
            className={`mt-4 inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary hover:bg-primary-hover ${focusRing}`}
          >
            Contact us
          </Link>
        </div>
      </div>
    </div>
  );
};

export default HelpCenter;

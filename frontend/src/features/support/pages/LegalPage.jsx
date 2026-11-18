import { useEffect } from "react";
import { Link, useLocation } from "react-router-dom";
import { usePageTitle } from "../../../utils/usePageTitle";
import { SUPPORT_EMAIL } from "../../../utils/support";

const Section = ({ id, title, children }) => (
  <section
    id={id}
    aria-labelledby={`${id}-heading`}
    className="mt-10 scroll-mt-24"
  >
    <h2 id={`${id}-heading`} className="font-display text-2xl font-semibold">
      {title}
    </h2>
    <div className="mt-3 space-y-3 text-sm leading-relaxed text-subtext">
      {children}
    </div>
  </section>
);

const LegalPage = () => {
  usePageTitle("Privacy, terms and cookies");
  const { hash } = useLocation();

  // Router links don't scroll to #sections by themselves
  useEffect(() => {
    if (!hash) {
      window.scrollTo({ top: 0 });
      return;
    }
    document
      .getElementById(hash.slice(1))
      ?.scrollIntoView({ behavior: "smooth" });
  }, [hash]);

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-10 text-text">
      <article className="mx-auto max-w-3xl">
        <h1 className="font-display text-3xl font-semibold sm:text-4xl">
          Privacy, terms and cookies
        </h1>
        <p className="mt-2 text-subtext">Last updated: November 2026</p>

        <div className="mt-6 rounded-2xl border border-primary/40 bg-surface p-4 text-sm">
          <strong>BlinkStay is a portfolio demo.</strong> It is not a real
          business, payments run in test mode, and no real money is charged.
          Please don't enter sensitive personal information.
        </div>

        <nav
          aria-label="On this page"
          className="mt-6 flex flex-wrap gap-4 text-sm font-semibold"
        >
          <Link to="#privacy" className="text-primary hover:underline">
            Privacy
          </Link>
          <Link to="#terms" className="text-primary hover:underline">
            Terms
          </Link>
          <Link to="#cookies" className="text-primary hover:underline">
            Cookies
          </Link>
        </nav>

        <Section id="privacy" title="Privacy Policy">
          <p>
            <strong className="text-text">What we store:</strong> your account
            details (name, email, password stored only as a secure hash,
            optional profile photo), your bookings, reviews and favorites, and,
            for hosts, the business details you submit.
          </p>
          <p>
            <strong className="text-text">Why:</strong> to run your account,
            show your bookings, and send you emails you need, such as sign-up
            codes, booking confirmations and cancellations.
          </p>
          <p>
            <strong className="text-text">Who else sees it:</strong> hosts see
            the name on bookings for their listings. Images are stored with
            Cloudinary, and test payments go through Razorpay. Card and UPI
            details are entered in Razorpay's window and never reach our
            servers.
          </p>
          <p>
            <strong className="text-text">Deleting your account:</strong> you
            can delete it from your profile. For 30 days it can be restored on
            request; after that your photo and reviews are removed.
          </p>
          <p>
            Questions? Email{" "}
            <a
              href={`mailto:${SUPPORT_EMAIL}`}
              className="font-semibold text-primary underline"
            >
              {SUPPORT_EMAIL}
            </a>
            .
          </p>
        </Section>

        <Section id="terms" title="Terms of Service">
          <p>
            BlinkStay is provided as a demonstration, without any guarantee that
            it is available or error-free. Data may be reset at any time.
          </p>
          <p>
            Bookings and payments are simulated. No stay is actually reserved
            and no money changes hands.
          </p>
          <p>
            Please be respectful: no abusive reviews, fake listings or attempts
            to break the service. Accounts that misuse it can be blocked.
          </p>
        </Section>

        <Section id="cookies" title="Cookies">
          <p>BlinkStay uses only the cookies it needs to work:</p>
          <ul className="list-disc space-y-1 pl-5">
            <li>
              <strong className="text-text">Login cookie:</strong> keeps you
              signed in. It can't be read by scripts on the page.
            </li>
            <li>
              <strong className="text-text">Security cookie:</strong> protects
              your account against forged requests from other websites.
            </li>
          </ul>
          <p>
            There are no advertising or tracking cookies, so there's nothing to
            turn off. Your theme choice is saved in your browser's local
            storage, not in a cookie.
          </p>
        </Section>
      </article>
    </div>
  );
};

export default LegalPage;

import { Link } from "react-router-dom";
import { FiSlash } from "react-icons/fi";
import { supportMailto } from "../../../utils/support";

const BlockedPage = () => {
  const reason = sessionStorage.getItem("blockedMessage");

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg px-4 text-text">
      <div className="w-full max-w-md rounded-2xl border border-border bg-surface p-8 text-center shadow-sm">
        <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-danger/10 text-danger">
          <FiSlash size={26} aria-hidden="true" />
        </span>
        <h1 className="mt-5 font-display text-2xl font-semibold">
          Your account is blocked
        </h1>
        <p className="mt-2 text-sm text-subtext">
          You can't use BlinkStay while your account is blocked.
          {reason ? ` ${reason}` : ""}
        </p>

        <div className="mt-6 flex flex-col gap-2 sm:flex-row sm:justify-center">
          <a
            href={supportMailto("Account blocked")}
            className="rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            Contact support
          </a>
          <Link
            to="/login"
            className="rounded-lg border border-border px-5 py-2.5 text-sm font-semibold text-text transition-colors hover:bg-bg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            Use another account
          </Link>
        </div>
      </div>
    </div>
  );
};

export default BlockedPage;

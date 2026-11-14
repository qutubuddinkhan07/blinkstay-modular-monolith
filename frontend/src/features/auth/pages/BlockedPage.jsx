import { useEffect } from "react";
import { Link, Navigate } from "react-router-dom";
import BlockedNotice from "../../../components/common/BlockedNotice";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import { usePageTitle } from "../../../utils/usePageTitle";

const MESSAGE_KEY = "blockedMessage"; // the server's reason, set by AuthContext
const NOTICE_KEY = "blockedNotice"; // "1" = the toast hasn't been shown yet

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const BlockedPage = () => {
  usePageTitle("Account blocked");
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    if (isAuthenticated) {
      // A logged-in user isn't blocked (the server just let them in), so forget the old notice
      sessionStorage.removeItem(MESSAGE_KEY);
      sessionStorage.removeItem(NOTICE_KEY);
      return;
    }

    // The page was reloaded on the way here, which wipes any toast, so show it now (once)
    if (sessionStorage.getItem(NOTICE_KEY) === "1") {
      sessionStorage.removeItem(NOTICE_KEY);
      notify.error("Your account has been blocked.", { id: "account-blocked" });
    }
  }, [isAuthenticated]);

  // Someone who is logged in has no business here, for example right after being unblocked
  if (isAuthenticated) return <Navigate to="/explore" replace />;

  const reason = sessionStorage.getItem(MESSAGE_KEY);

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg px-4 text-text">
      <div className="w-full max-w-md space-y-5 rounded-2xl border border-border bg-surface p-6 shadow-sm">
        <BlockedNotice message={reason} />

        <div className="flex flex-col gap-2 sm:flex-row">
          <Link
            to="/login"
            className={`flex-1 rounded-lg bg-primary px-5 py-2.5 text-center text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover ${focusRing}`}
          >
            Log in
          </Link>
          <Link
            to="/explore"
            className={`flex-1 rounded-lg border border-border px-5 py-2.5 text-center text-sm font-semibold text-text transition-colors hover:bg-bg ${focusRing}`}
          >
            Explore stays
          </Link>
        </div>
      </div>
    </div>
  );
};

export default BlockedPage;

import { Link } from "react-router-dom";
import BlockedNotice from "../../../components/common/BlockedNotice";

const BlockedPage = () => {
  const reason = sessionStorage.getItem("blockedMessage");

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg px-4 text-text">
      <div className="w-full max-w-md space-y-5 rounded-2xl border border-border bg-surface p-6 shadow-sm">
        <BlockedNotice message={reason} />
        <Link
          to="/login"
          className="block rounded-lg border border-border px-5 py-2.5 text-center text-sm font-semibold text-text transition-colors hover:bg-bg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
        >
          Use another account
        </Link>
      </div>
    </div>
  );
};

export default BlockedPage;

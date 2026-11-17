import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FiShield } from "react-icons/fi";
import { useAuth } from "../../../../context/AuthContext";
import { notify } from "../../../../utils/notify";
import {
  useSuspendListingMutation,
  useUnsuspendListingMutation,
} from "../../../../store/api";
import ReasonDialog from "../../../../components/common/ReasonDialog";
import ConfirmDialog from "../../../../components/common/ConfirmDialogBox";
import { focusRing } from "../create-listing/formStyles";

const btn = `cursor-pointer rounded-lg px-4 py-2 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;

// Listings suspended because of their owner: fixed on the users page, not here
const OWNER_CAUSE = {
  OWNER_BLOCKED: {
    to: "/admin/users",
    short: "Owner blocked: unblock",
    text: "Suspended because its owner is blocked.",
    linkText: "Unblock the owner",
    action: "Go to users",
  },
  OWNER_DELETED: {
    to: "/admin/users?view=deleted",
    short: "Owner deleted: restore",
    text: "Suspended because its owner deleted their account.",
    linkText: "Restore the owner",
    action: "Go to deleted users",
  },
};

// Renders nothing unless the viewer is an admin
const ListingAdminControls = ({ listing, className = "", compact = false }) => {
  const { isAdmin } = useAuth();
  const navigate = useNavigate();
  const [dialog, setDialog] = useState(null); // "suspend" | "unsuspend" | null

  const [suspend, { isLoading: suspending }] = useSuspendListingMutation();
  const [unsuspend, { isLoading: unsuspending }] =
    useUnsuspendListingMutation();
  const busy = suspending || unsuspending;

  if (!isAdmin) return null;

  const suspended = listing.status === "SUSPENDED";
  const ownerCause = suspended ? OWNER_CAUSE[listing.suspensionSource] : null;

  const handleSuspend = async (reason) => {
    try {
      const res = await suspend({ id: listing.id, reason }).unwrap();
      notify.success(res?.message || "Listing suspended.");
      setDialog(null);
    } catch (err) {
      // e.g. 409 "This listing has upcoming bookings and cannot be suspended. Cancel them first."
      notify.error(err?.message ?? "Couldn't suspend the listing.", {
        duration: err?.status === 409 ? 8000 : undefined,
      });
      if (err?.status === 409) setDialog(null); // retrying won't help until the bookings are cancelled
    }
  };

  const handleUnsuspend = async () => {
    try {
      const res = await unsuspend(listing.id).unwrap();
      notify.success(res?.message || "Listing restored.");
      setDialog(null);
    } catch (err) {
      setDialog(null);
      const message = err?.message ?? "Couldn't restore the listing.";

      if (err?.status === 409) {
        // the owner is blocked or deleted: fix the owner instead.
        // If the listing data doesn't say which yet, the server's words do.
        const cause =
          OWNER_CAUSE[listing.suspensionSource] ??
          (/deleted/i.test(message)
            ? OWNER_CAUSE.OWNER_DELETED
            : OWNER_CAUSE.OWNER_BLOCKED);

        notify.error(message, {
          duration: 8000,
          action: { label: cause.action, onClick: () => navigate(cause.to) },
        });
      } else {
        notify.error(message);
      }
    }
  };

  const control = ownerCause ? (
    compact ? (
      <Link
        to={ownerCause.to}
        className={`${btn} border border-border bg-bg text-text hover:bg-border/60`}
      >
        {ownerCause.short}
      </Link>
    ) : (
      <p className="text-sm text-subtext">
        {ownerCause.text}{" "}
        <Link
          to={ownerCause.to}
          className="font-semibold text-primary underline"
        >
          {ownerCause.linkText}
        </Link>{" "}
        to bring it back.
      </p>
    )
  ) : suspended ? (
    <button
      type="button"
      onClick={() => setDialog("unsuspend")}
      className={`${btn} bg-primary text-on-primary hover:bg-primary-hover`}
    >
      Unsuspend{compact ? "" : " listing"}
    </button>
  ) : (
    <button
      type="button"
      onClick={() => setDialog("suspend")}
      className={`${btn} border border-danger/50 bg-bg text-danger hover:bg-danger/10`}
    >
      Suspend{compact ? "" : " listing"}
    </button>
  );

  const dialogs = (
    <>
      <ReasonDialog
        open={dialog === "suspend"}
        title="Suspend this listing?"
        description={`"${listing.title}" will be hidden from guests. The owner will see your reason. A listing with upcoming bookings can't be suspended until they are cancelled.`}
        confirmLabel="Suspend listing"
        busyLabel="Suspending…"
        busy={busy}
        onConfirm={handleSuspend}
        onCancel={() => setDialog(null)}
      />

      <ConfirmDialog
        open={dialog === "unsuspend"}
        title="Unsuspend this listing?"
        description="It will go back to the status it had before it was suspended."
        confirmLabel="Unsuspend"
        busyLabel="Restoring…"
        tone="primary"
        busy={busy}
        onConfirm={handleUnsuspend}
        onCancel={() => setDialog(null)}
      />
    </>
  );

  if (compact) {
    return (
      <>
        {control}
        {dialogs}
      </>
    );
  }

  return (
    <section
      aria-label="Admin moderation"
      className={`flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface p-4 ${className}`}
    >
      <div className="flex items-center gap-2 text-sm font-semibold text-text">
        <FiShield size={16} className="text-subtext" aria-hidden="true" />
        Admin moderation
      </div>

      {control}
      {dialogs}
    </section>
  );
};

export default ListingAdminControls;

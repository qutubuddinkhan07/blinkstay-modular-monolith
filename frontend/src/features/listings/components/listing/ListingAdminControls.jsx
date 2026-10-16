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
  // only works if your DTO exposes suspensionSource; the 409 handling covers it otherwise
  const ownerBlocked =
    suspended && listing.suspensionSource === "OWNER_BLOCKED";

  const handleSuspend = async (reason) => {
    try {
      const res = await suspend({ id: listing.id, reason }).unwrap();
      notify.success(res?.message || "Listing suspended.");
      setDialog(null);
    } catch (err) {
      notify.error(err?.message ?? "Couldn't suspend the listing.");
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
        // suspended because the owner is blocked: unblock the owner instead
        notify.error(message, {
          duration: 8000,
          action: {
            label: "Go to users",
            onClick: () => navigate("/admin/users"),
          },
        });
      } else {
        notify.error(message);
      }
    }
  };

  const control = ownerBlocked ? (
    compact ? (
      <Link
        to="/admin/users"
        className={`${btn} border border-border bg-bg text-text hover:bg-border/60`}
      >
        Owner blocked: unblock
      </Link>
    ) : (
      <p className="text-sm text-subtext">
        Suspended because its owner is blocked.{" "}
        <Link
          to="/admin/users"
          className="font-semibold text-primary underline"
        >
          Unblock the owner
        </Link>{" "}
        to restore it.
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
        description={`"${listing.title}" will be hidden from guests. The owner will see your reason.`}
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

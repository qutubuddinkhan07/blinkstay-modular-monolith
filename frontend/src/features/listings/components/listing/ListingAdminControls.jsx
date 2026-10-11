import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FiShield } from "react-icons/fi";
import { useAuth } from "../../../../context/AuthContext";
import { notify } from "../../../../utils/notify";
import { handleApiError } from "../../../../api/errors/handleApiError";
import ReasonDialog from "../../../../components/common/ReasonDialog";
import ConfirmDialog from "../../../../components/common/ConfirmDialogBox";
import { suspendListing, unsuspendListing } from "../../listingService";
import { focusRing } from "../create-listing/formStyles";

const btn = `cursor-pointer rounded-lg px-4 py-2 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;

// Shown to admins only. `onChanged` should reload the listing after a change.
const ListingAdminControls = ({
  listing,
  onChanged,
  className = "",
  compact = false,
}) => {
  const { isAdmin } = useAuth();
  const navigate = useNavigate();
  const [dialog, setDialog] = useState(null); // "suspend" | "unsuspend" | null
  const [busy, setBusy] = useState(false);

  if (!isAdmin) return null;

  const suspended = listing.status === "SUSPENDED";
  // only set if your DTO exposes suspensionSource; the 409 handling below covers it otherwise
  const ownerBlocked =
    suspended && listing.suspensionSource === "OWNER_BLOCKED";

  const handleSuspend = async (reason) => {
    setBusy(true);
    try {
      const res = await suspendListing(listing.id, reason);
      notify.success(res.data?.message || "Listing suspended.");
      setDialog(null);
      onChanged?.();
    } catch (err) {
      notify.error(handleApiError(err, "listing blocking"));
    } finally {
      setBusy(false);
    }
  };

  const handleUnsuspend = async () => {
    setBusy(true);
    try {
      const res = await unsuspendListing(listing.id);
      notify.success(res.data?.message || "Listing restored.");
      setDialog(null);
      onChanged?.();
    } catch (err) {
      setDialog(null);
      const message = handleApiError(err);

      if (err.response?.status === 409) {
        // suspended because the owner is blocked: unblock the owner instead
        notify.error(message, {
          duration: 8000,
          action: {
            label: "Go to users",
            onClick: () => navigate("/admin/users"),
          },
        });
      } else {
        notify.error(message, "listing blocking");
      }
    } finally {
      setBusy(false);
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

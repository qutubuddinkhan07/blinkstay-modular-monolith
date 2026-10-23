import { useEffect, useState } from "react";
import ReasonDialog from "../../../components/common/ReasonDialog";
import ClampedText from "../../../components/common/ClampedText";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import { formatDateLabel, formatINR } from "../../../utils/format";
import {
  BOOKINGS_CHANGED,
  adminCancelBooking,
  cancelMyBooking,
  getCancellationPreview,
  hostCancelBooking,
} from "../bookingService";

const CALLS = {
  guest: cancelMyBooking,
  host: hostCancelBooking,
  admin: adminCancelBooking,
};

const WHO = { GUEST: "the guest", HOST: "the host", ADMIN: "an admin" };

// Who cancelled, what was refunded and why. viewer="guest" turns "the guest" into "you".
export const CancellationNote = ({ booking: b, viewer }) => {
  if (b.status !== "CANCELLED") return null;

  const by =
    b.cancelledByRole === "GUEST" && viewer === "guest"
      ? "you"
      : WHO[b.cancelledByRole];

  const refund =
    b.refundAmount == null
      ? "No payment was taken."
      : Number(b.refundAmount) > 0
        ? `Refunded ${formatINR(b.refundAmount, b.currency)}.`
        : "No refund was due.";

  return (
    <div className="mt-4 rounded-xl border border-border bg-muted p-3 text-sm">
      <p className="font-semibold text-text">
        {by ? `Cancelled by ${by}` : "Cancelled"}
      </p>
      <p className="mt-0.5 text-subtext">{refund}</p>
      {b.cancellationReason && (
        <>
          <p className="mt-2 text-xs font-semibold uppercase tracking-wide text-subtext">
            Reason
          </p>
          <ClampedText
            text={b.cancellationReason}
            className="text-sm text-text"
          />
        </>
      )}
    </div>
  );
};

const Notice = ({ tone = "info", children }) => (
  <div
    className={`rounded-xl border p-3 text-sm text-text ${
      tone === "warn"
        ? "border-danger/40 bg-danger/10"
        : "border-border bg-muted"
    }`}
  >
    {children}
  </div>
);

const COPY = {
  guest: { title: "Cancel this booking?", label: "Reason", required: false },
  host: {
    title: "Cancel this booking for the guest?",
    label: "Reason for the guest",
    required: true,
  },
  // set required to false if your admin endpoint accepts no reason
  admin: { title: "Cancel this booking?", label: "Reason", required: true },
};

const BookingCancelDialog = ({
  open,
  booking,
  role = "guest",
  onClose,
  onDone,
}) => {
  const [busy, setBusy] = useState(false);
  const [preview, setPreview] = useState({
    loading: false,
    data: null,
    error: null,
  });

  // Guests see what the cancellation will cost or refund before they confirm
  useEffect(() => {
    if (!open || role !== "guest" || !booking?.id) return;

    let ignore = false;
    setPreview({ loading: true, data: null, error: null });

    getCancellationPreview(booking.id)
      .then((data) => {
        if (!ignore) setPreview({ loading: false, data, error: null });
      })
      .catch((err) => {
        if (!ignore)
          setPreview({
            loading: false,
            data: null,
            error: handleApiError(err),
          });
      });

    return () => {
      ignore = true;
    };
  }, [open, role, booking?.id]);

  const finish = (result) => {
    window.dispatchEvent(new Event(BOOKINGS_CHANGED)); // the navbar's "awaiting payment" badge
    onDone?.(result);
  };

  const handleConfirm = async (reason) => {
    setBusy(true);
    try {
      const result = await CALLS[role](booking.id, reason);
      const refund = Number(result?.refundAmount);
      notify.success(
        refund > 0
          ? `Booking cancelled. ${formatINR(refund, booking.currency)} will be refunded.`
          : "Booking cancelled.",
      );
      finish(result);
    } catch (err) {
      notify.error(handleApiError(err)); // e.g. "the stay has already started"
      // 404 or 409: what we showed was out of date (already cancelled, stay started...). Refresh.
      if ([404, 409].includes(err.response?.status)) finish(null);
    } finally {
      setBusy(false);
    }
  };

  const summary = booking
    ? `${booking.listingTitle} · ${formatDateLabel(booking.checkIn)} – ${formatDateLabel(booking.checkOut)}`
    : "";

  let notice;
  let confirmDisabled = false;
  let noRefund = false; // true when the preview says the guest gets nothing back

  if (role === "guest") {
    if (preview.loading) {
      notice = <Notice>Checking your refund…</Notice>;
      confirmDisabled = true;
    } else if (preview.error) {
      notice = <Notice tone="warn">{preview.error}</Notice>;
      confirmDisabled = true;
    } else if (preview.data) {
      const d = preview.data;
      const refund = d.refundAmount == null ? null : Number(d.refundAmount);

      noRefund = d.cancellable !== false && refund === 0;
      confirmDisabled = d.cancellable === false;

      notice = (
        <Notice tone={d.cancellable === false || noRefund ? "warn" : "info"}>
          {noRefund && (
            <p className="font-semibold">
              You won't get a refund if you cancel now.
            </p>
          )}
          <p className={noRefund ? "mt-1" : ""}>{d.message}</p>
          {d.cancellable !== false && refund > 0 && (
            <p className="mt-1 font-semibold">
              You'll get back {formatINR(refund, booking?.currency)}.
            </p>
          )}
        </Notice>
      );
    }
  } else if (role === "host") {
    notice = (
      <Notice tone="warn">
        The guest will be refunded in full. They will see your reason.
      </Notice>
    );
  } else {
    notice = (
      <Notice>
        If the booking was paid, the guest is refunded and the host is told.
        Both will see the reason.
      </Notice>
    );
  }

  const copy = COPY[role];

  return (
    <ReasonDialog
      open={open}
      title={copy.title}
      description={summary}
      notice={notice}
      label={copy.label}
      required={copy.required}
      confirmLabel={noRefund ? "Cancel without refund" : "Cancel booking"}
      cancelLabel="Keep booking"
      busyLabel="Cancelling…"
      busy={busy}
      confirmDisabled={confirmDisabled}
      onConfirm={handleConfirm}
      onCancel={onClose}
    />
  );
};

export default BookingCancelDialog;

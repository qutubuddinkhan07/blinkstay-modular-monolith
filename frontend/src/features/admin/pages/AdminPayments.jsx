import { useCallback, useEffect, useRef, useState } from "react";
import { FiRefreshCw } from "react-icons/fi";
import { handleApiError } from "../../../api/errors/handleApiError";
import { notify } from "../../../utils/notify";
import { formatINR, formatTimestamp, plural } from "../../../utils/format";
import { usePageTitle } from "../../../utils/usePageTitle";
import ClampedText from "../../../components/common/ClampedText";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import { primaryBtn } from "../../user/formStyles";
import {
  getPaymentsNeedingAttention,
  resolvePayment,
  retryRefund,
} from "../adminService";

const smallBtn = `cursor-pointer rounded-lg border border-border bg-bg px-3 py-2 text-sm font-semibold text-text transition-colors hover:bg-border/60 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;

const statusLabel = (s) =>
  s ? s.charAt(0) + s.slice(1).toLowerCase().replaceAll("_", " ") : "Unknown";

const StatusPill = ({ status }) => (
  <span className="inline-flex items-center gap-1.5 rounded-full border border-border px-2.5 py-0.5 text-xs font-semibold text-text">
    <span
      className={`h-2 w-2 rounded-full ${
        status === "REFUND_FAILED" ? "bg-danger" : "bg-amber-500"
      }`}
      aria-hidden="true"
    />
    {statusLabel(status)}
  </span>
);

const AdminPayments = () => {
  usePageTitle("Payments needing attention");

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [dialog, setDialog] = useState(null); // { type: "retry" | "resolve", payment }
  const [busy, setBusy] = useState(false);
  const reqRef = useRef(0); // only the latest request may write state
  const timerRef = useRef(null);

  // quiet = refresh in the background (keep what's shown, toast on failure)
  const load = useCallback(async ({ quiet = false } = {}) => {
    const req = ++reqRef.current;
    if (!quiet) {
      setLoading(true);
      setError(null);
    }

    try {
      const res = await getPaymentsNeedingAttention();
      if (req !== reqRef.current) return;

      const d = res.data?.data;
      setRows(Array.isArray(d) ? d : (d?.content ?? []));
      setError(null);
    } catch (err) {
      if (req !== reqRef.current) return;
      if (quiet) notify.error(handleApiError(err));
      else setError(handleApiError(err, "payments"));
    } finally {
      if (req === reqRef.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    return () => {
      reqRef.current++; // ignore anything still in flight
      clearTimeout(timerRef.current);
    };
  }, [load]);

  const run = async (call, successMessage) => {
    setBusy(true);
    try {
      const res = await call(dialog.payment.paymentId);
      notify.success(res.data?.message || successMessage);
      setDialog(null);
      await load({ quiet: true });

      // refunds are sent a few seconds after the request: look again shortly
      clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => load({ quiet: true }), 4000);
    } catch (err) {
      notify.error(handleApiError(err));
      // 404 or 409: our list was out of date (already retried or resolved). Refresh.
      if ([404, 409].includes(err.response?.status)) {
        setDialog(null);
        load({ quiet: true });
      }
    } finally {
      setBusy(false);
    }
  };

  const p = dialog?.payment;
  const firstLoad = loading && rows.length === 0;

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-5xl">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="font-display text-3xl font-semibold">
              Payments needing attention
            </h1>
            <p className="mt-2 text-subtext">
              Refunds that failed and payments flagged for review.
              {!loading && !error && ` ${rows.length} open.`}
            </p>
          </div>

          <button
            type="button"
            onClick={() => load({ quiet: true })}
            className={`inline-flex items-center gap-2 ${smallBtn}`}
          >
            <FiRefreshCw size={14} aria-hidden="true" />
            Refresh
          </button>
        </div>

        {firstLoad && (
          <ul className="space-y-3" aria-busy="true">
            {[0, 1, 2].map((i) => (
              <li
                key={i}
                className="h-32 animate-pulse rounded-2xl bg-input motion-reduce:animate-none"
              />
            ))}
          </ul>
        )}

        {!loading && error && (
          <div
            role="alert"
            className="rounded-2xl border border-border bg-surface p-6 text-center"
          >
            <p className="text-danger">{error}</p>
            <button
              type="button"
              onClick={() => load()}
              className={`mt-4 cursor-pointer ${primaryBtn}`}
            >
              Try again
            </button>
          </div>
        )}

        {!loading && !error && rows.length === 0 && (
          <div className="rounded-2xl border border-border bg-surface p-8 text-center text-subtext">
            Nothing needs attention right now.
          </div>
        )}

        {!error && rows.length > 0 && (
          <ul className="space-y-3">
            {rows.map((row) => (
              <li
                key={row.paymentId}
                className="rounded-2xl border border-border bg-surface p-5"
              >
                <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-mono text-sm font-semibold text-text">
                        {row.bookingReference}
                      </span>
                      <StatusPill status={row.status} />
                      {row.provider && (
                        <span className="text-xs capitalize text-subtext">
                          {row.provider.toLowerCase()}
                        </span>
                      )}
                    </div>

                    <p className="mt-2 text-lg font-semibold text-text">
                      {formatINR(row.amount, row.currency)}
                    </p>
                    <p className="text-xs text-subtext">
                      Updated {formatTimestamp(row.updatedAt)}
                      {row.refundAttempts > 0 &&
                        ` · ${plural(row.refundAttempts, "refund attempt")}`}
                    </p>

                    {row.refundError && (
                      <div className="mt-3 rounded-xl border border-danger/40 bg-danger/10 p-3 text-sm">
                        <p className="font-semibold text-text">Refund error</p>
                        <ClampedText
                          text={row.refundError}
                          className="mt-1 text-subtext"
                        />
                      </div>
                    )}

                    {row.reviewReason && (
                      <div className="mt-3 rounded-xl border border-border bg-muted p-3 text-sm">
                        <p className="font-semibold text-text">Needs review</p>
                        <ClampedText
                          text={row.reviewReason}
                          className="mt-1 text-subtext"
                        />
                      </div>
                    )}
                  </div>

                  <div className="flex shrink-0 flex-wrap gap-2">
                    {row.status === "REFUND_FAILED" && (
                      <button
                        type="button"
                        onClick={() =>
                          setDialog({ type: "retry", payment: row })
                        }
                        className={`${smallBtn} !bg-primary !text-on-primary hover:!bg-primary-hover`}
                      >
                        Retry refund
                      </button>
                    )}
                    {row.reviewReason && (
                      <button
                        type="button"
                        onClick={() =>
                          setDialog({ type: "resolve", payment: row })
                        }
                        className={smallBtn}
                      >
                        Mark resolved
                      </button>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <ConfirmDialog
        open={dialog?.type === "retry"}
        title="Retry this refund?"
        description={`Sends the refund for ${p?.bookingReference ?? "this booking"} (${formatINR(p?.amount, p?.currency)}) again. It can take a few seconds.`}
        confirmLabel="Retry refund"
        busyLabel="Retrying…"
        tone="primary"
        busy={busy}
        onConfirm={() => run(retryRefund, "Refund retry started.")}
        onCancel={() => setDialog(null)}
      />

      <ConfirmDialog
        open={dialog?.type === "resolve"}
        title="Mark as resolved?"
        description={`Confirms you've dealt with ${p?.bookingReference ?? "this payment"}. It leaves this list.`}
        confirmLabel="Mark resolved"
        busyLabel="Saving…"
        tone="primary"
        busy={busy}
        onConfirm={() => run(resolvePayment, "Marked as resolved.")}
        onCancel={() => setDialog(null)}
      />
    </div>
  );
};

export default AdminPayments;

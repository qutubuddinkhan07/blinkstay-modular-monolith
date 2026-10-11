import { useEffect, useState } from "react";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import ClampedText from "../../../components/common/ClampedText";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";
import ReasonDialog from "../../../components/common/ReasonDialog";
import Pagination from "../../listings/components/explore-page/pagination/Pagination";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import {
  HOST_REQUESTS_CHANGED,
  approveHostRequest,
  fetchHostRequests,
  rejectHostRequest,
} from "../adminService";

const PAGE_SIZE = 10;

const TABS = [
  { value: "PENDING", label: "Pending" },
  { value: "APPROVED", label: "Approved" },
  { value: "REJECTED", label: "Rejected" },
];

const STATUS_DOT = {
  PENDING: "bg-amber-500",
  APPROVED: "bg-emerald-500",
  REJECTED: "bg-danger",
  CANCELLED: "bg-subtext",
};

const smallBtn = `cursor-pointer rounded-lg border border-border bg-bg px-3 py-2 text-sm font-semibold text-text transition-colors hover:bg-border/60 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;
const primaryBtn = `cursor-pointer rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;
const dangerBtn = `cursor-pointer rounded-lg border border-danger/50 bg-bg px-3 py-2 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;

const formatDate = (value) => {
  const d = new Date(value);
  return Number.isNaN(d.getTime())
    ? null
    : d.toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
      });
};

// tolerate a few field names for the applicant and the date
const toRow = (r) => ({
  ...r,
  applicantName:
    r.applicantName ??
    r.username ??
    r.user?.username ??
    r.applicant?.username ??
    "Unknown applicant",
  applicantEmail:
    r.applicantEmail ?? r.email ?? r.user?.email ?? r.applicant?.email ?? "",
  submittedAt: r.createdAt ?? r.submittedAt ?? r.requestedAt ?? null,
});

const toPage = (d = {}) => ({
  items: (d.content ?? d.items ?? []).map(toRow),
  totalPages: d.totalPages ?? 0,
  totalElements: d.totalElements ?? d.total ?? null,
});

const AdminHostRequests = () => {
  const [status, setStatus] = useState("PENDING");
  const [page, setPage] = useState(0);
  const [data, setData] = useState({
    items: [],
    totalPages: 0,
    totalElements: null,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  const [dialog, setDialog] = useState(null); // { type: "approve" | "reject", request }
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let ignore = false;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const res = await fetchHostRequests({ status, page, size: PAGE_SIZE });
        if (ignore) return;

        const next = toPage(res.data.data);

        // the last page emptied out after an approve or reject: step back one page
        if (next.items.length === 0 && page > 0 && next.totalPages > 0) {
          setPage(next.totalPages - 1);
          return;
        }
        setData(next);
      } catch (err) {
        if (ignore) return;
        console.error("Failed to load host requests", err);
        setError(handleApiError(err, "host requests"));
      } finally {
        if (!ignore) setLoading(false);
      }
    };

    load();
    return () => {
      ignore = true;
    };
  }, [status, page, reloadKey]);

  const afterAction = () => {
    setDialog(null);
    setReloadKey((k) => k + 1);
    window.dispatchEvent(new Event(HOST_REQUESTS_CHANGED)); // refresh the navbar badge
  };

  const handleApprove = async () => {
    setBusy(true);
    try {
      const res = await approveHostRequest(dialog.request.id);
      notify.success(res.data?.message || "Request approved.");
      afterAction();
    } catch (err) {
      notify.error(handleApiError(err)); // e.g. "This request is already approved"
      if (err.response?.status === 409) afterAction();
      else setDialog(null);
    } finally {
      setBusy(false);
    }
  };

  const handleReject = async (reason) => {
    setBusy(true);
    try {
      const res = await rejectHostRequest(dialog.request.id, reason);
      notify.success(res.data?.message || "Request rejected.");
      afterAction();
    } catch (err) {
      notify.error(handleApiError(err));
      if (err.response?.status === 409) afterAction();
    } finally {
      setBusy(false);
    }
  };

  const firstLoad = loading && data.items.length === 0;

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <h1 className="font-display text-3xl font-semibold">Host requests</h1>
          <p className="mt-2 text-subtext">
            Review applications from people who want to list properties.
            {data.totalElements != null && ` ${data.totalElements} found.`}
          </p>
        </div>

        <div
          className="mb-6 inline-flex rounded-lg border border-border bg-surface p-1"
          role="group"
          aria-label="Filter by status"
        >
          {TABS.map((t) => (
            <button
              key={t.value}
              type="button"
              aria-pressed={status === t.value}
              onClick={() => {
                setStatus(t.value);
                setPage(0);
              }}
              className={`cursor-pointer rounded-md px-4 py-1.5 text-sm font-semibold transition-colors ${focusRing} ${
                status === t.value
                  ? "bg-primary text-on-primary"
                  : "text-subtext hover:text-text"
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {firstLoad && (
          <ul className="space-y-4" aria-busy="true">
            {Array.from({ length: 3 }, (_, i) => (
              <li
                key={i}
                className="h-36 animate-pulse rounded-2xl border border-border bg-surface motion-reduce:animate-none"
              />
            ))}
          </ul>
        )}

        {!loading && error && (
          <div className="py-16 text-center">
            <h2 className="font-display text-xl font-semibold">
              Couldn't load host requests
            </h2>
            <p className="mt-2 text-subtext">{error}</p>
            <button
              type="button"
              onClick={() => setReloadKey((k) => k + 1)}
              className={`mt-5 ${primaryBtn}`}
            >
              Try again
            </button>
          </div>
        )}

        {!loading && !error && data.items.length === 0 && (
          <p className="py-16 text-center text-subtext">
            {status === "PENDING"
              ? "No pending requests. You're all caught up."
              : "Nothing here yet."}
          </p>
        )}

        {!error && data.items.length > 0 && (
          <div
            className={loading ? "opacity-60 transition-opacity" : ""}
            aria-busy={loading}
          >
            <ul className="space-y-4">
              {data.items.map((r) => (
                <li
                  key={r.id}
                  className="rounded-2xl border border-border bg-surface p-5 transition-colors duration-300"
                >
                  <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <h2 className="truncate font-display text-lg font-medium">
                          {r.businessName}
                        </h2>
                        <span className="inline-flex items-center gap-1.5 rounded-full border border-border px-2.5 py-0.5 text-xs font-semibold capitalize">
                          <span
                            className={`h-2 w-2 rounded-full ${STATUS_DOT[r.status] ?? "bg-subtext"}`}
                            aria-hidden="true"
                          />
                          {r.status?.toLowerCase()}
                        </span>
                      </div>

                      <p className="mt-1 text-sm text-subtext">
                        {r.applicantName}
                        {r.applicantEmail && (
                          <>
                            {" "}
                            ·{" "}
                            <span className="break-all">
                              {r.applicantEmail}
                            </span>
                          </>
                        )}
                        {r.submittedAt && formatDate(r.submittedAt) && (
                          <> · Sent {formatDate(r.submittedAt)}</>
                        )}
                      </p>
                      <p className="mt-1 text-sm text-subtext">{r.address}</p>

                      {r.website && (
                        <a
                          href={r.website}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="mt-1 inline-block break-all text-sm text-primary underline"
                        >
                          {r.website}
                        </a>
                      )}

                      <div className="mt-3 text-sm">
                        <ClampedText
                          text={r.description}
                          className="text-text"
                        />
                      </div>

                      {r.status === "REJECTED" && r.rejectionReason && (
                        <div className="mt-3 rounded-xl border border-danger/40 bg-danger/10 p-3 text-sm">
                          <p className="font-semibold">Rejection reason</p>
                          <ClampedText
                            text={r.rejectionReason}
                            className="mt-1 text-subtext"
                          />
                        </div>
                      )}
                    </div>

                    {r.status === "PENDING" && (
                      <div className="flex shrink-0 flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() =>
                            setDialog({ type: "approve", request: r })
                          }
                          className={primaryBtn}
                        >
                          Approve
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            setDialog({ type: "reject", request: r })
                          }
                          className={dangerBtn}
                        >
                          Reject
                        </button>
                      </div>
                    )}
                  </div>
                </li>
              ))}
            </ul>
            <Pagination
              page={page}
              totalPages={data.totalPages}
              onChange={setPage}
            />
          </div>
        )}
      </div>

      <ConfirmDialog
        open={dialog?.type === "approve"}
        title={`Approve ${dialog?.request?.businessName ?? "this request"}?`}
        description={`${dialog?.request?.applicantName ?? "The applicant"} becomes a host and can create listings straight away.`}
        confirmLabel="Approve"
        busyLabel="Approving…"
        tone="primary"
        busy={busy}
        onConfirm={handleApprove}
        onCancel={() => setDialog(null)}
      />

      <ReasonDialog
        open={dialog?.type === "reject"}
        title={`Reject ${dialog?.request?.businessName ?? "this request"}?`}
        description="The applicant will see your reason and can apply again."
        confirmLabel="Reject request"
        busyLabel="Rejecting…"
        busy={busy}
        onConfirm={handleReject}
        onCancel={() => setDialog(null)}
      />
    </div>
  );
};

export default AdminHostRequests;

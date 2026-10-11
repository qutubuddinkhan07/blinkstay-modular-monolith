import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import ClampedText from "../../../components/common/ClampedText";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import { ghostBtn, primaryBtn } from "../../user/formStyles";
import {
  applyForHost,
  cancelMyHostRequest,
  fetchMyHostRequest,
} from "../hostService";
import HostDetailsForm from "../components/HostDetailsForm";

const STATUS = {
  PENDING: { label: "Pending review", dot: "bg-amber-500" },
  APPROVED: { label: "Approved", dot: "bg-emerald-500" },
  REJECTED: { label: "Rejected", dot: "bg-danger" },
  CANCELLED: { label: "Cancelled", dot: "bg-subtext" },
};

const formatDate = (value) => {
  const d = new Date(value);
  return Number.isNaN(d.getTime())
    ? null
    : d.toLocaleDateString(undefined, {
        year: "numeric",
        month: "long",
        day: "numeric",
      });
};

const Card = ({ title, children }) => (
  <section className="rounded-2xl border border-border bg-surface p-6 shadow-sm">
    {title && (
      <h2 className="mb-4 font-display text-lg font-semibold">{title}</h2>
    )}
    {children}
  </section>
);

const Detail = ({ label, children }) => (
  <div>
    <dt className="text-xs font-semibold uppercase tracking-wide text-subtext">
      {label}
    </dt>
    <dd className="mt-0.5 break-words text-sm text-text">{children}</dd>
  </div>
);

const BecomeHost = () => {
  const { isManager, refreshUser } = useAuth();

  const [request, setRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reapply, setReapply] = useState(false); // REJECTED: showing the form again
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      const res = await fetchMyHostRequest();
      setRequest(res.data.data ?? null);
    } catch (err) {
      setError(handleApiError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isManager) {
      setLoading(false); // hosts have nothing to apply for
      return;
    }
    load();
  }, [isManager, load]);

  // Approved but this browser still has the old roles: pull the fresh user so the navbar updates
  useEffect(() => {
    if (request?.status === "APPROVED" && !isManager) refreshUser();
  }, [request?.status, isManager, refreshUser]);

  const handleApply = async (values) => {
    const body = {
      businessName: values.businessName,
      address: values.address,
      description: values.description,
      ...(values.website && { website: values.website }), // optional: leave it out when empty
    };

    try {
      const res = await applyForHost(body);
      notify.success(
        res.data?.message || "Application sent. We'll review it soon.",
      );
      setReapply(false);
      await load();
    } catch (err) {
      notify.error(handleApiError(err)); // e.g. "You already have a pending request"
      if (err.response?.status === 409) load(); // our view was out of date
    }
  };

  const handleCancel = async () => {
    setCancelling(true);
    try {
      const res = await cancelMyHostRequest();
      notify.success(res.data?.message || "Application cancelled.");
      setConfirmCancel(false);
      await load();
    } catch (err) {
      notify.error(handleApiError(err));
      setConfirmCancel(false);
      if (err.response?.status === 409) load();
    } finally {
      setCancelling(false);
    }
  };

  const shell = (children) => (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-2xl space-y-6">
        <div>
          <h1 className="font-display text-3xl font-semibold">Become a host</h1>
          <p className="mt-2 text-subtext">
            List your properties on BlinkStay. Tell us about your business and
            we'll review it.
          </p>
        </div>
        {children}
      </div>
    </div>
  );

  if (loading) {
    return shell(
      <div
        className="h-48 animate-pulse rounded-2xl border border-border bg-surface motion-reduce:animate-none"
        aria-busy="true"
      />,
    );
  }

  // already a host: nothing to apply for
  if (isManager) {
    return shell(
      <Card title="You're already a host">
        <p className="text-sm text-subtext">
          Your account can create and manage listings.
        </p>
        <div className="mt-4 flex flex-wrap gap-2">
          <Link to="/my-listings" className={`${primaryBtn} inline-block`}>
            My listings
          </Link>
          <Link to="/host/business" className={`${ghostBtn} inline-block`}>
            Business profile
          </Link>
        </div>
      </Card>,
    );
  }

  if (error) {
    return shell(
      <Card>
        <p className="font-semibold">Couldn't load your application</p>
        <p className="mt-1 text-sm text-subtext">{error}</p>
        <button
          type="button"
          onClick={() => {
            setLoading(true);
            load();
          }}
          className={`mt-4 cursor-pointer ${primaryBtn}`}
        >
          Try again
        </button>
      </Card>,
    );
  }

  const status = request?.status;
  const meta = STATUS[status];
  const showForm =
    !request || status === "CANCELLED" || (status === "REJECTED" && reapply);

  if (status === "APPROVED") {
    return shell(
      <Card title="You're approved">
        <p className="text-sm text-subtext">
          Your host account is being set up. If your menu doesn't show the host
          links yet, refresh your account.
        </p>
        <button
          type="button"
          onClick={() => refreshUser()}
          className={`mt-4 cursor-pointer ${primaryBtn}`}
        >
          Refresh my account
        </button>
      </Card>,
    );
  }

  if (showForm) {
    return shell(
      <Card title={status === "REJECTED" ? "Apply again" : "Your business"}>
        <HostDetailsForm
          initial={request} // pre-fills after a rejection or cancellation
          submitLabel="Send application"
          busyLabel="Sending…"
          onSubmit={handleApply}
          onCancel={status === "REJECTED" ? () => setReapply(false) : undefined}
        />
      </Card>,
    );
  }

  // PENDING or REJECTED: show what was sent
  return shell(
    <>
      <Card>
        <div className="mb-4 flex flex-wrap items-center gap-3">
          <span className="inline-flex items-center gap-1.5 rounded-full border border-border px-2.5 py-0.5 text-xs font-semibold">
            <span
              className={`h-2 w-2 rounded-full ${meta?.dot ?? "bg-subtext"}`}
              aria-hidden="true"
            />
            {meta?.label ?? status}
          </span>
          {request.createdAt && formatDate(request.createdAt) && (
            <span className="text-xs text-subtext">
              Sent {formatDate(request.createdAt)}
            </span>
          )}
        </div>

        {status === "REJECTED" && (
          <div
            role="alert"
            className="mb-5 rounded-xl border border-danger/40 bg-danger/10 p-4 text-sm"
          >
            <p className="font-semibold">Your application was not approved</p>
            {request.rejectionReason ? (
              <ClampedText
                text={request.rejectionReason}
                className="mt-1 text-subtext"
              />
            ) : (
              <p className="mt-1 text-subtext">No reason was given.</p>
            )}
          </div>
        )}

        <dl className="space-y-4">
          <Detail label="Business name">{request.businessName}</Detail>
          <Detail label="Address">{request.address}</Detail>
          <Detail label="About">
            <ClampedText text={request.description} />
          </Detail>
          {request.website && (
            <Detail label="Website">
              <a
                href={request.website}
                target="_blank"
                rel="noopener noreferrer"
                className="break-all text-primary underline"
              >
                {request.website}
              </a>
            </Detail>
          )}
        </dl>

        <div className="mt-6 flex flex-wrap gap-2 border-t border-border pt-4">
          {status === "PENDING" ? (
            <button
              type="button"
              onClick={() => setConfirmCancel(true)}
              className={`cursor-pointer rounded-lg border border-danger/50 bg-bg px-4 py-2 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 ${focusRing}`}
            >
              Cancel application
            </button>
          ) : (
            <button
              type="button"
              onClick={() => setReapply(true)}
              className={`cursor-pointer ${primaryBtn}`}
            >
              Apply again
            </button>
          )}
        </div>
      </Card>

      {status === "PENDING" && (
        <p className="text-sm text-subtext">
          We'll review your application as soon as we can. Check back here for
          the result.
        </p>
      )}

      <ConfirmDialog
        open={confirmCancel}
        title="Cancel your application?"
        description="You can send a new application any time."
        confirmLabel="Cancel application"
        busyLabel="Cancelling…"
        busy={cancelling}
        onConfirm={handleCancel}
        onCancel={() => setConfirmCancel(false)}
      />
    </>,
  );
};

export default BecomeHost;

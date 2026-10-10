import { FiAlertTriangle, FiInfo } from "react-icons/fi";
import ClampedText from "./ClampedText";
import { SUPPORT_EMAIL, supportMailto } from "../../utils/support";

const INFO = {
  DRAFT: "This listing is a draft. Only you and admins can see it.",
  PAUSED:
    "This listing is paused. Guests can't see or book it until it's resumed.",
};

const StatusBanner = ({
  status,
  reason,
  suspendedAt,
  title,
  className = "",
}) => {
  if (status === "SUSPENDED") {
    const since = suspendedAt
      ? new Date(suspendedAt).toLocaleDateString(undefined, {
          year: "numeric",
          month: "short",
          day: "numeric",
        })
      : null;

    return (
      <div
        role="alert"
        className={`rounded-xl border border-danger/40 bg-danger/10 p-4 text-left ${className}`}
      >
        <div className="flex items-start gap-3">
          <FiAlertTriangle
            className="mt-0.5 shrink-0 text-danger"
            size={18}
            aria-hidden="true"
          />
          <div className="min-w-0 text-sm">
            <p className="font-semibold text-text">
              This listing is suspended{since ? ` (since ${since})` : ""}
            </p>
            {reason ? (
              <ClampedText text={reason} className="mt-1 text-subtext" />
            ) : (
              <p className="mt-1 text-subtext">No reason was provided.</p>
            )}
            <p className="mt-2 text-subtext">
              Questions?{" "}
              <a
                href={supportMailto(
                  `Suspended listing${title ? `: ${title}` : ""}`,
                )}
                className="break-all font-semibold text-primary underline"
              >
                Contact support
              </a>{" "}
              <span className="sr-only">at {SUPPORT_EMAIL}</span>
            </p>
          </div>
        </div>
      </div>
    );
  }

  const text = INFO[status];
  if (!text) return null; // PUBLISHED and unknown statuses show nothing

  return (
    <div
      className={`flex items-start gap-3 rounded-xl border border-border bg-muted p-4 text-sm text-text ${className}`}
    >
      <FiInfo
        className="mt-0.5 shrink-0 text-subtext"
        size={18}
        aria-hidden="true"
      />
      <p>{text}</p>
    </div>
  );
};

export default StatusBanner;

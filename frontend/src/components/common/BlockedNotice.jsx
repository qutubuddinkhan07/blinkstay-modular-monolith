import { useState } from "react";
import { FiSlash } from "react-icons/fi";
import { SUPPORT_EMAIL, supportMailto } from "../../utils/support";

const LONG_MESSAGE = 160; // characters before we offer "Show more"

const BlockedNotice = ({ message }) => {
  const [expanded, setExpanded] = useState(false);
  const isLong = (message?.length ?? 0) > LONG_MESSAGE;

  return (
    <div
      role="alert"
      className="rounded-xl border border-danger/40 bg-danger/10 p-4 text-left"
    >
      <div className="flex items-start gap-3">
        <FiSlash
          className="mt-0.5 shrink-0 text-danger"
          size={18}
          aria-hidden="true"
        />
        <div className="min-w-0">
          <p className="text-sm font-semibold text-text">
            Your account is blocked
          </p>

          {message && (
            <>
              <p
                className={`mt-1 whitespace-pre-line break-words text-sm text-subtext ${
                  expanded ? "" : "line-clamp-3"
                }`}
              >
                {message}
              </p>
              {isLong && (
                <button
                  type="button"
                  onClick={() => setExpanded((v) => !v)}
                  aria-expanded={expanded}
                  className="mt-1 text-xs font-semibold text-primary hover:underline focus-visible:outline-2 focus-visible:outline-primary"
                >
                  {expanded ? "Show less" : "Show more"}
                </button>
              )}
            </>
          )}

          <p className="mt-3 text-sm text-subtext">
            If you think this is a mistake, contact{" "}
            <a
              href={supportMailto("Account blocked")}
              className="break-all font-semibold text-primary underline"
            >
              {SUPPORT_EMAIL}
            </a>
            .
          </p>
        </div>
      </div>
    </div>
  );
};

export default BlockedNotice;

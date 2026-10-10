import { useState } from "react";

const ClampedText = ({ text, limit = 160, className = "" }) => {
  const [open, setOpen] = useState(false);
  const isLong = (text?.length ?? 0) > limit;

  return (
    <>
      <p
        className={`whitespace-pre-line break-words ${open ? "" : "line-clamp-3"} ${className}`}
      >
        {text}
      </p>
      {isLong && (
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
          className="mt-1 cursor-pointer text-xs font-semibold text-primary hover:underline focus-visible:outline-2 focus-visible:outline-primary"
        >
          {open ? "Show less" : "Show more"}
        </button>
      )}
    </>
  );
};

export default ClampedText;

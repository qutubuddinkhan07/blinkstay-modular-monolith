import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { FiX } from "react-icons/fi";

const ZoomImage = ({
  src,
  alt = "",
  className = "", // box styles, e.g. "h-11 w-11 rounded-full"
  imgClassName = "object-cover", // how the image fits inside the box
  zoomSrc, // optional higher-res image for the preview
  caption, // optional text under the enlarged image
  fallback = null, // shown when there's no src or it fails to load
  zoomable = true,
  loading = "lazy",
}) => {
  const [failed, setFailed] = useState(false);
  const [open, setOpen] = useState(false);
  const triggerRef = useRef(null);
  const closeRef = useRef(null);
  const reduceMotion = useReducedMotion();

  useEffect(() => setFailed(false), [src]);

  // while the preview is open: lock page scroll, handle Escape, keep focus inside
  useEffect(() => {
    if (!open) return;

    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    closeRef.current?.focus();

    const onKey = (e) => {
      if (e.key === "Escape") setOpen(false);
      if (e.key === "Tab") e.preventDefault(); // the close button is the only focusable element
    };
    document.addEventListener("keydown", onKey);

    return () => {
      document.body.style.overflow = prevOverflow;
      document.removeEventListener("keydown", onKey);
      triggerRef.current?.focus(); // give focus back to the thumbnail
    };
  }, [open]);

  if (!src || failed) return fallback;

  const image = (
    <img
      src={src}
      alt={alt}
      loading={loading}
      referrerPolicy="no-referrer"
      onError={() => setFailed(true)}
      className={`h-full w-full ${imgClassName}`}
    />
  );

  if (!zoomable) {
    return (
      <span className={`block overflow-hidden ${className}`}>{image}</span>
    );
  }

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        onClick={() => setOpen(true)}
        aria-label={alt ? `View larger: ${alt}` : "View larger image"}
        aria-haspopup="dialog"
        className={`block shrink-0 cursor-zoom-in overflow-hidden focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${className}`}
      >
        {image}
      </button>

      {createPortal(
        <AnimatePresence>
          {open && (
            <motion.div
              role="dialog"
              aria-modal="true"
              aria-label={alt || "Image preview"}
              onClick={() => setOpen(false)}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.15 }}
              className="fixed inset-0 z-[70] flex flex-col items-center justify-center gap-3 bg-black/80 p-4"
            >
              <button
                ref={closeRef}
                type="button"
                onClick={() => setOpen(false)}
                aria-label="Close preview"
                className="absolute right-4 top-4 flex h-10 w-10 cursor-pointer items-center justify-center rounded-full bg-white/10 text-white transition-colors hover:bg-white/20 focus-visible:outline-2 focus-visible:outline-white"
              >
                <FiX size={22} />
              </button>

              <motion.img
                src={zoomSrc || src}
                alt={alt}
                onClick={(e) => e.stopPropagation()} // clicking the image itself doesn't close
                initial={reduceMotion ? false : { scale: 0.95 }}
                animate={{ scale: 1 }}
                transition={{ duration: 0.18 }}
                className="max-h-[85vh] max-w-full rounded-lg object-contain shadow-2xl"
              />

              {caption && (
                <p className="max-w-xl text-center text-sm text-white/90">
                  {caption}
                </p>
              )}
            </motion.div>
          )}
        </AnimatePresence>,
        document.body,
      )}
    </>
  );
};

export default ZoomImage;

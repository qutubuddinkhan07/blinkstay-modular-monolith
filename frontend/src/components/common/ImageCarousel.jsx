import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { motion, useReducedMotion } from "motion/react";
import { FiChevronLeft, FiChevronRight, FiX } from "react-icons/fi";

const MAX_DOTS = 6; // beyond this, show a "3 / 12" counter instead of dots

const toUrls = (images = []) =>
  images.map((i) => (typeof i === "string" ? i : i?.url)).filter(Boolean);

const arrowBase =
  "absolute top-1/2 z-10 flex -translate-y-1/2 cursor-pointer items-center justify-center rounded-full bg-black/50 text-white transition-opacity hover:bg-black/70 focus-visible:outline-2 focus-visible:outline-white";

/* ---------------- Full-screen viewer (portal) ---------------- */

const Lightbox = ({ urls, start, alt, caption, onClose }) => {
  const [idx, setIdx] = useState(start);
  const dialogRef = useRef(null);
  const touchX = useRef(null);
  const idxRef = useRef(idx);
  idxRef.current = idx;
  const reduceMotion = useReducedMotion();

  const close = useCallback(() => onClose(idxRef.current), [onClose]);
  const step = useCallback(
    (d) => setIdx((i) => Math.max(0, Math.min(urls.length - 1, i + d))),
    [urls.length],
  );

  useEffect(() => {
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const buttons = () => [
      ...(dialogRef.current?.querySelectorAll("button") ?? []),
    ];
    buttons()[0]?.focus(); // the close button comes first in the DOM

    const onKey = (e) => {
      if (e.key === "Escape") close();
      else if (e.key === "ArrowLeft") step(-1);
      else if (e.key === "ArrowRight") step(1);
      else if (e.key === "Tab") {
        // keep focus inside the viewer
        const f = buttons();
        if (f.length === 0) return;
        const first = f[0];
        const last = f[f.length - 1];
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };
    document.addEventListener("keydown", onKey);

    return () => {
      document.body.style.overflow = prevOverflow;
      document.removeEventListener("keydown", onKey);
    };
  }, [close, step]);

  const onTouchEnd = (e) => {
    if (touchX.current == null) return;
    const dx = e.changedTouches[0].clientX - touchX.current;
    touchX.current = null;
    if (Math.abs(dx) > 50) step(dx < 0 ? 1 : -1); // swipe left = next
  };

  return createPortal(
    <motion.div
      ref={dialogRef}
      role="dialog"
      aria-modal="true"
      aria-label={alt || "Image viewer"}
      onClick={close}
      onTouchStart={(e) => (touchX.current = e.touches[0].clientX)}
      onTouchEnd={onTouchEnd}
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.15 }}
      className="fixed inset-0 z-[70] flex flex-col items-center justify-center gap-3 bg-black/85 p-4"
    >
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          close();
        }}
        aria-label="Close viewer"
        className="absolute right-4 top-4 z-10 flex h-10 w-10 cursor-pointer items-center justify-center rounded-full bg-white/10 text-white transition-colors hover:bg-white/20 focus-visible:outline-2 focus-visible:outline-white"
      >
        <FiX size={22} />
      </button>

      <motion.img
        key={idx}
        src={urls[idx]}
        alt={alt ? `${alt} (${idx + 1} of ${urls.length})` : ""}
        onClick={(e) => e.stopPropagation()}
        initial={reduceMotion ? false : { opacity: 0.4, scale: 0.98 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.15 }}
        className="max-h-[80vh] max-w-full select-none rounded-lg object-contain shadow-2xl"
      />

      {urls.length > 1 && (
        <>
          {idx > 0 && (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                step(-1);
              }}
              aria-label="Previous image"
              className={`${arrowBase} left-3 h-11 w-11`}
            >
              <FiChevronLeft size={24} />
            </button>
          )}
          {idx < urls.length - 1 && (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                step(1);
              }}
              aria-label="Next image"
              className={`${arrowBase} right-3 h-11 w-11`}
            >
              <FiChevronRight size={24} />
            </button>
          )}
        </>
      )}

      <p className="text-center text-sm text-white/90" aria-live="polite">
        {urls.length > 1 && (
          <span className="mr-2 rounded-full bg-white/10 px-2 py-0.5 text-xs">
            {idx + 1} / {urls.length}
          </span>
        )}
        {caption}
      </p>
    </motion.div>,
    document.body,
  );
};

/* ---------------- Inline slider ---------------- */

const ImageCarousel = ({
  images,
  alt = "",
  className = "", // box styles: size, rounding, e.g. "h-32 w-full rounded-lg"
  imgClassName = "object-cover",
  zoomable = true, // false: no viewer (use this inside a link)
  fallback = null, // shown when there are no usable images
  caption, // optional text under the enlarged image
}) => {
  const urls = useMemo(() => toUrls(images), [images]);
  const [index, setIndex] = useState(0);
  const [lightboxAt, setLightboxAt] = useState(null); // null = closed
  const [failed, setFailed] = useState({});
  const trackRef = useRef(null);
  const triggerRef = useRef(null);
  const raf = useRef(0);
  const reduceMotion = useReducedMotion();

  const goTo = useCallback(
    (i, instant = false) => {
      const el = trackRef.current;
      if (!el) return;
      const next = Math.max(0, Math.min(urls.length - 1, i));
      el.scrollTo({
        left: next * el.clientWidth,
        behavior: instant || reduceMotion ? "auto" : "smooth",
      });
      setIndex(next);
    },
    [urls.length, reduceMotion],
  );

  // a different set of images: start from the first one
  useEffect(() => {
    setIndex(0);
    setFailed({});
    trackRef.current?.scrollTo({ left: 0, behavior: "auto" });
  }, [urls.length]);

  useEffect(() => () => cancelAnimationFrame(raf.current), []);

  // keeps the index in step with swipes
  const onScroll = () => {
    cancelAnimationFrame(raf.current);
    raf.current = requestAnimationFrame(() => {
      const el = trackRef.current;
      if (el && el.clientWidth)
        setIndex(Math.round(el.scrollLeft / el.clientWidth));
    });
  };

  const closeLightbox = useCallback(
    (finalIndex) => {
      setLightboxAt(null);
      goTo(finalIndex, true); // inline slider follows where the viewer ended
      triggerRef.current?.focus();
    },
    [goTo],
  );

  if (urls.length === 0) return fallback;

  const multiple = urls.length > 1;
  const stop = (e, fn) => {
    e.preventDefault(); // safe inside a link
    e.stopPropagation();
    fn();
  };

  return (
    <div className={`group relative overflow-hidden ${className}`}>
      <div
        ref={trackRef}
        onScroll={onScroll}
        className="flex h-full w-full snap-x snap-mandatory overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {urls.map((url, i) => (
          <div
            key={`${i}-${url}`}
            className="h-full w-full shrink-0 snap-center"
          >
            {failed[url] ? (
              <div className="flex h-full items-center justify-center bg-bg text-xs text-subtext">
                Image unavailable
              </div>
            ) : zoomable ? (
              <button
                type="button"
                tabIndex={i === index ? 0 : -1}
                onClick={(e) => {
                  triggerRef.current = e.currentTarget;
                  setLightboxAt(i);
                }}
                aria-haspopup="dialog"
                aria-label={`View larger: ${alt || "image"} ${i + 1} of ${urls.length}`}
                className="block h-full w-full cursor-zoom-in focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary"
              >
                <img
                  src={url}
                  alt=""
                  loading={i === 0 ? "eager" : "lazy"}
                  referrerPolicy="no-referrer"
                  onError={() => setFailed((f) => ({ ...f, [url]: true }))}
                  className={`h-full w-full ${imgClassName}`}
                />
              </button>
            ) : (
              <img
                src={url}
                alt={i === index ? alt : ""}
                loading={i === 0 ? "eager" : "lazy"}
                referrerPolicy="no-referrer"
                onError={() => setFailed((f) => ({ ...f, [url]: true }))}
                className={`h-full w-full ${imgClassName}`}
              />
            )}
          </div>
        ))}
      </div>

      {multiple && (
        <>
          {index > 0 && (
            <button
              type="button"
              onClick={(e) => stop(e, () => goTo(index - 1))}
              aria-label="Previous image"
              className={`${arrowBase} left-2 h-8 w-8 md:opacity-0 md:group-hover:opacity-100 md:group-focus-within:opacity-100`}
            >
              <FiChevronLeft size={18} />
            </button>
          )}
          {index < urls.length - 1 && (
            <button
              type="button"
              onClick={(e) => stop(e, () => goTo(index + 1))}
              aria-label="Next image"
              className={`${arrowBase} right-2 h-8 w-8 md:opacity-0 md:group-hover:opacity-100 md:group-focus-within:opacity-100`}
            >
              <FiChevronRight size={18} />
            </button>
          )}

          {urls.length <= MAX_DOTS ? (
            <div
              className="pointer-events-none absolute inset-x-0 bottom-2 flex justify-center gap-1.5"
              aria-hidden="true"
            >
              {urls.map((_, i) => (
                <span
                  key={i}
                  className={`h-1.5 rounded-full shadow transition-all ${
                    i === index ? "w-4 bg-white" : "w-1.5 bg-white/60"
                  }`}
                />
              ))}
            </div>
          ) : (
            <span
              className="pointer-events-none absolute bottom-2 right-2 rounded-full bg-black/50 px-2 py-0.5 text-xs text-white"
              aria-hidden="true"
            >
              {index + 1} / {urls.length}
            </span>
          )}

          <span className="sr-only" aria-live="polite">
            Image {index + 1} of {urls.length}
          </span>
        </>
      )}

      {lightboxAt !== null && (
        <Lightbox
          urls={urls}
          start={lightboxAt}
          alt={alt}
          caption={caption}
          onClose={closeLightbox}
        />
      )}
    </div>
  );
};

export default ImageCarousel;

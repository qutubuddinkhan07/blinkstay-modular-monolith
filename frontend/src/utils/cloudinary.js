// Serves a Cloudinary image in the best format for the browser, at high quality.
// Width is doubled for high-density (retina) screens; c_limit only shrinks, never enlarges.
// The stored original is never changed; this only affects the URL.
export const cld = (url, width) => {
  if (!url || !url.includes("/image/upload/")) return url;
  const t = [
    "f_auto",
    "q_auto:best",
    width && "c_limit",
    width && `w_${Math.round(width * 2)}`,
  ]
    .filter(Boolean)
    .join(",");
  return url.replace("/image/upload/", `/image/upload/${t}/`);
};

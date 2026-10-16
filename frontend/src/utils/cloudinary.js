// Adds automatic format and a max width to a Cloudinary URL
export const cld = (url, width) => {
  if (!url || !url.includes("/image/upload/")) return url;
  const t = ["f_auto", "q_auto", width && `w_${width}`]
    .filter(Boolean)
    .join(",");
  return url.replace("/image/upload/", `/image/upload/${t}/`);
};

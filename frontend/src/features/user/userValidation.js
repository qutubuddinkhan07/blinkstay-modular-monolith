// client-side rules, shared by the forms

export const validateUsername = (v) =>
  v.trim().length < 2 || v.trim().length > 50
    ? "Username must be 2 to 50 characters"
    : "";

export const validatePhone = (v) =>
  v === "" || /^\+?\d{7,15}$/.test(v)
    ? ""
    : "Phone must be 7 to 15 digits, optional +";

export const validateBio = (v) =>
  v.length > 300 ? "Bio can be at most 300 characters" : "";

export const validateNewPassword = (v) =>
  v.length < 8 || v.length > 72 ? "Password must be 8 to 72 characters" : "";

export const validateImage = (file, maxMb = 5) => {
  if (!file.type.startsWith("image/")) return "Please choose an image file";
  if (file.size > maxMb * 1024 * 1024) return `Image must be under ${maxMb} MB`;
  return "";
};

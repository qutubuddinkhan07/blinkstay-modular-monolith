const ROLE_PRIORITY = ["ADMIN", "HOTEL_MANAGER", "USER"];

export const roleLabel = (role) => role.replace(/_/g, " ").toLowerCase();

// An admin also holds HOTEL_MANAGER, so show only the highest role as the badge
export const displayRoles = (roles = []) => {
  const top = ROLE_PRIORITY.find((r) => roles.includes(r));
  return top ? [top] : [];
};

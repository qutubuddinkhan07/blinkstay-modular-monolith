export const handleApiError = (error, context = "") => {
  if (!error.response) {
    return error.message || "Network error. Please check your connection.";
  }

  const status = error.response.status;
  const serverMessage =
    typeof error.response.data === "string"
      ? error.response.data
      : error.response.data?.message;

  switch (status) {
    case 400:
      return serverMessage || "Bad request. Please check your inputs.";
    case 401:
      // differentiate between login failure and expired session
      if (context === "login") return "Invalid email or password.";
      return "Session expired. Please login again.";
    case 403:
      return "You don't have permission to do this.";
    case 404:
      return serverMessage || "Resource not found.";
    case 409:
      return serverMessage || "Conflict. Resource already exists.";
    case 500:
      if (serverMessage?.toLowerCase().includes("bad credentials")) {
        return "Invalid email or password.";
      }
      return "Server error. Please try again later.";
    default:
      return serverMessage || "Something went wrong.";
  }
};

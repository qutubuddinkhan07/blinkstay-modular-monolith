export const isBlockedError = (error) => {
  const status = error?.response?.status;
  const data = error?.response?.data;

  if (data?.code === "ACCOUNT_BLOCKED") return true;
  return (
    [400, 401, 403].includes(status) && /blocked/i.test(data?.message ?? "")
  );
};

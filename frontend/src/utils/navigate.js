let navigateFn = null;

export const setNavigate = (fn) => {
  navigateFn = fn;
};

export const navigateTo = (path) => {
  if (navigateFn) {
    navigateFn(path, { replace: true });
  }
};

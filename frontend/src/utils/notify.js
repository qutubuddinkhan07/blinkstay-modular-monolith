import { toast } from "sonner";

const normalize = ({ autoClose, ...rest } = {}) =>
  autoClose !== undefined ? { duration: autoClose, ...rest } : rest;

export const notify = {
  success: (msg, opts) => toast.success(msg, normalize(opts)),
  error: (msg, opts) => toast.error(msg, normalize(opts)),
  info: (msg, opts) => toast.info(msg, normalize(opts)),
  warn: (msg, opts) => toast.warning(msg, normalize(opts)), // Sonner calls it "warning"

  // Optional extras Sonner gives you for free
  loading: (msg, opts) => toast.loading(msg, normalize(opts)), // returns an id
  dismiss: (id) => toast.dismiss(id),
  promise: (promise, messages, opts) =>
    toast.promise(promise, messages, normalize(opts)),
};

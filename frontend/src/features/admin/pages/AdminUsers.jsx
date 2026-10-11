import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import ReasonDialog from "../../../components/common/ReasonDialog";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";
import Pagination from "../../listings/components/explore-page/pagination/Pagination";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import { blockUser, fetchUsers, unblockUser } from "../adminService";
import UserFilters from "../components/UserFilters";
import UsersTable from "../components/UsersTable";
import { getPrimaryRole } from "../../../utils/roles";

const PAGE_SIZE = 10;
const NO_FILTERS = { q: "", blocked: "all", role: "" };

// tolerate the field names the backend might use
const toRow = (u) => ({
  ...u,
  roles: u.roles ?? [],
  primaryRole: u.primaryRole ?? getPrimaryRole(u.roles ?? []),
  blocked: Boolean(u.blocked ?? u.isBlocked),
  blockReason: u.blockReason ?? null,
  blockedAt: u.blockedAt ?? null,
});

const toPage = (d = {}) => ({
  items: (d.content ?? d.items ?? []).map(toRow),
  totalPages: d.totalPages ?? 0,
  totalElements: d.totalElements ?? d.total ?? null,
});

const AdminUsers = () => {
  const { user: me } = useAuth();

  const [filters, setFilters] = useState(NO_FILTERS);
  const [page, setPage] = useState(0);
  const [data, setData] = useState({
    items: [],
    totalPages: 0,
    totalElements: null,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  const [dialog, setDialog] = useState(null); // { type: "block" | "unblock", user }
  const [busy, setBusy] = useState(false);

  const handleFilterChange = useCallback((partial) => {
    setFilters((f) => ({ ...f, ...partial }));
    setPage(0); // any filter change starts from the first page
  }, []);

  useEffect(() => {
    let ignore = false;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const res = await fetchUsers({ ...filters, page, size: PAGE_SIZE });
        if (ignore) return;

        const next = toPage(res.data.data);

        // the last page emptied out (e.g. after an unblock): step back one page
        if (next.items.length === 0 && page > 0 && next.totalPages > 0) {
          setPage(next.totalPages - 1);
          return;
        }
        setData(next);
      } catch (err) {
        if (ignore) return;
        console.error("Failed to load users", err);
        setError(handleApiError(err, "users"));
      } finally {
        if (!ignore) setLoading(false);
      }
    };

    load();
    return () => {
      ignore = true;
    };
  }, [filters, page, reloadKey]);

  const reload = () => setReloadKey((k) => k + 1);

  const handleBlock = async (reason) => {
    setBusy(true);
    try {
      const res = await blockUser(dialog.user.id, reason);
      notify.success(res.data?.message || "User blocked.");
      setDialog(null);
      reload();
    } catch (err) {
      notify.error(handleApiError(err, "user blocking"));
    } finally {
      setBusy(false);
    }
  };

  const handleUnblock = async () => {
    setBusy(true);
    try {
      const res = await unblockUser(dialog.user.id);
      notify.success(res.data?.message || "User unblocked.");
      setDialog(null);
      reload();
    } catch (err) {
      notify.error(handleApiError(err, "user blocking"));
    } finally {
      setBusy(false);
    }
  };

  const filtered =
    filters.q !== "" || filters.blocked !== "all" || filters.role !== "";
  const firstLoad = loading && data.items.length === 0;

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <h1 className="font-display text-3xl font-semibold">Manage users</h1>
          <p className="mt-2 text-subtext">
            Search accounts, and block or unblock users.
            {data.totalElements != null && ` ${data.totalElements} found.`}
          </p>
        </div>

        <div className="mb-6">
          <UserFilters filters={filters} onChange={handleFilterChange} />
        </div>

        {firstLoad && (
          <ul className="space-y-3" aria-busy="true">
            {Array.from({ length: 4 }, (_, i) => (
              <li
                key={i}
                className="h-20 animate-pulse rounded-2xl border border-border bg-surface motion-reduce:animate-none"
              />
            ))}
          </ul>
        )}

        {!loading && error && (
          <div className="py-16 text-center">
            <h2 className="font-display text-xl font-semibold">
              Couldn't load users
            </h2>
            <p className="mt-2 text-subtext">{error}</p>
            <button
              type="button"
              onClick={reload}
              className={`mt-5 cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`}
            >
              Try again
            </button>
          </div>
        )}

        {!loading && !error && data.items.length === 0 && (
          <div className="py-16 text-center text-subtext">
            <p>
              {filtered ? "No users match these filters." : "No users yet."}
            </p>
            {filtered && (
              <button
                type="button"
                onClick={() => {
                  setFilters(NO_FILTERS);
                  setPage(0);
                }}
                className={`mt-4 cursor-pointer rounded-lg border border-border px-4 py-2 text-sm font-semibold text-text hover:bg-surface ${focusRing}`}
              >
                Clear filters
              </button>
            )}
          </div>
        )}

        {!error && data.items.length > 0 && (
          <div
            className={loading ? "opacity-60 transition-opacity" : ""}
            aria-busy={loading}
          >
            <UsersTable
              users={data.items}
              currentUserId={me?.id}
              onBlock={(u) => setDialog({ type: "block", user: u })}
              onUnblock={(u) => setDialog({ type: "unblock", user: u })}
            />
            <Pagination
              page={page}
              totalPages={data.totalPages}
              onChange={setPage}
            />
          </div>
        )}
      </div>

      <ReasonDialog
        open={dialog?.type === "block"}
        title={`Block ${dialog?.user?.username ?? "this user"}?`}
        description="They will be signed out and can't use BlinkStay. If they are a host, their listings are suspended. They will see this reason."
        confirmLabel="Block user"
        busyLabel="Blocking…"
        busy={busy}
        onConfirm={handleBlock}
        onCancel={() => setDialog(null)}
      />

      <ConfirmDialog
        open={dialog?.type === "unblock"}
        title={`Unblock ${dialog?.user?.username ?? "this user"}?`}
        description="They can sign in again. If they are a host, listings that were live before the block are published again."
        confirmLabel="Unblock"
        busyLabel="Unblocking…"
        tone="primary"
        busy={busy}
        onConfirm={handleUnblock}
        onCancel={() => setDialog(null)}
      />
    </div>
  );
};

export default AdminUsers;

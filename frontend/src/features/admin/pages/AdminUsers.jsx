import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import ReasonDialog from "../../../components/common/ReasonDialog";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";
import Pagination from "../../listings/components/explore-page/pagination/Pagination";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import { usePageTitle } from "../../../utils/usePageTitle";
import {
  blockUser,
  fetchUsers,
  restoreUser,
  unblockUser,
} from "../adminService";
import UserFilters from "../components/UserFilters";
import UsersTable from "../components/UsersTable";
import { getPrimaryRole } from "../../../utils/roles";
import { useSearchParams } from "react-router-dom";

const PAGE_SIZE = 10;
const NO_FILTERS = { q: "", blocked: "all", role: "" };

const VIEWS = [
  { value: "active", label: "Accounts" },
  { value: "deleted", label: "Deleted" },
];

// tolerate the field names the backend might use
const toRow = (u) => ({
  ...u,
  roles: u.roles ?? [],
  primaryRole: u.primaryRole ?? getPrimaryRole(u.roles ?? []),
  blocked: Boolean(u.blocked ?? u.isBlocked),
  blockReason: u.blockReason ?? null,
  blockedAt: u.blockedAt ?? null,
  deleted: Boolean(u.deleted),
  deletedAt: u.deletedAt ?? null,
  purgeDueAt: u.purgeDueAt ?? null,
  dataPurged: Boolean(u.dataPurged),
});

const toPage = (d = {}) => ({
  items: (d.content ?? d.items ?? []).map(toRow),
  totalPages: d.totalPages ?? 0,
  totalElements: d.totalElements ?? d.total ?? null,
});

// The restore dialog says exactly what will and won't come back
const restoreText = (u) => {
  if (!u) return "";
  const parts = ["Their account becomes active again."];
  if (u.dataPurged) {
    parts.push(
      "Their profile photo and reviews were already removed and won't come back.",
    );
  }
  if (u.blocked) {
    parts.push(
      "They were blocked before deleting, so they will stay blocked. Unblock them separately if needed.",
    );
  }
  return parts.join(" ");
};

const AdminUsers = () => {
  usePageTitle("Manage users");
  const { user: me } = useAuth();
  const [searchParams] = useSearchParams();

  // ?view=deleted opens the Deleted tab (links from suspended listings use it)
  const [view, setView] = useState(() =>
    searchParams.get("view") === "deleted" ? "deleted" : "active",
  );
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

  const [dialog, setDialog] = useState(null); // { type: "block" | "unblock" | "restore", user }
  const [busy, setBusy] = useState(false);

  const isDeletedView = view === "deleted";

  const handleFilterChange = useCallback((partial) => {
    setFilters((f) => ({ ...f, ...partial }));
    setPage(0); // any filter change starts from the first page
  }, []);

  const changeView = (next) => {
    if (next === view) return;
    setView(next);
    setPage(0);
    setData({ items: [], totalPages: 0, totalElements: null }); // don't show the other tab's rows
  };

  useEffect(() => {
    let ignore = false;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const res = await fetchUsers({
          ...filters,
          deleted: isDeletedView,
          page,
          size: PAGE_SIZE,
        });
        if (ignore) return;

        const next = toPage(res.data.data);

        // the last page emptied out (after an unblock or a restore): step back one page
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
  }, [filters, page, reloadKey, isDeletedView]);

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
      // 409: the host has upcoming bookings; retrying won't help until they're cancelled
      if (err.response?.status === 409) setDialog(null);
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

  const handleRestore = async () => {
    if (busy) return;
    const target = dialog.user;
    setBusy(true);
    try {
      const res = await restoreUser(target.id);
      notify.success(res.data?.message || `${target.username} was restored.`);
      setDialog(null);

      // take the row off the Deleted list at once, then reload for the right count and paging
      setData((d) => ({
        ...d,
        items: d.items.filter((u) => u.id !== target.id),
        totalElements:
          d.totalElements != null ? Math.max(0, d.totalElements - 1) : null,
      }));
      reload();
    } catch (err) {
      notify.error(handleApiError(err, "account restore"));
      // 404 or 409: the list was out of date (already restored, or gone)
      if ([404, 409].includes(err.response?.status)) {
        setDialog(null);
        reload();
      }
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
            {isDeletedView
              ? "Accounts their owners deleted. You can restore them before or after their data is removed."
              : "Search accounts, and block or unblock users."}
            {data.totalElements != null && ` ${data.totalElements} found.`}
          </p>
        </div>

        <div
          className="mb-4 inline-flex rounded-lg border border-border bg-surface p-1"
          role="group"
          aria-label="Which accounts to show"
        >
          {VIEWS.map((v) => (
            <button
              key={v.value}
              type="button"
              aria-pressed={view === v.value}
              onClick={() => changeView(v.value)}
              className={`cursor-pointer rounded-md px-4 py-1.5 text-sm font-semibold transition-colors ${focusRing} ${
                view === v.value
                  ? "bg-primary text-on-primary"
                  : "text-subtext hover:text-text"
              }`}
            >
              {v.label}
            </button>
          ))}
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
              {filtered
                ? "No users match these filters."
                : isDeletedView
                  ? "No deleted accounts."
                  : "No users yet."}
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
              onRestore={(u) => setDialog({ type: "restore", user: u })}
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

      <ConfirmDialog
        open={dialog?.type === "restore"}
        title={`Restore ${dialog?.user?.username ?? "this account"}?`}
        description={restoreText(dialog?.user)}
        confirmLabel="Restore account"
        busyLabel="Restoring…"
        tone="primary"
        busy={busy}
        onConfirm={handleRestore}
        onCancel={() => setDialog(null)}
      />
    </div>
  );
};

export default AdminUsers;

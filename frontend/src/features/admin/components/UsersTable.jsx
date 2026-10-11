import { Link } from "react-router-dom";
import ClampedText from "../../../components/common/ClampedText";
import { roleLabel } from "../../../utils/roles";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import UserAvatar from "./UserAvatar";

const smallBtn = `cursor-pointer rounded-lg border border-border bg-bg px-3 py-2 text-sm font-semibold text-text transition-colors hover:bg-border/60 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;
const dangerBtn = `cursor-pointer rounded-lg border border-danger/50 bg-bg px-3 py-2 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;

const formatDate = (value) => {
  const d = new Date(value);
  return Number.isNaN(d.getTime())
    ? null
    : d.toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
      });
};

const UserRow = ({ user, currentUserId, onBlock, onUnblock }) => {
  const isSelf = String(user.id) === String(currentUserId);
  const isAdminRow = user.primaryRole === "ADMIN";
  const isHost = user.roles.includes("HOTEL_MANAGER"); // admins hold this role too, so they get "View listings"

  const extraRoles = user.roles.filter((r) => r !== user.primaryRole);

  // the backend rejects both of these, so don't offer them
  const blockLocked = isSelf
    ? "You can't block your own account"
    : isAdminRow
      ? "Admin accounts can't be blocked"
      : null;

  const joined = user.createdAt ? formatDate(user.createdAt) : null;
  const blockedOn = user.blockedAt ? formatDate(user.blockedAt) : null;

  return (
    <li className="rounded-2xl border border-border bg-surface p-4 transition-colors duration-300">
      <div className="flex flex-col gap-4 md:flex-row md:items-center">
        <div className="flex min-w-0 flex-1 items-center gap-3">
          <UserAvatar user={user} />
          <div className="min-w-0">
            <p className="truncate font-display text-base font-medium text-text">
              {user.username}
              {isSelf && (
                <span className="ml-2 text-xs font-normal text-subtext">
                  (you)
                </span>
              )}
            </p>
            <p className="truncate text-sm text-subtext">{user.email}</p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2 md:w-56 md:justify-start">
          {user.primaryRole && (
            <span className="rounded-full border border-border px-2.5 py-0.5 text-xs font-medium capitalize text-text">
              {roleLabel(user.primaryRole)}
            </span>
          )}
          {extraRoles.length > 0 && (
            <span
              title={`Also has: ${extraRoles.map(roleLabel).join(", ")}`}
              aria-label={`Also has: ${extraRoles.map(roleLabel).join(", ")}`}
              className="cursor-help rounded-full border border-border px-2 py-0.5 text-xs font-medium text-subtext"
            >
              {extraRoles.map(roleLabel).join(", ")}
            </span>
          )}

          {user.blocked && (
            <span className="rounded-full border border-danger/40 bg-danger/10 px-2.5 py-0.5 text-xs font-semibold text-danger">
              Blocked
            </span>
          )}
          {joined && (
            <span className="text-xs text-subtext">Joined {joined}</span>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          {isHost && (
            <Link
              to={`/admin/users/${user.id}/listings`}
              state={{ username: user.username }}
              className={smallBtn}
            >
              View listings
            </Link>
          )}

          {user.blocked ? (
            !isAdminRow &&
            !isSelf && (
              <button
                type="button"
                onClick={() => onUnblock(user)}
                className={smallBtn}
              >
                Unblock
              </button>
            )
          ) : (
            <button
              type="button"
              onClick={() => onBlock(user)}
              disabled={Boolean(blockLocked)}
              title={blockLocked ?? undefined}
              className={dangerBtn}
            >
              Block
            </button>
          )}
        </div>
      </div>

      {user.blocked && (
        <div className="mt-4 rounded-xl border border-danger/40 bg-danger/10 p-3 text-sm">
          <p className="font-semibold text-text">
            Blocked{blockedOn ? ` on ${blockedOn}` : ""}
          </p>
          {user.blockReason ? (
            <ClampedText
              text={user.blockReason}
              className="mt-1 text-subtext"
            />
          ) : (
            <p className="mt-1 text-subtext">No reason was recorded.</p>
          )}
        </div>
      )}
    </li>
  );
};

const UsersTable = ({ users, currentUserId, onBlock, onUnblock }) => (
  <ul className="space-y-3">
    {users.map((u) => (
      <UserRow
        key={u.id}
        user={u}
        currentUserId={currentUserId}
        onBlock={onBlock}
        onUnblock={onUnblock}
      />
    ))}
  </ul>
);

export default UsersTable;

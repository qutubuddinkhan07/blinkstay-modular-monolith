import { useEffect, useMemo, useRef, useState } from "react";
import { useBlocker } from "react-router-dom";
import { FiCalendar, FiFileText, FiMail, FiPhone } from "react-icons/fi";
import { useAuth } from "../../../context/AuthContext";
import { displayRoles, roleLabel } from "../../../utils/roles";
import AvatarUploader from "../components/AvatarUploader";
import EditProfileForm from "../components/EditProfileForm";
import ChangePasswordForm from "../components/ChangePasswordForm";
import DeleteAccountDialog from "../components/DeleteAccountDialog";
import UnsavedChangesDialog from "../components/UnsavedChangesDialog";
import { dangerBtn } from "../formStyles";

const Card = ({ title, description, children, danger, id }) => (
  <section
    id={id}
    className={`rounded-2xl border bg-surface p-6 shadow-sm ${
      danger ? "border-danger/40" : "border-border"
    }`}
  >
    <h2
      className={`font-display text-lg font-semibold ${danger ? "text-danger" : "text-text"}`}
    >
      {title}
    </h2>
    {description && <p className="mt-1 text-sm text-subtext">{description}</p>}
    <div className="mt-5">{children}</div>
  </section>
);

const InfoRow = ({ icon: Icon, label, value, emptyText, multiline }) => (
  <div className="flex gap-3">
    <Icon
      className="mt-0.5 shrink-0 text-subtext"
      size={16}
      aria-hidden="true"
    />
    <div className="min-w-0">
      <p className="text-xs font-semibold uppercase tracking-wide text-subtext">
        {label}
      </p>
      {value ? (
        <p
          className={`break-words text-sm text-text ${multiline ? "whitespace-pre-line" : ""}`}
        >
          {value}
        </p>
      ) : (
        <p className="text-sm italic text-subtext">{emptyText}</p>
      )}
    </div>
  </div>
);

const ProfilePage = () => {
  const { user } = useAuth();
  const [deleteOpen, setDeleteOpen] = useState(false);

  // --- unsaved-changes tracking: each section reports whether it's dirty ---
  const [dirtyMap, setDirtyMap] = useState({});
  const reporters = useMemo(() => {
    const make = (key) => (value) =>
      setDirtyMap((m) => (m[key] === value ? m : { ...m, [key]: value }));
    return {
      avatar: make("avatar"),
      profile: make("profile"),
      password: make("password"),
    };
  }, []);
  const isDirty = Object.values(dirtyMap).some(Boolean);
  const leavingRef = useRef(false); // set when the account was just deleted

  // in-app navigation (links, back/forward button)
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      !leavingRef.current &&
      isDirty &&
      currentLocation.pathname !== nextLocation.pathname,
  );

  // refresh / close tab
  useEffect(() => {
    if (!isDirty) return;
    const warn = (e) => {
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [isDirty]);

  const memberSince = user.createdAt
    ? new Date(user.createdAt).toLocaleDateString(undefined, {
        year: "numeric",
        month: "long",
      })
    : null;

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg text-text">
      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
        <h1 className="font-display text-3xl font-semibold">
          Account settings
        </h1>
        <p className="mt-1 text-sm text-subtext">
          Manage your profile, photo and security.
        </p>

        <div className="mt-8 grid gap-6 lg:grid-cols-[320px_1fr] lg:items-start">
          {/* Left: summary */}
          <aside className="space-y-6 lg:sticky lg:top-24">
            <section className="rounded-2xl border border-border bg-surface p-6 shadow-sm">
              <AvatarUploader onDirtyChange={reporters.avatar} />

              <div className="mt-5 text-center">
                <p className="truncate font-display text-xl font-semibold">
                  {user.username}
                </p>
                <div className="mt-2 flex justify-center gap-1.5">
                  {displayRoles(user.roles).map((r) => (
                    <span
                      key={r}
                      className="rounded-full border border-border px-2.5 py-0.5 text-[11px] font-medium capitalize text-subtext"
                    >
                      {roleLabel(r)}
                    </span>
                  ))}
                </div>
              </div>

              <div className="mt-6 space-y-4 border-t border-border pt-5">
                <InfoRow icon={FiMail} label="Email" value={user.email} />
                <InfoRow
                  icon={FiPhone}
                  label="Phone"
                  value={user.phone}
                  emptyText="Not added yet. Add one in personal details."
                />
                <InfoRow
                  icon={FiFileText}
                  label="Bio"
                  value={user.bio}
                  multiline
                  emptyText="No bio yet. Tell people about yourself."
                />
                {memberSince && (
                  <InfoRow
                    icon={FiCalendar}
                    label="Member since"
                    value={memberSince}
                  />
                )}
              </div>
            </section>
          </aside>

          {/* Right: forms */}
          <div className="space-y-6">
            <Card
              id="personal-details"
              title="Personal details"
              description="Update how you appear to hosts and guests."
            >
              <EditProfileForm onDirtyChange={reporters.profile} />
            </Card>

            <Card
              title="Change password"
              description="Use a strong password you don't use anywhere else."
            >
              <ChangePasswordForm onDirtyChange={reporters.password} />
            </Card>

            <Card
              title="Delete account"
              description="Permanently delete your account and sign out everywhere. This can't be undone."
              danger
            >
              <button
                type="button"
                onClick={() => setDeleteOpen(true)}
                className={dangerBtn}
              >
                Delete my account
              </button>
            </Card>
          </div>
        </div>
      </main>

      <DeleteAccountDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onDeleted={() => {
          leavingRef.current = true;
        }}
      />

      <UnsavedChangesDialog
        open={blocker.state === "blocked"}
        onStay={() => blocker.reset()}
        onLeave={() => blocker.proceed()}
      />
    </div>
  );
};

export default ProfilePage;

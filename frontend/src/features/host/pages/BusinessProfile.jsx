import { useEffect, useState } from "react";
import { useBlocker } from "react-router-dom";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import UnsavedChangesDialog from "../../user/components/UnsavedChangesDialog";
import { primaryBtn } from "../../user/formStyles";
import { fetchHostProfile, updateHostProfile } from "../hostService";
import HostDetailsForm from "../components/HostDetailsForm";

const BusinessProfile = () => {
  const [profile, setProfile] = useState(null); // null: not saved yet (hosts added by hand)
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    let ignore = false;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await fetchHostProfile();
        if (!ignore) setProfile(res.data.data ?? null);
      } catch (err) {
        if (!ignore) setError(handleApiError(err));
      } finally {
        if (!ignore) setLoading(false);
      }
    };

    load();
    return () => {
      ignore = true;
    };
  }, [reloadKey]);

  // unsaved-changes guard: in-app navigation and page refresh
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      dirty && currentLocation.pathname !== nextLocation.pathname,
  );
  useEffect(() => {
    if (!dirty) return;
    const warn = (e) => {
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  const handleSave = async (_values, changes) => {
    try {
      const res = await updateHostProfile(changes); // only the changed fields
      const saved = res.data?.data;
      setProfile((prev) => ({
        ...prev,
        ...(saved && typeof saved === "object" ? saved : changes),
      }));
      notify.success(res.data?.message || "Business profile updated");
    } catch (err) {
      notify.error(handleApiError(err));
    }
  };

  const since = profile?.approvedAt ? new Date(profile.approvedAt) : null;

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-2xl space-y-6">
        <div>
          <h1 className="font-display text-3xl font-semibold">
            Business profile
          </h1>
          <p className="mt-2 text-subtext">
            The details guests and admins see about your business.
            {since &&
              !Number.isNaN(since.getTime()) &&
              ` Host since ${since.toLocaleDateString(undefined, { year: "numeric", month: "long" })}.`}
          </p>
        </div>

        {loading && (
          <div
            className="h-64 animate-pulse rounded-2xl border border-border bg-surface motion-reduce:animate-none"
            aria-busy="true"
          />
        )}

        {!loading && error && (
          <div className="rounded-2xl border border-border bg-surface p-6">
            <p className="font-semibold">Couldn't load your business profile</p>
            <p className="mt-1 text-sm text-subtext">{error}</p>
            <button
              type="button"
              onClick={() => setReloadKey((k) => k + 1)}
              className={`mt-4 cursor-pointer ${primaryBtn}`}
            >
              Try again
            </button>
          </div>
        )}

        {!loading && !error && (
          <section className="rounded-2xl border border-border bg-surface p-6 shadow-sm">
            {!profile && (
              <p className="mb-5 rounded-lg border border-border bg-muted p-3 text-sm text-subtext">
                No business details saved yet. Fill them in below.
              </p>
            )}
            <HostDetailsForm
              initial={profile}
              submitLabel="Save changes"
              busyLabel="Saving…"
              requireChange
              onSubmit={handleSave}
              onDirtyChange={setDirty}
            />
          </section>
        )}
      </div>

      <UnsavedChangesDialog
        open={blocker.state === "blocked"}
        onStay={() => blocker.reset()}
        onLeave={() => blocker.proceed()}
      />
    </div>
  );
};

export default BusinessProfile;

import { useId, useState } from "react";
import {
  cardClass,
  labelClass,
  inputClass,
  focusRing,
} from "../../create-listing/formStyles";
import { ROOM_TYPES, CURRENCY, roomTypeLabel } from "./roomOptions";

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: CURRENCY,
  maximumFractionDigits: 2,
});

const smallBtn = `cursor-pointer rounded-lg border border-border bg-surface px-3 py-2 text-sm font-semibold text-text transition-colors hover:bg-border/60 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;
const dangerBtn = `cursor-pointer rounded-lg border border-danger/50 bg-surface px-3 py-2 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;
const primaryBtn = `cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover disabled:cursor-wait disabled:opacity-60 ${focusRing}`;

const emptyRoom = {
  roomType: "",
  price: "",
  totalRooms: "",
  availableRooms: "",
};

/* ------------------------------------------------------------------ */
/* Add / edit form (its own <form>, so it lives outside the listing form) */
/* ------------------------------------------------------------------ */

const RoomForm = ({
  initial,
  takenTypes = [], // room types this listing already has (other rooms)
  submitLabel,
  submitting,
  onSubmit,
  onCancel,
}) => {
  const id = useId();
  const [values, setValues] = useState(initial);
  const [error, setError] = useState("");

  // Hide the types this listing already has. A saved room whose type isn't in
  // our list is still kept selectable so editing it doesn't blank the field.
  const available = ROOM_TYPES.filter((t) => !takenTypes.includes(t.value));
  const typeOptions =
    !values.roomType || available.some((t) => t.value === values.roomType)
      ? available
      : [
          { value: values.roomType, label: roomTypeLabel(values.roomType) },
          ...available,
        ];

  const handleChange = (e) => {
    const { name, value } = e.target;
    setValues((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const price = Number(values.price);
    const totalRooms = parseInt(values.totalRooms, 10);
    const availableRooms = parseInt(values.availableRooms, 10);

    if (!values.roomType) return setError("Choose a room type.");
    if (takenTypes.includes(values.roomType))
      return setError(
        "This listing already has that room type. Edit it instead.",
      );
    if (!(price > 0)) return setError("Enter a price greater than 0.");
    if (!Number.isInteger(totalRooms) || totalRooms < 1)
      return setError("Total rooms must be at least 1.");
    if (
      !Number.isInteger(availableRooms) ||
      availableRooms < 0 ||
      availableRooms > totalRooms
    )
      return setError("Available rooms must be between 0 and the total.");

    setError("");
    await onSubmit({
      roomType: values.roomType,
      price,
      totalRooms,
      availableRooms,
    });
  };

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      className="rounded-xl border border-border bg-bg p-4"
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor={`${id}-type`} className={labelClass}>
            Room type
          </label>
          <select
            id={`${id}-type`}
            name="roomType"
            value={values.roomType}
            onChange={handleChange}
            className={inputClass}
          >
            <option value="" disabled>
              Select a type
            </option>
            {typeOptions.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor={`${id}-price`} className={labelClass}>
            Price ({CURRENCY})
          </label>
          <input
            id={`${id}-price`}
            name="price"
            type="number"
            inputMode="decimal"
            min="0"
            step="0.01"
            value={values.price}
            onChange={handleChange}
            placeholder="2500"
            className={inputClass}
          />
        </div>

        <div>
          <label htmlFor={`${id}-total`} className={labelClass}>
            Total rooms
          </label>
          <input
            id={`${id}-total`}
            name="totalRooms"
            type="number"
            inputMode="numeric"
            min="1"
            step="1"
            value={values.totalRooms}
            onChange={handleChange}
            placeholder="10"
            className={inputClass}
          />
        </div>

        <div>
          <label htmlFor={`${id}-available`} className={labelClass}>
            Available rooms
          </label>
          <input
            id={`${id}-available`}
            name="availableRooms"
            type="number"
            inputMode="numeric"
            min="0"
            step="1"
            value={values.availableRooms}
            onChange={handleChange}
            placeholder="10"
            className={inputClass}
          />
        </div>
      </div>

      {error && (
        <p role="alert" className="mt-3 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-4 flex gap-3">
        <button type="submit" disabled={submitting} className={primaryBtn}>
          {submitting ? "Saving..." : submitLabel}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={submitting}
          className={smallBtn}
        >
          Cancel
        </button>
      </div>
    </form>
  );
};

/* ------------------------------------------------------------------ */
/* Manager                                                             */
/* ------------------------------------------------------------------ */

// onCreate / onUpdate / onDelete must resolve to true on success, false on failure
const RoomsManager = ({
  rooms = [],
  isPublished,
  onCreate,
  onUpdate,
  onDelete,
}) => {
  const [adding, setAdding] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [confirmId, setConfirmId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const [saving, setSaving] = useState(false);

  const isLastRoom = rooms.length === 1;

  // One room entry per type: the add form hides types already used
  const usedTypes = rooms.map((r) => r.roomType);
  const allTypesUsed = ROOM_TYPES.every((t) => usedTypes.includes(t.value));

  const openAdd = () => {
    setEditingId(null);
    setConfirmId(null);
    setAdding(true);
  };

  const openEdit = (id) => {
    setAdding(false);
    setConfirmId(null);
    setEditingId(id);
  };

  const submitNew = async (room) => {
    setSaving(true);
    const ok = await onCreate(room);
    setSaving(false);
    if (ok) setAdding(false);
  };

  const submitEdit = async (room) => {
    setSaving(true);
    const ok = await onUpdate(editingId, room);
    setSaving(false);
    if (ok) setEditingId(null);
  };

  const confirmDelete = async (roomId) => {
    setDeletingId(roomId);
    await onDelete(roomId);
    setDeletingId(null);
    setConfirmId(null);
  };

  return (
    <div className={cardClass}>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-display text-xl font-medium text-text">Rooms</h2>

        {!adding && !allTypesUsed && (
          <button type="button" onClick={openAdd} className={smallBtn}>
            Add room
          </button>
        )}
      </div>

      <p className="mb-5 text-sm text-subtext">
        Rooms save on their own, separately from the listing details above. Each
        room type can be added once.
        {allTypesUsed && " Every room type has been added."}
      </p>

      {adding && (
        <div className="mb-4">
          <RoomForm
            initial={emptyRoom}
            takenTypes={usedTypes}
            submitLabel="Add room"
            submitting={saving}
            onSubmit={submitNew}
            onCancel={() => setAdding(false)}
          />
        </div>
      )}

      {rooms.length === 0 && !adding ? (
        <p className="rounded-xl border border-dashed border-border p-6 text-center text-sm text-subtext">
          No rooms yet. Add at least one room to publish this listing.
        </p>
      ) : (
        <ul className="space-y-3">
          {rooms.map((room) => (
            <li
              key={room.id}
              className="rounded-xl border border-border bg-bg p-4"
            >
              {editingId === room.id ? (
                <RoomForm
                  initial={{
                    roomType: room.roomType,
                    price: String(room.price),
                    totalRooms: String(room.totalRooms),
                    availableRooms: String(room.availableRooms),
                  }}
                  takenTypes={rooms
                    .filter((r) => r.id !== room.id)
                    .map((r) => r.roomType)}
                  submitLabel="Save room"
                  submitting={saving}
                  onSubmit={submitEdit}
                  onCancel={() => setEditingId(null)}
                />
              ) : (
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <p className="font-display text-base font-medium text-text">
                      {roomTypeLabel(room.roomType)}
                    </p>
                    <p className="mt-0.5 text-sm text-subtext">
                      {money.format(room.price)} · {room.availableRooms} of{" "}
                      {room.totalRooms} available
                    </p>
                  </div>

                  {confirmId === room.id ? (
                    <div className="sm:max-w-xs sm:text-right">
                      <p className="text-sm font-medium text-text">
                        Delete this room?
                      </p>
                      {isLastRoom && isPublished && (
                        <p className="mt-1 text-xs text-subtext">
                          It's the last room, so the listing will go back to
                          draft.
                        </p>
                      )}
                      <div className="mt-2 flex gap-2 sm:justify-end">
                        <button
                          type="button"
                          onClick={() => confirmDelete(room.id)}
                          disabled={deletingId === room.id}
                          className="cursor-pointer rounded-lg bg-danger px-3 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:cursor-wait disabled:opacity-60"
                        >
                          {deletingId === room.id ? "Deleting..." : "Delete"}
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmId(null)}
                          disabled={deletingId === room.id}
                          className={smallBtn}
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => openEdit(room.id)}
                        className={smallBtn}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setAdding(false);
                          setEditingId(null);
                          setConfirmId(room.id);
                        }}
                        className={dangerBtn}
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default RoomsManager;

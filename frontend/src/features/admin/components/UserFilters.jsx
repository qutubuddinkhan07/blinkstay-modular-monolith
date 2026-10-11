import { useEffect, useState } from "react";
import { FiSearch } from "react-icons/fi";
import { inputCls } from "../../user/formStyles";

const BLOCKED_OPTIONS = [
  { value: "all", label: "All users" },
  { value: "active", label: "Active" },
  { value: "blocked", label: "Blocked" },
];

const ROLE_OPTIONS = [
  { value: "", label: "All roles" },
  { value: "USER", label: "User" },
  { value: "HOTEL_MANAGER", label: "Hotel manager" },
  { value: "ADMIN", label: "Admin" },
];

// onChange must be a stable function (useCallback in the page)
const UserFilters = ({ filters, onChange }) => {
  const [text, setText] = useState(filters.q);

  // keep the box in sync when filters are cleared from outside
  useEffect(() => setText(filters.q), [filters.q]);

  // search after the user stops typing
  useEffect(() => {
    if (text.trim() === filters.q) return;
    const t = setTimeout(() => onChange({ q: text.trim() }), 400);
    return () => clearTimeout(t);
  }, [text, filters.q, onChange]);

  return (
    <div className="grid gap-3 sm:grid-cols-[1fr_180px_180px]">
      <div className="relative">
        <FiSearch
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-subtext"
          size={16}
          aria-hidden="true"
        />
        <input
          type="search"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Search by name or email"
          aria-label="Search users"
          className={`${inputCls} pl-9`}
        />
      </div>

      <select
        value={filters.blocked}
        onChange={(e) => onChange({ blocked: e.target.value })}
        aria-label="Filter by blocked status"
        className={inputCls}
      >
        {BLOCKED_OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>

      <select
        value={filters.role}
        onChange={(e) => onChange({ role: e.target.value })}
        aria-label="Filter by role"
        className={inputCls}
      >
        {ROLE_OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </div>
  );
};

export default UserFilters;

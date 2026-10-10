import { focusRing } from "../create-listing/formStyles";

const STATUS = ["DRAFT", "PUBLISHED", "SUSPENDED", "PAUSED"];
const CATEGORIES = [
  "BEACH",
  "MOUNTAIN",
  "CITY",
  "COUNTRYSIDE",
  "LUXURY",
  "BUDGET",
  "APARTMENT",
  "VILLA",
  "RESOURT",
  "HOTEL",
  "HOSTEL",
  "OTHER",
  "ROOM",
  "FARMS",
  "ROOMS",
  "MOUNTAINS",
  "TRENDING",
  "DOMES",
  "CASTLES",
  "BOATS",
  "ARCTIC",
  "ICONIC_CITIES",
].sort((a, b) => a.localeCompare(b));

const ListingFilters = ({
  search,
  setSearch,
  statusFilter,
  setStatusFilter,
  categoryFilter,
  setCategoryFilter,
}) => {
  return (
    <div className="mb-6 flex flex-wrap items-center gap-3">
      {/* Search by Name or Country */}
      <div className="relative flex-1 min-w-[240px">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by title or country..."
          className={`w-full rounded-lg border border-border bg-surface px-4 py-2.5 text-sm text-text placeholder:text-subtext transition-colors outline-none focus:border-primary ${focusRing}`}
        />
      </div>

      {/* Filter by Status */}
      <select
        value={statusFilter}
        onChange={(e) => setStatusFilter(e.target.value)}
        className={`rounded-lg border border-border bg-surface px-3 py-2.5 text-sm text-text transition-colors outline-none focus:border-primary ${focusRing}`}
      >
        <option value="">All statuses</option>
        {STATUS.map((status) => (
          <option key={status} value={status}>
            {status}
          </option>
        ))}
      </select>

      {/* Filter by Category */}
      <select
        value={categoryFilter}
        onChange={(e) => setCategoryFilter(e.target.value)}
        className={`rounded-lg border border-border bg-surface px-3 py-2.5 text-sm text-text transition-colors outline-none focus:border-primary ${focusRing}`}
      >
        <option value="">All Categories</option>
        {CATEGORIES.map((category) => (
          <option value={category} key={category}>
            {category.replace("_", " ")}
          </option>
        ))}
      </select>
    </div>
  );
};

export default ListingFilters;

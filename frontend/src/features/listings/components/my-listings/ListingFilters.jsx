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
    <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
      {/* Search by Name or Country - Full width on mobile, flexible on desktop */}
      <div className="relative w-full sm:flex-1 sm:min-w-[240px]">
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by title or country..."
          className={`w-full rounded-lg border border-border bg-surface px-4 py-2.5 text-sm text-text placeholder:text-subtext transition-colors outline-none focus:border-primary ${focusRing}`}
        />
      </div>

      {/* Filter controls container for mobile grouping */}
      <div className="flex gap-2">
        {/* Filter by Status */}
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className={`w-1/2 sm:w-auto rounded-lg border border-border bg-surface px-3 py-2.5 text-sm text-text transition-colors outline-none focus:border-primary ${focusRing}`}
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
          className={`w-1/2 sm:w-auto rounded-lg border border-border bg-surface px-3 py-2.5 text-sm text-text transition-colors outline-none focus:border-primary ${focusRing}`}
        >
          <option value="">All Categories</option>
          {CATEGORIES.map((category) => (
            <option value={category} key={category}>
              {category.replace("_", " ")}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
};

export default ListingFilters;

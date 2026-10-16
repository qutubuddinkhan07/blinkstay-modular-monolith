import { useLocation, useNavigate } from "react-router-dom";
import { FaHeart, FaRegHeart } from "react-icons/fa";
import { useAuth } from "../../../context/AuthContext";
import { useFavorites } from "../../../context/FavoriteContext";
import { notify } from "../../../utils/notify";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

// variant "icon": round heart for cards. variant "button": labelled button for the detail page.
const FavoriteButton = ({
  listingId,
  title = "",
  variant = "icon",
  className = "",
}) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated } = useAuth();
  const { isFavorite, toggle } = useFavorites();

  const active = isAuthenticated && isFavorite(listingId);
  const label = active ? "Remove from favorites" : "Add to favorites";

  const handleClick = (e) => {
    // the heart sits on top of a clickable card
    e.preventDefault();
    e.stopPropagation();

    if (!isAuthenticated) {
      notify.info("Log in to save favorites.");
      // Login reads from.pathname, so the query string (filters) rides along in it
      navigate("/login", {
        state: { from: { pathname: `${location.pathname}${location.search}` } },
      });
      return;
    }

    toggle(listingId);
  };

  if (variant === "button") {
    return (
      <button
        type="button"
        onClick={handleClick}
        aria-pressed={active}
        aria-label={title ? `${label}: ${title}` : label}
        className={`inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-surface px-4 py-2 text-sm font-semibold text-text transition-colors hover:bg-bg ${focusRing} ${className}`}
      >
        {active ? (
          <FaHeart className="text-danger" size={16} aria-hidden="true" />
        ) : (
          <FaRegHeart size={16} aria-hidden="true" />
        )}
        {active ? "Saved" : "Save"}
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      aria-pressed={active}
      aria-label={title ? `${label}: ${title}` : label}
      title={label}
      className={`flex h-9 w-9 cursor-pointer items-center justify-center rounded-full border border-border bg-surface/90 shadow-sm backdrop-blur transition hover:scale-110 active:scale-95 motion-reduce:transition-none motion-reduce:hover:scale-100 ${
        active ? "text-danger" : "text-subtext hover:text-danger"
      } ${focusRing} ${className}`}
    >
      {active ? (
        <FaHeart size={16} aria-hidden="true" />
      ) : (
        <FaRegHeart size={16} aria-hidden="true" />
      )}
    </button>
  );
};

export default FavoriteButton;

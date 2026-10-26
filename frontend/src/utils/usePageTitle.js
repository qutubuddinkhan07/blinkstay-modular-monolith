import { useEffect } from "react";

const BASE = "BlinkStay";

// usePageTitle("My bookings") -> "My bookings | BlinkStay"
// usePageTitle() or an empty value -> "BlinkStay"
export const usePageTitle = (title) => {
  useEffect(() => {
    const previous = document.title;
    document.title = title ? `${title} | ${BASE}` : BASE;
    return () => {
      document.title = previous;
    };
  }, [title]);
};

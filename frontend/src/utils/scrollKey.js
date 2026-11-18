// Scroll key for <ScrollRestoration getKey={scrollKey} />.
// - A new path (link to another page): new key, so the page opens at the top.
// - Same path, only the query string changed (dates, tabs, filters, month): same key,
//   so the page stays where it is.
// - Back / Forward: each history entry remembers its key, so its position comes back.
const keyOfEntry = new Map(); // location.key -> scroll key
let last = { pathname: null, key: null };

export const scrollKey = (location) => {
  let key = keyOfEntry.get(location.key);

  if (!key) {
    key = location.pathname === last.pathname ? last.key : location.key;
    keyOfEntry.set(location.key, key);
  }

  last = { pathname: location.pathname, key };
  return key;
};

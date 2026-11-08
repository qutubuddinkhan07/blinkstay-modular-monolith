import { useEffect, useRef, useState } from "react";

// loader: () => Promise<data>. Reloads when any of `deps` change, or when reload() is called.
export const useLoad = (loader, deps, onError) => {
  const [state, setState] = useState({
    data: null,
    loading: true,
    failed: false,
  });
  const [tick, setTick] = useState(0);
  const reqRef = useRef(0); // only the latest request may write state
  const loaderRef = useRef(loader);
  const onErrorRef = useRef(onError);
  loaderRef.current = loader;
  onErrorRef.current = onError;

  useEffect(() => {
    const req = ++reqRef.current;
    setState((s) => ({ ...s, loading: true, failed: false }));

    loaderRef
      .current()
      .then((data) => {
        if (req === reqRef.current)
          setState({ data, loading: false, failed: false });
      })
      .catch((err) => {
        if (req !== reqRef.current) return;
        setState({ data: null, loading: false, failed: true });
        onErrorRef.current?.(err);
      });

    return () => {
      reqRef.current++; // ignore anything still in flight
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  return { ...state, reload: () => setTick((t) => t + 1) };
};

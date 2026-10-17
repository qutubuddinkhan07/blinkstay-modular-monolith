import { useCallback, useEffect, useState } from "react";

// A countdown for "try again in Ns" buttons. start(60) begins it.
export const useCooldown = () => {
  const [until, setUntil] = useState(0);
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (until <= Date.now()) return;

    const timer = setInterval(() => {
      const t = Date.now();
      setNow(t);
      if (t >= until) clearInterval(timer);
    }, 500);
    return () => clearInterval(timer);
  }, [until]);

  const start = useCallback((seconds = 60) => {
    const t = Date.now();
    setNow(t);
    setUntil(t + seconds * 1000);
  }, []);

  // Count down to a fixed moment (ms timestamp), e.g. a payment hold ending
  const startUntil = useCallback((timestamp) => {
    setNow(Date.now());
    setUntil(timestamp);
  }, []);

  const secondsLeft = Math.max(0, Math.ceil((until - now) / 1000));
  return { secondsLeft, active: secondsLeft > 0, start, startUntil };
};

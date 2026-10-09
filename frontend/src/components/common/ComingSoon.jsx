import React from "react";
import { Link } from "react-router-dom";

const ComingSoon = ({
  title,
  description = "This page is being built and will be available soon.",
}) => {
  return (
    <div className="flex min-h-[calc(100vh-72px)] items-center justify-center bg-bg px-4 text-text">
      <div className="max-w-md text-center">
        <p className="text-xs font-semibold uppercase tracking-wide text-subtext">
          Coming soon
        </p>

        <h1 className="mt-2 font-display text-3xl font-semibold">{title}</h1>
        <p className="mt-2 text-subtext">{description}</p>

        <Link
          to={"/explore"}
          className="mt-5 inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover focus-visible:outline-offset-2 focus-visible:outline-primary"
        >
          Back to explore
        </Link>
      </div>
    </div>
  );
};

export default ComingSoon;

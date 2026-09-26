import React, { useEffect, useState } from "react";
import { fetchListings } from "./listingservice";
import { handleApiError } from "../../api/errors/handleApiError";

const ListingHome = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState({});

  const [page, setPage] = useState(0);
  const [country, setCountry] = useState("");

  const fetchData = async () => {
    try {
      setLoading(true);

      const { data } = await fetchListings({
        page,
        size: 10,
        sortBy: "createdAt",
        direction: "desc",
        country,
      });
      setData(data);

      console.log(data);
    } catch (err) {
      console.log("Failed to fetch listings ", err);

      setError((prev) => ({ ...prev, api: handleApiError(err, "listings") }));

      setData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [page, country]);

  //! Testing the ErrorPage
  // throw new Error("Testing ErrorPage");

  if (loading) return <div>Listings loading</div>;

  if (error.api) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold">Listings are unavailable</h2>

          <p className="text-gray-500 mt-2">{error.api}</p>

          <button
            onClick={fetchData}
            className="mt-4 px-4 py-2 bg-[#e01f59] border-[#0d4568] text-white rounded-lg"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <h2>Get all published listings</h2>

      <ul>
        {data?.content?.map((item) => {
          return <li key={item.id}>{item.title}</li>;
        })}
      </ul>

      <div>
        <button
          onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
          disabled={page === 0}
        >
          Previous Page
        </button>

        <span>
          Page {page + 1} of {data?.totalPages || 1}
        </span>

        <button
          onClick={() => setPage((prev) => prev + 1)}
          disabled={!data || data.last}
        >
          Next Page
        </button>
      </div>

      <select
        name="country"
        id="country"
        value={country}
        onChange={(e) => {
          setCountry(e.target.value);
          setPage(0);
        }}
      >
        <option value="india">India</option>
        <option value="brazil">Brazil</option>
      </select>
    </div>
  );
};

export default ListingHome;

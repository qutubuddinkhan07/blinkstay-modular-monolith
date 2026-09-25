import React, { useEffect, useState } from "react";
import { fetchListings } from "./listingservice";

const ListingHome = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  const [page, setPage] = useState(0);
  const [country, setCountry] = useState("");

  useEffect(() => {
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
        console.log(err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [page, country]);

  if (loading) return <div>Listings loading</div>;

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
          disabled={data.last}
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

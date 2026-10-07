import { useState } from "react";
import CreateListingForm from "../components/create-listing/CreateListingForm";
import { createListing } from "../listingService.js";
import { notify } from "../../../utils/notify.js";

const initialFormData = {
  title: "",
  location: "",
  description: "",
  country: "INDIA",
  amenities: [],
  category: "HOTEL",
};

const CreateListing = () => {
  const [formData, setFormData] = useState(initialFormData);
  const [images, setImages] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleLocationSelect = (locationData) => {
    setFormData((prev) => ({
      ...prev,
      location: locationData.location,
      country: locationData.country || prev.country,
    }));
  };

  const handleAmenitiesChange = (amenities) => {
    setFormData((prev) => ({
      ...prev,
      amenities,
    }));
  };

  const handleImagesChange = (nextImages) => {
    setImages(nextImages);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (images.length === 0) {
      notify.error("At least one image is required.");
      return;
    }

    try {
      setSubmitting(true);

      await createListing(formData, images);

      notify.success("Listing created successfully!");

      setFormData(initialFormData);
      setImages([]);
    } catch (error) {
      console.error("Create listing error:", error);

      notify.error(
        error.response?.data?.message || "Failed to create listing.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    // 60%: page bg + card surfaces · 30%: text, borders · 10%: primary accent
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-7xl">
        <div className="mb-8">
          <h1 className="font-display text-3xl font-semibold text-text">
            Create listing
          </h1>

          <p className="mt-2 text-subtext">
            Add your property details, location, amenities and images.
          </p>
        </div>

        <CreateListingForm
          formData={formData}
          images={images}
          submitting={submitting}
          onChange={handleChange}
          onLocationSelect={handleLocationSelect}
          onAmenitiesChange={handleAmenitiesChange}
          onImagesChange={handleImagesChange}
          onSubmit={handleSubmit}
        />
      </div>
    </div>
  );
};

export default CreateListing;

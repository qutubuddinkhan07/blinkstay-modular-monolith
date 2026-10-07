import ListingLocationPicker from "./ListingLocationPicker";
import ListingImageUploader from "./ListingImageUploader";
import AmenitySelector from "./AmenitySelector";
import {
  cardClass,
  headingClass,
  labelClass,
  inputClass,
  primaryButtonClass,
} from "./formStyles.js";

const CreateListingForm = ({
  formData,
  images,
  submitting,
  onChange,
  onLocationSelect,
  onAmenitiesChange,
  onImagesChange,
  onSubmit,
}) => {
  return (
    <form onSubmit={onSubmit}>
      <div className="grid gap-8 lg:grid-cols-2">
        {/* LEFT */}
        <div className="space-y-6">
          <div className={cardClass}>
            <h2 className={headingClass}>Listing details</h2>

            {/* Title */}
            <div className="mb-5">
              <label htmlFor="title" className={labelClass}>
                Title
              </label>
              <input
                id="title"
                type="text"
                name="title"
                value={formData.title}
                onChange={onChange}
                placeholder="Cozy Mountain Cabin"
                className={inputClass}
              />
            </div>

            {/* Description */}
            <div className="mb-5">
              <label htmlFor="description" className={labelClass}>
                Description
              </label>
              <textarea
                id="description"
                name="description"
                value={formData.description}
                onChange={onChange}
                rows={5}
                placeholder="A serene escape located in the lap of nature."
                className={inputClass}
              />
            </div>

            {/* Category */}
            <div className="mb-5">
              <label htmlFor="category" className={labelClass}>
                Category
              </label>
              <select
                id="category"
                name="category"
                value={formData.category}
                onChange={onChange}
                className={inputClass}
              >
                <option value="HOTEL">Hotel</option>
                <option value="RESORT">Resort</option>
                <option value="VILLA">Villa</option>
                <option value="APARTMENT">Apartment</option>
                <option value="HOSTEL">Hostel</option>
                <option value="CABIN">Cabin</option>
              </select>
            </div>

            {/* Country */}
            <div>
              <label htmlFor="country" className={labelClass}>
                Country
              </label>
              <select
                id="country"
                name="country"
                value={formData.country}
                onChange={onChange}
                className={inputClass}
              >
                <option value="INDIA">India</option>
                <option value="UNITED STATES">United States</option>
                <option value="UNITED KINGDOM">United Kingdom</option>
                <option value="UAE">United Arab Emirates</option>
                <option value="CANADA">Canada</option>
                <option value="AUSTRALIA">Australia</option>
              </select>
            </div>
          </div>

          <AmenitySelector
            amenities={formData.amenities}
            onChange={onAmenitiesChange}
          />

          <ListingImageUploader images={images} onChange={onImagesChange} />
        </div>

        {/* RIGHT */}
        <div>
          <ListingLocationPicker
            value={formData.location}
            onLocationSelect={onLocationSelect}
          />
        </div>
      </div>

      <button
        type="submit"
        disabled={submitting}
        className={`mt-8 w-full ${primaryButtonClass}`}
      >
        {submitting ? "Creating listing..." : "Create listing"}
      </button>
    </form>
  );
};

export default CreateListingForm;

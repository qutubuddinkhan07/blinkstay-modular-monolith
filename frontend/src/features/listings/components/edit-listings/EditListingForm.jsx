import ListingDetailsCard from "./ListingDetailsCard";
import ListingImagesManager from "./ListingImagesManager";
import AmenitySelector from "../create-listing/AmenitySelector";
import ListingLocationPicker from "../create-listing/ListingLocationPicker";
import { primaryButtonClass } from "../create-listing/formStyles";

const EditListingForm = ({
  formData,
  images,
  initialCoordinates,
  initialAddress,
  saving,
  dirty,
  onChange,
  onLocationSelect,
  onAmenitiesChange,
  onUploadImages,
  onDeleteImage,
  onSubmit,
}) => {
  return (
    <form onSubmit={onSubmit}>
      <div className="grid gap-8 lg:grid-cols-2">
        {/* LEFT */}
        <div className="space-y-6">
          <ListingDetailsCard formData={formData} onChange={onChange} />

          <AmenitySelector
            amenities={formData.amenities}
            onChange={onAmenitiesChange}
          />

          <ListingImagesManager
            images={images}
            onUpload={onUploadImages}
            onDelete={onDeleteImage}
          />
        </div>

        {/* RIGHT */}
        <div>
          <ListingLocationPicker
            value={formData.location}
            country={formData.country}
            initialCoordinates={initialCoordinates}
            initialAddress={initialAddress}
            onLocationSelect={onLocationSelect}
          />
        </div>
      </div>

      <div className="mt-8 flex items-center gap-4">
        <button
          type="submit"
          disabled={saving || !dirty}
          className={`flex-1 ${primaryButtonClass}`}
        >
          {saving ? "Saving..." : "Save changes"}
        </button>
        {!dirty && !saving && (
          <p className="text-sm text-subtext">No unsaved changes</p>
        )}
      </div>
    </form>
  );
};

export default EditListingForm;

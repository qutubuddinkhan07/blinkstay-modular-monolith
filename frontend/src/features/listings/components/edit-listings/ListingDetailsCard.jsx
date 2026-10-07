import {
  cardClass,
  headingClass,
  labelClass,
  inputClass,
} from "../create-listing/formStyles";

const CATEGORIES = [
  { value: "HOTEL", label: "Hotel" },
  { value: "RESORT", label: "Resort" },
  { value: "VILLA", label: "Villa" },
  { value: "APARTMENT", label: "Apartment" },
  { value: "HOSTEL", label: "Hostel" },
  { value: "CABIN", label: "Cabin" },
];

const COUNTRIES = [
  { value: "INDIA", label: "India" },
  { value: "UNITED STATES", label: "United States" },
  { value: "UNITED KINGDOM", label: "United Kingdom" },
  { value: "UAE", label: "United Arab Emirates" },
  { value: "CANADA", label: "Canada" },
  { value: "AUSTRALIA", label: "Australia" },
];

const ListingDetailsCard = ({ formData, onChange }) => {
  // The map picker can return a country that isn't in the list (e.g. FRANCE).
  // Without this, the <select> would silently show the first option while the
  // real value is something else.
  const countryOptions = COUNTRIES.some((c) => c.value === formData.country)
    ? COUNTRIES
    : [{ value: formData.country, label: formData.country }, ...COUNTRIES];

  return (
    <div className={cardClass}>
      <h2 className={headingClass}>Listing details</h2>

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
          {CATEGORIES.map((c) => (
            <option key={c.value} value={c.value}>
              {c.label}
            </option>
          ))}
        </select>
      </div>

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
          {countryOptions.map((c) => (
            <option key={c.value} value={c.value}>
              {c.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
};

export default ListingDetailsCard;

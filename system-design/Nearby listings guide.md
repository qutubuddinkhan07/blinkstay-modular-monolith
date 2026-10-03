# Nearby listings: backend + frontend wiring

Reference: <a href="https://claude.ai/share/26043955-a9b0-4cad-a5ad-3a6c88969b70">Chat reference</a>

Endpoint: `GET /api/v3/listings/{listingId}/nearby?radiusKm=50&limit=20`

Approach: take the listing's coordinates, ask the database for published listings inside a
**bounding box** (cheap, works on any database), then compute the exact distance in Java
(Haversine), drop anything outside the radius, sort by distance and cut to `limit`.

> I can't see your entity classes, so field/getter names below are assumptions
> (`listing.getGeometry().getLatitude()`, `getImages()`, `getRooms()`, ...).
> Rename them to match your code, and adjust number types (`double` vs `BigDecimal`).

---

## 1. DTO

```java
package blinkstay.listing.dto;

import java.util.UUID;

public record NearbyListingDto(
        UUID id,
        String title,
        String location,
        String thumbnailUrl,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Double startingPrice) {
}
```

## 2. Repository

```java
// ListingRepository.java
@Query("""
        select l from Listing l
        where l.status = :status
          and l.id <> :excludeId
          and l.geometry.latitude  between :minLat and :maxLat
          and l.geometry.longitude between :minLng and :maxLng
        """)
List<Listing> findInBoundingBox(@Param("status") ListingStatus status,
                                @Param("excludeId") UUID excludeId,
                                @Param("minLat") double minLat,
                                @Param("maxLat") double maxLat,
                                @Param("minLng") double minLng,
                                @Param("maxLng") double maxLng,
                                Pageable pageable);
```

If `images` / `rooms` are lazy collections, mapping them below triggers extra queries per
listing. Either add `@EntityGraph(attributePaths = {"images", "rooms"})` to this method, or
return a projection. Fine to start without it because the box is small.

## 3. Service

```java
// ListingService (interface): List<NearbyListingDto> getNearbyListings(UUID listingId, double radiusKm, int limit);

@Override
@Transactional(readOnly = true)
public List<NearbyListingDto> getNearbyListings(UUID listingId, double radiusKm, int limit) {
    Listing origin = listingRepository.findById(listingId)
            .orElseThrow(() -> new ResourceNotFoundException("Listing not found")); // your exception type

    double lat = origin.getGeometry().getLatitude();
    double lng = origin.getGeometry().getLongitude();

    // 1 degree of latitude ~ 111 km. Longitude shrinks with cos(latitude).
    double latDelta = radiusKm / 111.0;
    double lngDelta = radiusKm / (111.0 * Math.max(Math.cos(Math.toRadians(lat)), 0.01));

    List<Listing> candidates = listingRepository.findInBoundingBox(
            ListingStatus.PUBLISHED, listingId,
            lat - latDelta, lat + latDelta,
            lng - lngDelta, lng + lngDelta,
            PageRequest.of(0, 200)); // safety cap

    return candidates.stream()
            .map(l -> toNearbyDto(l, haversineKm(lat, lng,
                    l.getGeometry().getLatitude(), l.getGeometry().getLongitude())))
            .filter(dto -> dto.distanceKm() <= radiusKm)   // box corners are farther than the radius
            .sorted(Comparator.comparingDouble(NearbyListingDto::distanceKm))
            .limit(limit)
            .toList();
}

private NearbyListingDto toNearbyDto(Listing l, double distanceKm) {
    String thumbnail = l.getImages().stream()
            .min(Comparator.comparingInt(Image::getDisplayOrder))
            .map(Image::getImageUrl)
            .orElse(null);

    Double startingPrice = l.getRooms().stream()
            .map(r -> r.getPrice().doubleValue())
            .min(Double::compare)
            .orElse(null);

    return new NearbyListingDto(
            l.getId(), l.getTitle(), l.getLocation(), thumbnail,
            l.getGeometry().getLatitude(), l.getGeometry().getLongitude(),
            distanceKm, startingPrice);
}

private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
    final double R = 6371.0; // Earth radius in km
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
              * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
```

## 4. Controller

Your controller is already `@Validated`, so the constraints on params are enforced.

```java
@GetMapping("/{listingId}/nearby")
public ResponseEntity<ListingApiResponse<List<NearbyListingDto>>> getNearbyListings(
        @PathVariable UUID listingId,
        @RequestParam(defaultValue = "50") @DecimalMin("1") @DecimalMax("200") double radiusKm,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {

    List<NearbyListingDto> nearby = listingService.getNearbyListings(listingId, radiusKm, limit);

    return ResponseEntity.ok(ListingApiResponse.<List<NearbyListingDto>>builder()
            .success(true)
            .message("Nearby listings fetched successfully")
            .data(nearby)
            .build());
}
```

(`@DecimalMin`, `@DecimalMax`, `@Min`, `@Max` come from `jakarta.validation.constraints`.)

## 5. Security config

`{listingId}` matches one path segment only, so `/{id}/nearby` needs its own rule. Put it
**before** the `/api/v3/listings/**` rule:

```java
.requestMatchers(HttpMethod.GET, "/api/v3/listings/all").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v3/listings/my-listings").hasAuthority("HOTEL_MANAGER")
.requestMatchers(HttpMethod.GET, "/api/v3/listings/{listingId}").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v3/listings/{listingId}/nearby").permitAll()   // <-- new
.requestMatchers("/api/v3/listings/**").hasAnyAuthority("HOTEL_MANAGER", "ADMIN")
```

---

## 6. Frontend service

```js
// listingservice.js
export const fetchNearbyListings = (id, { radiusKm = 50, limit = 20 } = {}) => {
  return axiosInstance.get(`${LISTING_URL}/${id}/nearby`, {
    params: { radiusKm, limit },
  });
};
```

## 7. Listing.jsx changes

```jsx
import { fetchListingById, fetchNearbyListings } from "../listingservice";
import ListingMap from "../components/ListingMap";

// inside the component, next to your other state:
const [nearby, setNearby] = useState([]);

// separate effect: a failure here must never break the main page
useEffect(() => {
  let ignore = false;

  fetchNearbyListings(id, { radiusKm: 50, limit: 20 })
    .then((res) => {
      if (!ignore) setNearby(res.data.data ?? []);
    })
    .catch((err) => {
      console.error("Nearby listings failed", err);
      if (!ignore) setNearby([]);
    });

  return () => {
    ignore = true;
  };
}, [id]);

// in the JSX:
<ListingMap
  latitude={listing.geometry?.latitude}
  longitude={listing.geometry?.longitude}
  title={listing.title}
  address={listing.geometry?.address}
  nearby={nearby}
/>
```

---

## Scaling later

- **Index** `latitude` and `longitude` once you have thousands of listings.
- If you use PostgreSQL, **PostGIS** (`geography` column, GiST index, `ST_DWithin`) does the
  radius filter and distance sort in the database and replaces steps 2-3's math.
- For the Explore page, the same idea works as `GET /listings/in-bounds?minLat=&maxLat=&minLng=&maxLng=`,
  called from a Mapbox `moveend` event, so the map loads listings for whatever area the user pans to.
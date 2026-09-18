## Some basic rules to follow

1. <b>In a Controller (Most Common)</b>

You place `@PreAuthorize("hasRole('HOTEL_MANAGER')")` at the API layer to block unauthorized HTTP requests before they even reach your business logic.

```java
@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    // Anyone logged in might be able to view hotels, but...
    @GetMapping
    public List<Hotel> getAllHotels() {
        return hotelService.findAll();
    }

    // ONLY users with the 'HOTEL_MANAGER' role can create a new hotel
    @PreAuthorize("hasRole('HOTEL_MANAGER')")
    @PostMapping("/create")
    public ResponseEntity<Hotel> createHotel(@RequestBody HotelRequest request) {
        Hotel newHotel = hotelService.createNewHotel(request);
        return ResponseEntity.ok(newHotel);
    }
}
```

2. In a Service Class (For Deeper Security)

If you want to protect your core business logic—even if another internal developer accidentally exposes it via a public controller—you can put it right above your service methods.

```java
@Service
public class InventoryService {

    // Secures the method at the business logic layer
    @PreAuthorize("hasRole('HOTEL_MANAGER')")
    public void updateRoomAvailability(Long hotelId, int roomCount) {
        // Logic to update database
    }
}
```

Prerequisites to Make It WorkFor this annotation to actually stop unauthorized users, you must ensure two things are set up in your Spring Boot application:
1. <b>Enable Method Security:</b> You must add the `@EnableMethodSecurity` annotation to your security configuration class (usually where your `SecurityFilterChain` bean is defined).
2. <b>Role Prefixing:</b> Spring Security automatically prefixes hasRole(``'XYZ'``) checks with `ROLE_`. This means the user's authority string stored in your database or JWT token must literally be exactly `ROLE_HOTEL_MANAGER`.
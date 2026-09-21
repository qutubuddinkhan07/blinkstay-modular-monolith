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
2. <b>Role Prefixing:</b> Spring Security automatically prefixes hasRole(`'XYZ'`) checks with `ROLE_`. This means the user's authority string stored in your database or JWT token must literally be exactly `ROLE_HOTEL_MANAGER`.

# Spring Security Implementation with JWT

```pom.xml
<!-- For MockMultipartFile -->
<dependency>
	<groupId>org.springframework</groupId>
	<artifactId>spring-test</artifactId>
</dependency>

<!-- Spring Security -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-security-test</artifactId>
	<scope>test</scope>
</dependency>

<!-- JWT API -->
<dependency>
	<groupId>io.jsonwebtoken</groupId>
	<artifactId>jjwt-api</artifactId>
	<version>0.12.5</version>
</dependency

<!-- JWT Implementation -->
<dependency>
	<groupId>io.jsonwebtoken</groupId>
	<artifactId>jjwt-impl</artifactId>
	<version>0.12.5</version>
	<scope>runtime</scope>
</dependency

<!-- JSON Parser (Jackson) -->
<dependency>
	<groupId>io.jsonwebtoken</groupId>
	<artifactId>jjwt-jackson</artifactId>
	<version>0.12.5</version>
	<scope>runtime</scope>
</dependency>
```

1. The JWT Block (jjwt-api, jjwt-impl, jjwt-jackson)
   When to add them: When you want to implement token-based authentication (e.g., stateless logins where a user logs in once, gets a token, and sends that token in the header of every future request).

- `jjwt-api`: Provides the core interfaces and classes to create, sign, and parse JWTs in Java.
- `jjwt-impl`: The actual cryptographic engine that executes the token creation behind the scenes. It runs only at runtime.
- `jjwt-jackson`: Tells the JWT library to use Spring Boot's built-in Jackson library to serialize and deserialize the data payload (claims) inside your token into JSON format.

2. The Security Block (spring-boot-starter-security, spring-boot-starter-security-test)
   When to add them: When you need to protect your endpoints from unauthorized access, manage user roles, and write tests ensuring your security rules actually work.

- `spring-boot-starter-security`: Automatically locks down your entire application. You use this to configure which endpoints are public (like /login or /register) and which endpoints require a valid JWT token.
- `spring-boot-starter-security-test`: Adds specialized utilities for your testing suite. It allows you to simulate logged-in users, mock specific roles (like ADMIN or USER), or test authentication failures without spinning up a real database or generating a real token.

3. The Testing Block (spring-test)
   When to add them: When you are writing unit or integration tests for your controller layer, specifically if your API handles file or image uploads via MultipartFile.

- `spring-test`: Provides the `MockMultipartFile` class. If your API has an endpoint to upload a profile picture or a document, you cannot easily pass a real file stream during a fast automated test. `MockMultipartFile` simulates an uploaded file entirely in memory, letting you verify that your controller accepts the file correctly.
- Note: If you already have `spring-boot-starter-test` in your pom.xml, it usually pulls `spring-test` in automatically, so you may not even need to declare this manually.

<b>NOTE:</b> If you are building a system where a user logs in, receives a token, and uses that token to access secured areas or upload files, these are the dependencies needed in the project.

# Listing Module Flow

14. I would structure your Listing APIs like this

```
/api/listings

POST   /api/listings
GET    /api/listings
GET    /api/listings/{listingId}
PUT    /api/listings/{listingId}
DELETE /api/listings/{listingId}
Rooms:
/api/listings/{listingId}/rooms
```

```
POST   /api/listings/{listingId}/rooms
GET    /api/listings/{listingId}/rooms
GET    /api/listings/{listingId}/rooms/{roomId}
PUT    /api/listings/{listingId}/rooms/{roomId}
DELETE /api/listings/{listingId}/rooms/{roomId}
```

This is much cleaner than:

```
POST /rooms
```

because the URL itself expresses:
"I'm creating a room belonging to this listing."

15. One architectural point for your modular monolith
    You asked earlier:
    "If I don't create the user module first, how can Listing use User?"

The answer is: you don't need the User entity inside Listing.
Listing only needs:
UUID managerId;

Your Auth/User module owns:
User

and Listing owns:

```
Listing
ListingImage
ListingLocation
```

Room owns:
ListingRoom

So you're creating a boundary:

```
             AUTH/USER
                 │
                 │ userId
                 ▼
             LISTING
                 │
                 │ listingId
                 ▼
               ROOM
```

The IDs are the contracts between the modules.
Later, if you turn them into microservices:

```
user-service
     │
     │ HTTP/event
     ▼
listing-service
     │
     │ HTTP/event
     ▼
room-service
```

your domain model doesn't have to be completely redesigned.

```
1. AuthenticatedUser / Security principal
             ↓
2. @PreAuthorize role protection
             ↓
3. Listing DTOs
             ↓
4. Listing creation
             ↓
5. managerId automatically extracted from JWT
             ↓
6. Listing ownership authorization
             ↓
7. Listing CRUD
             ↓
8. ListingImage / Cloudinary
             ↓
9. ListingLocation
             ↓
10. ListingRoom module
             ↓
11. Room ownership checks through Listing
```

# Phase 2 Architecture

<a href="https://chatgpt.com/share/6ab1a0e7-21f8-83ee-a16d-7ab711f8bc06">Conversation Link</a>

Step 1 — Create listing

```
POST /api/v1/listings
```

Handles:

```
JWT
 ↓
get user ID
 ↓
verify HOTEL_MANAGER
 ↓
save Listing
```

Step 2 — Add geocoding

```
location
   ↓
GeocodingClient
   ↓
latitude + longitude
   ↓
ListingGeometry
```

Step 3 — Add images

```
MultipartFile[]
       ↓
ImageStorageService
       ↓
Cloudinary
       ↓
imageUrl + publicId
       ↓
ListingImage
```

Step 4 — Create room API

```
POST /api/v1/listings/{listingId}/rooms
```

```
JWT
 ↓
managerId
 ↓
find listing
 ↓
verify listing.managerId == managerId
 ↓
create ListingRoom
```

That last check is very important.

A hotel manager should not be able to do:

```
POST /listings/OTHER_MANAGER_LISTING_ID/rooms
```

and add rooms to someone else's listing.

## Backend flow

```
                       ┌──────────────────────┐
                       │   ListingController  │
                       └──────────┬───────────┘
                                  │
                                  ↓
                       ┌──────────────────────┐
                       │    ListingService    │
                       │   (orchestrator)     │
                       └──────┬─────┬─────┬───┘
                              │     │     │
                 ┌────────────┘     │     └─────────────┐
                 ↓                  ↓                   ↓
        ListingRepository    GeocodingClient    ImageStorageService
                 │                  │                   │
                 ↓                  ↓                   ↓
              MySQL            Geocoding API        Cloudinary
                 │
                 ↓
          ListingGeometry
          ListingImage


                     Room module
                          │
                          ↓
                    RoomController
                          │
                          ↓
                     RoomService
                          │
                          ↓
                  ListingRoomRepository
```

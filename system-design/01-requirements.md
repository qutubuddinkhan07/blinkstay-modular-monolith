# Project Structure
## Modular Monolith
```
blinkstay/
│
├── auth
├── user
├── hotel
├── room
├── booking
├── payment
├── review
└── notification
```

## Design Modules
```
com.blinkstay

├── auth
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── security
│
├── hotel
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
├── room
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
├── booking
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
├── payment
│   ├── controller
│   ├── service
│   └── ...
│
├── review
│   ├── controller
│   ├── service
│   └── ...
│
└── notification
    ├── service
    └── ...
```

# Current Path
```
             BUSINESS
                 ↓
          REQUIREMENTS
                 ↓
            USE CASES
                 ↓
          DATA / ENTITIES
                 ↓
             APIs
                 ↓
        BUSINESS LOGIC
                 ↓
          TRANSACTIONS
                 ↓
          CONCURRENCY
                 ↓
            CACHING
                 ↓
           SCALABILITY
                 ↓
        SERVICE BOUNDARIES
                 ↓
          MICROSERVICES
```


# User Module Requirements
## Requirement

### Functional Requirements
Define what your system must do from a user or system perspective (e.g. "A user can create an account," "The system sends a confirmation email").

1. User comes registers
2. User needs to give username, email and password
3. User may/may not give the image
4. 


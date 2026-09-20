# Folder Structure

```
src/
├── api/
│   └── axiosInstance.js
│
├── components/
│   └── common/
│
├── features/
│   ├── auth/
│   │   ├── pages/
│   │   ├── components/
│   │   └── authService.js
│   │
│   └── user/
│       ├── pages/
│       ├── components/
│       └── userService.js
│
├── context/
│   └── AuthContext.jsx
│
├── routes/
│   ├── AppRoutes.jsx
│   └── ProtectedRoute.jsx
│
├── utils/
│
├── App.jsx
├── main.jsx
└── index.css
```

## Axios should be centralized

Don't do this everywhere:

```js
axios.post("http://localhost:8080/api/v2/user/register-init");
```

Instead create:

```text
src/api/axiosInstance.js
```

For example:

```js
import axios from "axios";

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

export default axiosInstance;
```

Your `.env`:

```js
VITE_API_BASE_URL=http://localhost:8080
```

Then:

```js
import axiosInstance from "../../api/axiosInstance";

export const verifyOtp = (data) => {
  return axiosInstance.post("/api/v2/user/verify-otp", data);
};
```

This becomes particularly useful when you deploy:

```js
VITE_API_BASE_URL=https://your-backend.onrender.com
```

without changing all your API calls.

## Put JWT handling in Axios interceptors

Eventually something like this:

```
api/
├── axiosInstance.js
└── axiosInterceptors.js
```

For example:

```js
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});
```

Then you don't need:

```js
axios.get(url, {
  headers: {
    Authorization: `Bearer ${token}`,
  },
});
```

everywhere.
Your components simply do:

```js
listingService.getListings();
```

## Auth should be its own feature

```
features/auth/
```

Responsible for:

```
Login
Register
OTP verification
Logout
Forgot password
Reset password
Refresh token
```

for example:

```
auth/
├── pages/
│   ├── Login.jsx
│   ├── Register.jsx
│   └── VerifyOtp.jsx
│
├── components/
│   ├── LoginForm.jsx
│   ├── RegisterForm.jsx
│   └── OtpForm.jsx
│
├── authService.js
└── authUtils.js
```

Then do this:

```js
// authService.js

import axiosInstance from "../../api/axiosInstance";

export const login = (data) => axiosInstance.post("/api/v1/auth/login", data);

export const registerInit = (formData) =>
  axiosInstance.post("/api/v2/user/register-init", formData);

export const verifyOtp = (data) =>
  axiosInstance.post("/api/v2/user/verify-otp", data);
```

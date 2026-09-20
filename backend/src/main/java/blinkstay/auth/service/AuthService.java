package blinkstay.auth.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
	String authUsernameAndPasswordService(String username, String password);

	String logoutService(HttpServletRequest request);
}

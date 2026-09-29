package blinkstay.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
	void authUsernameAndPasswordService(String username, String password, HttpServletResponse response);

	String logoutService(HttpServletRequest request, HttpServletResponse response);
}

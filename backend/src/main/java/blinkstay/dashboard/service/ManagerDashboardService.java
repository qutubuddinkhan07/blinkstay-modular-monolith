package blinkstay.dashboard.service;

import java.util.UUID;

import blinkstay.dashboard.dto.ManagerDashboardDto;

public interface ManagerDashboardService {
	ManagerDashboardDto getManagerDashboard(UUID managerId);
}

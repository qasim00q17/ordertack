package ordertracker.service;

import ordertracker.dto.response.AdminDashboardResponse;

public interface AdminService {
    AdminDashboardResponse getDashboard();
}
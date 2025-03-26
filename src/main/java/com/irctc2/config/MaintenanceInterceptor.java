package com.irctc2.config;


import com.irctc2.train.service.MaintenanceModeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

    @Autowired
    private MaintenanceModeService maintenanceModeService; // See below

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (maintenanceModeService.isMaintenance()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE); // 503
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"🚧 Our system is currently undergoing scheduled maintenance. Please try again after some time. We appreciate your patience!\"}");
            return false; // Block further execution
        }
        return true;
    }
}

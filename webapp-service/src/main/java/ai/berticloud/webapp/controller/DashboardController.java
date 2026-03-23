/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.controller;

import ai.berticloud.webapp.client.AdminApiClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final AdminApiClient adminApiClient;

    public DashboardController(AdminApiClient adminApiClient) {
        this.adminApiClient = adminApiClient;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        try {
            model.addAttribute("tenantsCount", adminApiClient.listTenants().size());
        } catch (Exception e) {
            model.addAttribute("tenantsCount", "Error calculating");
        }
        return "dashboard";
    }
}

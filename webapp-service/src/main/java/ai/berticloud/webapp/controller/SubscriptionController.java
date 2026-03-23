/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.controller;

import ai.berticloud.webapp.client.AdminApiClient;
import ai.berticloud.webapp.dto.CreateSubscriptionForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final AdminApiClient adminApiClient;

    public SubscriptionController(AdminApiClient adminApiClient) {
        this.adminApiClient = adminApiClient;
    }

    @PostMapping("/create")
    public String create(@ModelAttribute CreateSubscriptionForm form) {
        adminApiClient.createSubscription(form);
        return "redirect:/tenants"; // Usually redirects back to tenant detail/list where action was initiated
    }

    @PostMapping("/{tenantId}/delete")
    public String delete(@PathVariable("tenantId") String tenantId) {
        adminApiClient.deleteSubscription(tenantId);
        return "redirect:/tenants";
    }
}

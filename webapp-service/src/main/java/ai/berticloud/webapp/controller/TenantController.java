/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.controller;

import ai.berticloud.webapp.client.AdminApiClient;
import ai.berticloud.webapp.dto.CreateTenantForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tenants")
public class TenantController {

    private final AdminApiClient adminApiClient;

    public TenantController(AdminApiClient adminApiClient) {
        this.adminApiClient = adminApiClient;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tenants", adminApiClient.listTenants());
        return "tenants/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new CreateTenantForm());
        return "tenants/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute CreateTenantForm form) {
        adminApiClient.createTenant(form);
        return "redirect:/tenants";
    }

    @PostMapping("/{tenantId}/delete")
    public String delete(@PathVariable("tenantId") String tenantId) {
        adminApiClient.deleteTenant(tenantId);
        return "redirect:/tenants";
    }
}

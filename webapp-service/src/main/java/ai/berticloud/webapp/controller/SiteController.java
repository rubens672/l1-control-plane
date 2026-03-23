/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.controller;

import ai.berticloud.webapp.client.AdminApiClient;
import ai.berticloud.webapp.dto.CreateSiteForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/sites")
public class SiteController {

    private final AdminApiClient adminApiClient;

    public SiteController(AdminApiClient adminApiClient) {
        this.adminApiClient = adminApiClient;
    }

    @GetMapping
    public String list(@RequestParam(value = "tenantId", required = false) String tenantId, Model model) {
        if (tenantId != null && !tenantId.isBlank()) {
            model.addAttribute("sites", adminApiClient.listSitesByTenant(tenantId));
            model.addAttribute("tenantId", tenantId);
        } else {
            // Se non c'è tenantId, per semplicità mostriamo una pagina per richiederlo o nulla
            // L'API di root in realtà accetta solo con tenantId nel nostro design (listSitesByTenant).
            model.addAttribute("sites", java.util.Collections.emptyList());
        }
        return "sites/list";
    }

    @GetMapping("/create")
    public String createForm(@RequestParam("tenantId") String tenantId, Model model) {
        CreateSiteForm form = new CreateSiteForm();
        form.setTenantId(tenantId);
        model.addAttribute("form", form);
        return "sites/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute CreateSiteForm form) {
        adminApiClient.createSite(form);
        return "redirect:/sites?tenantId=" + form.getTenantId();
    }

    @PostMapping("/{siteId}/delete")
    public String delete(@PathVariable("siteId") String siteId, @RequestParam("tenantId") String tenantId) {
        adminApiClient.deleteSite(siteId);
        return "redirect:/sites?tenantId=" + tenantId;
    }
}

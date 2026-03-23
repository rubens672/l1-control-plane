/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.controller;

import ai.berticloud.webapp.client.AdminApiClient;
import ai.berticloud.webapp.dto.BootstrapTokenResponse;
import ai.berticloud.webapp.dto.CreateDeviceForm;
import ai.berticloud.webapp.dto.DeviceDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Controller
@RequestMapping("/devices")
public class DeviceController {

    private final AdminApiClient adminApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeviceController(AdminApiClient adminApiClient) {
        this.adminApiClient = adminApiClient;
    }

    @GetMapping
    public String list(@RequestParam(value = "siteId", required = false) String siteId, Model model) {
        if (siteId != null && !siteId.isBlank()) {
            List<DeviceDto> devices = adminApiClient.listDevicesBySite(siteId);
            model.addAttribute("devices", devices);
            model.addAttribute("siteId", siteId);
        } else {
            model.addAttribute("devices", java.util.Collections.emptyList());
        }
        return "devices/list";
    }

    @GetMapping("/create")
    public String createForm(@RequestParam("siteId") String siteId, @RequestParam("tenantId") String tenantId, Model model) {
        CreateDeviceForm form = new CreateDeviceForm();
        form.setSiteId(siteId);
        form.setTenantId(tenantId);
        model.addAttribute("form", form);
        return "devices/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute CreateDeviceForm form) {
        adminApiClient.createDevice(form);
        return "redirect:/devices?siteId=" + form.getSiteId();
    }

    @GetMapping("/{deviceId}")
    public String detail(@PathVariable("deviceId") String deviceId, Model model) {
        model.addAttribute("deviceId", deviceId);
        // Poichè non abbiamo getDeviceById, passiamo solo l'id o filtriamo dalla lista (nella realtà aggiungeremo l'API)
        return "devices/detail";
    }

    @PostMapping("/{deviceId}/bootstrap-token")
    public ResponseEntity<byte[]> generateBootstrapToken(@PathVariable("deviceId") String deviceId) throws Exception {
        BootstrapTokenResponse tokenResponse = adminApiClient.generateBootstrapToken(deviceId);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tokenResponse);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bootstrap-" + deviceId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes());
    }

    @PostMapping("/{deviceId}/delete")
    public String delete(@PathVariable("deviceId") String deviceId, @RequestParam("siteId") String siteId) {
        adminApiClient.deleteDevice(deviceId);
        return "redirect:/devices?siteId=" + siteId;
    }
}

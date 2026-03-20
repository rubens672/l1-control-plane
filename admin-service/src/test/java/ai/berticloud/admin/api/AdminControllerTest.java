package ai.berticloud.admin.api;

import ai.berticloud.admin.api.dto.CreateDeviceRequest;
import ai.berticloud.admin.api.dto.CreateSiteRequest;
import ai.berticloud.admin.api.dto.CreateSubscriptionRequest;
import ai.berticloud.admin.api.dto.CreateTenantRequest;
import ai.berticloud.admin.db.AdminRepository;
import ai.berticloud.admin.security.BootstrapTokenIssuer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.mockito.Mockito;
import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Per ciascun endpoint ho creato un test che verifica il "percorso felice" (richiesta valida, risposta 200 OK) e un test
 * per le validazioni (ad esempio dati mancanti, che restituiscono 400 Bad Request). Nel caso del token, ho anche
 * verificato che la risposta JSON contenga i campi generati correttamente (bootstrapToken, enrollmentUrl, ecc.).
 *
 * @WebMvcTest(AdminController.class): Questa annotazione dice a Spring di caricare solo i componenti legati allo strato
 * web (MVC), come i Controller, i filtri, i convertitori Jackson, ecc. Non avvia l'intero contesto dell'applicazione,
 * quindi non inizializza la connessione al database, i servizi o i repository reali.
 *
 * @MockBean: Nel test ho definito AdminRepository repo; e BootstrapTokenIssuer issuer; annotandoli con @MockBean.
 * In questo modo Spring, non trovando i bean reali (perché @WebMvcTest non li ha caricati), crea degli oggetti "finti"
 * (Mock). Quando il Controller chiama repo.createTenant(...), sta in realtà chiamando il mock, il quale non fa nulla
 * se non registrare il fatto di essere stato chiamato.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 12 March 2026
 */

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminRepository repo;

    @MockBean
    private BootstrapTokenIssuer issuer;

    @Test
    void createTenant_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        CreateTenantRequest request = new CreateTenantRequest("tnt-001", "Cliente Roma", "PRO");

        mockMvc.perform(post("/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(repo).createTenant("tnt-001", "Cliente Roma", "PRO");
    }

    @Test
    void createTenant_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // tenantId is blank (violates @NotBlank)
        CreateTenantRequest request = new CreateTenantRequest("", "Cliente Roma", "PRO");

        mockMvc.perform(post("/v1/admin/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsertSubscription_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "tnt-001", "ACTIVE", Instant.parse("2026-03-01T00:00:00Z"), Instant.parse("2027-03-01T00:00:00Z"), 100);

        mockMvc.perform(post("/v1/admin/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(repo).upsertSubscription("tnt-001", "ACTIVE", request.validFrom(), request.validTo(), 100);
    }

    @Test
    void upsertSubscription_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // validTo is null (violates @NotNull)
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                "tnt-001", "ACTIVE", Instant.parse("2026-03-01T00:00:00Z"), null, 100);

        mockMvc.perform(post("/v1/admin/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSite_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        CreateSiteRequest request = new CreateSiteRequest("site-roma-001", "tnt-001", "Villa Roma", "Europe/Rome", "ACTIVE");

        mockMvc.perform(post("/v1/admin/sites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(repo).createSite("site-roma-001", "tnt-001", "Villa Roma", "Europe/Rome", "ACTIVE");
    }

    @Test
    void createSite_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // siteId is blank (violates @NotBlank)
        CreateSiteRequest request = new CreateSiteRequest("", "tnt-001", "Villa Roma", "Europe/Rome", "ACTIVE");

        mockMvc.perform(post("/v1/admin/sites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDevice_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        CreateDeviceRequest request = new CreateDeviceRequest("rpi-123", "tnt-001", "site-roma-001", "raspberry-pi4");

        mockMvc.perform(post("/v1/admin/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(repo).createDevicePending("rpi-123", "tnt-001", "site-roma-001", "raspberry-pi4");
    }

    @Test
    void createDevice_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // deviceId is blank (violates @NotBlank)
        CreateDeviceRequest request = new CreateDeviceRequest("", "tnt-001", "site-roma-001", "raspberry-pi4");

        mockMvc.perform(post("/v1/admin/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void issueBootstrap_ShouldReturnOkAndToken() throws Exception {
        String deviceId = "rpi-123";
        Instant expiresAt = Instant.now().plus(60, java.time.temporal.ChronoUnit.MINUTES);
        BootstrapTokenIssuer.IssuedToken mockToken = new BootstrapTokenIssuer.IssuedToken(
                deviceId, "plaintext-token-123", "hash-123", expiresAt, "https://enroll.example.com" ,"https://telemetry.example.com");

        Mockito.when(issuer.issueOneTimeToken(deviceId)).thenReturn(mockToken);

        mockMvc.perform(post("/v1/admin/devices/{deviceId}:bootstrapToken", deviceId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.deviceId").value(deviceId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.bootstrapToken").value("plaintext-token-123"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.enrollmentUrl").value("https://enroll.example.com"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.expiresAt").exists());

        verify(issuer).issueOneTimeToken(deviceId);
        verify(repo).setBootstrapToken(deviceId, "hash-123", expiresAt);
    }
}

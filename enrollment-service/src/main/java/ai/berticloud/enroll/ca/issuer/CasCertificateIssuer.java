package ai.berticloud.enroll.ca.issuer;

import com.google.cloud.security.privateca.v1.CaPoolName;
import com.google.cloud.security.privateca.v1.Certificate;
import com.google.cloud.security.privateca.v1.CertificateAuthorityServiceClient;
import com.google.cloud.security.privateca.v1.CreateCertificateRequest;
import com.google.protobuf.Duration;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.UUID;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Service per l'emissione di certificati client X.509 utilizzando Google Cloud Certificate Authority Service (CAS).
 * 
 * RESPONSABILITÀ:
 * 1) Ricevere la richiesta di emissione (CSR PEM)
 * 2) Inoltrare la richiesta a GCP CAS configurando i parametri del certificato
 * 3) Parsing del certificato ritornato da GCP per estrarre il seriale e la data di scadenza
 * 
 * Il trust model delega alla Private CA gestita su Google Cloud la corretta firma e distribuzione
 * della catena crittografica (PEM chain) e utilizza il CSR generato on-edge mantenedo la cifratura asimmetrica sicura.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 31 March 2026
 */
@Service
public class CasCertificateIssuer implements CertificateIssuer {
    private static final Logger log = LoggerFactory.getLogger(CasCertificateIssuer.class);

    private final String projectId;
    private final String location;
    private final String caPoolId;
    private final String caId; // optional
    private final int lifetimeDays;

    public CasCertificateIssuer(
            @Value("${gcp.cas.project-id}") String projectId,
            @Value("${gcp.cas.location}") String location,
            @Value("${gcp.cas.ca-pool-id}") String caPoolId,
            @Value("${gcp.cas.certificate-authority-id:}") String caId,
            @Value("${gcp.cas.lifetime-days:365}") int lifetimeDays) {
        this.projectId = projectId;
        this.location = location;
        this.caPoolId = caPoolId;
        this.caId = caId;
        this.lifetimeDays = lifetimeDays;
    }

    @Override
    public IssuedCertificate issueDeviceCertificate(String deviceId, String csrPem, String sanUri) throws Exception {
        log.info("Requesting CAS certificate for device: {}", deviceId);

        try (CertificateAuthorityServiceClient casClient = CertificateAuthorityServiceClient.create()) {
            String poolName = CaPoolName.of(projectId, location, caPoolId).toString();
            // Google CAS expects a unique ID for the certificate within the pool
            String certId = "device-" + deviceId + "-" + UUID.randomUUID().toString().substring(0, 8);

            Certificate certificate = Certificate.newBuilder()
                    .setPemCsr(csrPem)
                    .setLifetime(Duration.newBuilder().setSeconds(lifetimeDays * 86400L).build())
                    .build();

            CreateCertificateRequest.Builder requestBuilder = CreateCertificateRequest.newBuilder()
                    .setParent(poolName)
                    .setCertificateId(certId)
                    .setCertificate(certificate);

            if (caId != null && !caId.isBlank()) {
                requestBuilder.setIssuingCertificateAuthorityId(caId);
            }

            Certificate createdCert = casClient.createCertificate(requestBuilder.build());

            String clientCertPem = createdCert.getPemCertificate();

            StringBuilder chainPem = new StringBuilder();
            for (String chainPart : createdCert.getPemCertificateChainList()) {
                chainPem.append(chainPart);
                if (!chainPart.endsWith("\n")) chainPem.append("\n");
            }

            // Parse returned PEM to extract required fields
            X509Certificate x509Cert = parseCert(clientCertPem);
            String serialNumber = x509Cert.getSerialNumber().toString(16);
            Instant notAfter = x509Cert.getNotAfter().toInstant();

            log.info("Successfully received CAS certificate for device: {} (Serial: {})", deviceId, serialNumber);
            return new IssuedCertificate(clientCertPem, chainPem.toString(), serialNumber, notAfter);
        }
    }

    private X509Certificate parseCert(String pem) throws Exception {
        try (PEMParser p = new PEMParser(new StringReader(pem))) {
            Object o = p.readObject();
            if (!(o instanceof X509CertificateHolder)) {
                throw new IllegalArgumentException("Invalid cert PEM format returned from CAS");
            }
            return new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) o);
        }
    }
}

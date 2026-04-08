package ai.berticloud.ingest.auth;

import ai.berticloud.shared.identity.DeviceIdentity;
import ai.berticloud.shared.identity.SanUriParser;
import ai.berticloud.shared.identity.SanUriSelector;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

@Component
public class MtlsHeaderFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(MtlsHeaderFilter.class);
    private final RootCaPublicKeyProvider rootCaProvider;

    public MtlsHeaderFilter(RootCaPublicKeyProvider rootCaProvider) {
        this.rootCaProvider = rootCaProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String certHeader = request.getHeader("X-Device-Cert");
        if (certHeader == null || certHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Restore newlines if the header was flattened (e.g. spaces instead of newlines)
            if (!certHeader.contains("\n")) {
                certHeader = certHeader.replace(" -----END", "\n-----END").replace("CERTIFICATE----- ", "CERTIFICATE-----\n");
            }

            X509Certificate cert = parseCert(certHeader);

            // 1. Verify signature against Root CA
            cert.verify(rootCaProvider.getRootCaPublicKey());

            // 2. Verify validity dates
            cert.checkValidity();

            // 3. Extract SAN URI
            String deviceUrn = SanUriSelector.pickDeviceUrnFromCert(cert);
            if (deviceUrn == null) {
                throw new IllegalArgumentException("No valid SAN URI found in certificate");
            }
            DeviceIdentity id = SanUriParser.parse(deviceUrn);

            // 4. Calculate fingerprint
            String fp = calculateFingerprint(cert);

            // 5. Populate SecurityContext
            DeviceAuthenticationToken auth = new DeviceAuthenticationToken(id, fp);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            log.warn("Invalid client certificate in X-Device-Cert header: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"invalid_client_certificate\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static X509Certificate parseCert(String pem) throws Exception {
        try (PEMParser p = new PEMParser(new StringReader(pem))) {
            Object o = p.readObject();
            if (!(o instanceof X509CertificateHolder h))
                throw new IllegalArgumentException("Invalid cert PEM");
            return new JcaX509CertificateConverter().getCertificate(h);
        }
    }

    private static String calculateFingerprint(X509Certificate cert) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(cert.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

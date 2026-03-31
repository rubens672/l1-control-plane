package ai.berticloud.enroll.ca;

import ai.berticloud.shared.identity.DeviceIdentity;
import ai.berticloud.shared.identity.SanUriParser;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.stereotype.Service;

import java.io.StringReader;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Validazione del CSR inviato dal device.
 *
 * RESPONSABILITÀ:
 * 1) parse CSR PEM (PKCS#10)
 * 2) estrazione e validazione SAN URI dal CSR
 *
 * TRUST MODEL:
 * - CSR viene dal device. Il device può provare a inserire SAN non autorizzati.
 * - Perciò VALIDAZIONE è doppia:
 *   - formato SAN conforme
 *   - match tra SAN e record DB del device (fatto nel controller)
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@Service
public class CsrValidator {

  /**
   * Parse CSR e valida che:
   * - contenga SAN URI in extensionRequest
   * - SAN abbia il formato URN previsto
   * - deviceId nel SAN coincida con deviceId passato nell'endpoint (anti spoof)
   */
  public ParsedCsr parseAndValidateCsr(String csrPem, String expectedDeviceId) throws Exception{
    PKCS10CertificationRequest csr = parseCsr(csrPem);

    // estrai SAN (URI) e valida formato + deviceId nel path API
    String urn = extractFirstDeviceUrnFromCsr(csr);
    DeviceIdentity id = SanUriParser.parse(urn);

    if (!id.deviceId().equals(expectedDeviceId)) {
      throw new IllegalArgumentException("CSR SAN deviceId mismatch with path deviceId");
    }

    return new ParsedCsr(csr, id, urn);
  }

  // ===== Helpers CSR parsing =====
  private static PKCS10CertificationRequest parseCsr(String pem) {
    try (PEMParser p = new PEMParser(new StringReader(pem))) {
      Object o = p.readObject();
      if (!(o instanceof PKCS10CertificationRequest csr)) throw new IllegalArgumentException("Invalid CSR PEM");
      return csr;
    } catch (Exception e) {
      throw new RuntimeException("Cannot parse CSR", e);
    }
  }

  /**
   * Il CSR deve contenere l'attributo pkcs_9_at_extensionRequest.
   * Dentro troviamo le extensions richieste dal device (in particolare SAN).
   */
  private static Extensions extractExtensionsFromCsr(PKCS10CertificationRequest csr) throws Exception {
    for (org.bouncycastle.asn1.pkcs.Attribute attr : csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
      ASN1Encodable[] values = attr.getAttrValues().toArray();
      if (values.length > 0) return Extensions.getInstance(values[0]);
    }
    throw new IllegalArgumentException("CSR missing extensionRequest (SAN required)");
  }

  private static GeneralNames extractSubjectAltNames(Extensions exts) {
    Extension sanExt = exts.getExtension(Extension.subjectAlternativeName);
    if (sanExt == null) throw new IllegalArgumentException("CSR missing subjectAltName");
    return GeneralNames.getInstance(sanExt.getParsedValue());
  }

  /**
   * Cerca la prima SAN URI che matcha la nostra URN convention.
   * Se non presente -> reject.
   */
  private static String extractFirstDeviceUrnFromCsr(PKCS10CertificationRequest csr) throws Exception {
    Extensions exts = extractExtensionsFromCsr(csr);
    GeneralNames names = extractSubjectAltNames(exts);

    for (GeneralName gn : names.getNames()) {
      if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
        String uri = gn.getName().toString();
        if (uri.startsWith("urn:berticloudai:tenant:") && uri.contains(":site:") && uri.contains(":device:")) {
          return uri;
        }
      }
    }
    throw new IllegalArgumentException("CSR SAN URI missing or invalid");
  }

  /** Bundle parse CSR: CSR + identity + urn */
  public record ParsedCsr(PKCS10CertificationRequest csr, DeviceIdentity identity, String urn) {}
}

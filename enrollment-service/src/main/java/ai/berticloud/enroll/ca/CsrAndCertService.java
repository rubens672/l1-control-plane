package ai.berticloud.enroll.ca;

import ai.berticloud.enroll.util.CryptoUtil;
import ai.berticloud.shared.identity.DeviceIdentity;
import ai.berticloud.shared.identity.SanUriParser;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.*;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

@Service
public class CsrAndCertService {
  private static final SecureRandom RND = new SecureRandom();

  private final int validityDays;

  public CsrAndCertService(@Value("${app.cert.validityDays}") int validityDays) {
    this.validityDays = validityDays;
  }

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

  public SignedCert sign(CaMaterial ca, ParsedCsr parsed) throws Exception{
    try {
      Instant now = Instant.now();
      Date notBefore = Date.from(now.minus(5, ChronoUnit.MINUTES));
      Date notAfter = Date.from(now.plus(validityDays, ChronoUnit.DAYS));

      var csr = new JcaPKCS10CertificationRequest(parsed.csr());

      // Issuer = CA
      var issuer = new org.bouncycastle.asn1.x500.X500Name(ca.issuingCert().getSubjectX500Principal().getName());

      // Subject = quello del CSR (può essere minimale)
      var subject = csr.getSubject();

      // Serial random (positivo)
      BigInteger serial = new BigInteger(160, RND).abs();

      // Public key dal CSR
      var publicKey = csr.getPublicKey();

      var builder = new X509v3CertificateBuilder(
          issuer,
          serial,
          notBefore,
          notAfter,
          subject,
          SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())
      );

      // Extensions: KeyUsage, EKU, SAN (copiato dal CSR)
      builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
      builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
      builder.addExtension(Extension.extendedKeyUsage, false,
          new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth}));

      Extensions csrExt = extractExtensionsFromCsr(parsed.csr());
      GeneralNames sans = extractSubjectAltNames(csrExt);
      builder.addExtension(Extension.subjectAlternativeName, false, sans);

      var signer = new JcaContentSignerBuilder("SHA256withRSA").build(ca.issuingKey());
      X509Certificate cert = new JcaX509CertificateConverter().getCertificate(builder.build(signer));

      // fingerprint sha256 (DER)
      String fpHex = CryptoUtil.sha256Hex(cert.getEncoded());
      return new SignedCert(cert, serial.toString(16), cert.getNotAfter().toInstant(), fpHex);

    } catch (Exception e) {
      throw new RuntimeException("Cannot sign certificate", e);
    }
  }

  private static PKCS10CertificationRequest parseCsr(String pem) {
    try (PEMParser p = new PEMParser(new StringReader(pem))) {
      Object o = p.readObject();
      if (!(o instanceof PKCS10CertificationRequest csr)) throw new IllegalArgumentException("Invalid CSR PEM");
      return csr;
    } catch (Exception e) {
      throw new RuntimeException("Cannot parse CSR", e);
    }
  }

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

  public record ParsedCsr(PKCS10CertificationRequest csr, DeviceIdentity identity, String urn) {}

  public record SignedCert(X509Certificate cert, String certSerialHex, Instant notAfter, String fingerprintSha256Hex) {}
}
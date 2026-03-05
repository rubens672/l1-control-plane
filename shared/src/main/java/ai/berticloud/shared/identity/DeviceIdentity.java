package ai.berticloud.shared.identity;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Identità forte del device derivata dal certificato client mTLS.
 *
 * ORIGINE:
 * - Viene estratta dal SAN URI del certificato client:
 *     urn:berticloudai:tenant:<TENANT_ID>:site:<SITE_ID>:device:<DEVICE_ID>
 *
 * PERCHÉ È "FORTE":
 * - È dentro un certificato X.509 firmato dalla nostra CA.
 * - Il Load Balancer valida la chain e ci fornisce i SAN via header.
 *
 * NOTE:
 * - Questo record è usato da ingest-service per fare AuthZ e tagging Pub/Sub.
 * - È usato da enrollment-service per validare che il CSR contenga la SAN corretta.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public record DeviceIdentity(String tenantId, String siteId, String deviceId) {}
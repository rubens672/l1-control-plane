package ai.berticloud.ingest.auth;

import java.time.Instant;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Contesto autorizzativo "hot-path" per un device.
 *
 * PERCHÉ ESISTE:
 * - L'ingest deve essere veloce: non vogliamo fare join DB ad ogni POST /telemetry.
 * - Quindi estraiamo dal DB un "riassunto" delle info necessarie per decidere:
 *   - è un device valido?
 *   - tenant/site/subscription sono attivi?
 *   - subscription è scaduta?
 *   - fingerprint atteso combacia con quello presentato in mTLS?
 *   - qual è il limite max messaggi/minuto?
 *
 * NOTA:
 * - Questo oggetto entra in cache. Deve rimanere piccolo e serializzabile "in memoria".
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public record DeviceAuthContext(
    String tenantId,
    String siteId,
    String deviceId,
    String tenantStatus,
    String siteStatus,
    String deviceStatus,
    String subscriptionStatus,
    Instant subscriptionValidTo,
    String expectedFingerprintSha256,
    int maxMsgsPerMin
) {}
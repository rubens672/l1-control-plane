package ai.berticloud.ingest.auth;

import ai.berticloud.shared.identity.DeviceIdentity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

public class DeviceAuthenticationToken extends AbstractAuthenticationToken {
    private final DeviceIdentity deviceIdentity;
    private final String fingerprint;

    public DeviceAuthenticationToken(DeviceIdentity deviceIdentity, String fingerprint) {
        super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVICE")));
        this.deviceIdentity = deviceIdentity;
        this.fingerprint = fingerprint;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // non gestiamo credenziali qui
    }

    @Override
    public Object getPrincipal() {
        return deviceIdentity;
    }

    public DeviceIdentity getDeviceIdentity() {
        return deviceIdentity;
    }

    public String getFingerprint() {
        return fingerprint;
    }
}

package io.aeronic.cluster;

import io.aeron.security.CredentialsSupplier;
import org.agrona.collections.ArrayUtil;

public class AeronicCredentialsSupplier implements CredentialsSupplier {
    private final String name;

    public AeronicCredentialsSupplier(final String name)
    {
        this.name = name;
    }

    @Override
    public byte[] encodedCredentials()
    {
        return name.getBytes();
    }

    @Override
    public byte[] onChallenge(final byte[] encodedChallenge)
    {
        return ArrayUtil.EMPTY_BYTE_ARRAY;
    }
}
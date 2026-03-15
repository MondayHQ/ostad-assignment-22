package com.zubayer.customauthentication.utils;

import java.util.HexFormat;
import java.security.SecureRandom;

public class TokenGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateToken() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);

        return HexFormat.of().formatHex(randomBytes);
    }

}

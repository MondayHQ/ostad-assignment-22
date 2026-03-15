package com.zubayer.customauthentication.exceptions;

import java.time.LocalDateTime;

public record CustomErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp
) {}

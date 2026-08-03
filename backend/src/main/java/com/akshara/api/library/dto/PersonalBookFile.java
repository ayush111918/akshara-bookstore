package com.akshara.api.library.dto;

import org.springframework.core.io.Resource;

public record PersonalBookFile(
        Resource resource,
        String mediaType,
        String originalFilename
) {
}

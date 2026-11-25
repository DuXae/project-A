package com.duxae.projecta.web.dto;

import java.util.List;

public record BulkSaveResponse(
        int saved,
        List<ProductResponse> items
) {
}

package com.duxae.projecta.web.dto;

/**
 * Результат пачечного удаления: сколько записей реально удалено.
 */
public record BulkDeleteResponse(
        int deleted
) {
}

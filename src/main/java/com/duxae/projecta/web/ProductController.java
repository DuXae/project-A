package com.duxae.projecta.web;

import com.duxae.projecta.domain.Product;
import com.duxae.projecta.service.ProductService;
import com.duxae.projecta.web.dto.BulkDeleteRequest;
import com.duxae.projecta.web.dto.BulkDeleteResponse;
import com.duxae.projecta.web.dto.BulkSaveResponse;
import com.duxae.projecta.web.dto.PageResponse;
import com.duxae.projecta.web.dto.ProductBulkRequest;
import com.duxae.projecta.web.dto.ProductRequest;
import com.duxae.projecta.web.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller: приём формы и отдача сохранённых товаров.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductResponse> list(
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return PageResponse.from(productService.findAll(pageable), ProductResponse::from);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkSaveResponse saveBulk(@Valid @RequestBody ProductBulkRequest request) {
        List<Product> toSave = request.items().stream()
                .map(item -> new Product(item.name().trim(), item.quantity()))
                .toList();

        List<Product> saved = productService.saveAll(toSave);

        return new BulkSaveResponse(
                saved.size(),
                saved.stream().map(ProductResponse::from).toList()
        );
    }

    /**
     * Мягкое удаление одной записи (FR-8). Повторный вызов — 404 (FR-10).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.softDelete(id);
    }

    /**
     * Мягкое удаление пачки в одной транзакции (FR-9, NFR-1).
     */
    @PostMapping("/bulk-delete")
    public BulkDeleteResponse bulkDelete(@Valid @RequestBody BulkDeleteRequest request) {
        int deleted = productService.softDeleteAll(request.ids());
        return new BulkDeleteResponse(deleted);
    }
}

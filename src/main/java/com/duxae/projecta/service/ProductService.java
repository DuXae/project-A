package com.duxae.projecta.service;

import com.duxae.projecta.domain.Product;
import com.duxae.projecta.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Бизнес-логика товаров.
 *
 * Пачка строк сохраняется в одной транзакции, пачка удаляется тоже целиком
 * (NFR-1). Удаление мягкое: запись помечается датой и остаётся в БД (FR-6).
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Только активные записи: мягко удалённые не показываются (FR-7).
     */
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAllByDeletedAtIsNull(pageable);
    }

    @Transactional
    public List<Product> saveAll(List<Product> products) {
        return productRepository.saveAll(products);
    }

    /**
     * Мягкое удаление одной записи. Отсутствующая или уже удалённая запись —
     * та же ошибка 404 для клиента (FR-10).
     */
    @Transactional
    public void softDelete(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.markDeleted();
        productRepository.save(product);
    }

    /**
     * Мягкое удаление пачки в одной транзакции (NFR-1).
     * Возвращает количество фактически удалённых записей; id, которых нет
     * или которые уже удалены, не считаются ошибкой (R-5).
     */
    @Transactional
    public int softDeleteAll(Collection<Long> ids) {
        List<Product> products = productRepository.findAllByIdInAndDeletedAtIsNull(ids);
        products.forEach(Product::markDeleted);
        productRepository.saveAll(products);
        return products.size();
    }
}

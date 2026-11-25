package com.duxae.projecta.repository;

import com.duxae.projecta.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Доступ к товарам.
 *
 * Мягкое удаление (FR-6) требует явной фильтрации, поэтому методы со словами
 * "Active"/"DeletedAtIsNull" — единственный способ получить активные записи.
 * Никакой скрытой магии: фильтр видно в имени метода.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    List<Product> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);
}

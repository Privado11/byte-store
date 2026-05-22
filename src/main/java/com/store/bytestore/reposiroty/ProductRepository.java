package com.store.bytestore.reposiroty;

import com.store.bytestore.entities.Product;
import com.store.bytestore.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySlug(String slug);

    Page<Product> findByCategoryIdAndStatus(UUID categoryId, ProductStatus status, Pageable pageable);


    @Query(value = """
        SELECT * FROM products
        WHERE to_tsvector('spanish', name || ' ' || COALESCE(description, ''))
              @@ plainto_tsquery('spanish', :query)
        AND status = 'ACTIVE'
        """, nativeQuery = true)
    Page<Product> searchByText(String query, Pageable pageable);
}

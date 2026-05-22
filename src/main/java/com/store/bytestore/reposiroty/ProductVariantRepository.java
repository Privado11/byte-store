package com.store.bytestore.repository;

import com.store.bytestore.entities.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductId(UUID productId);

    List<ProductVariant> findByProductIdAndIsActiveTrue(UUID productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    List<ProductVariant> findByProductIdAndStockGreaterThan(UUID productId, int stock);

    @Modifying
    @Query("UPDATE ProductVariant v SET v.stock = v.stock - :quantity WHERE v.id = :id AND v.stock >= :quantity")
    int decrementStock(UUID id, int quantity);

    @Modifying
    @Query("UPDATE ProductVariant v SET v.stock = v.stock + :quantity WHERE v.id = :id")
    int incrementStock(UUID id, int quantity);
}
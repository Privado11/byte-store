package com.store.bytestore.reposiroty;

import com.store.bytestore.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductIdAndIsVisibleTrue(UUID productId, Pageable pageable);
    boolean existsByProductIdAndUserIdAndOrderId(UUID productId, UUID userId, UUID orderId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isVisible = true")
    Double calculateAvgRating(UUID productId);
}

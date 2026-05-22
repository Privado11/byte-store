package com.store.bytestore.reposiroty;


import com.store.bytestore.entities.UserToken;
import com.store.bytestore.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface UserTokenRepository extends JpaRepository<UserToken, UUID> {
    Optional<UserToken> findByTokenAndType(String token, TokenType type);

    @Modifying
    @Query("DELETE FROM UserToken t WHERE t.user.id = :userId AND t.type = :type")
    void deleteByUserIdAndType(UUID userId, TokenType type);
}

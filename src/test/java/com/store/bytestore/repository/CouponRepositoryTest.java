package com.store.bytestore.repository;

import com.store.bytestore.entities.Coupon;
import com.store.bytestore.enums.CouponType;
import com.store.bytestore.reposiroty.CouponRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CouponRepository — Tests de integración")
class CouponRepositoryTest extends AbstractIntegrationDBTest {

    @Autowired
    CouponRepository couponRepository;

    private Coupon percentage;
    private Coupon fixed;

    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();

        percentage = Coupon.builder()
                .code("DESCUENTO20")
                .description("20% de descuento")
                .type(CouponType.PERCENTAGE)
                .value(new BigDecimal("20.00"))
                .minOrderAmount(new BigDecimal("100000.00"))
                .maxDiscount(new BigDecimal("50000.00"))
                .usageLimit(100)
                .isActive(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        couponRepository.save(percentage);

        fixed = Coupon.builder()
                .code("FIJO15000")
                .description("$15.000 de descuento")
                .type(CouponType.FIXED_AMOUNT)
                .value(new BigDecimal("15000.00"))
                .isActive(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .build();
        couponRepository.save(fixed);

        couponRepository.flush();
    }

    @Nested
    @DisplayName("Crear cupón")
    class Create {

        @Test
        @DisplayName("Debe guardar un cupón y generar UUID")
        void shouldSaveCouponWithUUID() {
            Coupon newCoupon = Coupon.builder()
                    .code("NUEVO10")
                    .type(CouponType.PERCENTAGE)
                    .value(new BigDecimal("10.00"))
                    .isActive(true)
                    .validFrom(LocalDateTime.now())
                    .build();

            Coupon saved = couponRepository.save(newCoupon);

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("Debe asignar usedCount 0 por defecto")
        void shouldDefaultUsedCountZero() {
            Coupon saved = couponRepository.findById(percentage.getId()).orElseThrow();

            assertThat(saved.getUsedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Debe guardar cupón sin límite de uso (usageLimit null)")
        void shouldSaveCouponWithNullUsageLimit() {
            Coupon unlimited = Coupon.builder()
                    .code("SINLIMITE")
                    .type(CouponType.FIXED_AMOUNT)
                    .value(new BigDecimal("5000.00"))
                    .isActive(true)
                    .validFrom(LocalDateTime.now())
                    // usageLimit no se setea → null = ilimitado
                    .build();

            Coupon saved = couponRepository.save(unlimited);

            assertThat(saved.getUsageLimit()).isNull();
        }
    }

    @Nested
    @DisplayName("Leer cupones")
    class Read {

        @Test
        @DisplayName("Debe retornar todos los cupones")
        void shouldReturnAllCoupons() {
            List<Coupon> coupons = couponRepository.findAll();

            assertThat(coupons).hasSize(2);
        }

        @Test
        @DisplayName("findByCodeAndIsActiveTrue debe retornar cupón activo por código")
        void shouldFindActiveCouponByCode() {
            Optional<Coupon> found = couponRepository.findByCodeAndIsActiveTrue("DESCUENTO20");

            assertThat(found).isPresent();
            assertThat(found.get().getType()).isEqualTo(CouponType.PERCENTAGE);
            assertThat(found.get().getValue()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("findByCodeAndIsActiveTrue no debe retornar cupón inactivo")
        void shouldNotReturnInactiveCoupon() {
            percentage.setIsActive(false);
            couponRepository.saveAndFlush(percentage);

            Optional<Coupon> found = couponRepository.findByCodeAndIsActiveTrue("DESCUENTO20");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findByCodeAndIsActiveTrue debe retornar vacío para código inexistente")
        void shouldReturnEmptyForNonExistentCode() {
            Optional<Coupon> found = couponRepository.findByCodeAndIsActiveTrue("NOEXISTE");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Actualizar cupón")
    class Update {

        @Test
        @DisplayName("Debe incrementar usedCount correctamente")
        void shouldIncrementUsedCount() {
            percentage.setUsedCount(percentage.getUsedCount() + 1);
            couponRepository.saveAndFlush(percentage);

            Coupon updated = couponRepository.findById(percentage.getId()).orElseThrow();
            assertThat(updated.getUsedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Debe desactivar un cupón")
        void shouldDeactivateCoupon() {
            fixed.setIsActive(false);
            couponRepository.saveAndFlush(fixed);

            Coupon updated = couponRepository.findById(fixed.getId()).orElseThrow();
            assertThat(updated.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Debe actualizar la fecha de expiración")
        void shouldUpdateExpirationDate() {
            LocalDateTime newExpiry = LocalDateTime.now().plusDays(60);
            percentage.setExpiresAt(newExpiry);
            couponRepository.saveAndFlush(percentage);

            Coupon updated = couponRepository.findById(percentage.getId()).orElseThrow();
            assertThat(updated.getExpiresAt()).isAfter(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("Eliminar cupón")
    class Delete {

        @Test
        @DisplayName("Debe eliminar un cupón por ID")
        void shouldDeleteById() {
            couponRepository.deleteById(percentage.getId());

            assertThat(couponRepository.findById(percentage.getId())).isEmpty();
        }

        @Test
        @DisplayName("Debe eliminar todos los cupones")
        void shouldDeleteAll() {
            couponRepository.deleteAll();

            assertThat(couponRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Casos borde")
    class EdgeCases {

        @Test
        @DisplayName("Código de cupón debe ser único")
        void shouldNotAllowDuplicateCouponCode() {
            Coupon duplicate = Coupon.builder()
                    .code("DESCUENTO20")
                    .type(CouponType.PERCENTAGE)
                    .value(new BigDecimal("10.00"))
                    .isActive(true)
                    .validFrom(LocalDateTime.now())
                    .build();

            assertThrows(Exception.class,
                    () -> couponRepository.saveAndFlush(duplicate));
        }
    }
}

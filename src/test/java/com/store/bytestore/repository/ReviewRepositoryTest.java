package com.store.bytestore.repository;

import com.store.bytestore.entities.*;
import com.store.bytestore.entities.Order;
import com.store.bytestore.enums.*;
import com.store.bytestore.reposiroty.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ReviewRepository — Tests de integración")
class ReviewRepositoryTest extends AbstractIntegrationDBTest {

    @Autowired
    ReviewRepository reviewRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CategoryRepository categoryRepository;

    private User customer;
    private Product product;
    private Order order1;
    private Order order2;
    private Review review1;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        customer = User.builder()
                .firstName("Walter").lastName("Jiménez")
                .email("walter@mail.com").passwordHash("hash")
                .role(UserRole.CUSTOMER).isActive(true).build();
        userRepository.save(customer);

        Category cat = Category.builder()
                .name("Electrónica").slug("electronica").isActive(true).build();
        categoryRepository.save(cat);

        product = Product.builder()
                .category(cat).name("Laptop").slug("laptop")
                .price(new BigDecimal("3000000")).status(ProductStatus.ACTIVE).build();
        productRepository.save(product);

        Map<String, Object> address = Map.of(
                "city", "Bogotá", "country", "Colombia",
                "addressLine", "Calle 1", "fullName", "Walter", "phone", "300"
        );

        order1 = Order.builder()
                .user(customer).status(OrderStatus.DELIVERED)
                .subtotal(new BigDecimal("3000000")).total(new BigDecimal("3000000"))
                .shippingAddress(address).build();
        orderRepository.save(order1);

        order2 = Order.builder()
                .user(customer).status(OrderStatus.DELIVERED)
                .subtotal(new BigDecimal("3000000")).total(new BigDecimal("3000000"))
                .shippingAddress(address).build();
        orderRepository.save(order2);

        review1 = Review.builder()
                .product(product).user(customer).order(order1)
                .rating((short) 5)
                .title("Excelente producto")
                .body("Muy buena laptop, recomendada al 100%")
                .isVisible(true)
                .build();
        reviewRepository.save(review1);
        reviewRepository.flush();
    }

    @Nested
    @DisplayName("Crear reseña")
    class Create {

        @Test
        @DisplayName("Debe guardar una reseña y generar UUID")
        void shouldSaveReviewWithUUID() {
            Review newReview = Review.builder()
                    .product(product).user(customer).order(order2)
                    .rating((short) 4)
                    .title("Buena laptop")
                    .isVisible(true)
                    .build();

            Review saved = reviewRepository.save(newReview);

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("Debe guardar reseña con rating mínimo 1")
        void shouldSaveReviewWithMinRating() {
            Review newReview = Review.builder()
                    .product(product).user(customer).order(order2)
                    .rating((short) 1)
                    .isVisible(true)
                    .build();

            Review saved = reviewRepository.save(newReview);

            assertThat(saved.getRating()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("Debe guardar reseña sin título ni cuerpo (son opcionales)")
        void shouldSaveReviewWithoutTitleAndBody() {
            Review minimal = Review.builder()
                    .product(product).user(customer).order(order2)
                    .rating((short) 3)
                    .isVisible(true)
                    .build();

            Review saved = reviewRepository.save(minimal);

            assertThat(saved.getTitle()).isNull();
            assertThat(saved.getBody()).isNull();
        }
    }

    @Nested
    @DisplayName("Leer reseñas")
    class Read {

        @Test
        @DisplayName("findByProductIdAndIsVisibleTrue debe retornar solo reseñas visibles")
        void shouldFindVisibleReviewsByProduct() {
            Review hidden = Review.builder()
                    .product(product).user(customer).order(order2)
                    .rating((short) 2).isVisible(false).build();
            reviewRepository.saveAndFlush(hidden);

            Page<Review> visible = reviewRepository.findByProductIdAndIsVisibleTrue(
                    product.getId(), PageRequest.of(0, 10));

            assertThat(visible.getContent()).hasSize(1);
            assertThat(visible.getContent().get(0).getIsVisible()).isTrue();
        }

        @Test
        @DisplayName("existsByProductIdAndUserIdAndOrderId debe retornar true si ya reseñó")
        void shouldReturnTrueIfUserAlreadyReviewed() {
            boolean exists = reviewRepository.existsByProductIdAndUserIdAndOrderId(
                    product.getId(), customer.getId(), order1.getId());

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("existsByProductIdAndUserIdAndOrderId retorna false si aún no reseñó")
        void shouldReturnFalseIfUserHasNotReviewed() {
            boolean exists = reviewRepository.existsByProductIdAndUserIdAndOrderId(
                    product.getId(), customer.getId(), order2.getId());

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("calculateAvgRating debe calcular el promedio correctamente")
        void shouldCalculateAverageRating() {
            Review second = Review.builder()
                    .product(product).user(customer).order(order2)
                    .rating((short) 3).isVisible(true).build();
            reviewRepository.saveAndFlush(second);

            Double avg = reviewRepository.calculateAvgRating(product.getId());

            assertThat(avg).isEqualTo(4.0);
        }

        @Test
        @DisplayName("calculateAvgRating no debe incluir reseñas invisibles")
        void shouldExcludeInvisibleReviewsFromAvg() {
            Review hidden = Review.builder()
                    .product(product).user(customer).order(order2)
                    .rating((short) 1).isVisible(false).build();
            reviewRepository.saveAndFlush(hidden);

            Double avg = reviewRepository.calculateAvgRating(product.getId());

            assertThat(avg).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("Actualizar reseña")
    class Update {

        @Test
        @DisplayName("Debe ocultar una reseña cambiando isVisible a false")
        void shouldHideReview() {
            review1.setIsVisible(false);
            reviewRepository.saveAndFlush(review1);

            Review updated = reviewRepository.findById(review1.getId()).orElseThrow();
            assertThat(updated.getIsVisible()).isFalse();
        }

        @Test
        @DisplayName("Debe actualizar el cuerpo de la reseña")
        void shouldUpdateReviewBody() {
            review1.setBody("Actualicé mi opinión: sigue siendo excelente");
            reviewRepository.saveAndFlush(review1);

            Review updated = reviewRepository.findById(review1.getId()).orElseThrow();
            assertThat(updated.getBody()).contains("Actualicé");
        }
    }

    @Nested
    @DisplayName("Eliminar reseña")
    class Delete {

        @Test
        @DisplayName("Debe eliminar una reseña por ID")
        void shouldDeleteById() {
            reviewRepository.deleteById(review1.getId());

            assertThat(reviewRepository.findById(review1.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Casos borde")
    class EdgeCases {

        @Test
        @DisplayName("No debe permitir dos reseñas del mismo usuario para la misma orden y producto")
        void shouldNotAllowDuplicateReview() {
            Review duplicate = Review.builder()
                    .product(product).user(customer).order(order1) // misma combinación
                    .rating((short) 3).isVisible(true).build();

            assertThrows(Exception.class,
                    () -> reviewRepository.saveAndFlush(duplicate));
        }
    }
}

package com.store.bytestore.repository;

import com.store.bytestore.entities.*;
import com.store.bytestore.entities.Order;
import com.store.bytestore.enums.*;
import com.store.bytestore.reposiroty.CategoryRepository;
import com.store.bytestore.reposiroty.OrderRepository;
import com.store.bytestore.reposiroty.PaymentRepository;
import com.store.bytestore.reposiroty.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PaymentRepository — Tests de integración")
class PaymentRepositoryTest extends AbstractIntegrationDBTest {

    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CategoryRepository categoryRepository;

    private Order order1;
    private Order order2;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        User customer = User.builder()
                .firstName("Walter").lastName("Jiménez")
                .email("walter@mail.com").passwordHash("hash")
                .role(UserRole.CUSTOMER).isActive(true)
                .build();
        userRepository.save(customer);

        Map<String, Object> address = Map.of(
                "city", "Bogotá", "country", "Colombia",
                "addressLine", "Calle 123", "fullName", "Walter Jiménez", "phone", "300123"
        );

        order1 = Order.builder()
                .user(customer).status(OrderStatus.CONFIRMED)
                .subtotal(new BigDecimal("3000000"))
                .total(new BigDecimal("3000000"))
                .shippingAddress(address)
                .build();
        orderRepository.save(order1);

        order2 = Order.builder()
                .user(customer).status(OrderStatus.CONFIRMED)
                .subtotal(new BigDecimal("1000000"))
                .total(new BigDecimal("1000000"))
                .shippingAddress(address)
                .build();
        orderRepository.save(order2);

        // Pago para order1
        payment = Payment.builder()
                .order(order1)
                .stripePaymentIntent("pi_test_abc123")
                .amount(new BigDecimal("3000000"))
                .currency("COP")
                .status(PaymentStatus.COMPLETED)
                .paymentMethod("card")
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);
        paymentRepository.flush();
    }

    @Nested
    @DisplayName("Crear pago")
    class Create {

        @Test
        @DisplayName("Debe guardar un pago y generar UUID")
        void shouldSavePaymentWithUUID() {
            Payment newPayment = Payment.builder()
                    .order(order2)
                    .stripePaymentIntent("pi_test_xyz789")
                    .amount(new BigDecimal("1000000"))
                    .currency("COP")
                    .status(PaymentStatus.PENDING)
                    .build();

            Payment saved = paymentRepository.save(newPayment);

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("Debe asignar status PENDING por defecto")
        void shouldDefaultStatusPending() {
            Payment newPayment = Payment.builder()
                    .order(order2)
                    .amount(new BigDecimal("1000000"))
                    .currency("COP")
                    .build();

            Payment saved = paymentRepository.save(newPayment);

            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("Debe asignar currency COP por defecto")
        void shouldDefaultCurrencyCOP() {
            Payment saved = paymentRepository.findById(payment.getId()).orElseThrow();

            assertThat(saved.getCurrency()).isEqualTo("COP");
        }
    }

    @Nested
    @DisplayName("Leer pagos")
    class Read {

        @Test
        @DisplayName("findByOrderId debe retornar el pago de esa orden")
        void shouldFindPaymentByOrderId() {
            Optional<Payment> found = paymentRepository.findByOrderId(order1.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStripePaymentIntent()).isEqualTo("pi_test_abc123");
        }

        @Test
        @DisplayName("findByOrderId debe retornar vacío si la orden no tiene pago")
        void shouldReturnEmptyForOrderWithoutPayment() {
            Optional<Payment> found = paymentRepository.findByOrderId(order2.getId());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findByStripePaymentIntent debe retornar el pago correcto")
        void shouldFindByStripePaymentIntent() {
            Optional<Payment> found = paymentRepository
                    .findByStripePaymentIntent("pi_test_abc123");

            assertThat(found).isPresent();
            assertThat(found.get().getAmount()).isEqualByComparingTo("3000000");
        }

        @Test
        @DisplayName("findByStripePaymentIntent debe retornar vacío para ID inexistente")
        void shouldReturnEmptyForNonExistentStripeId() {
            Optional<Payment> found = paymentRepository
                    .findByStripePaymentIntent("pi_test_noexiste");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Actualizar pago")
    class Update {

        @Test
        @DisplayName("Debe actualizar el status del pago a REFUNDED")
        void shouldUpdatePaymentStatusToRefunded() {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.saveAndFlush(payment);

            Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("Debe registrar la fecha de pago")
        void shouldRegisterPaidAt() {
            LocalDateTime now = LocalDateTime.now();
            payment.setPaidAt(now);
            paymentRepository.saveAndFlush(payment);

            Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
            assertThat(updated.getPaidAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Eliminar pago")
    class Delete {

        @Test
        @DisplayName("Debe eliminar un pago por ID")
        void shouldDeleteById() {
            paymentRepository.deleteById(payment.getId());

            assertThat(paymentRepository.findById(payment.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Casos borde")
    class EdgeCases {

        @Test
        @DisplayName("Una orden no puede tener dos pagos (relación OneToOne)")
        void shouldNotAllowTwoPaymentsForSameOrder() {
            Payment duplicate = Payment.builder()
                    .order(order1) // misma orden
                    .stripePaymentIntent("pi_test_duplicado")
                    .amount(new BigDecimal("3000000"))
                    .currency("COP")
                    .status(PaymentStatus.PENDING)
                    .build();

            assertThrows(Exception.class,
                    () -> paymentRepository.saveAndFlush(duplicate));
        }
    }
}

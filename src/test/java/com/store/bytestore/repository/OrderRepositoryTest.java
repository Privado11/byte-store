package com.store.bytestore.repository;

import com.store.bytestore.entities.*;
import com.store.bytestore.entities.Order;
import com.store.bytestore.enums.OrderStatus;
import com.store.bytestore.enums.ProductStatus;
import com.store.bytestore.enums.UserRole;
import com.store.bytestore.reposiroty.CategoryRepository;
import com.store.bytestore.reposiroty.OrderRepository;
import com.store.bytestore.reposiroty.ProductRepository;
import com.store.bytestore.reposiroty.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderRepository — Tests de integración")
class OrderRepositoryTest extends AbstractIntegrationDBTest {

    @Autowired
    OrderRepository orderRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    com.store.bytestore.repository.ProductVariantRepository
            productVariantRepository;

    private User customer;
    private ProductVariant variant;
    private Order order1;
    private Order order2;

    private static final Map<String, Object> ADDRESS_SNAPSHOT = Map.of(
            "fullName",    "Walter Jiménez",
            "phone",       "3001234567",
            "addressLine", "Calle 123 #45-67",
            "city",        "Bogotá",
            "department",  "Cundinamarca",
            "country",     "Colombia"
    );

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        customer = User.builder()
                .firstName("Walter").lastName("Jiménez")
                .email("walter@mail.com").passwordHash("hash")
                .role(UserRole.CUSTOMER).isActive(true)
                .build();
        userRepository.save(customer);


        Category cat = Category.builder()
                .name("Electrónica").slug("electronica").isActive(true).build();
        categoryRepository.save(cat);

        Product product = Product.builder()
                .category(cat).name("Laptop").slug("laptop")
                .price(new BigDecimal("3000000"))
                .status(ProductStatus.ACTIVE)
                .build();
        productRepository.save(product);

        variant = ProductVariant.builder()
                .product(product).sku("LAP-001")
                .attributes(Map.of("color", "Negro"))
                .price(new BigDecimal("3000000")).stock(10)
                .build();
        productVariantRepository.save(variant);


        order1 = Order.builder()
                .user(customer)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("3000000"))
                .total(new BigDecimal("3000000"))
                .shippingAddress(ADDRESS_SNAPSHOT)
                .build();
        orderRepository.save(order1);

        order2 = Order.builder()
                .user(customer)
                .status(OrderStatus.DELIVERED)
                .subtotal(new BigDecimal("1500000"))
                .total(new BigDecimal("1500000"))
                .shippingAddress(ADDRESS_SNAPSHOT)
                .build();
        orderRepository.save(order2);

        orderRepository.flush();
    }

    @Nested
    @DisplayName("Crear orden")
    class Create {

        @Test
        @DisplayName("Debe guardar una orden y generar UUID")
        void shouldSaveOrderWithUUID() {
            Order newOrder = Order.builder()
                    .user(customer)
                    .status(OrderStatus.PENDING)
                    .subtotal(new BigDecimal("500000"))
                    .total(new BigDecimal("500000"))
                    .shippingAddress(ADDRESS_SNAPSHOT)
                    .build();

            Order saved = orderRepository.save(newOrder);

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("Debe asignar status PENDING por defecto")
        void shouldDefaultStatusPending() {
            Order saved = orderRepository.findById(order1.getId()).orElseThrow();

            assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("Debe guardar el snapshot de dirección como JSONB")
        void shouldPersistShippingAddressSnapshot() {
            Order saved = orderRepository.findById(order1.getId()).orElseThrow();

            assertThat(saved.getShippingAddress()).isNotNull();
            assertThat(saved.getShippingAddress().get("city")).isEqualTo("Bogotá");
            assertThat(saved.getShippingAddress().get("fullName")).isEqualTo("Walter Jiménez");
        }

        @Test
        @DisplayName("Debe asignar discountAmount y shippingAmount en cero por defecto")
        void shouldDefaultAmountsToZero() {
            Order saved = orderRepository.findById(order1.getId()).orElseThrow();

            assertThat(saved.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getShippingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Debe guardar timestamps createdAt y updatedAt")
        void shouldPopulateTimestamps() {
            Order saved = orderRepository.saveAndFlush(order1);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Leer órdenes")
    class Read {

        @Test
        @DisplayName("Debe retornar todas las órdenes")
        void shouldReturnAllOrders() {
            List<Order> orders = orderRepository.findAll();

            assertThat(orders).hasSize(2);
        }

        @Test
        @DisplayName("findByUserId debe retornar solo las órdenes del usuario")
        void shouldFindOrdersByUserId() {
            Page<Order> orders = orderRepository.findByUserId(
                    customer.getId(), PageRequest.of(0, 10));

            assertThat(orders.getContent()).hasSize(2);
            assertThat(orders.getContent())
                    .allMatch(o -> o.getUser().getId().equals(customer.getId()));
        }

        @Test
        @DisplayName("findByUserId debe retornar vacío para usuario sin órdenes")
        void shouldReturnEmptyForUserWithNoOrders() {
            User newUser = User.builder()
                    .firstName("Sin").lastName("Ordenes")
                    .email("sinordenes@mail.com").passwordHash("hash")
                    .build();
            userRepository.saveAndFlush(newUser);

            Page<Order> orders = orderRepository.findByUserId(
                    newUser.getId(), PageRequest.of(0, 10));

            assertThat(orders.getContent()).isEmpty();
        }

        @Test
        @DisplayName("findByStatus debe retornar solo órdenes con ese status")
        void shouldFindOrdersByStatus() {
            Page<Order> pending = orderRepository.findByStatus(
                    OrderStatus.PENDING, PageRequest.of(0, 10));

            assertThat(pending.getContent()).hasSize(1);
            assertThat(pending.getContent().get(0).getId()).isEqualTo(order1.getId());
        }

        @Test
        @DisplayName("findByIdWithItems debe traer la orden con sus ítems")
        void shouldFindOrderWithItems() {
            // Agregar un ítem a la orden
            OrderItem item = OrderItem.builder()
                    .order(order1)
                    .productVariant(variant)
                    .productName("Laptop")
                    .variantSku("LAP-001")
                    .variantAttrs(Map.of("color", "Negro"))
                    .unitPrice(new BigDecimal("3000000"))
                    .quantity(1)
                    .subtotal(new BigDecimal("3000000"))
                    .build();
            order1.getItems().add(item);
            orderRepository.saveAndFlush(order1);

            Optional<Order> found = orderRepository.findByIdWithItems(order1.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getItems()).hasSize(1);
            assertThat(found.get().getItems().get(0).getProductName()).isEqualTo("Laptop");
        }
    }

    @Nested
    @DisplayName("Actualizar orden")
    class Update {

        @Test
        @DisplayName("Debe cambiar el status de la orden")
        void shouldUpdateOrderStatus() {
            order1.setStatus(OrderStatus.CONFIRMED);
            orderRepository.saveAndFlush(order1);

            Order updated = orderRepository.findById(order1.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Debe aplicar descuento y recalcular total")
        void shouldApplyDiscount() {
            order1.setDiscountAmount(new BigDecimal("300000"));
            order1.setTotal(new BigDecimal("2700000"));
            orderRepository.saveAndFlush(order1);

            Order updated = orderRepository.findById(order1.getId()).orElseThrow();
            assertThat(updated.getDiscountAmount()).isEqualByComparingTo("300000");
            assertThat(updated.getTotal()).isEqualByComparingTo("2700000");
        }
    }

    @Nested
    @DisplayName("Eliminar orden")
    class Delete {

        @Test
        @DisplayName("Debe eliminar una orden por ID")
        void shouldDeleteById() {
            orderRepository.deleteById(order1.getId());

            assertThat(orderRepository.findById(order1.getId())).isEmpty();
            assertThat(orderRepository.count()).isEqualTo(1);
        }
    }
}

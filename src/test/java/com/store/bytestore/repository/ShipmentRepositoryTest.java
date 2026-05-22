package com.store.bytestore.repository;

import com.store.bytestore.entities.*;
import com.store.bytestore.entities.Order;
import com.store.bytestore.enums.*;
import com.store.bytestore.reposiroty.OrderRepository;
import com.store.bytestore.reposiroty.ShipmentRepository;
import com.store.bytestore.reposiroty.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShipmentRepository — Tests de integración")
class ShipmentRepositoryTest extends AbstractIntegrationDBTest {

    @Autowired
    ShipmentRepository shipmentRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    UserRepository userRepository;

    private Order order1;
    private Order order2;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        shipmentRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();

        User customer = User.builder()
                .firstName("Walter").lastName("Jiménez")
                .email("walter@mail.com").passwordHash("hash")
                .role(UserRole.CUSTOMER).isActive(true)
                .build();
        userRepository.save(customer);

        Map<String, Object> address = Map.of(
                "city", "Bogotá", "country", "Colombia",
                "addressLine", "Calle 123", "fullName", "Walter", "phone", "300"
        );

        order1 = Order.builder()
                .user(customer).status(OrderStatus.PROCESSING)
                .subtotal(new BigDecimal("3000000")).total(new BigDecimal("3000000"))
                .shippingAddress(address).build();
        orderRepository.save(order1);

        order2 = Order.builder()
                .user(customer).status(OrderStatus.PROCESSING)
                .subtotal(new BigDecimal("500000")).total(new BigDecimal("500000"))
                .shippingAddress(address).build();
        orderRepository.save(order2);

        shipment = Shipment.builder()
                .order(order1)
                .carrier("Servientrega")
                .trackingNumber("SRV-123456")
                .trackingUrl("https://servientrega.com/rastreo/SRV-123456")
                .status(ShipmentStatus.SHIPPED)
                .estimatedAt(LocalDate.now().plusDays(3))
                .build();
        shipmentRepository.save(shipment);
        shipmentRepository.flush();
    }

    @Nested
    @DisplayName("Crear envío")
    class Create {

        @Test
        @DisplayName("Debe guardar un envío y generar UUID")
        void shouldSaveShipmentWithUUID() {
            Shipment newShipment = Shipment.builder()
                    .order(order2)
                    .carrier("Coordinadora")
                    .trackingNumber("CRD-789012")
                    .status(ShipmentStatus.PREPARING)
                    .build();

            Shipment saved = shipmentRepository.save(newShipment);

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("Debe asignar status PREPARING por defecto")
        void shouldDefaultStatusPreparing() {
            Shipment newShipment = Shipment.builder()
                    .order(order2)
                    .build();

            Shipment saved = shipmentRepository.save(newShipment);

            assertThat(saved.getStatus()).isEqualTo(ShipmentStatus.PREPARING);
        }

        @Test
        @DisplayName("Debe guardar timestamps createdAt y updatedAt")
        void shouldPopulateTimestamps() {
            Shipment saved = shipmentRepository.saveAndFlush(shipment);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Leer envíos")
    class Read {

        @Test
        @DisplayName("findByOrderId debe retornar el envío de esa orden")
        void shouldFindShipmentByOrderId() {
            Optional<Shipment> found = shipmentRepository.findByOrderId(order1.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getTrackingNumber()).isEqualTo("SRV-123456");
        }

        @Test
        @DisplayName("findByOrderId debe retornar vacío si la orden no tiene envío")
        void shouldReturnEmptyForOrderWithoutShipment() {
            Optional<Shipment> found = shipmentRepository.findByOrderId(order2.getId());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findByTrackingNumber debe retornar el envío correcto")
        void shouldFindByTrackingNumber() {
            Optional<Shipment> found = shipmentRepository.findByTrackingNumber("SRV-123456");

            assertThat(found).isPresent();
            assertThat(found.get().getCarrier()).isEqualTo("Servientrega");
        }

        @Test
        @DisplayName("findByTrackingNumber debe retornar vacío para número inexistente")
        void shouldReturnEmptyForNonExistentTracking() {
            Optional<Shipment> found = shipmentRepository.findByTrackingNumber("NOEXISTE-000");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Actualizar envío — ciclo de vida completo")
    class Update {

        @Test
        @DisplayName("Debe avanzar de SHIPPED a IN_TRANSIT")
        void shouldAdvanceToInTransit() {
            shipment.setStatus(ShipmentStatus.IN_TRANSIT);
            shipmentRepository.saveAndFlush(shipment);

            Shipment updated = shipmentRepository.findById(shipment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        }

        @Test
        @DisplayName("Debe avanzar a DELIVERED y registrar deliveredAt")
        void shouldMarkAsDelivered() {
            LocalDateTime deliveredAt = LocalDateTime.now();
            shipment.setStatus(ShipmentStatus.DELIVERED);
            shipment.setDeliveredAt(deliveredAt);
            shipmentRepository.saveAndFlush(shipment);

            Shipment updated = shipmentRepository.findById(shipment.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
            assertThat(updated.getDeliveredAt()).isNotNull();
        }

        @Test
        @DisplayName("Debe agregar eventos de tracking al envío")
        void shouldAddTrackingEvents() {
            ShipmentEvent event = ShipmentEvent.builder()
                    .shipment(shipment)
                    .status(ShipmentStatus.IN_TRANSIT)
                    .description("Paquete en bodega Bogotá")
                    .location("Bogotá, Colombia")
                    .occurredAt(LocalDateTime.now())
                    .build();
            shipment.getEvents().add(event);
            shipmentRepository.saveAndFlush(shipment);

            Shipment updated = shipmentRepository.findById(shipment.getId()).orElseThrow();
            assertThat(updated.getEvents()).hasSize(1);
            assertThat(updated.getEvents().get(0).getDescription())
                    .isEqualTo("Paquete en bodega Bogotá");
        }

        @Test
        @DisplayName("Debe actualizar la fecha estimada de entrega")
        void shouldUpdateEstimatedDate() {
            LocalDate newEstimate = LocalDate.now().plusDays(5);
            shipment.setEstimatedAt(newEstimate);
            shipmentRepository.saveAndFlush(shipment);

            Shipment updated = shipmentRepository.findById(shipment.getId()).orElseThrow();
            assertThat(updated.getEstimatedAt()).isEqualTo(newEstimate);
        }
    }

    @Nested
    @DisplayName("Eliminar envío")
    class Delete {

        @Test
        @DisplayName("Debe eliminar el envío y sus eventos en cascada")
        void shouldDeleteShipmentWithEventsCascade() {
            ShipmentEvent event = ShipmentEvent.builder()
                    .shipment(shipment)
                    .status(ShipmentStatus.SHIPPED)
                    .description("Salió de bodega")
                    .occurredAt(LocalDateTime.now())
                    .build();
            shipment.getEvents().add(event);
            shipmentRepository.saveAndFlush(shipment);

            shipmentRepository.deleteById(shipment.getId());

            assertThat(shipmentRepository.findById(shipment.getId())).isEmpty();
        }
    }
}

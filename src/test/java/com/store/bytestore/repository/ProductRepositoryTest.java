package com.store.bytestore.repository;

import com.store.bytestore.entities.Category;
import com.store.bytestore.entities.Product;
import com.store.bytestore.entities.ProductVariant;
import com.store.bytestore.enums.ProductStatus;
import com.store.bytestore.reposiroty.CategoryRepository;
import com.store.bytestore.reposiroty.ProductRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ProductRepository — Tests de integración")
class ProductRepositoryTest extends AbstractIntegrationDBTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    private Category category;
    private Product laptop;
    private Product phone;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        category = Category.builder()
                .name("Electrónica")
                .slug("electronica")
                .isActive(true)
                .build();
        categoryRepository.save(category);

        laptop = Product.builder()
                .category(category)
                .name("Laptop Gamer")
                .slug("laptop-gamer")
                .description("Laptop para gaming de alto rendimiento")
                .price(new BigDecimal("3500000.00"))
                .status(ProductStatus.ACTIVE)
                .build();
        productRepository.save(laptop);

        phone = Product.builder()
                .category(category)
                .name("Smartphone Pro")
                .slug("smartphone-pro")
                .description("Teléfono inteligente de última generación")
                .price(new BigDecimal("1800000.00"))
                .status(ProductStatus.ACTIVE)
                .build();
        productRepository.save(phone);

        productRepository.flush();
    }

    @Nested
    @DisplayName("Crear producto")
    class Create {

        @Test
        @DisplayName("Debe guardar un producto y generar UUID")
        void shouldSaveProductWithUUID() {
            Product monitor = Product.builder()
                    .category(category)
                    .name("Monitor 4K")
                    .slug("monitor-4k")
                    .price(new BigDecimal("1200000.00"))
                    .build();

            Product saved = productRepository.save(monitor);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Monitor 4K");
        }

        @Test
        @DisplayName("Debe asignar status DRAFT por defecto")
        void shouldDefaultStatusDraft() {
            Product product = Product.builder()
                    .category(category)
                    .name("Teclado")
                    .slug("teclado")
                    .price(new BigDecimal("150000.00"))
                    .build();

            Product saved = productRepository.save(product);

            assertThat(saved.getStatus()).isEqualTo(ProductStatus.DRAFT);
        }

        @Test
        @DisplayName("Debe asignar reviewCount 0 por defecto")
        void shouldDefaultReviewCountZero() {
            Product product = Product.builder()
                    .category(category)
                    .name("Mouse")
                    .slug("mouse")
                    .price(new BigDecimal("80000.00"))
                    .build();

            Product saved = productRepository.save(product);

            assertThat(saved.getReviewCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Debe guardar timestamps createdAt y updatedAt")
        void shouldPopulateTimestamps() {
            Product saved = productRepository.saveAndFlush(laptop);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Leer productos")
    class Read {

        @Test
        @DisplayName("Debe retornar todos los productos")
        void shouldReturnAllProducts() {
            List<Product> products = productRepository.findAll();

            assertThat(products).hasSize(2);
        }

        @Test
        @DisplayName("findBySlug debe retornar el producto correcto")
        void shouldFindBySlug() {
            Optional<Product> found = productRepository.findBySlug("laptop-gamer");

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Laptop Gamer");
        }

        @Test
        @DisplayName("findBySlug debe retornar vacío si no existe")
        void shouldReturnEmptyForNonExistentSlug() {
            Optional<Product> found = productRepository.findBySlug("no-existe");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findByCategoryIdAndStatus debe retornar solo productos activos de esa categoría")
        void shouldFindByCategoryAndStatus() {
            // Agregar uno en DRAFT para verificar que no aparece
            Product draft = Product.builder()
                    .category(category)
                    .name("Producto Draft")
                    .slug("producto-draft")
                    .price(new BigDecimal("100000.00"))
                    .status(ProductStatus.DRAFT)
                    .build();
            productRepository.saveAndFlush(draft);

            Page<Product> result = productRepository.findByCategoryIdAndStatus(
                    category.getId(),
                    ProductStatus.ACTIVE,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                    .allMatch(p -> p.getStatus() == ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("searchByText debe encontrar productos por nombre")
        void shouldSearchProductsByText() {
            Page<Product> result = productRepository.searchByText(
                    "laptop",
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().get(0).getName()).containsIgnoringCase("Laptop");
        }

        @Test
        @DisplayName("searchByText debe encontrar por descripción")
        void shouldSearchByDescription() {
            Page<Product> result = productRepository.searchByText(
                    "gaming",
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("searchByText no debe retornar productos DRAFT")
        void shouldNotReturnDraftProductsInSearch() {
            Product draft = Product.builder()
                    .category(category)
                    .name("Laptop Draft")
                    .slug("laptop-draft")
                    .price(new BigDecimal("100000.00"))
                    .status(ProductStatus.DRAFT)
                    .build();
            productRepository.saveAndFlush(draft);

            Page<Product> result = productRepository.searchByText(
                    "laptop",
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .noneMatch(p -> p.getStatus() == ProductStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("Actualizar producto")
    class Update {

        @Test
        @DisplayName("Debe actualizar el precio del producto")
        void shouldUpdatePrice() {
            laptop.setPrice(new BigDecimal("3200000.00"));
            productRepository.saveAndFlush(laptop);

            Product updated = productRepository.findById(laptop.getId()).orElseThrow();
            assertThat(updated.getPrice()).isEqualByComparingTo("3200000.00");
        }

        @Test
        @DisplayName("Debe cambiar el status de DRAFT a ACTIVE")
        void shouldActivateProduct() {
            Product draft = Product.builder()
                    .category(category)
                    .name("Nuevo Producto")
                    .slug("nuevo-producto")
                    .price(new BigDecimal("500000.00"))
                    .status(ProductStatus.DRAFT)
                    .build();
            productRepository.saveAndFlush(draft);

            draft.setStatus(ProductStatus.ACTIVE);
            productRepository.saveAndFlush(draft);

            Product updated = productRepository.findById(draft.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("Debe actualizar avgRating y reviewCount")
        void shouldUpdateRatingFields() {
            laptop.setRating(new BigDecimal("4.5"));
            laptop.setReviewCount(10);
            productRepository.saveAndFlush(laptop);

            Product updated = productRepository.findById(laptop.getId()).orElseThrow();
            assertThat(updated.getRating()).isEqualByComparingTo("4.5");
            assertThat(updated.getReviewCount()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Eliminar producto")
    class Delete {

        @Test
        @DisplayName("Debe eliminar un producto por ID")
        void shouldDeleteById() {
            productRepository.deleteById(laptop.getId());

            assertThat(productRepository.findById(laptop.getId())).isEmpty();
        }

        @Test
        @DisplayName("Debe eliminar todos los productos")
        void shouldDeleteAll() {
            productRepository.deleteAll();

            assertThat(productRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Casos borde")
    class EdgeCases {

        @Test
        @DisplayName("Slug debe ser único")
        void shouldNotAllowDuplicateSlug() {
            Product duplicate = Product.builder()
                    .category(category)
                    .name("Otra Laptop")
                    .slug("laptop-gamer") // slug duplicado
                    .price(new BigDecimal("2000000.00"))
                    .build();

            assertThrows(Exception.class,
                    () -> productRepository.saveAndFlush(duplicate));
        }

        @Test
        @DisplayName("Debe soportar paginación correctamente")
        void shouldSupportPagination() {
            Page<Product> firstPage = productRepository.findAll(PageRequest.of(0, 1));

            assertThat(firstPage.getContent()).hasSize(1);
            assertThat(firstPage.getTotalElements()).isEqualTo(2);
            assertThat(firstPage.getTotalPages()).isEqualTo(2);
        }
    }
}

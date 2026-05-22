package com.store.bytestore.repository;

import com.store.bytestore.entities.Category;
import com.store.bytestore.reposiroty.CategoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CategoryRepository — Tests de integración")
class CategoryRepositoryTest extends AbstractIntegrationDBTest {

    @Autowired
    CategoryRepository categoryRepository;

    private Category electronics;
    private Category phones;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();

        electronics = Category.builder()
                .name("Electrónica")
                .slug("electronica")
                .isActive(true)
                .sortOrder(1)
                .build();
        categoryRepository.save(electronics);

        phones = Category.builder()
                .name("Teléfonos")
                .slug("telefonos")
                .parent(electronics)
                .isActive(true)
                .sortOrder(1)
                .build();
        categoryRepository.save(phones);

        categoryRepository.flush();
    }

    @Nested
    @DisplayName("Crear categoría")
    class Create {

        @Test
        @DisplayName("Debe guardar una categoría raíz y generar UUID")
        void shouldSaveRootCategoryWithUUID() {
            Category clothing = Category.builder()
                    .name("Ropa")
                    .slug("ropa")
                    .isActive(true)
                    .build();

            Category saved = categoryRepository.save(clothing);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getParent()).isNull();
        }

        @Test
        @DisplayName("Debe guardar una subcategoría con referencia al padre")
        void shouldSaveSubcategoryWithParent() {
            Category saved = categoryRepository.findById(phones.getId()).orElseThrow();

            assertThat(saved.getParent()).isNotNull();
            assertThat(saved.getParent().getId()).isEqualTo(electronics.getId());
        }

        @Test
        @DisplayName("Debe asignar isActive true por defecto")
        void shouldDefaultIsActiveTrue() {
            Category cat = Category.builder()
                    .name("Hogar")
                    .slug("hogar")
                    .build();

            Category saved = categoryRepository.save(cat);

            assertThat(saved.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Debe asignar sortOrder 0 por defecto")
        void shouldDefaultSortOrderZero() {
            Category cat = Category.builder()
                    .name("Deportes")
                    .slug("deportes")
                    .build();

            Category saved = categoryRepository.save(cat);

            assertThat(saved.getSortOrder()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Leer categorías")
    class Read {

        @Test
        @DisplayName("Debe retornar todas las categorías")
        void shouldReturnAllCategories() {
            List<Category> all = categoryRepository.findAll();

            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("findBySlug debe retornar la categoría correcta")
        void shouldFindBySlug() {
            Optional<Category> found = categoryRepository.findBySlug("electronica");

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Electrónica");
        }

        @Test
        @DisplayName("findBySlug debe retornar vacío si el slug no existe")
        void shouldReturnEmptyForNonExistentSlug() {
            Optional<Category> found = categoryRepository.findBySlug("no-existe");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findByParentIsNullAndIsActiveTrue debe retornar solo categorías raíz activas")
        void shouldReturnOnlyRootActiveCategories() {
            List<Category> roots = categoryRepository.findByParentIsNullAndIsActiveTrue();

            assertThat(roots).hasSize(1);
            assertThat(roots.get(0).getSlug()).isEqualTo("electronica");
        }

        @Test
        @DisplayName("No debe retornar categorías raíz inactivas")
        void shouldNotReturnInactiveRootCategories() {
            Category inactive = Category.builder()
                    .name("Inactiva")
                    .slug("inactiva")
                    .isActive(false)
                    .build();
            categoryRepository.saveAndFlush(inactive);

            List<Category> roots = categoryRepository.findByParentIsNullAndIsActiveTrue();

            assertThat(roots).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Actualizar categoría")
    class Update {

        @Test
        @DisplayName("Debe actualizar el nombre de la categoría")
        void shouldUpdateCategoryName() {
            electronics.setName("Electrónica y Tecnología");
            categoryRepository.saveAndFlush(electronics);

            Category updated = categoryRepository.findById(electronics.getId()).orElseThrow();
            assertThat(updated.getName()).isEqualTo("Electrónica y Tecnología");
        }

        @Test
        @DisplayName("Debe desactivar una categoría")
        void shouldDeactivateCategory() {
            electronics.setIsActive(false);
            categoryRepository.saveAndFlush(electronics);

            Category updated = categoryRepository.findById(electronics.getId()).orElseThrow();
            assertThat(updated.getIsActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Eliminar categoría")
    class Delete {

        @Test
        @DisplayName("Debe eliminar una categoría por ID")
        void shouldDeleteById() {
            categoryRepository.deleteById(electronics.getId());

            assertThat(categoryRepository.findById(electronics.getId())).isEmpty();
        }

        @Test
        @DisplayName("Debe reducir el conteo al eliminar")
        void shouldReduceCountOnDelete() {
            categoryRepository.delete(phones);
            categoryRepository.flush();

            assertThat(categoryRepository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Casos borde")
    class EdgeCases {

        @Test
        @DisplayName("Slug debe ser único")
        void shouldNotAllowDuplicateSlug() {
            Category duplicate = Category.builder()
                    .name("Otra Electrónica")
                    .slug("electronica") // slug duplicado
                    .build();

            assertThrows(Exception.class,
                    () -> categoryRepository.saveAndFlush(duplicate));
        }
    }
}

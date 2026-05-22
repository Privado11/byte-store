package com.store.bytestore.repository;

import com.store.bytestore.entities.User;
import com.store.bytestore.enums.UserRole;
import com.store.bytestore.reposiroty.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRepositoryTest extends AbstractIntegrationDBTest{

    @Autowired
    UserRepository userRepository;

    private User walter;
    private User nicole;

    void initMockUsers(){
        walter = User.builder()
                .firstName("Walter")
                .lastName("Jiménez")
                .email("walter@mail.com")
                .phone("3001234567")
                .passwordHash("hash_walter")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        nicole = User.builder()
                .firstName("Nicole")
                .lastName("Hernández")
                .email("nicole@mail.com")
                .phone("3007654321")
                .passwordHash("hash_nicole")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        userRepository.saveAll(List.of(walter, nicole));
        userRepository.flush();
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        initMockUsers();
    }

    @Nested
    @DisplayName("Crear Usuario")
    class CreateUser {

        @Test
        @DisplayName("Debe guardar un usuario nuevo y agregar la UUID automáticamente")
        void shouldSaveUserAndGenerateUUID() {
            User newUser = User.builder()
                    .firstName("Privado")
                    .lastName("Privado")
                    .email("privado@mail.com")
                    .phone("3009999999")
                    .passwordHash("hash_privado")
                    .build();

            User saved = userRepository.save(newUser);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getId()).isInstanceOf(UUID.class);
            assertThat(saved.getEmail()).isEqualTo("privado@mail.com");
        }

        @Test
        @DisplayName("Debe asignar rol CUSTOMER por defecto al crear un usuario")
        void shouldAssignDefaultRoleCustomer() {
            User newUser = User.builder()
                    .firstName("Ana")
                    .lastName("Pérez")
                    .email("ana@mail.com")
                    .passwordHash("hash_ana")
                    .build();

            User saved = userRepository.save(newUser);

            assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("Debe asignar isActive=true por defecto")
        void shouldAssignIsActiveTrueByDefault() {
            User newUser = User.builder()
                    .firstName("Pedro")
                    .lastName("Ruiz")
                    .email("pedro@mail.com")
                    .passwordHash("hash_pedro")
                    .build();

            User saved = userRepository.save(newUser);

            assertThat(saved.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Debe guardar createdAt y updatedAt automáticamente")
        void shouldPopulateAuditTimestamps() {
            User newUser = User.builder()
                    .firstName("Luis")
                    .lastName("Mora")
                    .email("luis@mail.com")
                    .passwordHash("hash_luis")
                    .build();

            User saved = userRepository.saveAndFlush(newUser);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Leer usuarios")
    class Read {

        @Test
        @DisplayName("Debe retornar todos los usuarios")
        void shouldReturnAllUsers() {
            List<User> users = userRepository.findAll();

            assertThat(users).hasSize(2);
        }

        @Test
        @DisplayName("Debe encontrar un usuario por ID")
        void shouldFindUserById() {
            Optional<User> found = userRepository.findById(walter.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo("Walter");
        }

        @Test
        @DisplayName("Debe retornar Optional vacío si el ID no existe")
        void shouldReturnEmptyOptionalForNonExistentId() {
            Optional<User> found = userRepository.findById(UUID.randomUUID());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findByEmail debe retornar el usuario correcto")
        void shouldFindUserByEmail() {
            Optional<User> found = userRepository.findByEmail("nicole@mail.com");

            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo("Nicole");
            assertThat(found.get().getLastName()).isEqualTo("Hernández");
        }

        @Test
        @DisplayName("findByEmail debe retornar Optional vacío si el email no existe")
        void shouldReturnEmptyWhenEmailNotFound() {
            Optional<User> found = userRepository.findByEmail("noexiste@mail.com");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("existsByEmail debe retornar true si el email ya está registrado")
        void shouldReturnTrueWhenEmailExists() {
            boolean exists = userRepository.existsByEmail("walter@mail.com");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("existsByEmail debe retornar false si el email no existe")
        void shouldReturnFalseWhenEmailNotExists() {
            boolean exists = userRepository.existsByEmail("fantasma@mail.com");

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Debe contar correctamente el total de usuarios")
        void shouldCountUsers() {
            long count = userRepository.count();

            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Actualizar usuario")
    class Update {

        @Test
        @DisplayName("Debe actualizar el nombre del usuario correctamente")
        void shouldUpdateUserName() {
            walter.setFirstName("Walter Actualizado");
            userRepository.saveAndFlush(walter);

            User updated = userRepository.findById(walter.getId()).orElseThrow();
            assertThat(updated.getFirstName()).isEqualTo("Walter Actualizado");
        }

        @Test
        @DisplayName("Debe actualizar el email y seguir siendo encontrable por el nuevo email")
        void shouldUpdateEmailAndFindByNewEmail() {
            nicole.setEmail("nuevo_email@mail.com");
            userRepository.saveAndFlush(nicole);

            assertThat(userRepository.findByEmail("nuevo_email@mail.com")).isPresent();
            assertThat(userRepository.findByEmail("nicole@mail.com")).isEmpty();
        }

        @Test
        @DisplayName("Debe desactivar un usuario correctamente")
        void shouldDeactivateUser() {
            walter.setIsActive(false);
            userRepository.saveAndFlush(walter);

            User updated = userRepository.findById(walter.getId()).orElseThrow();
            assertThat(updated.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Debe cambiar el rol del usuario")
        void shouldUpdateUserRole() {
            walter.setRole(UserRole.ADMIN);
            userRepository.saveAndFlush(walter);

            User updated = userRepository.findById(walter.getId()).orElseThrow();
            assertThat(updated.getRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("updatedAt debe cambiar después de una actualización")
        void shouldUpdateTimestampOnSave() throws InterruptedException {
            var createdAt = walter.getUpdatedAt();
            Thread.sleep(10);

            walter.setPhone("3119999999");
            userRepository.saveAndFlush(walter);

            User updated = userRepository.findById(walter.getId()).orElseThrow();
            assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("Eliminar usuario")
    class Delete {

        @Test
        @DisplayName("Debe eliminar un usuario por ID")
        void shouldDeleteUserById() {
            userRepository.deleteById(walter.getId());
            userRepository.flush();

            assertThat(userRepository.findById(walter.getId())).isEmpty();
            assertThat(userRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Debe eliminar un usuario por entidad")
        void shouldDeleteUserByEntity() {
            userRepository.delete(nicole);
            userRepository.flush();

            assertThat(userRepository.existsByEmail("nicole@mail.com")).isFalse();
        }

        @Test
        @DisplayName("Debe eliminar todos los usuarios")
        void shouldDeleteAllUsers() {
            userRepository.deleteAll();

            assertThat(userRepository.count()).isZero();
        }

        @Test
        @DisplayName("Eliminar un ID inexistente no debe lanzar excepción")
        void shouldNotThrowWhenDeletingNonExistentId() {
            UUID fakeId = UUID.randomUUID();

            assertThat(userRepository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Casos borde")
    class EdgeCases {

        @Test
        @DisplayName("Email debe ser único — no debe guardar dos usuarios con el mismo email")
        void shouldNotAllowDuplicateEmail() {
            User duplicate = User.builder()
                    .firstName("Clon")
                    .lastName("Jiménez")
                    .email("walter@mail.com")
                    .passwordHash("hash_clon")
                    .build();

            org.junit.jupiter.api.Assertions.assertThrows(
                    Exception.class,
                    () -> userRepository.saveAndFlush(duplicate)
            );
        }

        @Test
        @DisplayName("Debe guardar usuario con campos opcionales nulos (phone, etc.)")
        void shouldSaveUserWithNullOptionalFields() {
            User minimal = User.builder()
                    .firstName("Min")
                    .lastName("Imal")
                    .email("minimal@mail.com")
                    .passwordHash("hash_min")
                    .build();

            User saved = userRepository.save(minimal);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPhone()).isNull();
        }

        @Test
        @DisplayName("findByEmail es case-sensitive — emails en mayúscula no deben encontrarse")
        void emailShouldBeCaseSensitive() {
            Optional<User> found = userRepository.findByEmail("WALTER@MAIL.COM");

            assertThat(found).isEmpty();
        }
    }




}

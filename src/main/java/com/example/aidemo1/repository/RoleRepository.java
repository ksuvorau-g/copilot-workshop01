package com.example.aidemo1.repository;

import com.example.aidemo1.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Role entity.
 * Provides CRUD operations and custom queries for role management.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find a role by its name.
     *
     * @param name the role name (e.g., "ROLE_USER", "ROLE_ADMIN")
     * @return Optional containing the role if found
     */
    Optional<Role> findByName(String name);

    /**
     * Check if a role exists by its name.
     *
     * @param name the role name
     * @return true if role exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find a role by name, ignoring case.
     *
     * @param name the role name
     * @return Optional containing the role if found
     */
    Optional<Role> findByNameIgnoreCase(String name);

    /**
     * Delete a role by its name.
     *
     * @param name the role name
     */
    void deleteByName(String name);
}

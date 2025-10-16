package com.example.aidemo1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a role in the system.
 * Roles define permissions for users (e.g., ROLE_USER, ROLE_ADMIN).
 */
@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name cannot exceed 50 characters")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

}

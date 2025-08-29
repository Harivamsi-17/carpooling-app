package com.carpool.carpoolingapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: Generates getters, setters, toString(), etc.
@NoArgsConstructor // Lombok: Generates a no-argument constructor
@AllArgsConstructor // Lombok: Generates a constructor with all arguments
@Entity // JPA: Marks this class as a database entity
@Table(name = "users") // JPA: Specifies the table name in the database
public class User {

    @Id // JPA: Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // JPA: Configures auto-increment for the ID
    private Long id;

    @Column(nullable = false, unique = true) // JPA: Defines a column, cannot be null, must be unique
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING) // JPA: Store the enum as a string ("DRIVER") not a number (0)
    @Column(nullable = false)
    private UserRole role;

}
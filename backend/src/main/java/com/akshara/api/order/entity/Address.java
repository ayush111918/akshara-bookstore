package com.akshara.api.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "addresses",
        indexes = {
                @Index(
                        name = "idx_addresses_postal_code",
                        columnList = "postal_code"
                )
        }
)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "recipient_name",
            nullable = false,
            length = 150,
            updatable = false
    )
    private String recipientName;

    @Column(
            nullable = false,
            length = 20,
            updatable = false
    )
    private String phone;

    @Column(
            name = "address_line_1",
            nullable = false,
            length = 255,
            updatable = false
    )
    private String addressLine1;

    @Column(
            name = "address_line_2",
            length = 255,
            updatable = false
    )
    private String addressLine2;

    @Column(
            nullable = false,
            length = 100,
            updatable = false
    )
    private String city;

    @Column(
            nullable = false,
            length = 100,
            updatable = false
    )
    private String state;

    @Column(
            name = "postal_code",
            nullable = false,
            length = 20,
            updatable = false
    )
    private String postalCode;

    @Column(
            nullable = false,
            length = 100,
            updatable = false
    )
    private String country;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected Address() {
        // Required by JPA
    }

    public Address(
            String recipientName,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country
    ) {
        this.recipientName = recipientName;
        this.phone = phone;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    @PrePersist
    void beforeInsert() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
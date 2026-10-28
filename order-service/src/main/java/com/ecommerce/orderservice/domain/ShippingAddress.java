package com.ecommerce.orderservice.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class ShippingAddress {

    private String fullName;
    private String phone;
    private String street;
    private String city;
    private String county;
    private String postalCode;

    protected ShippingAddress() {
    }

    public ShippingAddress(String fullName, String phone, String street, String city, String county, String postalCode) {
        this.fullName = fullName;
        this.phone = phone;
        this.street = street;
        this.city = city;
        this.county = county;
        this.postalCode = postalCode;
    }

    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getCounty() { return county; }
    public String getPostalCode() { return postalCode; }
}

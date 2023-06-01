/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "identification")
public class Identification implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "id_type")
    private String idType;
    @Column(name = "id_number")
    private String idNumber;
    @Column(name = "expiryDate")
    private String expiryDate;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "middle_name")
    private String middleName;
    @Column(name = "photo", length = 50000)
    private String photo;
    @Column(name = "gender")
    private String gender;
    @Column(name = "issued_at")
    private String issuedAt;
    @Column(name = "issued_date")
    private String issuedDate;
    @Column(name = "dob")
    private String dob;
    @Column(name = "state_issuance")
    private String stateOfIssuance;
    @Column(name = "title")
    private String title;
    @Column(name = "nok_address")
    private String nokAddress;
    @Column(name = "nok_first_name")
    private String nokFirstName;
    @Column(name = "nok_last_name")
    private String nokLastName;
    @Column(name = "status")
    private String status;
    @Column(name = "validation_source")
    private String validationSource;
    @Column(name = "request_id")
    private String requestId;
    @ManyToOne
    private Customer customer;
    @ManyToOne
    private AppUser appUser;
    @Column(name = "failure_reason")
    private String failureReason;
}

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
@Table(name = "customer_account_restriction")
public class CustomerAccountRestrictions implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @ManyToOne
    private Customer customer;
    @Column(name = "request_id")
    private String requestId;
    @ManyToOne
    private AppUser appUser;
    @Column(name = "crud_type")
    private String crudType;
    @Column(name = "customer_or_account")
    private String customerOrAccount;
    @Column(name = "account")
    private boolean account;
    @Column(name = "posting_restriction", length = 1000)
    private String postingRestriction;
    @Column(name = "status")
    private String status;
}

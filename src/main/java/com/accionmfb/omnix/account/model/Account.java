/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.model;

import java.io.Serializable;
import java.math.BigDecimal;
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
@Table(name = "account")
public class Account implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "old_account_number")
    private String oldAccountNumber;
    @Column(name = "category")
    private String category;
    @ManyToOne
    private Product product;
    @ManyToOne
    private Branch branch;
    @ManyToOne
    private Customer customer;
    @Column(name = "status")
    private String status;
    @Column(name = "opened_with_bvn")
    private boolean openedWithBVN;
    @Column(name = "wallet")
    private boolean wallet;
    @Column(name = "account_balance")
    private BigDecimal accountBalance = BigDecimal.ZERO;
    @Column(name = "request_id")
    private String requestId;
    @Column(name = "otp")
    private String otp;
    @Column(name = "primary_account")
    private boolean primaryAccount = false;
    @ManyToOne
    private AppUser appUser;
    @Column(name = "time_period")
    private char timePeriod;
}

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
 * @author dofoleta
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sms")
@SuppressWarnings("PersistenceUnitPresent")
public class SMS implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "message", length = 5000)
    private String message = "";
    @Column(name = "sms_for")
    private String smsFor;
    @Column(name = "status")
    private String status;
    @ManyToOne
    private Account account;
    @ManyToOne
    private AppUser appUser;
    @Column(name = "failure_reason")
    private String failureReason = "";
    @Column(name = "request_id")
    private String requestId;
    @Column(name = "time_period")
    private char timePeriod;
    @Column(name = "sms_type")
    private char smsType;
}

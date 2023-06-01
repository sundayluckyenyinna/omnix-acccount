/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.model;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "customer_number")
    private String customerNumber;
    @Column(name = "title")
    private String title;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "other_name")
    private String otherName;
    @Column(name = "mnemonic")
    private String mnemonic;
    @Column(name = "dob")
    private LocalDate dob = LocalDate.parse("1900-01-01");
    @Column(name = "marital_status")
    private String maritalStatus;
    @Column(name = "gender")
    private String gender;
    @Column(name = "email")
    private String email = "";
    @ManyToOne
    private Branch branch;
    @ManyToOne
    private BVN bvn = null;
    @Column(name = "boarded")
    private boolean boarded = false;
    @ManyToOne
    private Identification nin;
    @ManyToOne
    private Identification driversLicense;
    @ManyToOne
    private Identification passport;
    @ManyToOne
    private Identification pvc;
    @Column(name = "other_id_number")
    private String otherIdNumber = "";
    @Column(name = "other_id_type")
    private String otherIdType = "";
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "telco")
    private String telco = "";
    @Column(name = "customer_type")
    private String customerType;
    @Column(name = "pin")
    private String pin = "";
    @Column(name = "password")
    private String password = "";
    @Column(name = "otp")
    private String otp = "";
    @Column(name = "otp_expire")
    private LocalDateTime otpExpiry;
    @Column(name = "security_question")
    private String securityQuestion = "";
    @Column(name = "security_answer")
    private String securityAnswer = "";
    @Column(name = "kyc_tier")
    private String kycTier;
    @Column(name = "unboard_at")
    private LocalDateTime unboardAt = LocalDateTime.parse("1900-01-01T00:00:00");
    @Column(name = "boarded_at")
    private LocalDateTime boardedAt = LocalDateTime.parse("1900-01-01T00:00:00");
    @Column(name = "deposit_limit")
    private BigDecimal depositLimit = BigDecimal.ZERO;
    @Column(name = "withdrawal_limit")
    private BigDecimal withdrawalLimit = BigDecimal.ZERO;
    @Column(name = "balance_limit")
    private BigDecimal balanceLimit = BigDecimal.ZERO;
    @Column(name = "daily_limit")
    private BigDecimal dailyLimit = BigDecimal.ZERO;
    @Column(name = "optout_date")
    private LocalDateTime optoutDate = LocalDateTime.parse("1900-01-01T00:00:00");
    @Column(name = "failure_reason")
    private String failureReason = "";
    @Column(name = "reason_for_status")
    private String reasonForStatus;
    @Column(name = "residence_state")
    private String residenceState;
    @Column(name = "residence_city")
    private String residenceCity;
    @Column(name = "residence_address")
    private String residenceAddress;
    @Column(name = "status")
    private String status = "Pending";
    @Column(name = "request_id")
    private String requestId;
    @ManyToOne
    private AppUser appUser;
    @Column(name = "education_level")
    private String educationLevel;
    @Column(name = "account_officer_code")
    private String accountOfficerCode;
    @Column(name = "other_officer_code")
    private String otherOfficerCode;
    @Column(name = "sector")
    private String sector;
    @Column(name = "time_period")
    private char timePeriod;
    @Column(name = "finger_print", length = 10000)
    private String fingerPrint;
    @Column(name = "ibanking_finger_print", length = 10000)
    private String iBankingFingerPrint;
    @Column(name = "referal_code")
    private String referalCode;
    @Column(name = "ussd_pin")
    private String ussdPin = "";
    @Column(name = "ussd_password")
    private String ussdPassword = "";
    @Column(name = "ussd_security_question")
    private String ussdSecurityQuestion = "";
    @Column(name = "ussd_security_answer")
    private String ussdSecurityAnswer = "";
    @Column(name = "ibanking_pin")
    private String iBankingPin = "";
    @Column(name = "ibanking_password")
    private String iBankingPassword = "";
    @Column(name = "ibanking_security_question")
    private String iBankingSecurityQuestion = "";
    @Column(name = "ibanking_security_answer")
    private String iBankingSecurityAnswer = "";
    @Column(name = "imei")
    private String imei = "";
}

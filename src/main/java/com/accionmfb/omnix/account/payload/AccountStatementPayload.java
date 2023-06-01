/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.payload;

import java.math.BigDecimal;
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
public class AccountStatementPayload {

    private String accountNumber;
    private String accountType;
    private String accountName;
    private String currency;
    private String openingBalance;
    private String branchName;
    private String printDate;
    private String customerAddress;
    private int id;
    private String valueDate;
    private String paymentDesc;
    private String referenceNo;
    private String postDate;
    private String credits;
    private String debits;
    private String balance;
    private String transDate;
    private String remarks;
    private String voucherNumber;
    private int totalCreditCount;
    private int totalDebitCount;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private String closingBalance;

    @Override
    public String toString() {
        return "AccountStatementPayload{" +
                "accountNumber='" + accountNumber + '\'' +
                ", accountType='" + accountType + '\'' +
                ", accountName='" + accountName + '\'' +
                ", currency='" + currency + '\'' +
                ", openingBalance='" + openingBalance + '\'' +
                ", branchName='" + branchName + '\'' +
                ", printDate='" + printDate + '\'' +
                ", customerAddress='" + customerAddress + '\'' +
                ", id=" + id +
                ", valueDate='" + valueDate + '\'' +
                ", paymentDesc='" + paymentDesc + '\'' +
                ", referenceNo='" + referenceNo + '\'' +
                ", postDate='" + postDate + '\'' +
                ", credits='" + credits + '\'' +
                ", debits='" + debits + '\'' +
                ", balance='" + balance + '\'' +
                ", transDate='" + transDate + '\'' +
                ", remarks='" + remarks + '\'' +
                ", voucherNumber='" + voucherNumber + '\'' +
                ", totalCreditCount=" + totalCreditCount +
                ", totalDebitCount=" + totalDebitCount +
                ", totalDebit=" + totalDebit +
                ", totalCredit=" + totalCredit +
                ", closingBalance='" + closingBalance + '\'' +
                '}';
    }
}

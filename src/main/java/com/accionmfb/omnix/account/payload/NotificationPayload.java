/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.payload;

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
public class NotificationPayload {

    private String amount;
    private String accountNumber;
    private String branch;
    private String transDate;
    private String transTime;
    private String narration;
    private String accountBalance;
    private String mobileNumber;
    private String requestId;
    private String token;
    private String accountType;
    private String lastName;
    private String otherName;
    private String email;
    private String emailType;
    private String startDate;
    private String endDate;
    private String emailSubject;
    private char smsType;
    private String smsFor;
}

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
 * @author dofoleta
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SMSResponsePayload {

    private String responseCode;
    private String responseDescription;
    private String mobileNumber;
    private String message;
    private String smsFor;
}

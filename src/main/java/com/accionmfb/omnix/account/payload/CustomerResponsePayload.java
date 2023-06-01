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
public class CustomerResponsePayload {

    private String responseCode;
    private String customerNumber;
    private String lastName;
    private String otherName;
    private String mnemonic;
    private String maritalStatus;
    private String gender;
    private String dob;
    private String branchCode;
    private String branchName;
    private String mobileNumber;
    private String stateOfResidence;
    private String cityOfResidence;
    private String residentialAddress;
    private String kyc;
}

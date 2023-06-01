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
public class NibssMerchantCreateResponse {

    private String returnCode;
    private String institutionNumber;
    private String mch_no;
    private String merchantName;
    private String merchantTIN;
    private String merchantAddress;
    private String merchantContactName;
    private String merchantPhoneNumber;
    private String merchantEmail;

}

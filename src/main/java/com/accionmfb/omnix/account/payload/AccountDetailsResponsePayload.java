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
public class AccountDetailsResponsePayload {

    private String accountName;
    private String accountNumber;
    private boolean wallet;
    private String productCode;
    private String productName;
    private String category;
    private String branch;
    private String branchCode;
    private boolean openedWithBVN;
    private String customerNumber;
    private String responseCode;
    private String mobileNumber;
    private String status;
}

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
public class OFSVersionPayload {

    private String t24Version;
    private char inputType;
    private String processingType;
    private int gtsControl;
    private int noOfAuth;
    private String userCredentials;
    private String branchCode;
    private String parameterList;
    private String transId;
    private String token;
    private String app;
}

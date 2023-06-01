/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.payload;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
public class VersionPayload {

    @NotBlank(message = "T24 Application is required")
    private String t24Application;
    @NotNull(message = "T24 Version is required")
    private String t24Version;
    @NotBlank(message = "T24 Input type is required")
    @Pattern(regexp = "^(I|A|R|V|H|S)$", message = "Input Type must be either I, A, R or V")
    private String inputType;
    @NotBlank(message = "Processing type is required")
    @Pattern(regexp = "^(PROCESS|VALIDATE)$", message = "Processing Type must be either PROCESS or VALIDATE")
    private String processingType;
    @NotBlank(message = "GTS Control is required")
    @Pattern(regexp = "^(1|2|3|4|5)$", message = "GTS Control must be between 1-5")
    private String gtsControl;
    @NotBlank(message = "Number of Authorizers is required")
    @Pattern(regexp = "[0-2]", message = "Number of Authorizers must be between 0-2")
    private String noOfAuth;
    @NotBlank(message = "Username is required")
    private String username;
    @NotBlank(message = "Password is required")
    private String password;
    @NotBlank(message = "Branch code is required")
    private String branchCode;
    @NotEmpty(message = "Parameter list is required")
    @Valid
    private List<VersionValuePair> parameterList;
    @NotNull(message = "T24 Transaction Id is required but could be blank")
    private String transId;
    @NotNull(message = "T24 Message Id is required but could be blank")
    private String messageId;
    @NotBlank(message = "App type is required")
    private String app;    
}

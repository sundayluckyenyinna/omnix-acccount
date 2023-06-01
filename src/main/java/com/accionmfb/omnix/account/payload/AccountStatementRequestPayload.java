/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.payload;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class AccountStatementRequestPayload {

    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotBlank(message = "NUBAN account number is required")
    @Pattern(regexp = "[0-9]{10}", message = "NUBAN account number must be 10 digit")
    @Schema(name = "Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String accountNumber;
    @NotNull(message = "Start date cannot be null")
    @NotEmpty(message = "Start date cannot be empty")
    @NotBlank(message = "Start date cannot be blank")
    @Pattern(regexp = "^\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])$", message = "Start date must be like 2021-01-13")
    private String startDate;
    @NotNull(message = "End date cannot be null")
    @NotEmpty(message = "End date cannot be empty")
    @NotBlank(message = "End date cannot be blank")
    @Pattern(regexp = "^\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])$", message = "End date must be like 2021-01-13")
    private String endDate;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;

    private String email;
    private String accountType;
}

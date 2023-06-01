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
public class AccountOpeningRequestPayload {

    @NotNull(message = "Moile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotNull(message = "Customer number cannot be null")
    @NotEmpty(message = "Customer number cannot be empty")
    @NotBlank(message = "Customer number cannot be blank")
    @Pattern(regexp = "[0-9]{1,}", message = "Valid customer number (CIF) required")
    private String customerNumber;
    @NotNull(message = "Branch code cannot be null")
    @NotEmpty(message = "Branch code cannot be empty")
    @NotBlank(message = "Branch code cannot be blank")
    @Pattern(regexp = "^[A-Za-z]{2}[0-9]{7}$", message = "Branch code like BB0010000 required")
    private String branchCode;
    @NotNull(message = "Account officer cannot be null")
    @NotEmpty(message = "Account officer cannot be empty")
    @NotBlank(message = "Account officer cannot be blank")
    @Pattern(regexp = "[0-9]{4}", message = "4 digit account officer required")
    private String accountOfficer;
    @NotNull(message = "Other officer cannot be null")
    @NotEmpty(message = "Other officer cannot be empty")
    @NotBlank(message = "Other officer cannot be blank")
    @Pattern(regexp = "[0-9]{4}", message = "4 digit other officer required")
    private String otherOfficer;
    @NotNull(message = "Product code cannot be null")
    @NotEmpty(message = "Product code cannot be empty")
    @NotBlank(message = "Product code cannot be blank")
    @Pattern(regexp = "[0-9]{4,5}", message = "4 or 5 digit product code required")
    private String productCode;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    private String bvn;
}

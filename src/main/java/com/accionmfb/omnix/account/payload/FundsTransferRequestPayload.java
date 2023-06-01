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
public class FundsTransferRequestPayload {

    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotNull(message = "Branch code cannot be null")
    @NotEmpty(message = "Branch code cannot be empty")
    @NotBlank(message = "Branch code cannot be blank")
    @Pattern(regexp = "^[A-Za-z]{2}[0-9]{7}$", message = "Branch code like BB0010000 required")
    private String branchCode;
    @NotBlank(message = "Debit account is required")
    @Pattern(regexp = "[0-9]{10}", message = "Debit account must be either 10 digit account number")
    @Schema(name = "Debit Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String debitAccount;
    @NotBlank(message = "Credit account is required")
    @Pattern(regexp = "[0-9]{10}", message = "Credit account must be either 10 digit account number")
    @Schema(name = "Credit Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String creditAccount;
    @NotBlank(message = "Transaction narration is required")
    @Schema(name = "Transaction Narration", example = "Cash Withdrawal IFO Brian Okon", description = "Transaction Narration")
    private String narration;
    @NotBlank(message = "Transaction amount is required")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Transaction Amount must contain only digits, comma or dot only")
    @Schema(name = "Transaction Amount", example = "1,000.00", description = "Transaction Amount")
    private String amount;
    @NotNull(message = "Transaction type cannot be null")
    @NotEmpty(message = "Transaction type cannot be empty")
    @NotBlank(message = "Transaction type cannot be blank")
    @Schema(name = "Transaction type", example = "ACTF", description = "Transaction Type")
    private String transType;
    @Schema(name = "AIS inputter", example = "BOKON", description = "AIS authorizer")
    private String aisInputter;
    @Schema(name = "AIS authorizer", example = "BOKON", description = "AIS authorizer")
    private String aisAuthorizer;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "PYLON67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotBlank(message = "Request ID is required")
    @Schema(name = "Request ID", example = "PYLON67XXTY78999GHTRE", description = "Request ID is required")
    private String requestId;
    @NotNull(message = "Token cannot be null")
    @NotEmpty(message = "Token cannot be empty")
    @NotBlank(message = "Token cannot be blank")
    private String token;
}

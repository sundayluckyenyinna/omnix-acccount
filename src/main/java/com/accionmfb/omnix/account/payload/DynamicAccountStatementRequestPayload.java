package com.accionmfb.omnix.account.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class DynamicAccountStatementRequestPayload
{
    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotBlank(message = "NUBAN account number is required")
    @Pattern(regexp = "[0-9]{10}", message = "NUBAN account number must be 10 digit")
    @Schema(name = "Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String accountNumber;
    private String startDate;
    private String endDate;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    private String accountType;
    private String imei;
    private String pin;
}

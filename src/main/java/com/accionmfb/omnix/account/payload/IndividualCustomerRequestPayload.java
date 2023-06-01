/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
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
public class IndividualCustomerRequestPayload {

    @NotNull(message = "Last name cannot be null")
    @NotEmpty(message = "Last name cannot be empty")
    @NotBlank(message = "Last name cannot be blank")
    private String lastName;
    @NotNull(message = "Other names cannot be null")
    @NotEmpty(message = "Other names cannot be empty")
    @NotBlank(message = "Other names cannot be blank")
    private String otherName;
    @NotNull(message = "Date of birth cannot be null")
    @NotEmpty(message = "Date of birth cannot be empty")
    @NotBlank(message = "Date of birth cannot be blank")
    @Pattern(regexp = "^\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])$", message = "Date of birth must be like 2021-01-13")
    private String dob;
    @NotNull(message = "Gender cannot be null")
    @NotEmpty(message = "Gender cannot be empty")
    @NotBlank(message = "Gender cannot be blank")
    @Pattern(regexp = "^(Female|Male)$", message = "Value must be either Female or Male")
    private String gender;
    @NotNull(message = "Marital status cannot be null")
    @NotEmpty(message = "Marital status cannot be empty")
    @NotBlank(message = "Marital status cannot be blank")
    @Pattern(regexp = "^(Single|Married|Divorced)$", message = "Value must be either Single, Married or Divorced")
    private String maritalStatus;
    @NotNull(message = "Branch code cannot be null")
    @NotEmpty(message = "Branch code cannot be empty")
    @NotBlank(message = "Branch code cannot be blank")
    private String branchCode;
    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotNull(message = "State of residence cannot be null")
    @NotEmpty(message = "State of residence cannot be empty")
    @NotBlank(message = "State of residence cannot be blank")
    private String stateOfResidence;
    @NotNull(message = "City of residence cannot be null")
    @NotEmpty(message = "City of residence cannot be empty")
    @NotBlank(message = "City of residence cannot be blank")
    private String cityOfResidence;
    @NotNull(message = "Residential address cannot be null")
    @NotEmpty(message = "Residential address cannot be empty")
    @NotBlank(message = "Residential address cannot be blank")
    private String residentialAddress;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @Email
    private String email;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
}

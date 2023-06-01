/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.payload;

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
public class VersionValuePair {

    @NotNull(message = "T24 Field name is required")
    @Pattern(regexp = "^([A-Za-z]{1,}?:[1-9]?:[1-9])$", message = "T24 Field name must be like CUSTOMER:1:1")
    private String fieldName;
    @NotNull(message = "T24 Field value is required")
    private String fieldValue;
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.accionmfb.omnix.account.payload;

import lombok.Data;

/**
 *
 * @author dakinkuolie
 */
@Data
public class ValidationPayload {
    
  private boolean error;
    private String response;
    private String plainTextPayload;
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.service;

import static com.accionmfb.omnix.account.constant.ApiPaths.LOCAL_TRANSFER;
import static com.accionmfb.omnix.account.constant.ApiPaths.LOCAL_TRANSFER_WITH_PL_INTERNAL;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author bokon
 */
@FeignClient(name = "omnix-fundstransfer", url = "${zuul.routes.fundstransferService.url}")
public interface FundsTransferService {

    @PostMapping(value = LOCAL_TRANSFER, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String localTransfer(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = LOCAL_TRANSFER_WITH_PL_INTERNAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String localTransferInternalAccount(@RequestHeader("Authorization") String bearerToken, String requestPayload);
}

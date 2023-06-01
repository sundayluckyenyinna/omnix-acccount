/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.service;

import static com.accionmfb.omnix.account.constant.ApiPaths.SMS_NOTIFICATION;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author bokon
 */
@FeignClient(name = "omnix-notification", url = "${zuul.routes.notificationService.url}")
public interface NotificationService {

    @PostMapping(value = SMS_NOTIFICATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String smsNotification(@RequestHeader("Authorization") String bearerToken, String requestPayload);
}

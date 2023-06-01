/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.accionmfb.omnix.account.repository;

import com.accionmfb.omnix.account.model.Account;
import com.accionmfb.omnix.account.model.AppUser;
import com.accionmfb.omnix.account.model.Customer;
import com.accionmfb.omnix.account.model.SMS;
import com.accionmfb.omnix.account.model.UserActivity;

/**
 *
 * @author dofoleta
 */
public interface SmsRepository {
     UserActivity createUserActivity(UserActivity userActivity);

    SMS createSMS(SMS sms);

    SMS updateSMS(SMS sms);

    AppUser getAppUserUsingUsername(String username);

    Customer getCustomerUsingMobileNumber(String mobileNumber);

    Account getCustomerAccount(Customer customer, String accountNumber);
}

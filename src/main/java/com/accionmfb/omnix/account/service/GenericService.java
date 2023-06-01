/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.service;

import com.accionmfb.omnix.account.model.Branch;
import com.accionmfb.omnix.account.payload.AccountStatementPayload;
import com.accionmfb.omnix.account.payload.FundsTransferRequestPayload;
import com.accionmfb.omnix.account.payload.LocalTransferWithInternalPayload;
import com.accionmfb.omnix.account.payload.NotificationPayload;
import com.accionmfb.omnix.account.payload.SMSRequestPayload;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author bokon
 */
public interface GenericService {

    public String postToSMSService(String requestEndPoint, String requestBody);

    String getBranchCode(String accountNumber);

    void generateLog(String app, String token, String logMessage, String logType, String logLevel, String requestId);

    void createUserActivity(String accountNumber, String activity, String amount, String channel, String message, String mobileNumber, char status);

    String postToT24(String requestBody);

    String encryptString(String textToEncrypt, String token);

    String decryptString(String textToDecrypt, String encryptionKey);

    String validateT24Response(String responseString);

    String getT24TransIdFromResponse(String response);

    String getTextFromOFSResponse(String ofsResponse, String textToExtract);

    String formatDateWithHyphen(String dateToFormat);

    String generateMnemonic(int max);

    String hashLocalTransferValidationRequest(FundsTransferRequestPayload requestPayload);

    String hashLocalTransferWithInternalValidationRequest(LocalTransferWithInternalPayload requestPayload);

    String hashSMSNotificationRequest(SMSRequestPayload requestPayload);

    char getTimePeriod();

    Branch getBranchUsingBranchCode(String branchCode);

    CompletableFuture<String> sendAccountOpeningSMS(NotificationPayload requestPayload);

    CompletableFuture<String> sendAccountStatementMail(NotificationPayload requestPayload, List<AccountStatementPayload> statementPayload);

    String formatOfsUserCredentials(String ofs, String userCredentials);

    String postToMiddleware(String path, String OFS);

    public CompletableFuture<String> sendSMS(NotificationPayload requestPayload);

    public String getDateStringFromUnformattedDate(String unFormattedDate);
}

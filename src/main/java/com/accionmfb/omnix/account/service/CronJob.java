package com.accionmfb.omnix.account.service;

import com.accionmfb.omnix.account.constant.ResponseCodes;
import com.accionmfb.omnix.account.jwt.JwtTokenUtil;
import com.accionmfb.omnix.account.model.Account;
import com.accionmfb.omnix.account.model.SMS;
import com.accionmfb.omnix.account.payload.AccountBalanceResponsePayload;
import com.accionmfb.omnix.account.payload.SMSPayload;
import com.accionmfb.omnix.account.payload.SMSResponsePayload;
import com.accionmfb.omnix.account.repository.AccountRepository;
import com.accionmfb.omnix.account.repository.SmsRepository;
import static com.accionmfb.omnix.account.service.AccountServiceImpl.getMessageTokens;
import com.google.gson.Gson;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *
 * @author dofoleta
 */

@Slf4j
//@Service
public class CronJob {

    @Value("${omnix.version.account}")
    private String accountVersion;
    @Value("${omnix.version.numbering.code}")
    private String accountNumberingCodeVersion;

    @Autowired
    JwtTokenUtil jwtToken;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    SmsRepository smsRepository;

    @Autowired
    MessageSource messageSource;

    @Autowired
    Gson gson;
    @Autowired
    Environment env;
    @Autowired
    GenericService genericService;


    @Scheduled(fixedDelay = 10000, initialDelay = 1000)
    public void finalizeAccountOpeningRequest() {
        List<Account> accounts = accountRepository.getAccountUsingStatus("STAGE1");
        if (accounts != null && !accounts.isEmpty()) {
            for (Account account : accounts) {
                String branchCode;
                try {
                    branchCode = account.getCustomer().getBranch().getBranchCode().trim();
                } catch (Exception e) {
                    branchCode = "NG0010068";
                }
                if (account.getCustomer().getCustomerNumber() != null || !account.getCustomer().getCustomerNumber().isEmpty()) {
                    String userCredentials = env.getProperty("omnix.channel.user.".concat(account.getAppUser().getChannel().toLowerCase()));
                    if (userCredentials == null) {
                        userCredentials = env.getProperty("omnix.channel.user.ussd");
                    }
                    String accountNumber = "";

                    //Generate the account numbering
                    StringBuilder ofsBase = new StringBuilder();
                    ofsBase.append("CUSTOMER.NO::=").append(account.getCustomer().getCustomerNumber()).append(",");
                    ofsBase.append("PDT.CODE::=").append(account.getProduct().getProductCode()).append(",");
                    ofsBase.append("CREATED.Y.N::=N");

                    String ofsRequest = accountNumberingCodeVersion + "," + userCredentials + "/" + branchCode + ",," + ofsBase;
                    String response = genericService.postToT24(ofsRequest);

                    if (response.contains("//1")) {
                        accountNumber = genericService.getTextFromOFSResponse(response, "ACCT.CODE:1:1");
                        //Check for Null
                        if (accountNumber.matches("[0-9]{10}")) {

                            account.setStatus("Intermediate");
                            account.setAccountNumber(accountNumber);
                            account.setOldAccountNumber(accountNumber);
                            accountRepository.updateAccount(account);
                        }
                    } else {
                        try {
                            String acct = response;
                            acct = acct.split("exist in another branch")[0].trim();
                            acct = acct.split(" ")[1].trim();
                            AccountBalanceResponsePayload oAcct = getAccountDetails(branchCode, userCredentials, acct);
                            if (oAcct != null) {
                                branchCode = oAcct.getBranchCode();
                                ofsBase = new StringBuilder();
                                ofsBase.append("CUSTOMER.NO::=").append(account.getCustomer().getCustomerNumber()).append(",");
                                ofsBase.append("PDT.CODE::=").append(account.getProduct().getProductCode()).append(",");
                                ofsBase.append("CREATED.Y.N::=N");

                                ofsRequest = accountNumberingCodeVersion + "," + userCredentials + "/" + branchCode + ",," + ofsBase;
                                response = genericService.postToT24(ofsRequest);

                                if (response.contains("//1")) {
                                    accountNumber = genericService.getTextFromOFSResponse(response, "ACCT.CODE:1:1");
                                    //Check for Null
                                    if (accountNumber.matches("[0-9]{10}")) {

                                        account.setStatus("Intermediate");
                                        account.setAccountNumber(accountNumber);
                                        account.setOldAccountNumber(accountNumber);
                                        accountRepository.updateAccount(account);
                                    }
                                } else {
                                    continue;
                                }
                            } else {
                                continue;
                            }
                        } catch (Exception e) {
                        }

                    }

                    //Update the account record status
                    String AO ="7801";
                    if (account.getCustomer().getAccountOfficerCode() == null || account.getCustomer().getAccountOfficerCode().isEmpty()) {
                        AO = "7801";
                    } else {
                        AO = account.getCustomer().getAccountOfficerCode();
                    }
                    ofsBase = new StringBuilder("");
                    ofsBase.append("CUSTOMER:1:1::=").append(account.getCustomer().getCustomerNumber()).append(",");
                    ofsBase.append("CURRENCY:1:1::=").append("NGN").append(",");
                    ofsBase.append("MNEMONIC:1:1::=").append(genericService.generateMnemonic(6)).append(",");
                    ofsBase.append("ACCOUNT.OFFICER:1:1::=").append(AO).append(",");
                    String OAO;
                    if (account.getCustomer().getOtherOfficerCode() == null || account.getCustomer().getOtherOfficerCode().isEmpty()) {
                        OAO = "9998";
                    } else {
                        OAO = account.getCustomer().getOtherOfficerCode();
                    }
                    ofsBase.append("OTHER.OFFICER:1:1::=").append(OAO).append(",");
//                    ofsBase.append("CATEGORY:1:1::=").append(account.getCategory()); // NOIMPUT FIELD

                    ofsRequest = accountVersion.trim() + "," + userCredentials + "/" + branchCode + "," + accountNumber + "," + ofsBase;
                    response = genericService.postToT24(ofsRequest);
                    if (response.contains("//1")) {
                        String nubanAccountNumber = genericService.getTextFromOFSResponse(response, "ALT.ACCT.ID:4:1");
                        account.setAccountNumber(nubanAccountNumber);
                        account.setStatus("SUCCESS");
                        accountRepository.updateAccount(account);
                        sendSMS(account);
                    } else {
                        if (response.contains("DUPLICATE")) {
                            account.setAccountNumber("");
                            account.setStatus("DUPLICATE");
                            accountRepository.updateAccount(account);
                        }
                    }
                }
            }
        }
    }

    private String sendSMS(Account account) {
        //Log the request

        StringBuilder smsMessage = new StringBuilder();
        smsMessage.append("Thanks for opening a ");
        smsMessage.append(account.getProduct().getProductName()).append(" account at our ");
        smsMessage.append(account.getBranch().getBranchName()).append(" branch. Your account number is ");
        smsMessage.append(account.getAccountNumber()).append(". Your future is Bright. #StaySafe");

        SMS newSMS = new SMS();
        newSMS.setAppUser(account.getAppUser());
        newSMS.setCreatedAt(LocalDateTime.now());
        newSMS.setFailureReason("");
        newSMS.setMessage(smsMessage.toString());
        newSMS.setMobileNumber(account.getCustomer().getMobileNumber());
        newSMS.setRequestId(account.getRequestId());
        newSMS.setSmsFor("New Account");
        newSMS.setSmsType('N');
        newSMS.setStatus("PENDING");
        newSMS.setTimePeriod(genericService.getTimePeriod());
        newSMS.setAccount(account);

        SMS createSMS = smsRepository.createSMS(newSMS);

        //Generate the SMS payload
        SMSPayload oSMSPayload = new SMSPayload();
        oSMSPayload.setMessage(newSMS.getMessage());
        oSMSPayload.setMobileNumber(newSMS.getMobileNumber());
        String ofsRequest = gson.toJson(oSMSPayload);

        String middlewareResponse = genericService.postToSMSService("/sms/send", ofsRequest);

        System.out.println("Middleware response: " + middlewareResponse);
        log.info("Middleware response: {}", middlewareResponse);
        SMSResponsePayload responsePayload = gson.fromJson(middlewareResponse, SMSResponsePayload.class);
        if ("00".equals(responsePayload.getResponseCode())) {
            createSMS.setStatus("SUCCESS");
            smsRepository.updateSMS(createSMS);

            SMSResponsePayload smsResponse = new SMSResponsePayload();
            smsResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            smsResponse.setMobileNumber(newSMS.getMobileNumber());
            smsResponse.setMessage(newSMS.getMessage());
            smsResponse.setSmsFor(newSMS.getSmsFor());
            return gson.toJson(smsResponse);
        }

        createSMS.setStatus("FAILED");
        createSMS.setFailureReason(responsePayload.getResponseDescription());
        smsRepository.updateSMS(createSMS);
        //Create activity log
        genericService.generateLog("SMS", "", "Success", "API Response", "INFO", newSMS.getMobileNumber());

        return "Failure";
    }

    private AccountBalanceResponsePayload getAccountDetails(String branchCode, String userCredentials, String accountNumber) {
        StringBuilder ofsBase = new StringBuilder("");
        ofsBase.append("ENQUIRY.SELECT,,")
                .append(userCredentials)
                .append("/")
                .append(branchCode)
                .append(",ACCION.ACCOUNT.DETAIL,ACCOUNT.NUMBER:EQ=")
                .append(accountNumber);

        String response = genericService.postToT24(ofsBase.toString());
        return parseAccountDetails(response, accountNumber);
    }

    private AccountBalanceResponsePayload parseAccountDetails(String sResponse, String accountNumber) {
        sResponse = sResponse.replaceFirst(",\"", "^");
        String[] items = getMessageTokens(sResponse, "^");
        ArrayList list = new ArrayList();
        int count = 0;
        String testParam = "";
        for (int k = 0; k < items.length; k++) {
            testParam = items[0];
            if (k > 0) {
                list.add(items[k]);
                count++;
            }
        }

        if (testParam.contains("SECURITY VIOLATION DURING SIGN ON PROCESS")) {

            AccountBalanceResponsePayload accountResponse = new AccountBalanceResponsePayload();
            accountResponse.setAccountNumber(accountNumber);
            accountResponse.setAvailableBalance("0.00".replace("\"", "").trim());
            accountResponse.setLedgerBalance("0.00".replace("\"", "").trim());
            accountResponse.setResponseCode(ResponseCodes.FORMAT_EXCEPTION.getResponseCode());
            return accountResponse;
        }
        if (testParam.contains("No records were found that matched the selection criteria")) {
            AccountBalanceResponsePayload accountResponse = new AccountBalanceResponsePayload();
            accountResponse.setAccountNumber(accountNumber);
            accountResponse.setAvailableBalance("0.00".replace("\"", "").trim());
            accountResponse.setLedgerBalance("0.00".replace("\"", "").trim());
            accountResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            return accountResponse;
        }
        for (int k = 0; k < count; k++) {
            DecimalFormat formatter = new DecimalFormat("###.##");
            AccountBalanceResponsePayload accountResponse = new AccountBalanceResponsePayload();
            String val = ((String) list.get(k)).replaceAll("\"", "");
            items = val.split("\t");
            accountResponse.setAccountNumber(accountNumber);

            accountResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());

            try {
                accountResponse.setBranchCode(items[13].trim());
            } catch (Exception ex) {
            }

            return accountResponse;
        }
        return null;
    }
}

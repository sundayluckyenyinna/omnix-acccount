/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.service;

import com.accionmfb.omnix.account.constant.ResponseCodes;
import com.accionmfb.omnix.account.jwt.JwtTokenUtil;
import com.accionmfb.omnix.account.model.Account;
import com.accionmfb.omnix.account.model.AccountOfficer;
import com.accionmfb.omnix.account.model.AppUser;
import com.accionmfb.omnix.account.model.Branch;
import com.accionmfb.omnix.account.model.Customer;
import com.accionmfb.omnix.account.model.CustomerAccountRestrictions;
import com.accionmfb.omnix.account.model.PostingRestriction;
import com.accionmfb.omnix.account.model.Product;
import com.accionmfb.omnix.account.model.SMS;
import com.accionmfb.omnix.account.payload.AccountBalancePayload;
import com.accionmfb.omnix.account.payload.AccountBalanceResponsePayload;
import com.accionmfb.omnix.account.payload.AccountBalancesResponsePayload;
import com.accionmfb.omnix.account.payload.AccountDetailsResponsePayload;
import com.accionmfb.omnix.account.payload.AccountListResponsePayload;
import com.accionmfb.omnix.account.payload.AccountNumberPayload;
import com.accionmfb.omnix.account.payload.AccountOfficerPayload;
import com.accionmfb.omnix.account.payload.AccountOpeningRequestPayload;
import com.accionmfb.omnix.account.payload.AccountStatementPayload;
import com.accionmfb.omnix.account.payload.AccountStatementRequestPayload;
import com.accionmfb.omnix.account.payload.AccountStatementResponsePayload;
import com.accionmfb.omnix.account.payload.AccountandMobileRequestPayload;
import com.accionmfb.omnix.account.payload.BranchAccountOfficerResponsePayload;
import com.accionmfb.omnix.account.payload.BranchListResponsePayload;
import com.accionmfb.omnix.account.payload.BranchPayload;
import com.accionmfb.omnix.account.payload.ChargeTypes;
import com.accionmfb.omnix.account.payload.DynamicAccountStatementRequestPayload;
import com.accionmfb.omnix.account.payload.DynamicQuery;
import com.accionmfb.omnix.account.payload.FundsTransferRequestPayload;
import com.accionmfb.omnix.account.payload.LocalTransferWithInternalPayload;
import com.accionmfb.omnix.account.payload.MobileNumberPayload;
import com.accionmfb.omnix.account.payload.NotificationPayload;
import com.accionmfb.omnix.account.payload.OmniResponsePayload;
import com.accionmfb.omnix.account.payload.PostingRestrictionPayload;
import com.accionmfb.omnix.account.payload.PostingRestrictionResponsePayload;
import com.accionmfb.omnix.account.payload.ProductPayload;
import com.accionmfb.omnix.account.payload.ProductResponsePayload;
import com.accionmfb.omnix.account.payload.RestrictionRequestPayload;
import com.accionmfb.omnix.account.payload.SMSPayload;
import com.accionmfb.omnix.account.payload.SMSResponsePayload;
import com.accionmfb.omnix.account.repository.AccountRepository;
import com.accionmfb.omnix.account.repository.SmsRepository;
import com.google.gson.Gson;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    MessageSource messageSource;
    @Autowired
    GenericService genericService;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    SmsRepository smsRepository;

    @Value("${omnix.version.account}")
    private String accountVersion;
    @Value("${omnix.expense.accountopen.bonus}")
    private String accountOpenExpenseAccount;
    @Value("${omnix.enquiry.account.statement}")
    private String accountStatementEnquiry;
    @Value("${omnix.income.pl.account.statement}")
    private String accountStatementIncomeAccount;
    @Value("${omnix.charges.account.statement}")
    private String accountStatementCharge;
    @Value("${omnix.digital.branch.code}")
    private String digitalBranchCode;
    @Value("${omnix.version.numbering.code}")
    private String accountNumberingCodeVersion;
    @Value("${account.statement.stamp}")
    private String stamp;

    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    FundsTransferService ftService;
    @Autowired
    Gson gson;
    Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);
    @Autowired
    ApplicationContext applicationContext;
    private final BCryptPasswordEncoder bCryptEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean validateAccountBalancePayload(String token, AccountNumberPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "accountBalanceFallback")
    public String getAccountBalance(String token, AccountNumberPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Account Balance", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Account account = accountRepository.getAccountUsingAccountNumber(requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Balance", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Balance", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Account Balance", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Balance", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }
            String branchCode = account.getBranch().getBranchCode(); 
            StringBuilder ofsBase = new StringBuilder("");
            ofsBase.append("ENQUIRY.SELECT,,")
                    .append(userCredentials)
                    .append("/")
                    .append(branchCode)
                    .append(",ACCION.ACCOUNT.DETAIL,ACCOUNT.NUMBER:EQ=")
                    .append(requestPayload.getAccountNumber());

            String newOfsRequest = genericService.formatOfsUserCredentials(ofsBase.toString(), userCredentials);
            //Generate the OFS Request log
            genericService.generateLog("Account Balance", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            response = genericService.postToT24(ofsBase.toString());
            //Generate the OFS Response log
            genericService.generateLog("Account Balance", token, response, "OFS Response", "INFO", requestPayload.getRequestId());
            String validationResponse = genericService.validateT24Response(response);
            if (validationResponse != null) {
                errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
                errorResponse.setResponseMessage(validationResponse);
                //Log the error
                genericService.generateLog("Account Balance", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Account Balance", "", channel, validationResponse, requestPayload.getAccountNumber(), 'F');
                return gson.toJson(errorResponse);
            }

            //Log the error
            genericService.generateLog("Account Balance", token, response, "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getAccountNumber(), "Account Balance", "", channel, "Success", requestPayload.getAccountNumber(), 'S');
            AccountBalanceResponsePayload accountResponse = parseAccountDetails(response, requestPayload);
            DecimalFormat df = new DecimalFormat("###,###,###.00");
            double value = Double.valueOf(accountResponse.getAvailableBalance());
            accountResponse.setAvailableBalance(df.format(value));
            value = Double.valueOf(accountResponse.getLedgerBalance());
            accountResponse.setLedgerBalance(df.format(value));

            return gson.toJson(accountResponse);
        } catch (NoSuchMessageException ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Account Balance", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getAccountNumber(), "Account Balance", "", channel, ex.getMessage(), requestPayload.getAccountNumber(), 'F');
            return response;
        }
    }

    @SuppressWarnings("unused")
    public String accountBalanceFallback(String token, AccountNumberPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.account", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateAccountDetailsPayload(String token, AccountNumberPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "accountDetailsFallback")
    public String getAccountDetails(String token, AccountNumberPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response;
        genericService.generateLog("Account Details", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Account account = accountRepository.getAccountUsingAccountNumber(requestPayload.getAccountNumber());
            if (account == null) {
                
                // check in core banking
                
                
                
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Details", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Details", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Account Details", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Details", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            AccountDetailsResponsePayload responsePayload = new AccountDetailsResponsePayload();
            responsePayload.setAccountName(account.getCustomer().getLastName() + " " + account.getCustomer().getOtherName());
            responsePayload.setAccountNumber(account.getAccountNumber());
            responsePayload.setBranch(account.getBranch().getBranchName());
            responsePayload.setBranchCode(account.getBranch().getBranchCode());
            responsePayload.setCategory(account.getCategory());
            responsePayload.setCustomerNumber(account.getCustomer().getCustomerNumber());
            responsePayload.setOpenedWithBVN(account.isOpenedWithBVN());
            responsePayload.setProductCode(account.getProduct().getProductCode());
            responsePayload.setProductName(account.getProduct().getProductName());
            responsePayload.setWallet(account.isWallet());
            responsePayload.setMobileNumber(account.getCustomer().getMobileNumber());
            responsePayload.setStatus(account.getStatus());
            responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            String responseJson = gson.toJson(responsePayload);
            //Log the error
            genericService.generateLog("Account Details", token, responseJson, "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getAccountNumber(), "Account Details", "", channel, "Success", requestPayload.getAccountNumber(), 'S');
            return responseJson;
        } catch (NoSuchMessageException ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Account Details", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getAccountNumber(), "Account Details", "", channel, ex.getMessage(), requestPayload.getAccountNumber(), 'F');
            return response;
        }
    }

    @SuppressWarnings("unused")
    public String accountDetailsFallback(String token, AccountNumberPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.account", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    @HystrixCommand(fallbackMethod = "getProductFallback")
    public String getProducts(String token) {
        OmniResponsePayload responsePayload = new OmniResponsePayload();
        try {
            List<Product> products = accountRepository.getProducts();
            if (products == null) {
                responsePayload.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                responsePayload.setResponseMessage(messageSource.getMessage("appMessages.record.product.empty", new Object[0], Locale.ENGLISH));
                String response = gson.toJson(responsePayload);

                //Log the error
                genericService.generateLog("Product", token, messageSource.getMessage("appMessages.record.product.empty", new Object[0], Locale.ENGLISH), "API Error", "DEBUG", "");
                return response;
            }

            ProductResponsePayload response = new ProductResponsePayload();
            response.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            response.setProducts(products);
            return gson.toJson(response);
        } catch (NoSuchMessageException ex) {
            responsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setResponseMessage(ex.getMessage());
            String response = gson.toJson(responsePayload);

            //Log the error
            genericService.generateLog("Product", token, ex.getMessage(), "API Error", "DEBUG", "");
            return response;
        }
    }

    @SuppressWarnings("unused")
    public String getProductFallback(String token) {
        return messageSource.getMessage("appMessages.fallback.account", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateAccountOpeningRequestPayload(String token, AccountOpeningRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getCustomerNumber());
        rawString.add(requestPayload.getBranchCode());
        rawString.add(requestPayload.getProductCode());
        rawString.add(requestPayload.getAccountOfficer());
        rawString.add(requestPayload.getOtherOfficer());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
//    @HystrixCommand(fallbackMethod = "accountOpeningFallback")
    public String processAccountOpening(String token, AccountOpeningRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response;
        genericService.generateLog("Account Opening", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Account Opening", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Opening", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            Customer oCustomer = accountRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (oCustomer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Opening", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Opening", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            Product product = accountRepository.getProductUsingProductCode(requestPayload.getProductCode().trim());
            if (product == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.record.product.noexist", new Object[]{requestPayload.getProductCode()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Opening", token, messageSource.getMessage("appMessages.record.product.noexist", new Object[]{requestPayload.getProductCode()}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(oCustomer.getCustomerNumber(), "Account Opening", "", channel, messageSource.getMessage("appMessages.record.product.noexist", new Object[]{requestPayload.getProductCode()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            // set values and destroy customer object to free some memory
            Account account = accountRepository.getAccountUsingProduct(oCustomer, product);
            String sCustomerNumber = oCustomer.getCustomerNumber();
            String firstName = oCustomer.getLastName();
            String otherName = null;
            try {
                otherName = oCustomer.getOtherName().replace("null", "");
            } catch (Exception e) {
                otherName = "";
            }
            String branchName = oCustomer.getBranch().getBranchName();
            String branchCode = oCustomer.getBranch().getBranchCode();
            String bvn = requestPayload.getBvn();

            Account newAccount = new Account();
            newAccount.setCustomer(oCustomer);

            // destroy customer object
            oCustomer = null;

            //Check the customer number
            if (!sCustomerNumber.equalsIgnoreCase(requestPayload.getCustomerNumber())) {
                errorResponse.setResponseCode(ResponseCodes.CUSTOMER_NUMBER_MOBILE_MISMATCH.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.mobile.mismatch", new Object[]{requestPayload.getCustomerNumber(), sCustomerNumber}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Opening", token, messageSource.getMessage("appMessages.customer.mobile.mismatch", new Object[]{requestPayload.getCustomerNumber(), sCustomerNumber}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(sCustomerNumber, "Account Opening", "", channel, messageSource.getMessage("appMessages.customer.mobile.mismatch", new Object[]{requestPayload.getCustomerNumber(), sCustomerNumber}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if the branch is valid
            Branch branch = genericService.getBranchUsingBranchCode(requestPayload.getBranchCode());
            if (branch == null) {
                //Log the error
                genericService.generateLog("Account Opening", token, messageSource.getMessage("appMessages.branch.noexist", new Object[]{requestPayload.getBranchCode()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Opening", "", channel, messageSource.getMessage("appMessages.branch.noexist", new Object[0], Locale.ENGLISH), requestPayload.getBranchCode(), 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.branch.noexist", new Object[]{requestPayload.getBranchCode()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);
                return response;
            }

            //Check if the customer has account with the same product
//            Account account = accountRepository.getAccountUsingProduct(customer, product);
            if (account == null) {
                //Persist the record in the DB --- NG0010068 is the branch code for Digital Branch  
//                Account newAccount = new Account();
                newAccount.setAppUser(appUser);
                newAccount.setBranch(branch);
                newAccount.setCategory(product.getCategoryCode());
                newAccount.setCreatedAt(LocalDateTime.now());
//                newAccount.setCustomer(customer);

                newAccount.setProduct(product);
                newAccount.setStatus("Pending");
                newAccount.setWallet(false);
                newAccount.setRequestId(requestPayload.getRequestId());
                newAccount.setTimePeriod(genericService.getTimePeriod());
                if (requestPayload.getBvn() != null && !requestPayload.getBvn().isEmpty()) {
                    newAccount.setOpenedWithBVN(true);
                } else {
                    newAccount.setOpenedWithBVN(false);
                }

                Account createdAccount = accountRepository.createAccount(newAccount);

                //Generate the account numbering
                OmniResponsePayload returnedResponse = generateAccountNumbering(sCustomerNumber,
                        userCredentials, branch.getBranchCode(),
                        product.getProductCode(), token, requestPayload.getRequestId());

//                OmniResponsePayload returnedResponse = gson.fromJson(accountNumberResponse, OmniResponsePayload.class);
                if (returnedResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.FAILED_TRANSACTION.getResponseCode())) {
                    errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.numbering.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH));
                    response = gson.toJson(errorResponse);

                    //Log the error
                    genericService.generateLog("Account Opening", token, returnedResponse.getResponseMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(sCustomerNumber, "Account Opening", "", channel, returnedResponse.getResponseMessage(), requestPayload.getMobileNumber(), 'F');
                    return response;
                }

                //Update the account record status
                String accountNumber = returnedResponse.getResponseMessage();
                createdAccount.setStatus("Intermediate");
                createdAccount.setAccountNumber(accountNumber);
                createdAccount.setOldAccountNumber(accountNumber);
                accountRepository.updateAccount(createdAccount);

                StringBuilder ofsBase = new StringBuilder("");
                ofsBase.append("CUSTOMER:1:1::=").append(sCustomerNumber).append(",");
                ofsBase.append("CURRENCY:1:1::=").append("NGN").append(",");
                ofsBase.append("MNEMONIC:1:1::=").append(genericService.generateMnemonic(6)).append(",");
                ofsBase.append("ACCOUNT.OFFICER:1:1::=").append(requestPayload.getAccountOfficer()).append(",");
                ofsBase.append("OTHER.OFFICER:1:1::=").append(requestPayload.getOtherOfficer()).append(",");
//                ofsBase.append("CATEGORY:1:1::=").append(product.getCategoryCode());

                String ofsRequest = accountVersion.trim() + "," + userCredentials
                        + "/" + branch.getBranchCode().trim() + "," + accountNumber + "," + ofsBase;
                String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                //Generate the OFS Response log
                genericService.generateLog("Account Opening", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                response = genericService.postToT24(ofsRequest);
                //Generate the OFS Response log
                genericService.generateLog("Account Opening", token, response, "OFS Response", "INFO", requestPayload.getRequestId());
                if (!response.contains("//1")) {
                    errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.opening.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH));
                    response = gson.toJson(response);

                    //Log the error
                    genericService.generateLog("Account Opening", token, messageSource.getMessage("appMessages.account.opening.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(sCustomerNumber, "Account Opening", "", channel, messageSource.getMessage("appMessages.account.opening.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                    return response;
                }

                //Update the account record
                String nubanAccountNumber = genericService.getTextFromOFSResponse(response, "ALT.ACCT.ID:4:1");
                createdAccount.setAccountNumber(nubanAccountNumber);
                createdAccount.setStatus("SUCCESS");
                Account updatedAccount = accountRepository.updateAccount(createdAccount);

                //Check if the vendor is to be paid bonus
                if (appUser.isPayAccountOpenBonus()) {
                    //Check if the account number is provided
                    if (appUser.getAccountNumber() != null && !appUser.getAccountNumber().equalsIgnoreCase("")) {
                        FundsTransferRequestPayload localFT = new FundsTransferRequestPayload();
                        localFT.setAmount(appUser.getAccountOpenBonus());
                        localFT.setCreditAccount(appUser.getAccountNumber());
                        localFT.setDebitAccount(accountOpenExpenseAccount);
                        localFT.setMobileNumber(updatedAccount.getCustomer().getMobileNumber());
                        localFT.setNarration("BONUS FOR OPENING ACCOUNT " + createdAccount.getAccountNumber());
                        localFT.setTransType("ACTF");
                        localFT.setAisAuthorizer("");
                        localFT.setAisInputter("");
                        localFT.setRequestId(requestPayload.getRequestId());
                        localFT.setToken(token);
                        localFT.setHash(genericService.hashLocalTransferValidationRequest(localFT));
                        requestJson = gson.toJson(localFT);

                        //Call the funds transfer microservices
                        ftService.localTransfer(token, requestJson);
                    }
                }

                //Generate the response
                AccountDetailsResponsePayload accountResponse = new AccountDetailsResponsePayload();
                accountResponse.setAccountName(firstName + " " + otherName);
                accountResponse.setAccountNumber(nubanAccountNumber);
                accountResponse.setBranch(branchName);
                accountResponse.setBranchCode(branchCode);
                accountResponse.setCategory(product.getCategoryCode());
                accountResponse.setCustomerNumber(sCustomerNumber);
                if (bvn != null && !bvn.isEmpty()) {
                    accountResponse.setOpenedWithBVN(true);
                } else {
                    accountResponse.setOpenedWithBVN(false);
                }
                accountResponse.setProductCode(product.getProductCode());
                accountResponse.setProductName(product.getProductName());
                accountResponse.setWallet(false);
                accountResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response = gson.toJson(accountResponse);

                NotificationPayload smsPayload = new NotificationPayload();
                smsPayload.setAccountNumber(nubanAccountNumber);
                smsPayload.setAccountType(product.getProductName());
                smsPayload.setBranch(branchName);
                smsPayload.setMobileNumber(requestPayload.getMobileNumber());
//                smsPayload.setAccountNumber(accountNumber);
//                smsPayload.setMessage(smsMessage.toString());
                smsPayload.setSmsFor("New Account");
                smsPayload.setSmsType('C');
                smsPayload.setRequestId(requestPayload.getRequestId());
                smsPayload.setToken(token);

                genericService.sendSMS(smsPayload);

//                prepareAndSendSMS(nubanAccountNumber, product.getProductName(), branch.getBranchName(), requestPayload.getMobileNumber(), requestPayload.getRequestId(), token, appUser);
                //Log the error
                genericService.generateLog("Account Opening", token, response, "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(accountNumber, "Account Opening", "", channel, "Success", requestPayload.getMobileNumber(), 'S');
                return response;
            }

            if ("Intermediate".equalsIgnoreCase(account.getStatus())) {
                StringBuilder ofsBase = new StringBuilder("");
                ofsBase.append("CUSTOMER:1:1::=").append(sCustomerNumber).append(",");
                ofsBase.append("CURRENCY:1:1::=").append("NGN").append(",");
                ofsBase.append("MNEMONIC:1:1::=").append(genericService.generateMnemonic(6)).append(",");
                ofsBase.append("ACCOUNT.OFFICER:1:1::=").append(requestPayload.getAccountOfficer()).append(",");
                ofsBase.append("OTHER.OFFICER:1:1::=").append(requestPayload.getOtherOfficer()).append(",");
//                ofsBase.append("MNEMONIC:1:1::=").append(genericService.generateMnemonic(6)).append(",");
//                ofsBase.append("ACCOUNT.OFFICER:1:1::=").append(requestPayload.getAccountOfficer()).append(",");
//                ofsBase.append("OTHER.OFFICER:1:1::=").append(requestPayload.getOtherOfficer()).append(",");
//                ofsBase.append("CATEGORY:1:1::=").append(product.getCategoryCode());

                String ofsRequest = accountVersion.trim() + "," + userCredentials
                        + "/" + branch.getBranchCode().trim() + "," + account.getAccountNumber() + "," + ofsBase;
                String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                //Generate the OFS Request log
                genericService.generateLog("Account Opening", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                response = genericService.postToT24(ofsRequest);
                //Generate the OFS Response log
                genericService.generateLog("Account Opening", token, response, "OFS Response", "INFO", requestPayload.getRequestId());
                if (!response.contains("//1")) {
                    errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.opening.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH));
                    response = gson.toJson(response);

                    //Log the error
                    genericService.generateLog("Account Opening", token, messageSource.getMessage("appMessages.account.opening.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(sCustomerNumber, "Account Opening", "", channel, messageSource.getMessage("appMessages.account.opening.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                    return response;
                }

                //Update the account record
                account.setStatus("SUCCESS");
                Account updatedAccount = accountRepository.updateAccount(account);

                //Check if the vendor is to be paid bonus
                if (appUser.isPayAccountOpenBonus()) {
                    //Check if the account number is provided
                    if (appUser.getAccountNumber() != null && !appUser.getAccountNumber().equalsIgnoreCase("")) {
                        FundsTransferRequestPayload localFT = new FundsTransferRequestPayload();
                        localFT.setAmount(appUser.getAccountOpenBonus());
                        localFT.setCreditAccount(appUser.getAccountNumber());
                        localFT.setDebitAccount(accountOpenExpenseAccount);
                        localFT.setMobileNumber(updatedAccount.getCustomer().getMobileNumber());
                        localFT.setNarration("BONUS FOR OPENING ACCOUNT " + account.getAccountNumber());
                        localFT.setTransType("ACTF");
                        localFT.setAisAuthorizer("");
                        localFT.setAisInputter("");
                        localFT.setRequestId(requestPayload.getRequestId());
                        localFT.setToken(token);
                        localFT.setHash(genericService.hashLocalTransferValidationRequest(localFT));
                        requestJson = gson.toJson(localFT);

                        //Call the funds transfer microservices
                        ftService.localTransfer(token, requestJson);
                    }
                }

                //Generate the response
                AccountDetailsResponsePayload accountResponse = new AccountDetailsResponsePayload();
                accountResponse.setAccountName(firstName + " " + otherName);
                accountResponse.setAccountNumber(account.getAccountNumber());
                accountResponse.setBranch(branchName);
                accountResponse.setBranchCode(branchCode);
                accountResponse.setCategory(product.getCategoryCode());
                accountResponse.setCustomerNumber(sCustomerNumber);
                if (bvn != null && !bvn.isEmpty()) {
                    accountResponse.setOpenedWithBVN(true);
                } else {
                    accountResponse.setOpenedWithBVN(false);
                }
                accountResponse.setProductCode(product.getProductCode());
                accountResponse.setProductName(product.getProductName());
                accountResponse.setWallet(false);
                accountResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response = gson.toJson(accountResponse);

                //Start a thread to send the account opening SMS
                NotificationPayload smsPayload = new NotificationPayload();
                smsPayload.setAccountNumber(account.getAccountNumber());
                smsPayload.setAccountType(product.getProductName());
                smsPayload.setBranch(branchName);
                smsPayload.setMobileNumber(requestPayload.getMobileNumber());
//                smsPayload.setMessage(smsMessage.toString());
                smsPayload.setSmsFor("New Account");
                smsPayload.setSmsType('C');
                smsPayload.setRequestId(requestPayload.getRequestId());
                smsPayload.setToken(token);

                genericService.sendSMS(smsPayload);

                //prepareAndSendSMS(account.getAccountNumber(), product.getProductName(), branch.getBranchName(), requestPayload.getMobileNumber(), requestPayload.getRequestId(), token, appUser);
                //Log 
                genericService.generateLog("Account Opening", token, response, "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(account.getAccountNumber(), "Account Opening", "", channel, "Success", requestPayload.getMobileNumber(), 'S');
                return response;
            }

            if ("Pending".equalsIgnoreCase(account.getStatus())) {
                //Generate the account numbering
                OmniResponsePayload returnedResponse = generateAccountNumbering(sCustomerNumber, userCredentials,
                        branch.getBranchCode(), product.getProductCode(), token, requestPayload.getRequestId());
//                OmniResponsePayload returnedResponse = gson.fromJson(accountNumberResponse, OmniResponsePayload.class);

                if (returnedResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.FAILED_TRANSACTION.getResponseCode())) {
                    errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.numbering.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH));
                    response = gson.toJson(errorResponse);

                    //Log the error
                    genericService.generateLog("Account Opening", token, returnedResponse.getResponseMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(sCustomerNumber, "Account Opening", "", channel, returnedResponse.getResponseMessage(), requestPayload.getMobileNumber(), 'F');
                    return response;
                }

                //Update the account information
                String accountNumber = returnedResponse.getResponseMessage();
                account.setAccountNumber(accountNumber);
                account.setOldAccountNumber(accountNumber);
                account.setStatus("Intermediate");
                accountRepository.updateAccount(account);

                StringBuilder ofsBase = new StringBuilder("");
                ofsBase.append("CUSTOMER:1:1::=").append(sCustomerNumber).append(",");
                ofsBase.append("CURRENCY:1:1::=").append("NGN").append(",");
                ofsBase.append("MNEMONIC:1:1::=").append(genericService.generateMnemonic(6)).append(",");
                ofsBase.append("ACCOUNT.OFFICER:1:1::=").append(requestPayload.getAccountOfficer()).append(",");
                ofsBase.append("OTHER.OFFICER:1:1::=").append(requestPayload.getOtherOfficer()).append(",");
//                ofsBase.append("CATEGORY:1:1::=").append(product.getCategoryCode());

                String ofsRequest = accountVersion.trim() + "," + userCredentials
                        + "/" + branch.getBranchCode().trim() + "," + account.getAccountNumber() + "," + ofsBase;
                String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                //Generate the OFS Response log
                genericService.generateLog("Account Opening", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                response = genericService.postToT24(ofsRequest);
                //Generate the OFS Response log
                genericService.generateLog("Account Opening", token, response, "OFS Response", "INFO", requestPayload.getRequestId());
                if (!response.contains("//1")) {
                    errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.opening.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH));
                    String responseJson = gson.toJson(response);

                    //Log the error
                    genericService.generateLog("Account Opening", token, messageSource.getMessage("appMessages.account.opening.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(sCustomerNumber, "Account Opening", "", channel, messageSource.getMessage("appMessages.account.opening.failed", new Object[]{sCustomerNumber}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                    return responseJson;
                }

                //Update the account record
                String nubanAccountNumber = genericService.getTextFromOFSResponse(response, "ALT.ACCT.ID:4:1");
                account.setAccountNumber(nubanAccountNumber);
                account.setStatus("SUCCESS");
                Account updatedAccount = accountRepository.updateAccount(account);

                //Check if the vendor is to be paid bonus
                if (appUser.isPayAccountOpenBonus()) {
                    //Check if the account number is provided
                    if (appUser.getAccountNumber() != null && !appUser.getAccountNumber().equalsIgnoreCase("")) {
                        FundsTransferRequestPayload localFT = new FundsTransferRequestPayload();
                        localFT.setAmount(appUser.getAccountOpenBonus());
                        localFT.setCreditAccount(appUser.getAccountNumber());
                        localFT.setDebitAccount(accountOpenExpenseAccount);
                        localFT.setMobileNumber(updatedAccount.getCustomer().getMobileNumber());
                        localFT.setNarration("BONUS FOR OPENING ACCOUNT " + account.getAccountNumber());
                        localFT.setTransType("ACTF");
                        localFT.setAisAuthorizer("");
                        localFT.setAisInputter("");
                        localFT.setRequestId(requestPayload.getRequestId());
                        localFT.setToken(token);
                        localFT.setHash(genericService.hashLocalTransferValidationRequest(localFT));
                        requestJson = gson.toJson(localFT);

                        //Call the funds transfer microservices
                        ftService.localTransfer(token, requestJson);
                    }
                }

                //Generate the response
                AccountDetailsResponsePayload accountResponse = new AccountDetailsResponsePayload();
                accountResponse.setAccountName(firstName + " " + otherName);
                accountResponse.setAccountNumber(nubanAccountNumber);
                accountResponse.setBranch(branchName);
                accountResponse.setBranchCode(branchCode);
                accountResponse.setCategory(product.getCategoryCode());
                accountResponse.setCustomerNumber(sCustomerNumber);
                if (bvn != null && !bvn.isEmpty()) {
                    accountResponse.setOpenedWithBVN(true);
                } else {
                    accountResponse.setOpenedWithBVN(false);
                }
                accountResponse.setProductCode(product.getProductCode());
                accountResponse.setProductName(product.getProductName());
                accountResponse.setWallet(false);
                accountResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response = gson.toJson(accountResponse);

                //Start a thread to send the account opening SMS
                NotificationPayload smsPayload = new NotificationPayload();
                smsPayload.setAccountNumber(nubanAccountNumber);
                smsPayload.setAccountType(product.getProductName());
                smsPayload.setBranch(branch.getBranchName());
                smsPayload.setMobileNumber(requestPayload.getMobileNumber());
                smsPayload.setRequestId(requestPayload.getRequestId());
                smsPayload.setSmsFor("Account Opening");
                smsPayload.setToken(token);

                genericService.generateLog("Account Opening SMS", token, gson.toJson(smsPayload), "API Request", "INFO", requestPayload.getRequestId());
//                CompletableFuture<String> resp = genericService.sendAccountOpeningSMS(smsPayload);
                String resp = sendSMS(appUser, smsPayload);
                genericService.generateLog("Account Opening SMS", token, resp, "API Response", "INFO", requestPayload.getRequestId());

                //Log the error
                genericService.generateLog("Account Opening", token, response, "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(accountNumber, "Account Opening", "", channel, "Success", requestPayload.getMobileNumber(), 'S');
                return response;
            }

            //Account record exist already. Return the details
            AccountDetailsResponsePayload accountResponse = new AccountDetailsResponsePayload();
            accountResponse.setAccountName(firstName + " " + otherName);
            accountResponse.setAccountNumber(account.getAccountNumber());
            accountResponse.setBranch(branchName);
            accountResponse.setBranchCode(branchCode);
            accountResponse.setCategory(product.getCategoryCode());
            accountResponse.setCustomerNumber(sCustomerNumber);
            if (bvn != null && !bvn.isEmpty()) {
                accountResponse.setOpenedWithBVN(true);
            } else {
                accountResponse.setOpenedWithBVN(false);
            }
            accountResponse.setProductCode(product.getProductCode());
            accountResponse.setProductName(product.getProductName());
            accountResponse.setWallet(false);
            accountResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            response = gson.toJson(accountResponse);
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Account Opening", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Account Opening", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');
            return response;
        }
    }

    @SuppressWarnings("unused")
    public String accountOpeningFallback(String token, AccountOpeningRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.account", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateMobileNumberPayload(String token, MobileNumberPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "globalAccountFallback")
    public String getCustomerGlobalAccounts(String token, MobileNumberPayload requestPayload) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        try {
            Customer customer = accountRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber().trim());
            if (customer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Customer Global Account", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Customer Global Account", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Get the accounts beloginging to the customer
            List<Account> accounts = accountRepository.getCustomerAccounts(customer);
            if (accounts == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.list.notexist", new Object[0], Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Customer Global Account", token, messageSource.getMessage("appMessages.account.list.notexist", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Customer Global Account", "", channel, messageSource.getMessage("appMessages.account.list.notexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            List<AccountDetailsResponsePayload> accountList = new ArrayList<>();
            for (Account acc : accounts) {
                // Skip the restricted account
                if (channel.equalsIgnoreCase("MOB") || channel.equalsIgnoreCase("USSD") || channel.equalsIgnoreCase("IBANK")) {
                    if (acc.getCategory() != null && (acc.getCategory().equals("6005") || acc.getCategory().equals("6014"))) {
                        continue;
                    }
                }
                AccountDetailsResponsePayload accDetails = new AccountDetailsResponsePayload();
                accDetails.setAccountName(acc.getCustomer().getLastName() + " " + acc.getCustomer().getOtherName());
                accDetails.setAccountNumber(acc.getAccountNumber());
                accDetails.setBranch(acc.getBranch().getBranchName());
                accDetails.setBranchCode(acc.getBranch().getBranchCode());
                accDetails.setCategory(acc.getCategory());
                accDetails.setCustomerNumber(acc.getCustomer().getCustomerNumber());
                accDetails.setOpenedWithBVN(acc.isOpenedWithBVN());
                accDetails.setProductCode(acc.getProduct().getProductCode());
                accDetails.setProductName(acc.getProduct().getProductName());
                accDetails.setWallet(acc.isWallet());
                accDetails.setStatus(acc.getStatus());
                accountList.add(accDetails);
            }

            AccountListResponsePayload accountListesponse = new AccountListResponsePayload();
            accountListesponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            accountListesponse.setAccount(accountList);
            response = gson.toJson(accountListesponse);

            //Log the error
            genericService.generateLog("Customer Global Account", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(customer.getCustomerNumber(), "Customer Global Account", "", channel, "Success", requestPayload.getMobileNumber(), 'S');
            return response;

        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);
            //Log the response
            genericService.generateLog("Airtime Self", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            return response;
        }
    }

    @SuppressWarnings("unused")
    public String globalAccountFallback(String token, MobileNumberPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.account", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    @HystrixCommand(fallbackMethod = "walletAccountOpeningFallback")
    public String processWalletAccountOpening(String token, AccountOpeningRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Wallet Account Opening", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Wallet Account Opening", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Wallet Account Opening", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            Customer customer = accountRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Wallet Account Opening", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Wallet Account Opening", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            Product product = accountRepository.getProductUsingProductCode(requestPayload.getProductCode().trim());
            if (product == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.record.product.noexist", new Object[]{requestPayload.getProductCode()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Wallet Account Opening", token, messageSource.getMessage("appMessages.record.product.noexist", new Object[]{requestPayload.getProductCode()}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Wallet Account Opening", "", channel, messageSource.getMessage("appMessages.record.product.noexist", new Object[]{requestPayload.getProductCode()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check the customer number
            if (!customer.getCustomerNumber().equalsIgnoreCase(requestPayload.getCustomerNumber())) {
                errorResponse.setResponseCode(ResponseCodes.CUSTOMER_NUMBER_MOBILE_MISMATCH.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.mobile.mismatch", new Object[]{requestPayload.getCustomerNumber(), customer.getCustomerNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Wallet Account Opening", token, messageSource.getMessage("appMessages.customer.mobile.mismatch", new Object[]{requestPayload.getCustomerNumber(), customer.getCustomerNumber()}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Wallet Account Opening", "", channel, messageSource.getMessage("appMessages.customer.mobile.mismatch", new Object[]{requestPayload.getCustomerNumber(), customer.getCustomerNumber()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if the customer has account with the same product
            Account account = accountRepository.getAccountUsingProduct(customer, product);
            if (account == null) {
                //Persist the record in the DB --- NG0010068 is the branch code for Digital Branch  
                Account newAccount = new Account();
                Branch branch = accountRepository.getBranchUsingBranchCode("NG0010068");
                newAccount.setAppUser(appUser);
                newAccount.setBranch(branch);
                newAccount.setCategory(product.getCategoryCode());
                newAccount.setCreatedAt(LocalDateTime.now());
                newAccount.setCustomer(customer);
                newAccount.setOpenedWithBVN(false);
                newAccount.setProduct(product);
                newAccount.setStatus("Pending");
                newAccount.setWallet(false);
                newAccount.setRequestId(requestPayload.getRequestId());
                newAccount.setTimePeriod(genericService.getTimePeriod());
                Account createdAccount = accountRepository.createAccount(newAccount);

                //Generate the account numbering
                OmniResponsePayload returnedResponse = generateAccountNumbering(customer.getCustomerNumber(), userCredentials, customer.getBranch().getBranchCode(), product.getProductCode(), token, requestPayload.getRequestId());
//                OmniResponsePayload returnedResponse = gson.fromJson(accountNumberResponse, OmniResponsePayload.class);
                if (returnedResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.FAILED_TRANSACTION.getResponseCode())) {
                    errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.numbering.failed", new Object[]{customer.getCustomerNumber()}, Locale.ENGLISH));
                    response = gson.toJson(errorResponse);

                    //Log the error
                    genericService.generateLog("Wallet Account Opening", token, returnedResponse.getResponseMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(customer.getCustomerNumber(), "Wallet Account Opening", "", channel, returnedResponse.getResponseMessage(), requestPayload.getMobileNumber(), 'F');
                    return response;
                }

                //Update the account record status
                String accountNumber = returnedResponse.getResponseMessage();
                createdAccount.setAccountNumber(accountNumber);
                createdAccount.setOldAccountNumber(accountNumber);
                createdAccount.setStatus("SUCCESS");
                createdAccount.setWallet(true);
                accountRepository.updateAccount(createdAccount);

                //Generate the response
                AccountDetailsResponsePayload accountResponse = new AccountDetailsResponsePayload();
                accountResponse.setAccountName(customer.getLastName() + " " + customer.getOtherName());
                accountResponse.setAccountNumber(accountNumber);
                accountResponse.setBranch(customer.getBranch().getBranchName());
                accountResponse.setBranchCode(customer.getBranch().getBranchCode());
                accountResponse.setCategory(product.getCategoryCode());
                accountResponse.setCustomerNumber(customer.getCustomerNumber());
                accountResponse.setOpenedWithBVN(false);
                accountResponse.setProductCode(product.getProductCode());
                accountResponse.setProductName(product.getProductName());
                accountResponse.setWallet(true);
                accountResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response = gson.toJson(accountResponse);

                NotificationPayload smsPayload = new NotificationPayload();
                smsPayload.setAccountNumber(accountNumber);
                smsPayload.setAccountType(product.getProductName());
                smsPayload.setBranch(customer.getBranch().getBranchName());
                smsPayload.setMobileNumber(requestPayload.getMobileNumber());
//                smsPayload.setAccountNumber(accountNumber);
//                smsPayload.setMessage(smsMessage.toString());
                smsPayload.setSmsFor("New Account");
                smsPayload.setSmsType('C');
                smsPayload.setRequestId(requestPayload.getRequestId());
                smsPayload.setToken(token);

                genericService.sendSMS(smsPayload);

                //Log the error
                genericService.generateLog("Wallet Account Opening", token, response, "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(accountNumber, "Wallet Account Opening", "", channel, "Success", requestPayload.getMobileNumber(), 'S');
                return response;
            }

            //Account record exist already
            if ("SUCCESS".equalsIgnoreCase(account.getStatus())) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.record.product.exist", new Object[]{requestPayload.getProductCode()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Wallet Account Opening", token, messageSource.getMessage("appMessages.record.product.exist", new Object[]{requestPayload.getProductCode()}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Wallet Account Opening", "", channel, messageSource.getMessage("appMessages.record.product.exist", new Object[]{requestPayload.getProductCode()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            if ("Pending".equalsIgnoreCase(account.getStatus())) {
                //Generate the account numbering
                OmniResponsePayload returnedResponse = generateAccountNumbering(customer.getCustomerNumber(), userCredentials,
                        customer.getBranch().getBranchCode(), product.getProductCode(), token, requestPayload.getRequestId());
//                OmniResponsePayload returnedResponse = gson.fromJson(accountNumberResponse, OmniResponsePayload.class);

                if (returnedResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.FAILED_TRANSACTION.getResponseCode())) {
                    errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.numbering.failed", new Object[]{customer.getCustomerNumber()}, Locale.ENGLISH));
                    response = gson.toJson(errorResponse);

                    //Log the error
                    genericService.generateLog("Wallet Account Opening", token, returnedResponse.getResponseMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity(customer.getCustomerNumber(), "Wallet Account Opening", "", channel, returnedResponse.getResponseMessage(), requestPayload.getMobileNumber(), 'F');
                    return response;
                }

                String accountNumber = returnedResponse.getResponseMessage();
                //Update the account record status
                account.setStatus("SUCCESS");
                account.setAccountNumber(accountNumber);
                account.setWallet(true);
                accountRepository.updateAccount(account);

                //Generate the response
                AccountDetailsResponsePayload accountResponse = new AccountDetailsResponsePayload();
                accountResponse.setAccountName(customer.getLastName() + " " + customer.getOtherName());
                accountResponse.setAccountNumber(accountNumber);
                accountResponse.setBranch(customer.getBranch().getBranchName());
                accountResponse.setBranchCode(customer.getBranch().getBranchCode());
                accountResponse.setCategory(product.getCategoryCode());
                accountResponse.setCustomerNumber(customer.getCustomerNumber());
                accountResponse.setOpenedWithBVN(false);
                accountResponse.setProductCode(product.getProductCode());
                accountResponse.setProductName(product.getProductName());
                accountResponse.setWallet(true);
                accountResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                response = gson.toJson(accountResponse);

                NotificationPayload smsPayload = new NotificationPayload();
                smsPayload.setAccountNumber(accountNumber);
                smsPayload.setAccountType(product.getProductName());
                smsPayload.setBranch(customer.getBranch().getBranchName());
                smsPayload.setMobileNumber(requestPayload.getMobileNumber());
//                smsPayload.setAccountNumber(accountNumber);
//                smsPayload.setMessage(smsMessage.toString());
                smsPayload.setSmsFor("New Account");
                smsPayload.setSmsType('C');
                smsPayload.setRequestId(requestPayload.getRequestId());
                smsPayload.setToken(token);

                genericService.sendSMS(smsPayload);

                //Log the error
                genericService.generateLog("Wallet Account Opening", token, response, "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(accountNumber, "Wallet Account Opening", "", channel, "Success", requestPayload.getMobileNumber(), 'S');
                return response;
            }

        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Wallet Account Opening", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Wallet Account Opening", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');
            return response;
        }
        return null;
    }

    @SuppressWarnings("unused")
    public String walletAccountOpeningFallback(String token, AccountOpeningRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.account", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public boolean validateAccountStatementPayload(String token, AccountStatementRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getAccountNumber());
        rawString.add(requestPayload.getStartDate());
        rawString.add(requestPayload.getEndDate());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "accountStatementFallback")
    public String getAccountStatement(String token, AccountStatementRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Account Statement", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = accountRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Statement", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check for customer account for the debit
            Account account = accountRepository.getCustomerAccount(customer, requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Account Statement", "", channel, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if this a wallet
            if (account.isWallet()) {
                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Account Statement", "", channel, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Statement", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            StringBuilder ofsBase = new StringBuilder();
            ofsBase.append("ACCOUNT:EQ=").append(requestPayload.getAccountNumber()).append(",");
            ofsBase.append("BOOKING.DATE:RG=").append(requestPayload.getStartDate().replace("-", ""))
                    .append(" ").append(requestPayload.getEndDate().replace("-", ""));

            String ofsRequest = "ENQUIRY.SELECT,," + userCredentials.trim() + "/"
                    + account.getBranch().getBranchCode() + "," + accountStatementEnquiry.trim() + "," + ofsBase;
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Account Statement", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            String middlewareResponse = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Account Statement", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
            String validationResponse = genericService.validateT24Response(middlewareResponse);
            if (validationResponse != null) {
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(validationResponse);
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Account Statement", "", channel, validationResponse, requestPayload.getMobileNumber(), 'F');
                return response;
            }

            List<AccountStatementPayload> reportCollection = new ArrayList<>();
            BigDecimal openingBalance = BigDecimal.ZERO;
            BigDecimal currentBalance = BigDecimal.ZERO;
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            int totalDebitCount = 0;
            int totalCreditCount = 0;
            openingBalance = BigDecimal.ZERO;
            currentBalance = openingBalance;
            String[] splitString = middlewareResponse.split("JV.NO::JV.NO,");
            if (splitString[1].contains("NO ENT") || splitString[1].contains("TRIES FOR PERIOD")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.statement.noentry", new Object[]{requestPayload.getStartDate(), requestPayload.getEndDate()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.account.statement.noentry", new Object[]{requestPayload.getStartDate(), requestPayload.getEndDate()}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Account Statement", "", channel, messageSource.getMessage("appMessages.account.statement.noentry", new Object[]{requestPayload.getStartDate(), requestPayload.getEndDate()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            String[] detailsSplitString = splitString[1].replace("	", "*").replace("\",\"", "|").replace("\"", "").split("\\|");
            openingBalance = BigDecimal.ZERO;
            currentBalance = openingBalance;
            int i;
            for (i = 0; i < detailsSplitString.length; i++) {
                String[] fields = detailsSplitString[i].split("\\*");
                if (fields.length >= 7) {
                    AccountStatementPayload accountStatementPayload = new AccountStatementPayload();
                    accountStatementPayload.setId(i);
                    accountStatementPayload.setValueDate(fields[0].trim());
                    accountStatementPayload.setTransDate(fields[3].trim());
                    accountStatementPayload.setCredits(fields[4].trim());
                    accountStatementPayload.setDebits(fields[5].trim());
                    accountStatementPayload.setBalance(fields[6].trim());
                    accountStatementPayload.setRemarks(fields[1] + " " + detailsSplitString[i + 1]);
                    accountStatementPayload.setVoucherNumber("");
                    accountStatementPayload.setReferenceNo(fields[2]);
                    BigDecimal debit = BigDecimal.ZERO;
                    BigDecimal credit = BigDecimal.ZERO;
                    if (fields[5].trim().replace(",", "").length() != 0) {
                        debit = new BigDecimal(fields[5].trim().replace(",", ""));
                        currentBalance = currentBalance.subtract(debit.abs());
                        totalDebitCount++;
                        totalDebit = totalDebit.subtract(debit);
                    }
                    if (fields[4].trim().replace(",", "").length() != 0) {
                        credit = new BigDecimal(fields[4].trim().replace(",", ""));
                        currentBalance = currentBalance.add(credit);
                        totalCreditCount++;
                        totalCredit = totalCredit.add(credit);
                    }
                    reportCollection.add(accountStatementPayload);
                }
            }

            AccountStatementPayload data = new AccountStatementPayload();
            data.setId(99999);
            data.setTotalCreditCount(totalCreditCount);
            data.setTotalDebitCount(totalDebitCount);
            data.setTotalCredit(totalCredit);
            data.setTotalDebit(totalDebit);
            data.setOpeningBalance(openingBalance.toString());
            data.setClosingBalance(openingBalance.add(totalCredit).subtract(totalDebit).toString());
            data.setReferenceNo("Summary");
            // The transSummary should not be added to the list.
//            reportCollection.add(data);

            reportCollection.sort(Comparator.comparingInt(AccountStatementPayload::getId));
            // Sort the list by date
            reportCollection = reportCollection
                    .stream()
                    .map(object -> {
                        AccountStatementPayload payload = object;
                        String properDate = genericService
                                .getDateStringFromUnformattedDate(payload.getTransDate());
                        payload.setTransDate(properDate);
                        return payload;
                    })
                    .sorted(Comparator.comparing(load -> LocalDate
                    .parse(((AccountStatementPayload) load).getTransDate())).reversed())
                    .collect(Collectors.toList());

            AccountStatementResponsePayload statementResponse = new AccountStatementResponsePayload();
            statementResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            statementResponse.setData(reportCollection);
            statementResponse.setBalanceSummary(data);
            statementResponse.setStamp(stamp);

            response = gson.toJson(statementResponse);
            return response;
//            reportCollection.add(data);
//            reportCollection.sort(Comparator.comparingInt(AccountStatementPayload::getId));
//
//            AccountStatementResponsePayload statementResponse = new AccountStatementResponsePayload();
//            statementResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
//            statementResponse.setData(reportCollection);
//            response = gson.toJson(statementResponse);
//            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Account Statement", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Account Statement", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');
            return response;
        }
    }

    @Override
    @HystrixCommand(fallbackMethod = "accountStatementFallback")
    public String getAccountStatementOfficial(String token, AccountStatementRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Account Statement Official", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = accountRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement Official", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Statement Official", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check for customer account
            Account account = accountRepository.getCustomerAccount(customer, requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement Official", token, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Account Statement Official", "", channel, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if this a wallet
            if (account.isWallet()) {
                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement Official", token, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Posting Restriction", "", channel, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Account Statement Official", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Statement Official", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            StringBuilder ofsBase = new StringBuilder();
            ofsBase.append("ACCOUNT:EQ=").append(requestPayload.getAccountNumber()).append(",");
            ofsBase.append("BOOKING.DATE:RG=").append(requestPayload.getStartDate().replace("-", ""))
                    .append(" ").append(requestPayload.getEndDate().replace("-", ""));

            String ofsRequest = "ENQUIRY.SELECT,," + userCredentials.trim() + "/"
                    + account.getBranch().getBranchCode() + "," + accountStatementEnquiry.trim() + "," + ofsBase;
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Account Statement", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            String middlewareResponse = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Account Statement", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
            String validationResponse = genericService.validateT24Response(middlewareResponse);
            if (validationResponse != null) {
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(validationResponse);
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Account Statement", "", channel, validationResponse, requestPayload.getMobileNumber(), 'F');
                return response;
            }

            List<AccountStatementPayload> reportCollection = new ArrayList<>();
            BigDecimal openingBalance = BigDecimal.ZERO;
            BigDecimal currentBalance = BigDecimal.ZERO;
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            int totalDebitCount = 0;
            int totalCreditCount = 0;
            openingBalance = BigDecimal.ZERO;
            currentBalance = openingBalance;
            String[] splitString = middlewareResponse.split("JV.NO::JV.NO,");
            if (splitString[1].contains("NO ENT") || splitString[1].contains("TRIES FOR PERIOD")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.statement.noentry", new Object[]{requestPayload.getStartDate(), requestPayload.getEndDate()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.account.statement.noentry", new Object[]{requestPayload.getStartDate(), requestPayload.getEndDate()}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Account Statement", "", channel, messageSource.getMessage("appMessages.account.statement.noentry", new Object[]{requestPayload.getStartDate(), requestPayload.getEndDate()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            String[] detailsSplitString = splitString[1].replace("	", "*").replace("\",\"", "|").replace("\"", "").split("\\|");
            openingBalance = BigDecimal.ZERO;
            currentBalance = openingBalance;
            int i;
            for (i = 0; i < detailsSplitString.length; i++) {
                String[] fields = detailsSplitString[i].split("\\*");
                if (fields.length >= 7) {
                    AccountStatementPayload accountStatementPayload = new AccountStatementPayload();
                    accountStatementPayload.setId(i);
                    accountStatementPayload.setValueDate(fields[0].trim());
                    accountStatementPayload.setTransDate(fields[3].trim());
                    accountStatementPayload.setCredits(fields[4].trim());
                    accountStatementPayload.setDebits(fields[5].trim());
                    accountStatementPayload.setBalance(fields[6].trim());
                    accountStatementPayload.setRemarks(fields[1] + " " + detailsSplitString[i + 1]);
                    accountStatementPayload.setVoucherNumber("");
                    accountStatementPayload.setReferenceNo(fields[2]);
                    BigDecimal debit = BigDecimal.ZERO;
                    BigDecimal credit = BigDecimal.ZERO;
                    if (fields[5].trim().replace(",", "").length() != 0) {
                        debit = new BigDecimal(fields[5].trim().replace(",", ""));
                        currentBalance = currentBalance.subtract(debit.abs());
                        totalDebitCount++;
                        totalDebit = totalDebit.subtract(debit);
                    }
                    if (fields[4].trim().replace(",", "").length() != 0) {
                        credit = new BigDecimal(fields[4].trim().replace(",", ""));
                        currentBalance = currentBalance.add(credit);
                        totalCreditCount++;
                        totalCredit = totalCredit.add(credit);
                    }
                    reportCollection.add(accountStatementPayload);
                }
            }

            String directAccountStatement = this.getAccountStatement(token, requestPayload);
            AccountStatementResponsePayload directPayload = gson.fromJson(directAccountStatement, AccountStatementResponsePayload.class);
            reportCollection = directPayload.getData();
            reportCollection.add(directPayload.getBalanceSummary());

            log.info("Report collection from T24: " + reportCollection.stream().map(AccountStatementPayload::toString).collect(Collectors.toList()));

            //Charge the customer for the transaction
            LocalTransferWithInternalPayload localFT = new LocalTransferWithInternalPayload();
            localFT.setAmount(accountStatementCharge);
            localFT.setCreditAccount(accountStatementIncomeAccount);
            localFT.setDebitAccount(account.getAccountNumber());
            localFT.setMobileNumber(customer.getMobileNumber());
            localFT.setNarration("ACCOUNT STATEMENT CHARGE FOR " + account.getAccountNumber());
            localFT.setTransType("ACTF");
            localFT.setBranchCode(digitalBranchCode); //Defaulted to digital branch
            localFT.setAuthorizer("0");
            localFT.setInputter("0");
            localFT.setNoOfAuthorizer("0");
            double vat = Double.parseDouble(accountStatementCharge) * 0.075; // VAT is 7.5%
            List<ChargeTypes> charges = new ArrayList<>();
            ChargeTypes chargeVat = new ChargeTypes();
            chargeVat.setChargeAmount(String.valueOf(vat));
            chargeVat.setChargeType("CBNCHRG");
            charges.add(chargeVat);
            localFT.setChargeTypes(charges);
            localFT.setRequestId(requestPayload.getRequestId());
            localFT.setToken(token);
            localFT.setHash(genericService.hashLocalTransferWithInternalValidationRequest(localFT));
            requestJson = gson.toJson(localFT);

            //Call the funds transfer microservices
            String ftResponseJson = ftService.localTransferInternalAccount(token, requestJson);
            OmniResponsePayload omniResponse = gson.fromJson(ftResponseJson, OmniResponsePayload.class);
            if (!omniResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                return ftResponseJson;
            }

            //Call the task executor
            NotificationPayload emailPayload = new NotificationPayload();
            emailPayload.setEmail(customer.getEmail() == null || customer.getEmail().isEmpty() || customer.getEmail().isBlank() ? requestPayload.getEmail() : customer.getEmail());
            emailPayload.setEmailSubject("Account Statement");
            emailPayload.setLastName(customer.getLastName());
            emailPayload.setOtherName(customer.getOtherName());
            emailPayload.setAccountNumber(requestPayload.getAccountNumber());
            emailPayload.setAccountType(account.getProduct().getProductName());
            emailPayload.setBranch(customer.getBranch().getBranchName());
            emailPayload.setStartDate(requestPayload.getStartDate());
            emailPayload.setEndDate(requestPayload.getEndDate());
            genericService.sendAccountStatementMail(emailPayload, reportCollection);

            omniResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            omniResponse.setResponseMessage(messageSource.getMessage("appMessages.account.statement.email", new Object[0], Locale.ENGLISH));
            response = gson.toJson(omniResponse);
            return response;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error in account service: " + ex.getMessage());
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Account Statement Official", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Account Statement Official", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');
            return response;
        }
    }

    @SuppressWarnings("unused")
    public String accountStatementFallback(String token, AccountStatementRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.account", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public Object checkIfSameRequestId(String requestId) {
        try {
            Account accountRecord = accountRepository.getRecordUsingRequestId(requestId);
            return accountRecord == null;
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public Object checkIfSamePostingRestrictionRequestId(String requestId) {
        try {
            CustomerAccountRestrictions restrictionRecord = accountRepository.getPostingRestrictionRecordUsingRequestId(requestId);
            return restrictionRecord == null;
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public boolean validatePostingRestrictionRequestPayload(String token, RestrictionRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getAccountNumber());
        for (String str : requestPayload.getPostingRestrictions()) {
            rawString.add(str);
        }
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processAddPostingRestriction(String token, RestrictionRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Posting Restriction", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = accountRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Posting Restriction", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check for customer account for the debit
            Account account = accountRepository.getCustomerAccount(customer, requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Posting Restriction", "", channel, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if this a wallet
            if (account.isWallet()) {
                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Posting Restriction", "", channel, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Posting Restriction", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if any posting restriction is inputted
            if (requestPayload.getPostingRestrictions().isEmpty()) {
                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.request.posting.restriction.empty", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Posting Restriction", "", channel, messageSource.getMessage("appMessages.request.posting.restriction.empty", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.request.posting.restriction.empty", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Persist the restrictions
            CustomerAccountRestrictions newRestrict = new CustomerAccountRestrictions();
            newRestrict.setAccount(true);
            newRestrict.setAppUser(appUser);
            newRestrict.setCreatedAt(LocalDateTime.now());
            newRestrict.setCrudType("ADD");
            newRestrict.setCustomer(customer);
            newRestrict.setCustomerOrAccount("ACCOUNT");
            newRestrict.setMobileNumber(requestPayload.getMobileNumber());
            newRestrict.setPostingRestriction(requestPayload.getPostingRestrictions().toString());
            newRestrict.setRequestId(requestPayload.getRequestId());
            newRestrict.setStatus("PENDING");
            CustomerAccountRestrictions createRestriction = accountRepository.createPostingRestriction(newRestrict);

            StringBuilder ofsBase = new StringBuilder("");
            int i = 1;
            for (String str : requestPayload.getPostingRestrictions()) {
                PostingRestriction postingRestrict = accountRepository.getPostingRestrictionUsingName(str);
                if (postingRestrict != null) {
                    ofsBase.append("POSTING.RESTRICT:").append(i).append(":1::=").append(postingRestrict.getPostingRestrictionId()).append(",");
                    i++;
                }

            }

            String ofsRequest = accountVersion.trim() + "," + userCredentials
                    + "/" + customer.getBranch().getBranchCode().trim() + "," + requestPayload.getAccountNumber() + "," + ofsBase;
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Posting Restriction", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            String middlewareResponse = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Posting Restriction", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
            if (!middlewareResponse.contains("//1")) {
                createRestriction.setStatus("FAILED");
                accountRepository.updatePostingRestriction(createRestriction);

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(middlewareResponse);
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Posting Restriction", "", channel, middlewareResponse, requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Update the status of the transaction
            createRestriction.setStatus("SUCCESS");
            accountRepository.updatePostingRestriction(createRestriction);
            //Log the error
            genericService.generateLog("Posting Restriction", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(customer.getCustomerNumber(), "Posting Restriction", "", channel, "Success", requestPayload.getMobileNumber(), 'S');
            OmniResponsePayload omniResponse = new OmniResponsePayload();
            omniResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            omniResponse.setResponseMessage(messageSource.getMessage("appMessages.posting.restriction.success", new Object[0], Locale.ENGLISH));
            response = gson.toJson(omniResponse);
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Posting Restriction", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Posting Restriction", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');
            return response;
        }
    }

    @Override
    public String processRemovePostingRestriction(String token, RestrictionRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Posting Restriction", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = accountRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Posting Restriction", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check for customer account for the debit
            Account account = accountRepository.getCustomerAccount(customer, requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Posting Restriction", "", channel, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if this a wallet
            if (account.isWallet()) {
                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Posting Restriction", "", channel, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Posting Restriction", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if any posting restriction is inputted
            if (requestPayload.getPostingRestrictions().isEmpty()) {
                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.request.posting.restriction.empty", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Posting Restriction", "", channel, messageSource.getMessage("appMessages.request.posting.restriction.empty", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.request.posting.restriction.empty", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Persist the restrictions
            CustomerAccountRestrictions newRestrict = new CustomerAccountRestrictions();
            newRestrict.setAccount(true);
            newRestrict.setAppUser(appUser);
            newRestrict.setCreatedAt(LocalDateTime.now());
            newRestrict.setCrudType("REMOVE");
            newRestrict.setCustomer(customer);
            newRestrict.setCustomerOrAccount("ACCOUNT");
            newRestrict.setMobileNumber(requestPayload.getMobileNumber());
            newRestrict.setPostingRestriction(requestPayload.getPostingRestrictions().toString());
            newRestrict.setRequestId(requestPayload.getRequestId());
            newRestrict.setStatus("PENDING");
            CustomerAccountRestrictions createRestriction = accountRepository.createPostingRestriction(newRestrict);

            StringBuilder ofsBase = new StringBuilder("");
            //Get the customer posting restrictions 
            String ofsRequest = accountVersion.trim().replace("/I/", "/S/") + "," + userCredentials
                    + "/" + customer.getBranch().getBranchCode().trim() + "," + requestPayload.getAccountNumber() + "," + ofsBase;
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Posting Restriction", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            String middlewareResponse = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Posting Restriction", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
            if (!middlewareResponse.contains("//1")) {
                createRestriction.setStatus("FAILED");
                accountRepository.updatePostingRestriction(createRestriction);

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(middlewareResponse);
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Posting Restriction", "", channel, middlewareResponse, requestPayload.getMobileNumber(), 'F');
                return response;
            }

            List<String> currentRestrictions = new ArrayList<>();
            for (int i = 1; i < 10; i++) {
                String restriction = genericService.getTextFromOFSResponse(middlewareResponse, "POSTING.RESTRICT:" + i + ":1");
                if (restriction != null) {
                    currentRestrictions.add(restriction);
                }
            }

            if (currentRestrictions.isEmpty()) {
                //Log the error
                genericService.generateLog("Posting Restriction", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Posting Restriction", "", channel, "Success", requestPayload.getMobileNumber(), 'S');
                OmniResponsePayload omniResponse = new OmniResponsePayload();
                omniResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                omniResponse.setResponseMessage(messageSource.getMessage("appMessages.posting.restriction.none", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(omniResponse);
                return response;
            }

            //Loop through the selected restrictions
            for (String str : requestPayload.getPostingRestrictions()) {
                PostingRestriction postingRestrict = accountRepository.getPostingRestrictionUsingName(str);
                if (postingRestrict != null) {
                    if (currentRestrictions.contains(postingRestrict.getPostingRestrictionId())) {
                        //Remove the selected from the list
                        currentRestrictions.remove(postingRestrict.getPostingRestrictionId());
                    }
                }
            }

            //Clear the current posting restriction
            ofsBase.append("POSTING.RESTRICT::=");
            ofsRequest = accountVersion.trim() + "," + userCredentials
                    + "/" + customer.getBranch().getBranchCode().trim() + "," + requestPayload.getAccountNumber() + "," + ofsBase;
            newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Posting Restriction", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            middlewareResponse = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Posting Restriction", token, response, "OFS Response", "INFO", requestPayload.getRequestId());
            //Reset the string base
            ofsBase = new StringBuilder("");
            int i = 1;
            for (String restrict : currentRestrictions) {
                ofsBase.append("POSTING.RESTRICT:").append(i).append(":1::=").append(restrict).append(",");
                i++;
            }

            ofsRequest = accountVersion.trim() + "," + userCredentials
                    + "/" + customer.getBranch().getBranchCode().trim() + "," + requestPayload.getAccountNumber() + "," + ofsBase;
            middlewareResponse = genericService.postToT24(ofsRequest);

            if (!middlewareResponse.contains("//1")) {
                createRestriction.setStatus("FAILED");
                accountRepository.updatePostingRestriction(createRestriction);

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(middlewareResponse);
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Posting Restriction", "", channel, middlewareResponse, requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Update the status of the transaction
            createRestriction.setStatus("SUCCESS");
            accountRepository.updatePostingRestriction(createRestriction);

            //Log the error
            genericService.generateLog("Posting Restriction", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(customer.getCustomerNumber(), "Posting Restriction", "", channel, "Success", requestPayload.getMobileNumber(), 'S');
            OmniResponsePayload omniResponse = new OmniResponsePayload();
            omniResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            omniResponse.setResponseMessage(messageSource.getMessage("appMessages.posting.restriction.success", new Object[0], Locale.ENGLISH));
            response = gson.toJson(omniResponse);
            return response;

        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Posting Restriction", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Posting Restriction", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');
            return response;
        }
    }

    @Override
    public String processFetchPostingRestriction(String token) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        try {
            List<PostingRestriction> restrictions = accountRepository.getPostingRestrictions();
            response = gson.toJson(restrictions);
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            return response;
        }
    }

    @Override
    public String processFetchBranchAccountOfficers(String token) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        try {
            List<Branch> branches = accountRepository.getBranches();
            if (branches != null) {
                List<BranchAccountOfficerResponsePayload> branchAcctOfficers = new ArrayList<>();
                for (Branch br : branches) {
                    List<AccountOfficer> branchAccountOfficers = accountRepository.getBranchAccountOfficers(br);
                    if (branchAccountOfficers != null) {
                        List<AccountOfficerPayload> accountOfficers = new ArrayList<>();
                        for (AccountOfficer aoff : branchAccountOfficers) {
                            AccountOfficerPayload acctOfficer = new AccountOfficerPayload();
                            acctOfficer.setAccountOfficerCode(aoff.getCode());
                            acctOfficer.setAccountOfficerName(aoff.getName());
                            accountOfficers.add(acctOfficer);
                        }
                        BranchAccountOfficerResponsePayload brAccOfficer = new BranchAccountOfficerResponsePayload();
                        brAccOfficer.setBranch(br.getBranchName());
                        brAccOfficer.setAccountOfficers(accountOfficers);
                        branchAcctOfficers.add(brAccOfficer);
                    }
                }

                response = gson.toJson(branchAcctOfficers);
                return response;
            }
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            return response;
        }
    }

    @Override
    public String processFetchBranches(String token) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        try {
            String[] states = new String[]{"Abia", "Adamawa", "Akwa Ibom", "Anambra", "Bauchi", "Bayelsa", "Benue",
                "Borno", "Cross River", "Delta", "Ebonyi", "Edo", "Ekiti", "Enugu", "FCT - Abuja", "Gombe",
                "Imo", "Jigawa", "Kaduna", "Kano", "Katsina", "Kebbi", "Kogi", "Kwara", "Lagos", "Nasarawa",
                "Niger", "Ogun", "Ondo", "Osun", "Oyo", "Plateau", "Rivers", "Sokoto", "Taraba", "Yobe", "Zamfara"
            };
            List<BranchListResponsePayload> branches = new ArrayList<>();
            for (String st : states) {
                List<Branch> stateBranch = accountRepository.getBranches(st);
                if (stateBranch != null) {
                    List<BranchPayload> branchList = new ArrayList<>();
                    for (Branch br : stateBranch) {
                        BranchPayload newBranch = new BranchPayload();
                        newBranch.setBranchCode(br.getBranchCode());
                        newBranch.setBranchName(br.getBranchName());
                        branchList.add(newBranch);
                    }
                    BranchListResponsePayload stateBranches = new BranchListResponsePayload();
                    stateBranches.setState(st);
                    stateBranches.setBranches(branchList);
                    branches.add(stateBranches);
                }
            }

            response = gson.toJson(branches);
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            return response;
        }
    }

    @Override
    public String processFetchProducts(String token) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String response = "";
        try {
            List<ProductPayload> productList = new ArrayList<>();
            List<Product> products = accountRepository.getProducts();
            if (products != null) {
                for (Product pr : products) {
                    ProductPayload newProduct = new ProductPayload();
                    newProduct.setProductName(pr.getProductName());
                    newProduct.setProductCode(pr.getCategoryCode());

                    productList.add(newProduct);
                }
                response = gson.toJson(productList);
                return response;
            }

            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            return response;
        }
    }

    @Override
    public boolean validateAccountBalancesPayload(String token, MobileNumberPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String getAccountBalances(String token, MobileNumberPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Account Balances", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = accountRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Balances", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Balances", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Account Balance", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Balance", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Get all accounts belonging to the customer
            //Get the accounts beloginging to the customer
            List<Account> accounts = accountRepository.getCustomerAccounts(customer);
            if (accounts == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.list.notexist", new Object[0], Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Balances", token, messageSource.getMessage("appMessages.account.list.notexist", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Balances", "", channel, messageSource.getMessage("appMessages.account.list.notexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Initialize the array to hold the balances
            List<AccountBalancePayload> accountList = new ArrayList<>();
            for (Account acc : accounts) {
                if (channel.equalsIgnoreCase("MOB") || channel.equalsIgnoreCase("USSD") || channel.equalsIgnoreCase("IBANK")) {
                    if (acc.getCategory() != null && (acc.getCategory().equals("6005") || acc.getCategory().equals("6014"))) {
                        continue;
                    }
                }
                AccountNumberPayload payload = new AccountNumberPayload();
                payload.setAccountNumber(acc.getAccountNumber());
                payload.setRequestId(requestPayload.getRequestId());
                response = getAccountBalance(token, payload);

                String validationResponse = genericService.validateT24Response(response);
                if (validationResponse == null) {
                    AccountBalancePayload accountResponse = gson.fromJson(response, AccountBalancePayload.class);
                    accountList.add(accountResponse);
                }
            }

            //Log the error
            genericService.generateLog("Account Balances", token, response, "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Account Balances", "", channel, "Success", requestPayload.getMobileNumber(), 'S');
            AccountBalancesResponsePayload accountResponse = new AccountBalancesResponsePayload();

            //Check if the list is empty
            if (accountList.isEmpty()) {
                accountResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                accountResponse.setBalances(accountList);
                return response;
            }
            accountResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            accountResponse.setBalances(accountList);
            response = gson.toJson(accountResponse);
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Account Balance", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Account Balance", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');
            return response;
        }
    }

    @Override
    public String processFetchPostingRestriction(String token, AccountNumberPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Posting Restriction", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Account account = accountRepository.getAccountUsingAccountNumber(requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Posting Restriction", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check if this a wallet
            if (account.isWallet()) {
                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Posting Restriction", "", channel, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), account.getCustomer().getMobileNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Posting Restriction", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Posting Restriction", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Get the customer posting restrictions 
            String ofsRequest = accountVersion.trim().replace("/I/", "/S/") + "," + userCredentials
                    + "/" + account.getCustomer().getBranch().getBranchCode().trim() + "," + requestPayload.getAccountNumber();
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Posting Restriction", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            String middlewareResponse = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Posting Restriction", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
            if (!middlewareResponse.contains("//1")) {
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(middlewareResponse);
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Posting Restriction", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(account.getCustomer().getCustomerNumber(), "Posting Restriction", "", channel, middlewareResponse, account.getCustomer().getMobileNumber(), 'F');
                return response;
            }

            List<PostingRestrictionPayload> restrictionList = new ArrayList<>();
            for (int i = 1; i < 10; i++) {
                String restriction = genericService.getTextFromOFSResponse(middlewareResponse, "POSTING.RESTRICT:" + i + ":1");
                if (restriction != null) {
                    PostingRestriction ps = accountRepository.getPostingRestrictionUsingId(restriction);
                    PostingRestrictionPayload newRestrict = new PostingRestrictionPayload();
                    newRestrict.setPostingRestrictionDesc(ps.getPostingRestrictionDesc());
                    newRestrict.setPostingRestrictionId(ps.getPostingRestrictionId());
                    newRestrict.setPostingRestrictionType(ps.getPostingRestrictionType());
                    restrictionList.add(newRestrict);
                }
            }

            PostingRestrictionResponsePayload successResponse = new PostingRestrictionResponsePayload();
            successResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            successResponse.setPostingRestriction(restrictionList);
            response = gson.toJson(successResponse);

            //Log the error
            genericService.generateLog("Posting Restriction", token, middlewareResponse, "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(account.getCustomer().getCustomerNumber(), "Posting Restriction", "", channel, "Success", account.getCustomer().getMobileNumber(), 'F');
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Posting Restriction", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getAccountNumber(), "Posting Restriction", "", channel, ex.getMessage(), requestPayload.getAccountNumber(), 'F');
            return response;
        }
    }

    private OmniResponsePayload generateAccountNumbering(String customerNumber, String userCredentials, String branchCode, String productId, String token, String requestId) {
        OmniResponsePayload responsePayload = new OmniResponsePayload();
        try {
            StringBuilder ofsBase = new StringBuilder();
            ofsBase.append("CUSTOMER.NO::=").append(customerNumber).append(",");
            ofsBase.append("PDT.CODE::=").append(productId).append(",");
            ofsBase.append("CREATED.Y.N::=N");
            String ofsRequest = accountNumberingCodeVersion + "," + userCredentials.trim() + "/" + branchCode.trim() + ",," + ofsBase;
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS log
            genericService.generateLog("Account Numbering", token, newOfsRequest, "OFS Request", "INFO", requestId);
            String response = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Account Numbering", token, response, "OFS Response", "INFO", requestId);
            if (response.contains("//1")) {
                String accountNumber = genericService.getTextFromOFSResponse(response, "ACCT.CODE:1:1");
                //Check for Null
                if (!accountNumber.matches("[0-9]{10}")) {
                    responsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    responsePayload.setResponseMessage(messageSource.getMessage("appMessages.account.numbering.failed", new Object[]{customerNumber}, Locale.ENGLISH) + response);
                    return responsePayload;
                } else {
                    responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                    responsePayload.setResponseMessage(accountNumber);
                    return responsePayload;
                }
            }
            responsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            responsePayload.setResponseMessage(response);
            return responsePayload;
        } catch (NoSuchMessageException ex) {
            responsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            responsePayload.setResponseMessage(ex.getMessage());
            return responsePayload;
        }
    }

    @Override
    public boolean validateAccountMobileNumberPayload(String token, AccountandMobileRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    @HystrixCommand(fallbackMethod = "accountMiniStatementFallback")
    public String getMiniAccountStatement(String token, AccountandMobileRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Mini Account Statement", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = accountRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Mini Account Statement", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Mini Account Statement", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check for customer account for the debit
            Account account = accountRepository.getCustomerAccount(customer, requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Mini Account Statement", token, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Mini Account Statement", "", channel, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if this a wallet
            if (account.isWallet()) {
                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Mini Account Statement", token, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Mini Account Statement", "", channel, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Mini Account Statement", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Mini Account Statement", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            String startDate = LocalDate.now().minusDays(90).toString().replace("-", "");
            String endDate = LocalDate.now().toString().replace("-", "");
            StringBuilder ofsBase = new StringBuilder();
            ofsBase.append("ACCOUNT:EQ=").append(requestPayload.getAccountNumber()).append(",");
            ofsBase.append("BOOKING.DATE:RG=").append(startDate)
                    .append(" ").append(endDate);

            String ofsRequest = "ENQUIRY.SELECT,," + userCredentials.trim() + "/"
                    + account.getBranch().getBranchCode() + "," + accountStatementEnquiry.trim() + "," + ofsBase;
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Mini Account Statement", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            response = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Mini Account Statement", token, response, "OFS Response", "INFO", requestPayload.getRequestId());
            String validationResponse = genericService.validateT24Response(response);
            if (validationResponse != null) {
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(validationResponse);
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Mini Account Statement", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Mini Account Statement", "", channel, validationResponse, requestPayload.getMobileNumber(), 'F');
                return response;
            }

            List<AccountStatementPayload> reportCollection = new ArrayList<>();
            String[] splitString = response.split("JV.NO::JV.NO,");
            String[] detailsSplitString = splitString[1].replace("	", "*").replace("\",\"", "|").replace("\"", "").split("\\|");
            int i;
            for (i = 0; i < detailsSplitString.length; i++) {
                String[] fields = detailsSplitString[i].split("\\*");
                if (fields.length >= 7 && !fields[0].equalsIgnoreCase("")) {
                    AccountStatementPayload accountStatementPayload = new AccountStatementPayload();
                    accountStatementPayload.setId(i);
                    accountStatementPayload.setValueDate(fields[0].trim());
                    accountStatementPayload.setTransDate(fields[3].trim());
                    accountStatementPayload.setCredits(fields[4].trim());
                    accountStatementPayload.setDebits(fields[5].trim());
                    accountStatementPayload.setBalance(fields[6].trim());
                    accountStatementPayload.setRemarks(fields[1].trim() + " " + detailsSplitString[i + 1].trim());
                    accountStatementPayload.setVoucherNumber("");
                    accountStatementPayload.setReferenceNo(fields[2].trim());

                    reportCollection.add(accountStatementPayload);
                }
            }

            reportCollection.sort(Comparator.comparingInt(AccountStatementPayload::getId));
            if (reportCollection.isEmpty()) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage("No transaction for the period " + startDate + "-" + endDate);
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Mini Account Statement", token, "No transaction for the period " + startDate + "-" + endDate, "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Mini Account Statement", "", channel, "No transaction for the period " + startDate + "-" + endDate, requestPayload.getMobileNumber(), 'F');
                return response;
            }
            reportCollection.sort(Comparator.comparingInt(AccountStatementPayload::getId).reversed());
            reportCollection = reportCollection.stream().limit(10).collect(Collectors.toList());
            AccountStatementResponsePayload statementResponse = new AccountStatementResponsePayload();
            statementResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            statementResponse.setData(reportCollection);
            response = gson.toJson(statementResponse);
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Mini Account Statement", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Mini Account Statement", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');
            return response;
        }
    }

    @SuppressWarnings("unused")
    public String accountMiniStatementFallback(String token, AccountandMobileRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.account", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    private String sendSMS(AppUser appUser, NotificationPayload smsPayload) {
        //Log the request

        StringBuilder smsMessage = new StringBuilder();
        smsMessage.append("Thanks for opening a ");
        smsMessage.append(smsPayload.getAccountType()).append(" account at our ");
        smsMessage.append(smsPayload.getBranch()).append(" branch. Your account number is ");
        smsMessage.append(smsPayload.getAccountNumber()).append(". Your future is Bright. #StaySafe");

        SMS newSMS = new SMS();
        newSMS.setAppUser(appUser);
        newSMS.setCreatedAt(LocalDateTime.now());
        newSMS.setFailureReason("");
        newSMS.setMessage(smsMessage.toString());
        newSMS.setMobileNumber(smsPayload.getMobileNumber());
        newSMS.setRequestId(smsPayload.getRequestId());
        newSMS.setSmsFor(smsPayload.getSmsFor());
        newSMS.setSmsType('N');
        newSMS.setStatus("PENDING");
        newSMS.setTimePeriod(genericService.getTimePeriod());
        //newSMS.setAccount(account);

        SMS createSMS = smsRepository.createSMS(newSMS);

        //Generate the SMS payload
        SMSPayload oSMSPayload = new SMSPayload();
        oSMSPayload.setMessage(newSMS.getMessage());
        oSMSPayload.setMobileNumber(newSMS.getMobileNumber());
        String ofsRequest = gson.toJson(oSMSPayload);

        String middlewareResponse = genericService.postToMiddleware("/sms/send", ofsRequest);

        SMSResponsePayload responsePayload = gson.fromJson(middlewareResponse, SMSResponsePayload.class);
        if ("00".equals(responsePayload.getResponseCode())) {
            createSMS.setStatus("SUCCESS");
            smsRepository.updateSMS(createSMS);
            //Create activity log
            genericService.generateLog("SMS", "", "Success", "API Response", "INFO", newSMS.getMobileNumber());
            SMSResponsePayload smsResponse = new SMSResponsePayload();
            smsResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            smsResponse.setMobileNumber(newSMS.getMobileNumber());
            smsResponse.setMessage(newSMS.getMessage());
            smsResponse.setSmsFor(newSMS.getSmsFor());
            String response = gson.toJson(smsResponse);
            genericService.generateLog("SMS", response, "Success", "API Response", "INFO", newSMS.getMobileNumber());
            return response;
        }

        createSMS.setStatus("FAILED");
        createSMS.setFailureReason(responsePayload.getResponseDescription());
        smsRepository.updateSMS(createSMS);
        //Create activity log
        genericService.generateLog("SMS", "", "Success", "API Response", "INFO", newSMS.getMobileNumber());

        return "Failure";
    }

    private AccountBalanceResponsePayload parseAccountDetails(String sResponse, AccountNumberPayload requestPayload) {
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
            accountResponse.setAccountNumber(requestPayload.getAccountNumber());
            accountResponse.setAvailableBalance("0.00".replace("\"", "").trim());
            accountResponse.setLedgerBalance("0.00".replace("\"", "").trim());
            accountResponse.setResponseCode(ResponseCodes.FORMAT_EXCEPTION.getResponseCode());
            return accountResponse;
        }
        if (testParam.contains("No records were found that matched the selection criteria")) {
            AccountBalanceResponsePayload accountResponse = new AccountBalanceResponsePayload();
            accountResponse.setAccountNumber(requestPayload.getAccountNumber());
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
            accountResponse.setAccountNumber(requestPayload.getAccountNumber());

            accountResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());

            double lockedAmount = 0;
            try {
                if (items.length > 22) {
                    lockedAmount = Double.parseDouble(items[22].trim().replaceAll(",", ""));
                }
            } catch (NumberFormatException numberFormatException) {
            }
            try {
                double availBal = Double.parseDouble(items[5].trim()) - lockedAmount;
                accountResponse.setAvailableBalance(formatter.format(availBal).replace("\"", "").trim());
            } catch (NumberFormatException numberFormatException) {
            }
            try {
                double LedgerBal = Double.parseDouble(items[8].trim());
                accountResponse.setLedgerBalance(formatter.format(LedgerBal).replace("\"", "").trim());
            } catch (NumberFormatException numberFormatException) {
            }

            return accountResponse;
        }
        return null;
    }

    public static String[] getMessageTokens(String msg, String delim) {
        StringTokenizer tokenizer = new StringTokenizer(msg, delim);
        ArrayList tokens = new ArrayList(10);
        for (; tokenizer.hasMoreTokens(); tokens.add(tokenizer.nextToken())) {
        }
        return (String[]) (String[]) tokens.toArray(new String[1]);
    }

    @Override
    public String getAccountStatementDynamic(String token, DynamicQuery dynamicQuery, DynamicAccountStatementRequestPayload requestPayload
    ) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        String response = "";
        genericService.generateLog("Account Statement", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            Customer customer = accountRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            if (customer == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Statement", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check for customer account for the debit
            Account account = accountRepository.getCustomerAccount(customer, requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.NO_PRIMARY_ACCOUNT.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Account Statement", "", channel, messageSource.getMessage("appMessages.account.noprimary", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check if this a wallet
            if (account.isWallet()) {
                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Account Statement", "", channel, messageSource.getMessage("appMessages.account.iswallet", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = accountRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Account Statement", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //validate IMEI
            boolean imeiMatch = bCryptEncoder.matches(requestPayload.getImei(), customer.getImei());
            if (!imeiMatch) {
                genericService.generateLog("Customer Details", token, messageSource.getMessage("appMessages.customer.imei.invalid", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                genericService.createUserActivity("", "Validate IMEI", "appMessages.customer.imei.invalid", channel, messageSource.getMessage("appMessages.customer.imei.invalid", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.IMEI_MISMATCH.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.imei.invalid", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Validate the PIN
            boolean pinMatch = bCryptEncoder.matches(requestPayload.getPin(), customer.getPin());
            if (!pinMatch) {
                //Log the error
                genericService.generateLog("Status Update", token, messageSource.getMessage("appMessages.mismatch.pin", new Object[]{"PIN", "Mobile ", requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getRequestId(), "Status Update", "", channel, messageSource.getMessage("appMessages.mismatch.pin", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.PASSWORD_PIN_MISMATCH.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.mismatch.pin", new Object[]{"PIN", "Mobile ", requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            // Automate the start and the end date in space of one month
            requestPayload = updateDynamicRequestDate(requestPayload);

            StringBuilder ofsBase = new StringBuilder();
            ofsBase.append("ACCOUNT:EQ=").append(requestPayload.getAccountNumber()).append(",");
            ofsBase.append("BOOKING.DATE:RG=").append(requestPayload.getStartDate().replace("-", ""))
                    .append(" ").append(requestPayload.getEndDate().replace("-", ""));

            String ofsRequest = "ENQUIRY.SELECT,," + userCredentials.trim() + "/"
                    + account.getBranch().getBranchCode() + "," + accountStatementEnquiry.trim() + "," + ofsBase;
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Account Statement", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            String middlewareResponse = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Account Statement", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
            String validationResponse = genericService.validateT24Response(middlewareResponse);
            if (validationResponse != null) {
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(validationResponse);
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Account Statement", "", channel, validationResponse, requestPayload.getMobileNumber(), 'F');
                return response;
            }

            List<AccountStatementPayload> reportCollection = new ArrayList<>();
            BigDecimal openingBalance = BigDecimal.ZERO;
            BigDecimal currentBalance = BigDecimal.ZERO;
            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            int totalDebitCount = 0;
            int totalCreditCount = 0;
            openingBalance = BigDecimal.ZERO;
            currentBalance = openingBalance;
            String[] splitString = middlewareResponse.split("JV.NO::JV.NO,");
            if (splitString[1].contains("NO ENT") || splitString[1].contains("TRIES FOR PERIOD")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.statement.noentry", new Object[]{requestPayload.getStartDate(), requestPayload.getEndDate()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Account Statement", token, messageSource.getMessage("appMessages.account.statement.noentry", new Object[]{requestPayload.getStartDate(), requestPayload.getEndDate()}, Locale.ENGLISH), "API Error", "DEBUG", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(customer.getCustomerNumber(), "Account Statement", "", channel, messageSource.getMessage("appMessages.account.statement.noentry", new Object[]{requestPayload.getStartDate(), requestPayload.getEndDate()}, Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                return response;
            }

            String[] detailsSplitString = splitString[1].replace("	", "*").replace("\",\"", "|").replace("\"", "").split("\\|");
            openingBalance = BigDecimal.ZERO;
            currentBalance = openingBalance;
            int i;
            for (i = 0; i < detailsSplitString.length; i++) {
                String[] fields = detailsSplitString[i].split("\\*");
                if (fields.length >= 7) {
                    AccountStatementPayload accountStatementPayload = new AccountStatementPayload();
                    accountStatementPayload.setId(i);
                    accountStatementPayload.setValueDate(fields[0].trim());
                    accountStatementPayload.setTransDate(fields[3].trim());
                    accountStatementPayload.setCredits(fields[4].trim());
                    accountStatementPayload.setDebits(fields[5].trim());
                    accountStatementPayload.setBalance(fields[6].trim());
                    accountStatementPayload.setRemarks(fields[1] + " " + detailsSplitString[i + 1]);
                    accountStatementPayload.setVoucherNumber("");
                    accountStatementPayload.setReferenceNo(fields[2]);
                    BigDecimal debit = BigDecimal.ZERO;
                    BigDecimal credit = BigDecimal.ZERO;
                    if (fields[5].trim().replace(",", "").length() != 0) {
                        debit = new BigDecimal(fields[5].trim().replace(",", ""));
                        currentBalance = currentBalance.subtract(debit.abs());
                        totalDebitCount++;
                        totalDebit = totalDebit.subtract(debit);
                    }
                    if (fields[4].trim().replace(",", "").length() != 0) {
                        credit = new BigDecimal(fields[4].trim().replace(",", ""));
                        currentBalance = currentBalance.add(credit);
                        totalCreditCount++;
                        totalCredit = totalCredit.add(credit);
                    }
                    reportCollection.add(accountStatementPayload);
                }
            }
            AccountStatementPayload data = new AccountStatementPayload();
            data.setId(99999);
            data.setTotalCreditCount(totalCreditCount);
            data.setTotalDebitCount(totalDebitCount);
            data.setTotalCredit(totalCredit);
            data.setTotalDebit(totalDebit);
            data.setOpeningBalance(openingBalance.toString());
            data.setClosingBalance(openingBalance.add(totalCredit).subtract(totalDebit).toString());
            data.setReferenceNo("Summary");

            // The transSummary should not be added to the list.
//            reportCollection.add(data);
            reportCollection.sort(Comparator.comparingInt(AccountStatementPayload::getId));

            // Sort the list by date
            reportCollection = reportCollection
                    .stream()
                    .map(object -> {
                        AccountStatementPayload payload = object;
                        String properDate = genericService
                                .getDateStringFromUnformattedDate(payload.getTransDate());
                        payload.setTransDate(properDate);
                        return payload;
                    })
                    .sorted(Comparator.comparing(load -> LocalDate
                    .parse(((AccountStatementPayload) load).getTransDate())).reversed())
                    .collect(Collectors.toList());

            // Now format dynamically
            reportCollection = getDynamicAccountCollection(reportCollection, dynamicQuery);

            AccountStatementResponsePayload statementResponse = new AccountStatementResponsePayload();
            statementResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            statementResponse.setData(reportCollection);
            statementResponse.setBalanceSummary(data);
            statementResponse.setStamp(stamp);

            response = gson.toJson(statementResponse);
            return response;
        } catch (Exception ex) {
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            response = gson.toJson(errorResponse);

            //Log the error
            genericService.generateLog("Account Statement", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Account Statement", "", channel, ex.getMessage(), requestPayload.getMobileNumber(), 'F');
            return response;
        }
    }

    private DynamicAccountStatementRequestPayload updateDynamicRequestDate(DynamicAccountStatementRequestPayload requestPayload) {

        // Get the start and the end date
        String startDateString = requestPayload.getStartDate();
        String endDateString = requestPayload.getEndDate();

        if (endDateString == null) {
            endDateString = LocalDate.now().toString();
        }
        if (startDateString == null) {
            startDateString = LocalDate.now().minusYears(20).toString();
        }

        DynamicAccountStatementRequestPayload res = new DynamicAccountStatementRequestPayload();
        res.setStartDate(startDateString);
        res.setEndDate(endDateString);
        res.setHash(requestPayload.getHash());
        res.setAccountType(requestPayload.getAccountType());
        res.setRequestId(requestPayload.getRequestId());
        res.setAccountNumber(requestPayload.getAccountNumber());
        res.setMobileNumber(requestPayload.getMobileNumber());
        return res;
    }

    private List<AccountStatementPayload> getDynamicAccountCollection(
            List<AccountStatementPayload> reportCollection,
            DynamicQuery dynamicQuery
    ) {
        List<AccountStatementPayload> result;
        result = reportCollection.stream().limit(dynamicQuery.getSize()).collect(Collectors.toList());
        return result;
    }
}

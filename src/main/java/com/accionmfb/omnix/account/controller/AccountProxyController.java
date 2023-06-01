package com.accionmfb.omnix.account.controller;

import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_BALANCE;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_BALANCES;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_DETAILS;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_OPENING;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_STATEMENT;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_STATEMENT_DYNAMIC;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_STATEMENT_OFFICIAL;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_TYPE;
import static com.accionmfb.omnix.account.constant.ApiPaths.ADD_POSTING_RESTRICT;
import static com.accionmfb.omnix.account.constant.ApiPaths.BRANCH_ACCOUNT_OFFICERS;
import static com.accionmfb.omnix.account.constant.ApiPaths.BRANCH_LIST;
import static com.accionmfb.omnix.account.constant.ApiPaths.CUSTOMER_ACCOUNTS;
import static com.accionmfb.omnix.account.constant.ApiPaths.FETCH_POSTING_RESTRICT;
import static com.accionmfb.omnix.account.constant.ApiPaths.HEADER_STRING;
import static com.accionmfb.omnix.account.constant.ApiPaths.MINI_ACCOUNT_STATEMENT;
import static com.accionmfb.omnix.account.constant.ApiPaths.POSTING_RESTRICTIONS;
import static com.accionmfb.omnix.account.constant.ApiPaths.PRODUCT_LIST;
import static com.accionmfb.omnix.account.constant.ApiPaths.REMOVE_POSTING_RESTRICT;
import static com.accionmfb.omnix.account.constant.ApiPaths.STATISTICS_MEMORY;
import static com.accionmfb.omnix.account.constant.ApiPaths.TOKEN_PREFIX;
import static com.accionmfb.omnix.account.constant.ApiPaths.WALLET_ACCOUNT_OPENING;
import com.accionmfb.omnix.account.constant.ResponseCodes;
import com.accionmfb.omnix.account.exception.ExceptionResponse;
import com.accionmfb.omnix.account.jwt.JwtTokenUtil;
import com.accionmfb.omnix.account.payload.AccountNumberPayload;
import com.accionmfb.omnix.account.payload.AccountOpeningRequestPayload;
import com.accionmfb.omnix.account.payload.AccountStatementRequestPayload;
import com.accionmfb.omnix.account.payload.AccountandMobileRequestPayload;
import com.accionmfb.omnix.account.payload.DynamicAccountStatementRequestPayload;
import com.accionmfb.omnix.account.payload.DynamicQuery;
import com.accionmfb.omnix.account.payload.GenericPayload;
import com.accionmfb.omnix.account.payload.MemoryStats;
import com.accionmfb.omnix.account.payload.MobileNumberPayload;
import com.accionmfb.omnix.account.payload.RestrictionRequestPayload;
import com.accionmfb.omnix.account.payload.ValidationPayload;
import com.accionmfb.omnix.account.security.AesService;
import com.accionmfb.omnix.account.security.LogService;
import com.accionmfb.omnix.account.security.PgpService;
import com.accionmfb.omnix.account.service.AccountService;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author dofoleta
 */
@RestController
@RequestMapping(value = "/proxy")
@Tag(name = "AccountProxy", description = "Account PROXY REST API")
@RefreshScope
public class AccountProxyController {

    @Autowired
    AccountService accountService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    LogService logService;
    @Autowired
    Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    PgpService pgpService;
    @Autowired
    AesService aesService;

    @Value("${security.pgp.encryption.publicKey}")
    private String recipientPublicKeyFile;

    @Value("${security.aes.encryption.key}")
    private String aesEncryptionKey;

    @Value("${security.option}")
    private String securityOption;

    private ValidationPayload validateChannelAndRequest(String role, GenericPayload requestPayload, String token) {
        ExceptionResponse exResponse = new ExceptionResponse();
        boolean userHasRole = jwtToken.userHasRole(token, role);
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));
            String exceptionJson = gson.toJson(exResponse);
            logService.logInfo("Create Individual CustomerW ith Bvn", token, messageSource.getMessage("appMessages.user.hasnorole", new Object[]{0}, Locale.ENGLISH), "API Response", exceptionJson);
            ValidationPayload validatorPayload = new ValidationPayload();
            if (securityOption.equalsIgnoreCase("AES")) {
                validatorPayload.setResponse(aesService.encryptFlutterString(exceptionJson, aesEncryptionKey));
            } else {
                validatorPayload.setResponse(pgpService.encryptString(exceptionJson, recipientPublicKeyFile));
            }
        }
        if (securityOption.equalsIgnoreCase("AES")) {
            return aesService.validateRequest(requestPayload);
        }
        return pgpService.validateRequest(requestPayload);
    }

    /*
    Authenticate IMEI before processing request for new customers
    Capture the IMEI for existing customer at login - do this one time for each customer.
     */
    @Operation(summary = "Get mini account statement")
    @PostMapping(value = MINI_ACCOUNT_STATEMENT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> miniAccountStatement(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("MINI_ACCOUNT_STATEMENT", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            AccountandMobileRequestPayload oAccountandMobileRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), AccountandMobileRequestPayload.class);
            String response = accountService.getMiniAccountStatement(token, oAccountandMobileRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }

            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }

    }

    @Operation(summary = "Get account statement")
    @PostMapping(value = ACCOUNT_STATEMENT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> accountStatement(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");

        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT", requestPayload, token);
        //Check if the user has role
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            AccountStatementRequestPayload oAccountStatementRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), AccountStatementRequestPayload.class);
            String response = accountService.getAccountStatement(token, oAccountStatementRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }

            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }

    }

    @Operation(summary = "Get account statement official")
    @PostMapping(value = ACCOUNT_STATEMENT_OFFICIAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> accountStatementOfficial(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT_OFFICIAL", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            AccountStatementRequestPayload oAccountStatementRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), AccountStatementRequestPayload.class);
            String response = accountService.getAccountStatementOfficial(token, oAccountStatementRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @Operation(summary = "Get account statement")
    @PostMapping(value = ACCOUNT_STATEMENT_DYNAMIC, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> accountStatementDynamic(  @Valid @RequestBody GenericPayload requestPayload,  @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "startDate", required = false) String startDate,    @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "timeQuery", required = false) String timeQuery,  @RequestParam(value = "backTimeValue", required = false) Integer backTimeValue, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_STATEMENT_OFFICIAL", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            DynamicAccountStatementRequestPayload oDynamicAccountStatementRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), DynamicAccountStatementRequestPayload.class);
            DynamicQuery dynamicQuery = new DynamicQuery(size, startDate, endDate, timeQuery, backTimeValue);
            String response = accountService.getAccountStatementDynamic(token, dynamicQuery, oDynamicAccountStatementRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }

            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @PostMapping(value = ACCOUNT_BALANCE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get account balance")
    public ResponseEntity<Object> accountBalance(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_BALANCE", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            AccountNumberPayload oAccountNumberPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), AccountNumberPayload.class);
            String response = accountService.getAccountBalance(token, oAccountNumberPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @Operation(summary = "Get account balances")
    @PostMapping(value = ACCOUNT_BALANCES, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> accountBalances(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");

        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_BALANCES", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            MobileNumberPayload oMobileNumberPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), MobileNumberPayload.class);
            String response = accountService.getAccountBalances(token, oMobileNumberPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                try {
                    String res=aesService.encryptFlutterString(response, aesEncryptionKey);
                    return new ResponseEntity<>(res, HttpStatus.OK);                
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @Operation(summary = "Get account details")
    @PostMapping(value = ACCOUNT_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> accountDetails(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");

//Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_DETAILS", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            AccountNumberPayload oAccountNumberPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), AccountNumberPayload.class);
            String response = accountService.getAccountDetails(token, oAccountNumberPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @Operation(summary = "New account without BVN")
    @PostMapping(value = ACCOUNT_OPENING, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> accountOpening(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_OPENING", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            AccountOpeningRequestPayload oAccountOpeningRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), AccountOpeningRequestPayload.class);
            String response = accountService.processAccountOpening(token, oAccountOpeningRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @Operation(summary = "List of Products")
    @GetMapping(value = ACCOUNT_TYPE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getProduct(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        String response = accountService.getProducts(token);
        if (securityOption.equalsIgnoreCase("AES")) {
            return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
        }
        return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
    }

    @PostMapping(value = CUSTOMER_ACCOUNTS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get account details")
    public ResponseEntity<Object> globalaccount(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("ACCOUNT_GLOBAL", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            MobileNumberPayload oMobileNumberPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), MobileNumberPayload.class);
            String response = accountService.getCustomerGlobalAccounts(token, oMobileNumberPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @Operation(summary = "New account without BVN")
    @PostMapping(value = WALLET_ACCOUNT_OPENING, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> walletaccountOpening(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("WALLET_ACCOUNT_OPENING", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            AccountOpeningRequestPayload oAccountOpeningRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), AccountOpeningRequestPayload.class);
            String response = accountService.processWalletAccountOpening(token, oAccountOpeningRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @PostMapping(value = ADD_POSTING_RESTRICT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add Posting Restriction")
    public ResponseEntity<Object> addPostingRestriction(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("POSTING_RESTRICTION", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            RestrictionRequestPayload oRestrictionRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), RestrictionRequestPayload.class);
            String response = accountService.processAddPostingRestriction(token, oRestrictionRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @PostMapping(value = REMOVE_POSTING_RESTRICT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "New account without BVN")
    public ResponseEntity<Object> removePostingRestriction(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("POSTING_RESTRICTION", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            RestrictionRequestPayload oRestrictionRequestPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), RestrictionRequestPayload.class);
            String response = accountService.processAddPostingRestriction(token, oRestrictionRequestPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @GetMapping(value = POSTING_RESTRICTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch list of posting restrictions")
    public ResponseEntity<Object> getPostingRestrictions(HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "POSTING_RESTRICTION");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String response = gson.toJson(exResponse);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }

        //Valid request
        String response = accountService.processFetchPostingRestriction(token);
        if (securityOption.equalsIgnoreCase("AES")) {
            return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
        }
        return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
    }

    @PostMapping(value = FETCH_POSTING_RESTRICT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch Customer Posting Restriction")
    public ResponseEntity<Object> fetchPostingRestriction(@Valid @RequestBody GenericPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        ValidationPayload oValidatorPayload = validateChannelAndRequest("POSTING_RESTRICTION", requestPayload, token);
        if (oValidatorPayload.isError()) {
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(oValidatorPayload.getResponse(), aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(oValidatorPayload.getResponse(), recipientPublicKeyFile), HttpStatus.OK);
        } else {
            //Valid request
            AccountNumberPayload oAccountNumberPayload = gson.fromJson(oValidatorPayload.getPlainTextPayload(), AccountNumberPayload.class);
            String response = accountService.processFetchPostingRestriction(token, oAccountNumberPayload);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }
    }

    @GetMapping(value = BRANCH_LIST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch branch list")
    public ResponseEntity<Object> getBranchList(HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_OPENING");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String response = gson.toJson(exResponse);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }

        //Valid request
        String response = accountService.processFetchBranches(token);
        if (securityOption.equalsIgnoreCase("AES")) {
            return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
        }
        return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
    }

    @GetMapping(value = BRANCH_ACCOUNT_OFFICERS, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ftech branch account officers")
    public ResponseEntity<Object> getBranchaccountOfficers(HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_OPENING");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String response = gson.toJson(exResponse);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }

        //Valid request
        String response = accountService.processFetchBranchAccountOfficers(token);
        if (securityOption.equalsIgnoreCase("AES")) {
            return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
        }
        return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
    }

    @GetMapping(value = PRODUCT_LIST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch products")
    public ResponseEntity<Object> getProducts(HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_OPENING");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String response = gson.toJson(exResponse);
            if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
        }

        //Valid request
        String response = accountService.processFetchProducts(token);
        if (securityOption.equalsIgnoreCase("AES")) {
                return new ResponseEntity<>(aesService.encryptFlutterString(response, aesEncryptionKey), HttpStatus.OK);
            }
            return new ResponseEntity<>(pgpService.encryptString(response, recipientPublicKeyFile), HttpStatus.OK);
    }

    @GetMapping(value = STATISTICS_MEMORY, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch the JVM statistics")
    public MemoryStats getMemoryStatistics(HttpServletRequest httpRequest) {
        MemoryStats stats = new MemoryStats();
        stats.setHeapSize(Runtime.getRuntime().totalMemory());
        stats.setHeapMaxSize(Runtime.getRuntime().maxMemory());
        stats.setHeapFreeSize(Runtime.getRuntime().freeMemory());
        return stats;
    }
}

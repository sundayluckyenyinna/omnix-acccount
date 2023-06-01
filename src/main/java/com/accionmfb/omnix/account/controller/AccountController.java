/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.controller;

import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_BALANCE;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_BALANCES;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_DETAILS;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_OPENING;
import static com.accionmfb.omnix.account.constant.ApiPaths.ACCOUNT_STATEMENT;
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
import com.accionmfb.omnix.account.payload.MemoryStats;
import com.accionmfb.omnix.account.payload.MobileNumberPayload;
import com.accionmfb.omnix.account.payload.RestrictionRequestPayload;
import com.accionmfb.omnix.account.service.AccountService;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author bokon
 */
@RestController
@Tag(name = "account", description = "account Microservice REST API")
@RefreshScope
public class AccountController {

    @Autowired
    AccountService accountService;
    @Autowired
    MessageSource messageSource;
    private Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;
    Logger logger = LoggerFactory.getLogger(AccountController.class);

    AccountController() {
        gson = new Gson();
    }

    @PostMapping(value = MINI_ACCOUNT_STATEMENT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get mini account statement")
    public ResponseEntity<Object> miniAccountStatement(@Valid @RequestBody AccountandMobileRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_STATEMENT");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        //Check if the request is valid
        boolean payloadValid = accountService.validateAccountMobileNumberPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.getMiniAccountStatement(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = ACCOUNT_STATEMENT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get account statement")
    public ResponseEntity<Object> accountStatement(@Valid @RequestBody AccountStatementRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_STATEMENT");
        System.out.println("User has role: " + userHasRole);
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        //Check if the request is valid
        boolean payloadValid = accountService.validateAccountStatementPayload(token, requestPayload);
        System.out.println("Request is valid: " + payloadValid);
        if (true) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.getAccountStatement(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = ACCOUNT_STATEMENT_OFFICIAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get account statement official")
    public ResponseEntity<Object> accountStatementOfficial(@Valid @RequestBody AccountStatementRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_STATEMENT");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        //Check if the request is valid
        boolean payloadValid = accountService.validateAccountStatementPayload(token, requestPayload);
        if (true) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.getAccountStatementOfficial(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = ACCOUNT_BALANCE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get account balance")
    public ResponseEntity<Object> accountBalance(@Valid @RequestBody AccountNumberPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_BALANCE");        
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = accountService.validateAccountBalancePayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.getAccountBalance(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = ACCOUNT_BALANCES, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get account balances")
    public ResponseEntity<Object> accountBalances(@Valid @RequestBody MobileNumberPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_BALANCE");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = accountService.validateAccountBalancesPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.getAccountBalances(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = ACCOUNT_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get account details")
    public ResponseEntity<Object> accountDetails(@Valid @RequestBody AccountNumberPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");       
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_DETAILS");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = accountService.validateAccountDetailsPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.getAccountDetails(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = ACCOUNT_OPENING, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "New account without BVN")
    public ResponseEntity<Object> accountOpening(@Valid @RequestBody AccountOpeningRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_OPENING");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = accountService.validateAccountOpeningRequestPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
//                ITransaction transaction = Sentry.startTransaction("processOrderBatch()", "task");
//                try {                    String response = accountService.processAccountOpening(token, requestPayload);
//                    return new ResponseEntity<>(response, HttpStatus.OK);
//                } catch (Exception e) {
//                    transaction.setThrowable(e);
//                    transaction.setStatus(SpanStatus.INTERNAL_ERROR);
//                    throw e;
//                } finally {
//                    transaction.finish();
//                }
                String response = accountService.processAccountOpening(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @GetMapping(value = ACCOUNT_TYPE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List of Products")
    public ResponseEntity<Object> getProduct(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        String response = accountService.getProducts(token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = CUSTOMER_ACCOUNTS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get account details")
    public ResponseEntity<Object> globalaccount(@Valid @RequestBody MobileNumberPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");      
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ACCOUNT_GLOBAL");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = accountService.validateMobileNumberPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.getCustomerGlobalAccounts(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = WALLET_ACCOUNT_OPENING, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "New account without BVN")
    public ResponseEntity<Object> walletaccountOpening(@Valid @RequestBody AccountOpeningRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "WALLET_ACCOUNT_OPENING");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = accountService.validateAccountOpeningRequestPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.processWalletAccountOpening(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = ADD_POSTING_RESTRICT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add Posting Restriction")
    public ResponseEntity<Object> addPostingRestriction(@Valid @RequestBody RestrictionRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "POSTING_RESTRICTION");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = accountService.validatePostingRestrictionRequestPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSamePostingRestrictionRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.processAddPostingRestriction(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = REMOVE_POSTING_RESTRICT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "New account without BVN")
    public ResponseEntity<Object> removePostingRestriction(@Valid @RequestBody RestrictionRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "POSTING_RESTRICTION");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = accountService.validatePostingRestrictionRequestPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSamePostingRestrictionRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.processRemovePostingRestriction(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
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

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        //Valid request
        String response = accountService.processFetchPostingRestriction(token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = FETCH_POSTING_RESTRICT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch Customer Posting Restriction")
    public ResponseEntity<Object> fetchPostingRestriction(@Valid @RequestBody AccountNumberPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "POSTING_RESTRICTION");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = accountService.validateAccountBalancePayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = accountService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = accountService.processFetchPostingRestriction(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
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

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        //Valid request
        String response = accountService.processFetchBranches(token);
        return new ResponseEntity<>(response, HttpStatus.OK);
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

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        //Valid request
        String response = accountService.processFetchBranchAccountOfficers(token);
        return new ResponseEntity<>(response, HttpStatus.OK);
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

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        //Valid request
        String response = accountService.processFetchProducts(token);
        return new ResponseEntity<>(response, HttpStatus.OK);
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.service;

import com.accionmfb.omnix.account.payload.AccountNumberPayload;
import com.accionmfb.omnix.account.payload.AccountOpeningRequestPayload;
import com.accionmfb.omnix.account.payload.AccountStatementRequestPayload;
import com.accionmfb.omnix.account.payload.AccountandMobileRequestPayload;
import com.accionmfb.omnix.account.payload.DynamicAccountStatementRequestPayload;
import com.accionmfb.omnix.account.payload.DynamicQuery;
import com.accionmfb.omnix.account.payload.MobileNumberPayload;
import com.accionmfb.omnix.account.payload.RestrictionRequestPayload;

/**
 *
 * @author bokon
 */
public interface AccountService {

    boolean validateAccountBalancePayload(String token, AccountNumberPayload requestPayload);

    String getAccountBalance(String token, AccountNumberPayload requestPayload);

    boolean validateAccountBalancesPayload(String token, MobileNumberPayload requestPayload);

    String getAccountBalances(String token, MobileNumberPayload requestPayload);

    boolean validateAccountDetailsPayload(String token, AccountNumberPayload requestPayload);

    String getAccountDetails(String token, AccountNumberPayload requestPayload);

    String getProducts(String token);

    boolean validateAccountOpeningRequestPayload(String token, AccountOpeningRequestPayload requestPayload);

    String processAccountOpening(String token, AccountOpeningRequestPayload requestPayload);

    boolean validateMobileNumberPayload(String token, MobileNumberPayload requestPayload);

    String getCustomerGlobalAccounts(String token, MobileNumberPayload requestPayload);

    String processWalletAccountOpening(String token, AccountOpeningRequestPayload requestPayload);

    boolean validateAccountStatementPayload(String token, AccountStatementRequestPayload requestPayload);

    String getAccountStatement(String token, AccountStatementRequestPayload requestPayload);

    String getAccountStatementOfficial(String token, AccountStatementRequestPayload requestPayload);

    boolean validateAccountMobileNumberPayload(String token, AccountandMobileRequestPayload requestPayload);
    
    String getMiniAccountStatement(String token, AccountandMobileRequestPayload requestPayload);

    Object checkIfSameRequestId(String requestId);

    Object checkIfSamePostingRestrictionRequestId(String requestId);

    boolean validatePostingRestrictionRequestPayload(String token, RestrictionRequestPayload requestPayload);

    String processAddPostingRestriction(String token, RestrictionRequestPayload requestPayload);

    String processRemovePostingRestriction(String token, RestrictionRequestPayload requestPayload);

    String processFetchPostingRestriction(String token);

    String processFetchBranchAccountOfficers(String token);

    String processFetchBranches(String token);

    String processFetchProducts(String token);

    String processFetchPostingRestriction(String token, AccountNumberPayload requestPayload);
    public String getAccountStatementDynamic(String token, DynamicQuery dynamicQuery, DynamicAccountStatementRequestPayload requestPayload);

}

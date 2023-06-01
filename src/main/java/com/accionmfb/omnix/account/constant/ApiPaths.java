/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.constant;

/**
 *
 * @author bokon
 */
public class ApiPaths {

    /**
     * This class includes the name and API end points of other microservices
     * that we need to communicate. NOTE: WRITE EVERYTHING IN ALPHABETICAL ORDER
     */
    //A
    public static final String ACCOUNT_BALANCE = "/balance";
    public static final String ACCOUNT_BALANCES = "/balances";
    public static final String ACCOUNT_DETAILS = "/details";
    public static final String ACCOUNT_FOR_EXISTING_CUSTOMER = "/existing";
    public static final String ACCOUNT_OPENING = "/new";
    public static final String ACCOUNT_TYPE = "/products";
    public static final String ACCOUNT_STATEMENT = "/statement";
    public static final String ACCOUNT_STATEMENT_OFFICIAL = "/statement/official";
    public static final String MINI_ACCOUNT_STATEMENT = "/statement/mini";
    public static final String ADD_POSTING_RESTRICT = "/restriction/add";
    public static final String ACCOUNT_STATEMENT_DYNAMIC = "/statement/dynamic";
    //B
    public static final String BASE_API = "/omnix/api";
    public static final String BRANCH_LIST = "/branch/list";
    public static final String BRANCH_ACCOUNT_OFFICERS = "/branch/account-officer";
    //C
    public static final String CREATE_CUSTOMER = "/customer/new";
    public static final String CUSTOMER_ACCOUNTS = "/global";
    //D
    //E
    //F
    public static final String FETCH_POSTING_RESTRICT = "/customer/restrictions";
    //G
    //H
    public static final String HEADER_STRING = "Authorization";
    //L
    public static final String LOCAL_TRANSFER = "/local";
    public static final String LOCAL_TRANSFER_WITH_PL_INTERNAL = "/local/internal-account";
    //O
    //P
    public static final String POSTING_RESTRICTIONS = "/restriction/list";
    public static final String PRODUCT_LIST = "/product/list";
    //M
    public static final String STATISTICS_MEMORY = "/actuator/stats";
    //R
    public static final String REMOVE_POSTING_RESTRICT = "/restriction/remove";
    //S
    public static final String SMS_NOTIFICATION = "/sms/send";
    //T
    public static final String TOKEN_PREFIX = "Bearer";
    //W
    public static final String WALLET_ACCOUNT_OPENING = "/new/wallet";
}

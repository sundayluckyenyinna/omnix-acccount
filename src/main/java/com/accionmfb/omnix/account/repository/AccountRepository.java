/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.repository;

import com.accionmfb.omnix.account.model.Account;
import com.accionmfb.omnix.account.model.AccountOfficer;
import com.accionmfb.omnix.account.model.AppUser;
import com.accionmfb.omnix.account.model.BVN;
import com.accionmfb.omnix.account.model.Branch;
import com.accionmfb.omnix.account.model.Customer;
import com.accionmfb.omnix.account.model.CustomerAccountRestrictions;
import com.accionmfb.omnix.account.model.PostingRestriction;
import com.accionmfb.omnix.account.model.Product;
import com.accionmfb.omnix.account.model.UserActivity;
import java.util.List;

/**
 *
 * @author bokon
 */
public interface AccountRepository {

    public Customer getCustomerUsingCustomerId(long id);
    public List<Account> getAccountUsingStatus(String status);
    UserActivity createUserActivity(UserActivity userActivity);

    List<Product> getProducts();

    Product getProductUsingProductCode(String productCode);

    Account getAccountUsingAccountNumber(String accountNumber);

    Account getAccountUsingProduct(Customer customer, Product product);

    Customer getCustomerUsingCustomerNumber(String customerNumber);

    Customer getCustomerUsingMobileNumber(String mobileNumber);

    Branch getBranchUsingBranchCode(String branchCode);

    Account createAccount(Account account);

    Account updateAccount(Account account);

    Customer getCustomerUsingBVN(BVN bvn);

    BVN getBVN(String bvn);

    AppUser getAppUserUsingUsername(String username);

    List<Account> getCustomerAccounts(Customer customer);

    Account getRecordUsingRequestId(String requestId);

    CustomerAccountRestrictions getPostingRestrictionRecordUsingRequestId(String requestId);

    PostingRestriction getPostingRestrictionUsingName(String postingRestrictionName);

    PostingRestriction getPostingRestrictionUsingId(String postingRestrictionId);

    List<PostingRestriction> getPostingRestrictions();

    List<Branch> getBranches();

    List<Branch> getBranches(String state);

    List<AccountOfficer> getBranchAccountOfficers(Branch branch);

    Account getCustomerAccount(Customer customer, String accountNumber);

    CustomerAccountRestrictions createPostingRestriction(CustomerAccountRestrictions customerAccountRestriction);

    CustomerAccountRestrictions updatePostingRestriction(CustomerAccountRestrictions customerAccountRestriction);
    List<Account> getIntermediateAccounts(String  status);
}

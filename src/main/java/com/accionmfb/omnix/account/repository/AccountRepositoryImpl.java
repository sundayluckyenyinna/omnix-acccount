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
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author bokon
 */
@Repository
@Transactional
public class AccountRepositoryImpl implements AccountRepository {

    @PersistenceContext
    EntityManager em;

    @Override
    public UserActivity createUserActivity(UserActivity userActivity) {
        em.persist(userActivity);
        em.flush();
        return userActivity;
    }

    @Override
    public List<Product> getProducts() {
        TypedQuery<Product> query = em.createQuery("SELECT t FROM Product t", Product.class);
        List<Product> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Product getProductUsingProductCode(String productCode) {
        TypedQuery<Product> query = em.createQuery("SELECT t FROM Product t WHERE t.categoryCode = :productCode", Product.class)
                .setParameter("productCode", productCode);
        List<Product> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Account getAccountUsingAccountNumber(String accountNumber) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.accountNumber = :accountNumber OR t.oldAccountNumber = :accountNumber", Account.class)
                .setParameter("accountNumber", accountNumber);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Account> getAccountUsingStatus(String status) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.status = :status or t.status = 'Intermediate'", Account.class)
                .setParameter("status", status);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Account getAccountUsingProduct(Customer customer, Product product) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.customer = :customer AND t.product = :product", Account.class)
                .setParameter("product", product)
                .setParameter("customer", customer);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Customer getCustomerUsingCustomerNumber(String customerNumber) {
        TypedQuery<Customer> query = em.createQuery("SELECT t FROM Customer t WHERE t.customerNumber = :customerNumber", Customer.class)
                .setParameter("customerNumber", customerNumber);
        List<Customer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Customer getCustomerUsingCustomerId(long id) {
        TypedQuery<Customer> query = em.createQuery("SELECT t FROM Customer t WHERE t.id = :id", Customer.class)
                .setParameter("id", id);
        List<Customer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Customer getCustomerUsingMobileNumber(String mobileNumber) {
        TypedQuery<Customer> query = em.createQuery("SELECT t FROM Customer t WHERE t.mobileNumber = :mobileNumber", Customer.class)
                .setParameter("mobileNumber", mobileNumber);
        List<Customer> record = query.getResultList();
        if (record.isEmpty()) {
            // check in T24. If foud, create in Omnix and pull-in all associated accounts from T24.

            return null;
        }
        return record.get(0);
    }

    @Override
    public Branch getBranchUsingBranchCode(String branchCode) {
        TypedQuery<Branch> query = em.createQuery("SELECT t FROM Branch t WHERE t.branchCode = :branchCode", Branch.class)
                .setParameter("branchCode", branchCode);
        List<Branch> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Account createAccount(Account account) {
        em.persist(account);
        em.flush();
        return account;
    }

    @Override
    public Account updateAccount(Account account) {
        em.merge(account);
        em.flush();
        return account;
    }

    @Override
    public BVN getBVN(String bvn) {
        TypedQuery<BVN> query = em.createQuery("SELECT t FROM BVN t WHERE t.customerBvn = :bvn", BVN.class)
                .setParameter("bvn", bvn);
        List<BVN> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Customer getCustomerUsingBVN(BVN bvn) {
        TypedQuery<Customer> query = em.createQuery("SELECT t FROM Customer t WHERE t.bvn = :bvn", Customer.class)
                .setParameter("bvn", bvn);
        List<Customer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public AppUser getAppUserUsingUsername(String username) {
        TypedQuery<AppUser> query = em.createQuery("SELECT t FROM AppUser t WHERE t.username = :username", AppUser.class)
                .setParameter("username", username);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Account> getCustomerAccounts(Customer customer) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.customer = :customer", Account.class)
                .setParameter("customer", customer);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Account getRecordUsingRequestId(String requestId) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.requestId = :requestId", Account.class)
                .setParameter("requestId", requestId);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public CustomerAccountRestrictions getPostingRestrictionRecordUsingRequestId(String requestId) {
        TypedQuery<CustomerAccountRestrictions> query = em.createQuery("SELECT t FROM CustomerAccountRestrictions t WHERE t.requestId = :requestId", CustomerAccountRestrictions.class)
                .setParameter("requestId", requestId);
        List<CustomerAccountRestrictions> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public PostingRestriction getPostingRestrictionUsingName(String postingRestrictionName) {
        TypedQuery<PostingRestriction> query = em.createQuery("SELECT t FROM PostingRestriction t WHERE t.postingRestrictionDesc = :postingRestrictionName", PostingRestriction.class)
                .setParameter("postingRestrictionName", postingRestrictionName);
        List<PostingRestriction> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<PostingRestriction> getPostingRestrictions() {
        TypedQuery<PostingRestriction> query = em.createQuery("SELECT t FROM PostingRestriction t", PostingRestriction.class);
        List<PostingRestriction> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Branch> getBranches() {
        TypedQuery<Branch> query = em.createQuery("SELECT t FROM Branch t", Branch.class);
        List<Branch> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<Branch> getBranches(String state) {
        TypedQuery<Branch> query = em.createQuery("SELECT t FROM Branch t WHERE t.branchState = :state", Branch.class)
                .setParameter("state", state);
        List<Branch> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<AccountOfficer> getBranchAccountOfficers(Branch branch) {
        TypedQuery<AccountOfficer> query = em.createQuery("SELECT t FROM AccountOfficer t WHERE t.branch = :branch", AccountOfficer.class)
                .setParameter("branch", branch);
        List<AccountOfficer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Account getCustomerAccount(Customer customer, String accountNumber) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.customer = :customer AND t.accountNumber = :accountNumber OR t.oldAccountNumber = :accountNumber", Account.class)
                .setParameter("customer", customer)
                .setParameter("accountNumber", accountNumber);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public CustomerAccountRestrictions createPostingRestriction(CustomerAccountRestrictions customerAccountRestriction) {
        em.persist(customerAccountRestriction);
        em.flush();
        return customerAccountRestriction;
    }

    @Override
    public CustomerAccountRestrictions updatePostingRestriction(CustomerAccountRestrictions customerAccountRestriction) {
        em.merge(customerAccountRestriction);
        em.flush();
        return customerAccountRestriction;
    }

    @Override
    public PostingRestriction getPostingRestrictionUsingId(String postingRestrictionId) {
        TypedQuery<PostingRestriction> query = em.createQuery("SELECT t FROM PostingRestriction t WHERE t.postingRestrictionId = :postingRestrictionId", PostingRestriction.class)
                .setParameter("postingRestrictionId", postingRestrictionId);
        List<PostingRestriction> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Account> getIntermediateAccounts(String status) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.status = :status", Account.class)
                .setParameter("status", status);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }
}

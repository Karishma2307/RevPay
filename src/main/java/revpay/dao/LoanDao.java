package revpay.dao;

import java.util.List;

import revpay.model.Loan;

public interface LoanDao {

    long createLoan(Loan loan);

    void updateLoan(Loan loan);

    Loan findById(long loanId);

    java.util.List<Loan> findByBusinessUser(long businessUserId);
}

package revpay.dao;

import java.util.List;

import revpay.model.LoanRepayment;

public interface LoanRepaymentDao {

    long createRepayment(LoanRepayment repayment);

    java.util.List<LoanRepayment> findByLoanId(long loanId);
}

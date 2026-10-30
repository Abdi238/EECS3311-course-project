package budgetbuddy.persistence;

import budgetbuddy.model.Transaction;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository {
    void saveAll(List<Transaction> txs);

    void update(Transaction tx);

    Transaction findById(long id);

    List<Transaction> findBetween(LocalDate from, LocalDate to);

    List<Transaction> findByMerchant(String merchant);

    boolean exists(Transaction tx);
}

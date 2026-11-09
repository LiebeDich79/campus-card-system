package com.campus.card.dao;

import com.campus.card.entity.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 账户读写。最早写成"先查余额、够就 UPDATE"，压测时两个请求同时读到旧余额，
 * 直接把卡刷成了负数；后来把判断和扣减并进一条 SQL 才修好，见 atomicDeduct()。
 * TODO 多实例部署时，这里的余额/积分更新还得配合 Redis 或分布式锁再评估。
 */
@Repository
public class AccountDao {
    private final JdbcTemplate jdbc;

    public AccountDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Account> MAPPER = new RowMapper<Account>() {
        @Override
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
            Account a = new Account();
            a.setId(rs.getLong("id"));
            a.setCardId(rs.getLong("card_id"));
            a.setBalance(rs.getBigDecimal("balance"));
            a.setPoints(rs.getInt("points"));
            a.setFrozenAmount(rs.getBigDecimal("frozen_amount"));
            a.setVersion(rs.getInt("version"));
            a.setUpdatedAt(rs.getTimestamp("updated_at"));
            return a;
        }
    };

    public Account findByCardId(Long cardId) {
        List<Account> list = jdbc.query("SELECT * FROM account WHERE card_id = ?", MAPPER, cardId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 开户时同步创建账户，初始余额由请求指定（默认 0）。 */
    public int create(Long cardId, BigDecimal balance) {
        return jdbc.update("INSERT INTO account (card_id, balance, points, frozen_amount, version) "
                + "VALUES (?,?,0,0.00,0)", cardId, balance);
    }

    /**
     * 原子扣款：把"判断余额"和"扣减余额"合并为一条带条件的 UPDATE，
     * 由数据库行锁保证并发安全；version 乐观锁避免丢失更新。
     *
     * @return 受影响行数，0 表示余额不足或版本冲突
     */
    public int atomicDeduct(Long cardId, BigDecimal amount, int expectedVersion) {
        return jdbc.update("UPDATE account SET balance = balance - ?, version = version + 1 "
                        + "WHERE card_id = ? AND balance >= ? AND version = ?",
                amount, cardId, amount, expectedVersion);
    }

    public int atomicAdd(Long cardId, BigDecimal amount) {
        return jdbc.update("UPDATE account SET balance = balance + ?, version = version + 1 WHERE card_id = ?",
                amount, cardId);
    }

    public int addPoints(Long cardId, int delta) {
        return jdbc.update("UPDATE account SET points = points + ? WHERE card_id = ?", delta, cardId);
    }

    /** 当日已消费金额，用于单日限额校验。 */
    public BigDecimal todayConsumed(Long cardId) {
        BigDecimal sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(actual_amount), 0) FROM transaction "
                        + "WHERE card_id = ? AND tx_type = 'CONSUME' AND status = 'SUCCESS' "
                        + "AND DATE(created_at) = CURDATE()", BigDecimal.class, cardId);
        return sum == null ? BigDecimal.ZERO : sum;
    }
}

package com.campus.card.dao;

import com.campus.card.entity.Transaction;
import com.campus.card.entity.TransactionItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 数据访问层：交易主表、明细、分页查询与统计。 */
@Repository
public class TransactionDao {
    private final JdbcTemplate jdbc;

    public TransactionDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Transaction> MAPPER = new RowMapper<Transaction>() {
        @Override
        public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
            Transaction t = new Transaction();
            t.setId(rs.getLong("id"));
            t.setTxNo(rs.getString("tx_no"));
            t.setCardId(rs.getLong("card_id"));
            t.setCardNo(rs.getString("card_no"));
            t.setTerminalId((Long) rs.getObject("terminal_id"));
            t.setTerminalNo(rs.getString("terminal_no"));
            t.setTxType(rs.getString("tx_type"));
            t.setAmount(rs.getBigDecimal("amount"));
            t.setDiscountAmount(rs.getBigDecimal("discount_amount"));
            t.setActualAmount(rs.getBigDecimal("actual_amount"));
            t.setStrategyType(rs.getString("strategy_type"));
            t.setStatus(rs.getString("status"));
            t.setIdempotencyKey(rs.getString("idempotency_key"));
            t.setOriginalTxNo(rs.getString("original_tx_no"));
            t.setRemark(rs.getString("remark"));
            t.setOperator(rs.getString("operator"));
            t.setCreatedAt(rs.getTimestamp("created_at"));
            t.setSettledAt(rs.getTimestamp("settled_at"));
            return t;
        }
    };

    public int insert(Transaction t) {
        return jdbc.update("INSERT INTO transaction (tx_no, card_id, card_no, terminal_id, terminal_no, tx_type, "
                        + "amount, discount_amount, actual_amount, strategy_type, status, idempotency_key, "
                        + "original_tx_no, remark, operator, settled_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                t.getTxNo(), t.getCardId(), t.getCardNo(), t.getTerminalId(), t.getTerminalNo(),
                t.getTxType(), t.getAmount(), t.getDiscountAmount(), t.getActualAmount(),
                t.getStrategyType(), t.getStatus(), t.getIdempotencyKey(), t.getOriginalTxNo(),
                t.getRemark(), t.getOperator(), t.getSettledAt());
    }

    public Long findIdByTxNo(String txNo) {
        return jdbc.queryForObject("SELECT id FROM transaction WHERE tx_no = ?", Long.class, txNo);
    }

    /** 建造者组装的明细一次性写入，与主表在同一个 @Transactional 事务内。 */
    public void insertItems(String txNo, List<TransactionItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Long txId = findIdByTxNo(txNo);
        for (TransactionItem item : items) {
            jdbc.update("INSERT INTO transaction_item (tx_id, tx_no, item_name, quantity, unit_price, amount) "
                            + "VALUES (?,?,?,?,?,?)",
                    txId, txNo, item.getItemName(), item.getQuantity(), item.getUnitPrice(), item.getAmount());
        }
    }

    public Transaction findByTxNo(String txNo) {
        List<Transaction> list = jdbc.query("SELECT * FROM transaction WHERE tx_no = ?", MAPPER, txNo);
        if (list.isEmpty()) {
            return null;
        }
        Transaction tx = list.get(0);
        tx.setItems(findItems(tx.getId()));
        return tx;
    }

    public Transaction findByIdempotencyKey(String key) {
        List<Transaction> list = jdbc.query("SELECT * FROM transaction WHERE idempotency_key = ?", MAPPER, key);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<TransactionItem> findItems(Long txId) {
        return jdbc.query("SELECT * FROM transaction_item WHERE tx_id = ? ORDER BY id",
                new RowMapper<TransactionItem>() {
                    @Override
                    public TransactionItem mapRow(ResultSet rs, int rowNum) throws SQLException {
                        TransactionItem i = new TransactionItem();
                        i.setId(rs.getLong("id"));
                        i.setTxId(rs.getLong("tx_id"));
                        i.setTxNo(rs.getString("tx_no"));
                        i.setItemName(rs.getString("item_name"));
                        i.setQuantity(rs.getInt("quantity"));
                        i.setUnitPrice(rs.getBigDecimal("unit_price"));
                        i.setAmount(rs.getBigDecimal("amount"));
                        return i;
                    }
                }, txId);
    }

    /** 多条件分页查询：参数全部使用占位符，按时间倒序。 */
    public List<Transaction> query(String cardNo, String terminalType, String startDate, String endDate,
                                   int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT t.* FROM transaction t LEFT JOIN terminal m "
                + "ON t.terminal_id = m.id WHERE 1 = 1");
        List<Object> args = new ArrayList<Object>();
        if (cardNo != null && cardNo.length() > 0) {
            sql.append(" AND t.card_no = ?");
            args.add(cardNo);
        }
        if (terminalType != null && terminalType.length() > 0) {
            sql.append(" AND m.terminal_type = ?");
            args.add(terminalType);
        }
        if (startDate != null && startDate.length() > 0) {
            sql.append(" AND t.created_at >= ?");
            args.add(startDate + " 00:00:00");
        }
        if (endDate != null && endDate.length() > 0) {
            sql.append(" AND t.created_at <= ?");
            args.add(endDate + " 23:59:59");
        }
        sql.append(" ORDER BY t.id DESC LIMIT ? OFFSET ?");
        args.add(size);
        args.add(Math.max(0, page - 1) * size);
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public long count(String cardNo, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM transaction WHERE 1 = 1");
        List<Object> args = new ArrayList<Object>();
        if (cardNo != null && cardNo.length() > 0) {
            sql.append(" AND card_no = ?");
            args.add(cardNo);
        }
        if (startDate != null && startDate.length() > 0) {
            sql.append(" AND created_at >= ?");
            args.add(startDate + " 00:00:00");
        }
        if (endDate != null && endDate.length() > 0) {
            sql.append(" AND created_at <= ?");
            args.add(endDate + " 23:59:59");
        }
        Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return n == null ? 0L : n;
    }

    public List<Map<String, Object>> groupByTerminal(String startDate, String endDate) {
        String sql = "SELECT COALESCE(m.terminal_type, 'UNKNOWN') AS terminal_type, COUNT(*) AS tx_count, "
                + "COALESCE(SUM(t.actual_amount), 0) AS total_amount FROM transaction t "
                + "LEFT JOIN terminal m ON t.terminal_id = m.id "
                + "WHERE t.status = 'SUCCESS' AND t.created_at >= ? AND t.created_at <= ? "
                + "GROUP BY COALESCE(m.terminal_type, 'UNKNOWN')";
        return jdbc.queryForList(sql, startDate + " 00:00:00", endDate + " 23:59:59");
    }

    public BigDecimal sumActual(String startDate, String endDate, String txType) {
        BigDecimal sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(actual_amount), 0) FROM transaction "
                        + "WHERE status = 'SUCCESS' AND tx_type = ? AND created_at >= ? AND created_at <= ?",
                BigDecimal.class, txType, startDate + " 00:00:00", endDate + " 23:59:59");
        return sum == null ? BigDecimal.ZERO : sum;
    }

    public int markReversed(String txNo) {
        return jdbc.update("UPDATE transaction SET status = 'REVERSED' WHERE tx_no = ? AND status = 'SUCCESS'", txNo);
    }

    public int updateSettled(String txNo) {
        return jdbc.update("UPDATE transaction SET settled_at = NOW() WHERE tx_no = ?", txNo);
    }
}

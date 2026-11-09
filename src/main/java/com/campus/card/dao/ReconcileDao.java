package com.campus.card.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public class ReconcileDao {
    private final JdbcTemplate jdbc;

    public ReconcileDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 观察者按交易实时累加终端当日汇总；ON DUPLICATE KEY UPDATE 保证幂等。 */
    public int accumulate(Long terminalId, String settleDate, BigDecimal amount) {
        return jdbc.update("INSERT INTO reconcile_record (terminal_id, settle_date, tx_count, total_amount, status) "
                        + "VALUES (?,?,1,?,'BALANCED') ON DUPLICATE KEY UPDATE "
                        + "tx_count = tx_count + 1, total_amount = total_amount + VALUES(total_amount)",
                terminalId, settleDate, amount);
    }

    /** 按终端与日期重算对账结果，返回差异笔数。 */
    public List<Map<String, Object>> settle(String startDate, String endDate) {
        return jdbc.queryForList(
                "SELECT m.terminal_no, m.terminal_type, DATE(t.created_at) AS settle_date, "
                        + "COUNT(*) AS tx_count, SUM(t.actual_amount) AS total_amount "
                        + "FROM transaction t JOIN terminal m ON t.terminal_id = m.id "
                        + "WHERE t.status = 'SUCCESS' AND t.tx_type IN ('CONSUME','REFUND') "
                        + "AND DATE(t.created_at) BETWEEN ? AND ? "
                        + "GROUP BY m.terminal_no, m.terminal_type, DATE(t.created_at) ORDER BY settle_date DESC",
                startDate, endDate);
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList("SELECT r.*, m.terminal_no FROM reconcile_record r "
                + "LEFT JOIN terminal m ON r.terminal_id = m.id ORDER BY r.settle_date DESC, r.id DESC");
    }
}

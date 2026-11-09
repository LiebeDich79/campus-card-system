package com.campus.card.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PointsLogDao {
    private final JdbcTemplate jdbc;

    public PointsLogDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(Long cardId, String txNo, int change, int after) {
        return jdbc.update("INSERT INTO points_log (card_id, tx_no, points_change, balance_after) "
                + "VALUES (?,?,?,?)", cardId, txNo, change, after);
    }
}

package com.campus.card.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class NotifyLogDao {
    private final JdbcTemplate jdbc;

    public NotifyLogDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(String txNo, String channel, String content) {
        return jdbc.update("INSERT INTO notify_log (tx_no, channel, content, status) VALUES (?,?,?,'SENT')",
                txNo, channel, content);
    }

    public List<Map<String, Object>> recent(int limit) {
        return jdbc.queryForList("SELECT * FROM notify_log ORDER BY id DESC LIMIT ?", limit);
    }
}

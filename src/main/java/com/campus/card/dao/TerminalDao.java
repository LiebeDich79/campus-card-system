package com.campus.card.dao;

import com.campus.card.entity.Terminal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class TerminalDao {
    private final JdbcTemplate jdbc;

    public TerminalDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Terminal> MAPPER = new RowMapper<Terminal>() {
        @Override
        public Terminal mapRow(ResultSet rs, int rowNum) throws SQLException {
            Terminal t = new Terminal();
            t.setId(rs.getLong("id"));
            t.setTerminalNo(rs.getString("terminal_no"));
            t.setTerminalType(rs.getString("terminal_type"));
            t.setLocation(rs.getString("location"));
            t.setVendor(rs.getString("vendor"));
            t.setSecret(rs.getString("secret"));
            t.setStatus(rs.getString("status"));
            t.setCreatedAt(rs.getTimestamp("created_at"));
            return t;
        }
    };

    public Terminal findByTerminalNo(String terminalNo) {
        List<Terminal> list = jdbc.query("SELECT * FROM terminal WHERE terminal_no = ?", MAPPER, terminalNo);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Terminal> findAll() {
        return jdbc.query("SELECT * FROM terminal ORDER BY id", MAPPER);
    }

    public boolean isValid(String terminalNo, String sign) {
        Terminal t = findByTerminalNo(terminalNo);
        if (t == null || !"ENABLED".equals(t.getStatus())) {
            return false;
        }
        // 真实项目中此处应校验 HMAC 签名；演示环境仅校验签名非空与密钥存在
        return sign != null && sign.length() > 0 && t.getSecret() != null;
    }
}

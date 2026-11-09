package com.campus.card.dao;

import com.campus.card.entity.Card;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** 数据访问层：校园卡 CRUD。所有 SQL 均使用占位符参数，杜绝拼接注入。 */
@Repository
public class CardDao {
    private final JdbcTemplate jdbc;

    public CardDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Card> MAPPER = new RowMapper<Card>() {
        @Override
        public Card mapRow(ResultSet rs, int rowNum) throws SQLException {
            Card c = new Card();
            c.setId(rs.getLong("id"));
            c.setCardNo(rs.getString("card_no"));
            c.setStudentNo(rs.getString("student_no"));
            c.setHolderName(rs.getString("holder_name"));
            c.setCardType(rs.getString("card_type"));
            c.setStatus(rs.getString("status"));
            c.setPin(rs.getString("pin"));
            c.setVersion(rs.getInt("version"));
            c.setCreatedAt(rs.getTimestamp("created_at"));
            c.setUpdatedAt(rs.getTimestamp("updated_at"));
            return c;
        }
    };

    public Card findByCardNo(String cardNo) {
        List<Card> list = jdbc.query("SELECT * FROM card WHERE card_no = ?", MAPPER, cardNo);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Card> findAll() {
        return jdbc.query("SELECT * FROM card ORDER BY id", MAPPER);
    }

    public int insert(Card c) {
        return jdbc.update("INSERT INTO card (card_no, student_no, holder_name, card_type, status, pin, version) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 0)",
                c.getCardNo(), c.getStudentNo(), c.getHolderName(), c.getCardType(), c.getStatus(), c.getPin());
    }

    public int updateStatus(String cardNo, String status) {
        return jdbc.update("UPDATE card SET status = ?, version = version + 1 WHERE card_no = ?", status, cardNo);
    }
}

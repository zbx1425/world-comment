package cn.zbx1425.worldcomment.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface StatementConsumer {

    void accept(PreparedStatement stmt) throws SQLException;
}

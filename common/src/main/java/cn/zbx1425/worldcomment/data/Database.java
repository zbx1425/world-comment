package cn.zbx1425.worldcomment.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.function.Consumer;

public class Database {

    public static Database INSTANCE;

    private final Connection dbConn;

    public final CommentTable comments;

    public final DimensionTable dimensions;

    public Database(Path dbPath) throws SQLException {
        dbConn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        comments = new CommentTable(this);
        dimensions = new DimensionTable(this);
        init();
    }

    public static void loadDatabase(MinecraftServer server) throws SQLException {
        INSTANCE = new Database(
                Path.of(server.getWorldPath(LevelResource.ROOT).toString(), "world-comment.db")
        );
    }

    public void close() throws SQLException {
        for (PreparedStatement stmt : preparedStatements.values()) {
            stmt.close();
        }
        preparedStatements.clear();
        dbConn.close();
    }

    private void init() throws SQLException {
        comments.init();
        dimensions.init();
    }

    private final HashMap<String, PreparedStatement> preparedStatements = new HashMap<>();

    public void execute(String command) throws SQLException {
        prepareStatement(command).execute();
    }

    public void execute(String command, StatementConsumer params) throws SQLException {
        PreparedStatement stmt = prepareStatement(command);
        params.accept(stmt);
        stmt.execute();
    }

    public ResultSet executeQuery(String command) throws SQLException {
        return prepareStatement(command).executeQuery();
    }

    public ResultSet executeQuery(String command, StatementConsumer params) throws SQLException {
        PreparedStatement stmt = prepareStatement(command);
        params.accept(stmt);
        return stmt.executeQuery();
    }

    private PreparedStatement prepareStatement(String command) throws SQLException {
        PreparedStatement stmt = preparedStatements.get(command);
        if (stmt == null) {
            stmt = dbConn.prepareStatement(command);
            preparedStatements.put(command, stmt);
        }
        return stmt;
    }
}

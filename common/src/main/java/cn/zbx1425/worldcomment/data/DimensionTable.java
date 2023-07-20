package cn.zbx1425.worldcomment.data;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.resources.ResourceLocation;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DimensionTable {

    public Database db;

    public Object2IntMap<ResourceLocation> dimensionMap = new Object2IntOpenHashMap<>();
    public ObjectList<ResourceLocation> idMap = new ObjectArrayList<>();

    public DimensionTable(Database db) {
        this.db = db;
    }

    public void init() throws SQLException {
        db.execute("""
            CREATE TABLE IF NOT EXISTS dimensions (
                id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                resourceLocation    TEXT
            );
            """);
        dimensionMap.clear();
        try (ResultSet result = db.executeQuery("SELECT * FROM dimensions;")) {
            while (result.next()) {
                ResourceLocation name = new ResourceLocation(result.getString(2));
                int id = result.getInt(1);
                dimensionMap.put(name, id);
                idMap.size(Math.max(idMap.size(), id));
                idMap.set(id - 1, name);
            }
        }
    }

    public int getMaxId() throws SQLException {
        try (ResultSet result = db.executeQuery("SELECT id FROM dimensions ORDER BY id DESC LIMIT 1;")) {
            if (!result.next()) {
                return 0;
            } else {
                return result.getInt(1);
            }
        }
    }

    public ResourceLocation getDimensionById(int id) {
        return idMap.get(id - 1);
    }

    public int getDimensionId(ResourceLocation dimension) throws SQLException {
        if (!dimensionMap.containsKey(dimension)) {
            db.execute("INSERT INTO dimensions (resourceLocation) VALUES (?);", params -> {
                params.setString(1, dimension.toString());
            });
            int newId = getMaxId();
            dimensionMap.put(dimension, newId);
            idMap.size(Math.max(idMap.size(), newId));
            idMap.set(newId - 1, dimension);
            return newId;
        } else {
            return dimensionMap.getInt(dimension);
        }
    }
}

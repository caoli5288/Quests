package me.blackvein.quests;

import com.mengcraft.simpleorm.MongoWrapper;
import com.mongodb.BasicDBObject;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;
import java.util.UUID;

public class Mongo {

    private static MongoWrapper mongoWrapper;
    private static Yaml yaml;

    public static void setMongoWrapper(MongoWrapper mongoWrapper) {
        Mongo.mongoWrapper = mongoWrapper;
        yaml = new Yaml();
    }

    public static boolean isEnabled() {
        return !(mongoWrapper == null);
    }

    public static Map<String, Object> loadQuest(UUID id) {
        return mongoWrapper.open("yzh", "yzh_quest").call(coll -> ((BasicDBObject) coll.findOne(new BasicDBObject("_id", id))));
    }

    public static void saveQuest(UUID id, String flat) {
        Map<String, Object> _map = (Map<String, Object>) yaml.load(flat);
        _map.put("_id", id);
        mongoWrapper.open("yzh", "yzh_quest").open(coll -> coll.save(new BasicDBObject(_map)));
    }
}

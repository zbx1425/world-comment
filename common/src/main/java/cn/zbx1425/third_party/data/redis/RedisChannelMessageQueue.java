package cn.zbx1425.third_party.data.redis;

import java.util.ArrayList;
import java.util.List;

//Todo: not implemented, even not a queue
public class RedisChannelMessageQueue {
    private List<String> CachedMessage = new ArrayList<String>();
    public RedisChannelMessageQueue() {}

    public synchronized void append(String Channel, String Data) {
        CachedMessage.add(Channel+"#"+Data);
    }

    public synchronized String next() {
        String ret = CachedMessage.get(0);
        CachedMessage.remove(0);
        return ret;
    }
}

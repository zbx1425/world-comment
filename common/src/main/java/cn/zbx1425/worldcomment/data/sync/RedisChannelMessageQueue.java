package cn.zbx1425.worldcomment.data.sync;

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
        if (CachedMessage.isEmpty()) {
            return "";
        }



        String ret = CachedMessage.get(0);
        CachedMessage.remove(0);
        return ret;
    }
}

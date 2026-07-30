package net.zmods.daedalus.event;

import net.zmods.daedalus.ChatErrorReporter;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;

import java.util.*;

public class LuaScheduler {
    private static final LuaScheduler INSTANCE = new LuaScheduler();
    public static LuaScheduler getInstance() { return INSTANCE; }

    private record PendingResume(long wakeTick, LuaThread thread) {}
    private final List<PendingResume> pending = new ArrayList<>();

    public void scheduleResume(long wakeTick, LuaThread thread) {
        pending.add(new PendingResume(wakeTick, thread));
    }

    // Call this once per server tick, before/after Events.TICK fires
    public void tick() {
        long now = TickTracker.get();
        Iterator<PendingResume> it = pending.iterator();
        while (it.hasNext()) {
            PendingResume p = it.next();
            if (now >= p.wakeTick) {
                it.remove();
                try {
                    p.thread.resume(LuaValue.NONE);
                } catch (Exception e) {
                    ChatErrorReporter.report("Error resuming sleeping script", e);
                }
            }
        }
    }
}
package net.zmods.daedalus.module;

public class ModuleContext {
    //load each module into its own thread
    private static final ThreadLocal<String> currentModuleId = new ThreadLocal<>();

    public static void set(String moduleId) {
        currentModuleId.set(moduleId);
    }

    public static String getCurrentModuleId() {
        return currentModuleId.get();
    }

    public static void clear() {
        currentModuleId.remove();
    }
}
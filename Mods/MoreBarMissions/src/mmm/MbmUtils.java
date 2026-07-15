package mmm;

import com.fs.starfarer.api.Global;
import mmm.missions.MbmBarEventManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

public class MbmUtils {
    public static final String MOD_ID = "MoreBarMissions";
    public static final String LUNA_LIB_ID = "lunalib";
    public static final boolean DEBUG = Global.getSettings().getBoolean("MmmBmDebug");

    private static final Logger log = Global.getLogger(MbmUtils.class);
    static {
        if (DEBUG) {
            log.setLevel(Level.ALL);
        }
    }

    // Returns the boolean setting called name in settings.json. If LunaLib is installed then return the same setting
    // from LunaLib instead.
    public static int getInteger(String name) {
        int value = Global.getSettings().getInt(name);
        if (LUNA_LIB_ENABLED) {
            Integer luna_value = lunalib.lunaSettings.LunaSettings.getInt(MOD_ID, name);
            if (luna_value != null) return luna_value;
            log.error("LunaSettings.getInteger failed for " + name);
        }
        return value;
    }

    public static final boolean LUNA_LIB_ENABLED = Global.getSettings().getModManager().isModEnabled(LUNA_LIB_ID);

    // Load settings from LunaLib if the mod is enabled. Otherwise, load them from modSettings.json file. Only the
    // LunaLib settings are loaded here.
    public static void loadSettings() {
        MbmBarEventManager.MAX_ACTIVE_EVENTS = getInteger("MmmBmBarEvents");
        MbmBarEventManager.ADD_ACTIVE_COOL_DOWN_DAYS = getInteger("MmmBmAddActiveCoolDownDays");
    }

    // Reflection utilities; the important thing is that java.lang.reflect.Method/Field are never imported in order
    // to get around SecurityException.
    public static class ReflectionHandles {
        // Here key is java.lang.reflect.Method.invoke, java.lang.reflect.Field.get, java.lang.reflect.Field.set, etc.
        private static Map<String, ReflectionHandles> handles = new HashMap<>();

        // Must be class of java.lang.reflect.Method or java.lang.reflect.Field
        public Class<?> clazz;
        // Must be method handle to Method.setAccessibleHandle or Field.setAccessibleHandle
        public MethodHandle setAccessibleHandle;
        // Must be method handle to Method.invoke or Field.get
        public MethodHandle handle;

        private ReflectionHandles(Class<?> clazz, MethodHandle setAccessibleHandle, MethodHandle handle) {
            this.clazz = clazz;
            this.setAccessibleHandle = setAccessibleHandle;
            this.handle = handle;
        }

        public static ReflectionHandles get(String key)
                throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
            if (handles.isEmpty()) {
                // This is used instead of java.lang.reflect.Method/Field to avoid SecurityException.
                Class<?> methodClass = Class.forName("java.lang.reflect.Method", false,
                        Class.class.getClassLoader());
                Class<?> fieldClass = Class.forName("java.lang.reflect.Field", false,
                        Class.class.getClassLoader());

                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle methodSetAccessibleHandle = lookup.findVirtual(methodClass, "setAccessible",
                        MethodType.methodType(Void.TYPE, Boolean.TYPE));
                MethodHandle fieldSetAccessibleHandle = lookup.findVirtual(fieldClass, "setAccessible",
                        MethodType.methodType(Void.TYPE, Boolean.TYPE));
                MethodHandle invokeHandle = lookup.findVirtual(methodClass, "invoke",
                        MethodType.methodType(Object.class, Object.class, Object[].class));
                MethodHandle getHandle = lookup.findVirtual(fieldClass, "get",
                        MethodType.methodType(Object.class, Object.class));
                MethodHandle setHandle = lookup.findVirtual(fieldClass, "set",
                        MethodType.methodType(Void.TYPE, Object.class, Object.class));

                handles.put("java.lang.reflect.Method.invoke",
                        new ReflectionHandles(methodClass, methodSetAccessibleHandle, invokeHandle));
                handles.put("java.lang.reflect.Field.get",
                        new ReflectionHandles(fieldClass, fieldSetAccessibleHandle, getHandle));
                handles.put("java.lang.reflect.Field.set",
                        new ReflectionHandles(fieldClass, fieldSetAccessibleHandle, setHandle));
            }

            return handles.get(key);
        }
    }

    // Uses reflection to call the methodName method of object in clazz class, with the provided argument and
    // associated parameter type. Here clazz must be the class where methodName is declared. Returns null on error.
    public static Object reflectionInvoke(Class<?> clazz, Object object, String methodName, Class<?> parameterType,
                                          Object arg) {
        try {
            // Must be a java.lang.reflect.Method
            Object method = clazz.getDeclaredMethod(methodName, parameterType);
            ReflectionHandles handles = ReflectionHandles.get("java.lang.reflect.Method.invoke");
            handles.setAccessibleHandle.invoke(method, true);
            return handles.handle.invoke(method, object, arg);
        } catch (Throwable e) {
            log.error("invoke:", e);
        }
        return null;
    }

    // Uses reflection to get the Object fieldName field of object in clazz class. Here clazz must be the class where
    // fieldName is declared. Returns null on error.
    public static Object reflectionGet(Class<?> clazz, Object object, String fieldName) {
        try {
            // Must be a java.lang.reflect.Field
            Object field = clazz.getDeclaredField(fieldName);
            ReflectionHandles handles = ReflectionHandles.get("java.lang.reflect.Field.get");
            handles.setAccessibleHandle.invoke(field, true);
            return handles.handle.invoke(field, object);
        } catch (Throwable e) {
            log.error("invoke:", e);
        }
        return null;
    }

    // Common implementation of reflectionSet* functions.
    private static void reflectionSetImpl(String key, Class<?> clazz, Object object, String fieldName, Object value) {
        try {
            // Must be a java.lang.reflect.Field
            Object field = clazz.getDeclaredField(fieldName);
            ReflectionHandles handles = ReflectionHandles.get(key);
            handles.setAccessibleHandle.invoke(field, true);
            handles.handle.invoke(field, object, value);
        } catch (Throwable e) {
            log.error("invoke:", e);
            throw new RuntimeException(e);
        }
    }

    // Uses reflection to set the Object fieldName field of object in clazz class. Here clazz must be the class where
    // fieldName is declared.
    public static void reflectionSet(Class<?> clazz, Object object, String fieldName, Object value) {
        try {
            // Must be a java.lang.reflect.Field
            Object field = clazz.getDeclaredField(fieldName);
            ReflectionHandles handles = ReflectionHandles.get("java.lang.reflect.Field.set");
            handles.setAccessibleHandle.invoke(field, true);
            handles.handle.invoke(field, object, value);
        } catch (Throwable e) {
            log.error("invoke:", e);
            throw new RuntimeException(e);
        }
    }
}

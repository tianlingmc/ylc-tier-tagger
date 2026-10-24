package com.ylcmc.tiertagger.core;

import com.mojang.authlib.GameProfile;

import java.lang.reflect.Method;

/**
 * 反射取玩家名。
 *
 * <p>GameProfile 在 1.21.9 之前是普通类（getName），之后改成了 record（name）。
 * 这里用反射兼容两边，编译期不绑定任何一个方法名。
 */
public final class PlayerNames {

    private static final Method NAME;
    private static final Method GET_NAME;

    static {
        Method name = null;
        Method getName = null;
        try {
            name = GameProfile.class.getMethod("name");
        } catch (Throwable ignored) {
            // 1.21.9 之前没有 name()
        }
        try {
            getName = GameProfile.class.getMethod("getName");
        } catch (Throwable ignored) {
            // 1.21.9 之后没有 getName()
        }
        NAME = name;
        GET_NAME = getName;
    }

    private PlayerNames() {
    }

    /** 取玩家名，取不到返回 null。 */
    public static String of(GameProfile profile) {
        if (profile == null) {
            return null;
        }
        try {
            if (NAME != null) {
                return (String) NAME.invoke(profile);
            }
            if (GET_NAME != null) {
                return (String) GET_NAME.invoke(profile);
            }
        } catch (Throwable ignored) {
            // 取不到就当没这个人
        }
        return null;
    }
}

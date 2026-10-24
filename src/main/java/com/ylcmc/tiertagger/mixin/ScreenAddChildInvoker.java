package com.ylcmc.tiertagger.mixin;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 给 Screen 暴露一个添加子元素的调用入口。
 *
 * <p>必须用 @Invoker 而不是 @Shadow：addDrawableChild 是泛型方法
 * {@code <T extends Element & Drawable & Selectable> T addDrawableChild(T)}，
 * Loom 1.14+ 默认不产出 refMap、在 remap 阶段直接改写 Mixin 字节码，
 * 遇到 @Shadow 同名泛型方法时会按目标类反查并把擦除签名误写成
 * (Lclass_364;)Lclass_364;（Screen）而非正确的 (Lclass_6377;)Lclass_6377;（Element），
 * 导致运行时找不到方法而崩溃。
 *
 * <p>@Invoker 的方法名是 invokeAddDrawableChild（与 addDrawableChild 不同名），
 * Loom 不会把它当成“对泛型目标方法的 shadow 反查”，只按本方法的声明改写描述符，
 * 于是得到正确的 method_37063(Lclass_6377;)Lclass_6377;。
 * method_37063 / class_6377 在 1.20.1~1.21.11 的 intermediary 映射里稳定不变。
 */
@Mixin(Screen.class)
public interface ScreenAddChildInvoker {
    @Invoker("addDrawableChild")
    Element invokeAddDrawableChild(Element element);
}

package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.gui.ConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 ESC 暂停菜单的「模组」按钮正下方，加一个我们自己的「PVPTIER 设置」按钮，
 * 点击打开 {@link ConfigScreen}（返回时回到暂停菜单）。
 *
 * <p>暂停菜单用 GridWidget 两列布局，「模组」在右列，正下方通常还有「辅助功能」等按钮，
 * 所以不是简单压在 Mods 下面，而是：找到「模组」按钮后，沿它所在的右列一路向下扫描，
 * 自动避开所有已有按钮，把我们的按钮放到该列最底部的空白处。这样不会和任何原版/ModMenu
 * 的按钮重叠。
 *
 * <p>加按钮必须用 {@link ScreenAddChildInvoker#invokeAddDrawableChild}（项目里已有的 Invoker），
 * 因为 addDrawableChild 是泛型方法，直接 shadow 在 Loom 1.14+ remap 下会崩。
 * 访问目标成员统一走 {@code self}（把 this 强转成 GameMenuScreen），与 OptionsScreenMixin 一致。
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {

    private static final String BUTTON_LABEL = "PVPTIER 设置";

    @Inject(method = "init", at = @At("RETURN"))
    private void ylcTierTaggerAddConfigButton(CallbackInfo ci) {
        GameMenuScreen self = (GameMenuScreen) (Object) this;

        // 窗口尺寸变化会重新 init，避免重复添加
        for (Element element : self.children()) {
            if (element instanceof ClickableWidget bw && BUTTON_LABEL.equals(bw.getMessage().getString())) {
                return;
            }
        }

        ClickableWidget mods = null;
        for (Element e : self.children()) {
            if (e instanceof ClickableWidget cw) {
                Text msg = cw.getMessage();
                TextContent content = msg.getContent();
                if (content instanceof TranslatableTextContent tt) {
                    String key = tt.getKey();
                    // 兼容原版「menu.mods」与 ModMenu 改写后的 key
                    if (key != null && (key.equals("menu.mods") || key.toLowerCase().contains("modmenu"))) {
                        mods = cw;
                        break;
                    }
                }
            }
        }
        if (mods == null) {
            return;
        }

        int baseX = mods.getX();
        int w = mods.getWidth();
        int h = 20;

        // 从「模组」按钮下方开始，沿右列向下推开所有已有按钮，找到第一个不重叠的位置。
        int y = mods.getY() + mods.getHeight() + 4;
        boolean moved;
        do {
            moved = false;
            for (Element e : self.children()) {
                if (e instanceof ClickableWidget cw && cw != mods) {
                    boolean overlapX = cw.getX() < baseX + w && cw.getX() + cw.getWidth() > baseX;
                    boolean overlapY = cw.getY() < y + h && cw.getY() + cw.getHeight() > y;
                    if (overlapX && overlapY) {
                        y = cw.getY() + cw.getHeight() + 4;
                        moved = true;
                    }
                }
            }
        } while (moved && y + h <= self.height - 4);

        if (y + h > self.height - 4) {
            y = self.height - 4 - h;
        }

        ButtonWidget button = ButtonWidget.builder(Text.literal(BUTTON_LABEL),
                        b -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client != null) {
                                client.setScreen(new ConfigScreen(Text.literal("PVPTIER 榜单设置"), self));
                            }
                        })
                .dimensions(baseX, y, w, h)
                .build();

        ((ScreenAddChildInvoker) self).invokeAddDrawableChild(button);
    }
}

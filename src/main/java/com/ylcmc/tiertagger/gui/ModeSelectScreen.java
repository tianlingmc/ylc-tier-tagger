package com.ylcmc.tiertagger.gui;

import com.ylcmc.tiertagger.core.TierConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 模式选择界面：列出八个模式，点一下即选中并返回。
 */
public class ModeSelectScreen extends Screen {

    public ModeSelectScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        TierConfig cfg = TierConfig.get();

        int buttonWidth = 200;
        int x = this.width / 2 - buttonWidth / 2;
        int y = 34;

        for (TierConfig.Mode mode : TierConfig.Mode.values()) {
            boolean selected = cfg.selectedMode == mode;
            final boolean wasSelected = selected;
            addDrawableChild(ButtonWidget.builder(
                            Text.literal((wasSelected ? "● " : "○ ") + mode.label),
                            button -> {
                                cfg.selectedMode = mode;
                                cfg.save();
                                if (client != null) {
                                    client.setScreen(new ConfigScreen(Text.literal("PVPTIER 榜单设置")));
                                }
                            })
                    .dimensions(x, y, buttonWidth, 20)
                    .build());
            y += 22;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("返回"), button -> close())
                .dimensions(x, y + 6, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, this.title, 8, 8, 0xffffff, true);
        context.drawText(textRenderer,
                Text.literal("● 表示当前选中的模式。选定模式没有段位时按显示规则处理。"),
                8, 20, 0xaaaaaa, false);
    }
}

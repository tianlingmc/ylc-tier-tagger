package com.ylcmc.tiertagger.gui;

import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 模式选择界面：列出八个模式，每个前面带对应的图标。
 * 点一下即选中（写入共享草稿，不立即保存）并返回设置页。
 */
public class ModeSelectScreen extends Screen {

    /** 上一级设置界面，用于返回。 */
    private final ConfigScreen parent;

    /** 与设置页共享的草稿。 */
    private final TierConfig draft;

    public ModeSelectScreen(Text title, ConfigScreen parent, TierConfig draft) {
        super(title);
        this.parent = parent;
        this.draft = draft;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(220, this.width - 40);
        int x = this.width / 2 - buttonWidth / 2;
        int y = 40;

        for (TierConfig.Mode mode : TierConfig.Mode.values()) {
            TierRegistry.Mode rm = TierRegistry.get(mode.key);
            String icon = rm == null ? "" : String.valueOf(rm.icon);
            boolean selected = draft.selectedMode == mode;
            Text label = Text.literal(icon + "  " + mode.label + (selected ? "（当前）" : ""));
            final TierConfig.Mode m = mode;
            addDrawableChild(ButtonWidget.builder(label, b -> {
                        draft.selectedMode = m;
                        if (client != null) {
                            client.setScreen(parent);
                        }
                    })
                    .dimensions(x, y, buttonWidth, 20)
                    .build());
            y += 24;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("返回"), b -> {
                    if (client != null) {
                        client.setScreen(parent);
                    }
                })
                .dimensions(x, y + 6, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, this.title, 8, 8, 0xffffffff, true);
        context.drawText(textRenderer,
                Text.literal("点击选中一个模式；选定模式没有段位时按显示规则处理。"),
                8, 24, 0xff9aa0a6, false);
    }
}

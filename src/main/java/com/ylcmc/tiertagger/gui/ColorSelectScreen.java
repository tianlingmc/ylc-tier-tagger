package com.ylcmc.tiertagger.gui;

import com.ylcmc.tiertagger.core.TierConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * 段位颜色选择界面：把预设色板铺成网格，点一下即选中（写入共享草稿，不立即保存）。
 *
 * <p>取代原来「点一下轮换到下一个颜色」的做法——色板有十几种，轮换既难找也容易点过头。
 *
 * <p>行外观是自绘的：{@link ButtonWidget} 只负责接收点击，色块与文字在 {@link #render} 里画。
 */
public class ColorSelectScreen extends Screen {

    /** 可选的颜色（RGB，画的时候才补 alpha）。 */
    private static final int[] PALETTE = {
            0xe8ba3a, 0xd5b355, 0xc4d3e7, 0xa0a7b2, 0xf89f5a, 0xc67b42,
            0x81749a, 0x655b79, 0x8f82a8, 0xffffff,
            0xff5555, 0x55dd55, 0x5555ff, 0xffaa00, 0x00dddd,
            0x2b2f36, 0x6b7280, 0x9ca3af, 0x38bdf8, 0xa78bfa
    };

    private static final int COLUMNS = 5;
    private static final int CELL_H = 28;
    private static final int CELL_GAP = 6;

    /** 上一级设置界面，用于返回。 */
    private final ConfigScreen parent;

    /** 与设置页共享的草稿。 */
    private final TierConfig draft;

    /** 正在改哪个段位的颜色。 */
    private final String tier;

    /** 每个色块的位置与颜色，render 里画。 */
    private final List<Cell> cells = new ArrayList<>();

    private static final class Cell {
        final int x, y, w, h, color;

        Cell(int x, int y, int w, int h, int color) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.color = color;
        }
    }

    public ColorSelectScreen(Text title, ConfigScreen parent, TierConfig draft, String tier) {
        super(title);
        this.parent = parent;
        this.draft = draft;
        this.tier = tier;
    }

    @Override
    protected void init() {
        cells.clear();

        int margin = 24;
        int usable = this.width - 2 * margin;
        int cellW = (usable - (COLUMNS - 1) * CELL_GAP) / COLUMNS;
        int startX = margin;
        int startY = 44;

        for (int i = 0; i < PALETTE.length; i++) {
            final int color = PALETTE[i];
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = startX + col * (cellW + CELL_GAP);
            int y = startY + row * (CELL_H + CELL_GAP);

            cells.add(new Cell(x, y, cellW, CELL_H, color));

            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
                        draft.setColor(tier, color);
                        if (client != null) {
                            client.setScreen(parent);
                        }
                    })
                    .dimensions(x, y, cellW, CELL_H)
                    .build());
        }

        int rows = (PALETTE.length + COLUMNS - 1) / COLUMNS;
        int backY = startY + rows * (CELL_H + CELL_GAP) + 6;
        addDrawableChild(ButtonWidget.builder(Text.literal("返回"), b -> {
                    if (client != null) {
                        client.setScreen(parent);
                    }
                })
                .dimensions(startX, backY, 80, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawText(textRenderer, this.title, 24, 12, 0xffffffff, true);
        context.drawText(textRenderer,
                Text.literal("点击选中一个颜色，自动返回设置页；改动在点「保存」后才写入配置。"),
                24, 28, 0xff9aa0a6, false);

        int current = draft.colorOf(tier);
        for (Cell cell : cells) {
            boolean selected = cell.color == current;
            boolean hover = mouseX >= cell.x && mouseX <= cell.x + cell.w
                    && mouseY >= cell.y && mouseY <= cell.y + cell.h;

            int bg = selected ? 0xff363b45 : (hover ? 0xff2f333b : 0xff26292f);
            context.fill(cell.x, cell.y, cell.x + cell.w, cell.y + cell.h, bg);

            // 选中的那一项再描一圈边，方便一眼找到当前值
            if (selected) {
                context.fill(cell.x, cell.y, cell.x + cell.w, cell.y + 1, 0xfff0f0f0);
                context.fill(cell.x, cell.y + cell.h - 1, cell.x + cell.w, cell.y + cell.h, 0xfff0f0f0);
                context.fill(cell.x, cell.y, cell.x + 1, cell.y + cell.h, 0xfff0f0f0);
                context.fill(cell.x + cell.w - 1, cell.y, cell.x + cell.w, cell.y + cell.h, 0xfff0f0f0);
            }

            int sw = 14;
            int sx = cell.x + 6;
            int sy = cell.y + (cell.h - sw) / 2;
            context.fill(sx, sy, sx + sw, sy + sw, 0xFF000000 | cell.color);

            Text hex = Text.literal("#" + String.format("%06X", cell.color));
            context.drawText(textRenderer, hex, sx + sw + 6, cell.y + (cell.h - 8) / 2,
                    selected ? 0xffffffff : 0xffc8cdd2, false);
        }
    }

    /**
     * 按 ESC 退回上一级（设置页），而不是整个关掉。
     *
     * <p>与 ConfigScreen 同理：重写 close() 而非 keyPressed()，避开两个版本范围
     * 之间 keyPressed 签名不一致的问题。
     */
    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}

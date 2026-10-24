package com.ylcmc.tiertagger.gui;

import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 自绘的设置界面，不依赖 cloth-config。
 *
 * <p>用 /ylctier 打开。所有改动先写在内存草稿（draft）上，只有点底部「保存」才写回
 * 配置文件；「取消」/ 顶部「返回」直接丢弃；「回复默认」把草稿重置成出厂值。
 * 内容区可滚动（右侧 ↑/↓ 按钮），适配窗口偏小的屏幕。
 */
public class ConfigScreen extends Screen {

    private static final int HEADER_H = 40;   // 顶部标题导航栏高度
    private static final int FOOTER_H = 36;   // 底部确认脚行高度
    private static final int MARGIN = 24;     // 左右留白
    private static final int ROW_H = 22;      // 单行高度
    private static final int ROW_GAP = 4;     // 行间距
    private static final int COL_GAP = 8;     // 段位颜色两列之间的间距
    private static final int SCROLL_W = 16;   // 右侧滚动按钮宽度
    private static final int SCROLL_GUTTER = SCROLL_W + 6; // 滚动按钮占用的右侧空间

    /** 点击颜色按钮时循环切换的预设色。 */
    private static final int[] PALETTE = {
            0xe8ba3a, 0xd5b355, 0xc4d3e7, 0xa0a7b2, 0xf89f5a, 0xc67b42,
            0x81749a, 0x655b79, 0x8f82a8, 0xffffff,
            0xff5555, 0x55dd55, 0x5555ff, 0xffaa00, 0x00dddd
    };

    /** 打开本界面之前的界面，用于「返回」。 */
    private final Screen parent;

    /** 编辑中的草稿，保存时才写回 TierConfig.get()。 */
    private TierConfig draft;

    /** 内容区已向上滚动的像素量。 */
    private int scrollOffset = 0;

    /** 内容超出可视区的最大滚动量。 */
    private int maxScroll = 0;

    /** 内容区每一行的渲染信息（含说明文字行与按钮行）。 */
    private final List<Row> rows = new ArrayList<>();

    private static final class Spec {
        enum Kind { SETTING, COLOR, NOTE, SECTION }
        Kind kind;
        Text label;
        Supplier<Text> value;     // 右侧文字（SETTING 用）
        Supplier<Integer> swatch; // 右侧色块（COLOR 用）
        Runnable onClick;        // 点击回调
        boolean center = false;  // 说明文字是否居中
        int x, y, w, h;
    }

    private static final class Row {
        final ButtonWidget button;
        final Text label;
        final Supplier<Text> value;
        final Supplier<Integer> swatch;
        final boolean note;
        final boolean center;
        final int x, y, w, h;

        Row(ButtonWidget button, Text label, Supplier<Text> value, Supplier<Integer> swatch,
            boolean note, boolean center, int x, int y, int w, int h) {
            this.button = button;
            this.label = label;
            this.value = value;
            this.swatch = swatch;
            this.note = note;
            this.center = center;
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    public ConfigScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (draft == null) {
            draft = TierConfig.snapshotOf(TierConfig.get());
        }
        rows.clear();

        int contentTop = HEADER_H;
        int contentBottom = this.height - FOOTER_H;
        int x = MARGIN;
        int fullW = this.width - 2 * MARGIN;

        // 先按整宽排版，算出是否需要滚动；需要的话给右侧滚动按钮让出空间重排。
        List<Spec> specs = buildSpecs(contentTop, x, fullW);
        maxScroll = Math.max(0, computeHeight(specs, contentTop) - (contentBottom - contentTop));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;
        if (maxScroll > 0) {
            specs = buildSpecs(contentTop, x, fullW - SCROLL_GUTTER);
        }

        materialize(specs, contentTop, contentBottom);
        buildHeader();
        buildFooter();
        buildScroll(contentTop, contentBottom);
    }

    /** 组装所有内容行（不含滚动偏移的绝对坐标）。 */
    private List<Spec> buildSpecs(int contentTop, int x, int contentW) {
        List<Spec> specs = new ArrayList<>();
        int[] cursor = {contentTop};

        // 说明文字
        Spec note = new Spec();
        note.kind = Spec.Kind.NOTE;
        note.label = Text.literal("显示规则为「只用选中的模式」时，只有优先模式有段位才会显示。");
        note.h = 16; note.x = x; note.w = contentW; note.y = cursor[0]; note.center = false;
        specs.add(note);
        cursor[0] += note.h + 2;

        // 开关 / 选择器
        specs.add(setting("总开关",
                () -> Text.literal(draft.enabled ? "开" : "关"),
                () -> draft.enabled = !draft.enabled, x, contentW, cursor));
        specs.add(setting("Tab 列表显示",
                () -> Text.literal(draft.showOnTab ? "开" : "关"),
                () -> draft.showOnTab = !draft.showOnTab, x, contentW, cursor));
        specs.add(setting("聊天栏显示",
                () -> Text.literal(draft.showInChat ? "开" : "关"),
                () -> draft.showInChat = !draft.showInChat, x, contentW, cursor));
        specs.add(setting("显示位置",
                () -> Text.literal(draft.renderLocation.label),
                () -> draft.renderLocation = draft.renderLocation == TierConfig.RenderLocation.LEFT
                        ? TierConfig.RenderLocation.RIGHT : TierConfig.RenderLocation.LEFT,
                x, contentW, cursor));
        specs.add(setting("优先模式",
                () -> {
                    TierRegistry.Mode m = TierRegistry.get(draft.selectedMode.key);
                    String icon = m == null ? "" : String.valueOf(m.icon);
                    return Text.literal(icon + " " + draft.selectedMode.label + "  ▸");
                },
                () -> {
                    if (client != null) {
                        client.setScreen(new ModeSelectScreen(Text.literal("选择 tier 模式"), this, draft));
                    }
                },
                x, contentW, cursor));
        specs.add(setting("显示规则",
                () -> Text.literal(draft.displayRule.label),
                () -> {
                    TierConfig.DisplayRule[] v = TierConfig.DisplayRule.values();
                    draft.displayRule = v[(draft.displayRule.ordinal() + 1) % v.length];
                },
                x, contentW, cursor));

        // 段位颜色分区标题
        Spec section = new Spec();
        section.kind = Spec.Kind.SECTION;
        section.label = Text.literal("段位颜色");
        section.h = 24; section.x = x; section.w = contentW; section.y = cursor[0]; section.center = true;
        specs.add(section);
        cursor[0] += section.h + ROW_GAP;

        // 段位颜色两列网格
        int colW = (contentW - COL_GAP) / 2;
        int gridStart = cursor[0];
        int tierRows = (TierConfig.TIER_NAMES.length + 1) / 2;
        for (int i = 0; i < TierConfig.TIER_NAMES.length; i++) {
            final String tier = TierConfig.TIER_NAMES[i];
            int col = i % 2;
            int r = i / 2;
            Spec cs = new Spec();
            cs.kind = Spec.Kind.COLOR;
            cs.label = Text.literal(tier);
            cs.swatch = () -> draft.colorOf(tier);
            cs.onClick = () -> cycleColor(tier);
            cs.x = x + col * (colW + COL_GAP);
            cs.y = gridStart + r * (ROW_H + ROW_GAP);
            cs.w = colW;
            cs.h = ROW_H;
            specs.add(cs);
        }
        cursor[0] = gridStart + tierRows * (ROW_H + ROW_GAP);

        return specs;
    }

    private static int computeHeight(List<Spec> specs, int contentTop) {
        int bottom = contentTop;
        for (Spec s : specs) {
            bottom = Math.max(bottom, s.y + s.h);
        }
        return bottom - contentTop;
    }

    private static Spec setting(String label, Supplier<Text> value, Runnable onClick,
                                int x, int w, int[] cursor) {
        Spec s = new Spec();
        s.kind = Spec.Kind.SETTING;
        s.label = Text.literal(label);
        s.value = value;
        s.onClick = onClick;
        s.x = x; s.w = w; s.h = ROW_H; s.y = cursor[0];
        cursor[0] += ROW_H + ROW_GAP;
        return s;
    }

    /** 据滚动偏移把可见行落到屏幕上，并登记给 render() 叠加文字。 */
    private void materialize(List<Spec> specs, int contentTop, int contentBottom) {
        for (Spec s : specs) {
            int py = s.y - scrollOffset;
            if (py < contentTop || py + s.h > contentBottom) {
                continue;
            }
            if (s.kind == Spec.Kind.SETTING || s.kind == Spec.Kind.COLOR) {
                ButtonWidget btn = ButtonWidget.builder(Text.empty(), b -> {
                            if (s.onClick != null) {
                                s.onClick.run();
                            }
                            if (client != null && client.currentScreen == ConfigScreen.this) {
                                clearAndInit();
                            }
                        })
                        .dimensions(s.x, py, s.w, s.h)
                        .build();
                addDrawableChild(btn);
                rows.add(new Row(btn, s.label, s.value, s.swatch, false, false, s.x, py, s.w, s.h));
            } else {
                rows.add(new Row(null, s.label, null, null, true, s.center, s.x, py, s.w, s.h));
            }
        }
    }

    private void buildHeader() {
        int bw = 60, bh = 20;
        int bx = this.width - bw - 8;
        int by = 8;
        addDrawableChild(ButtonWidget.builder(Text.literal("← 返回"), b -> closeToParent())
                .dimensions(bx, by, bw, bh)
                .build());
    }

    private void buildFooter() {
        int totalW = Math.min(this.width - 2 * MARGIN, 360);
        int btnW = (totalW - 2 * 8) / 3;
        int startX = this.width / 2 - totalW / 2;
        int by = this.height - FOOTER_H + (FOOTER_H - 20) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("保存"), b -> save())
                .dimensions(startX, by, btnW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("回复默认"), b -> resetDefaults())
                .dimensions(startX + btnW + 8, by, btnW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("取消"), b -> closeToParent())
                .dimensions(startX + 2 * (btnW + 8), by, btnW, 20).build());
    }

    private void buildScroll(int contentTop, int contentBottom) {
        if (maxScroll <= 0) {
            return;
        }
        int sx = this.width - MARGIN - SCROLL_W;
        int upY = contentTop + 2;
        int downY = contentBottom - 20 - 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("↑"), b -> scrollBy(-(ROW_H + ROW_GAP)))
                .dimensions(sx, upY, SCROLL_W, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("↓"), b -> scrollBy(ROW_H + ROW_GAP))
                .dimensions(sx, downY, SCROLL_W, 18).build());
    }

    private void scrollBy(int delta) {
        int next = scrollOffset + delta;
        if (next < 0) next = 0;
        if (next > maxScroll) next = maxScroll;
        if (next != scrollOffset) {
            scrollOffset = next;
            clearAndInit();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // 标题导航栏
        context.drawText(textRenderer, this.title, MARGIN, 8, 0xffffff, true);
        int count = YlcTierTaggerClient.SERVICE.size();
        context.drawText(textRenderer,
                Text.literal("数据来源：pvptier.ylcmc.top　当前 " + count + " 人"),
                MARGIN, 24, 0x9aa0a6, false);

        // 在按钮上叠加「标签左、值右」的文字
        for (Row r : rows) {
            if (r.note) {
                int tx;
                if (r.center) {
                    tx = this.width / 2 - textRenderer.getWidth(r.label) / 2;
                } else {
                    tx = r.x;
                }
                context.drawText(textRenderer, r.label, tx, r.y + (r.h - 8) / 2, 0x9aa0a6, false);
                continue;
            }
            int midY = r.y + (r.h - 8) / 2;
            if (r.label != null) {
                context.drawText(textRenderer, r.label, r.x + 6, midY, 0xe6e6e6, false);
            }
            if (r.swatch != null) {
                int c = r.swatch.get();
                int sq = 12;
                drawSwatch(context, r.x + r.w - sq - 8, r.y + (r.h - sq) / 2, sq, c);
            } else if (r.value != null) {
                Text v = r.value.get();
                int tw = textRenderer.getWidth(v);
                context.drawText(textRenderer, v, r.x + r.w - tw - 8, midY, 0xffffff, false);
            }
        }
    }

    /** 画一个带 1px 描边的色块。 */
    private static void drawSwatch(DrawContext ctx, int x, int y, int size, int color) {
        ctx.fill(x - 1, y - 1, x + size + 1, y, 0x2a2a2a);
        ctx.fill(x - 1, y + size, x + size + 1, y + size + 1, 0x2a2a2a);
        ctx.fill(x - 1, y - 1, x, y + size + 1, 0x2a2a2a);
        ctx.fill(x + size, y - 1, x + size + 1, y + size + 1, 0x2a2a2a);
        ctx.fill(x, y, x + size, y + size, color);
    }

    /** 保存草稿并回到上级界面。 */
    private void save() {
        TierConfig real = TierConfig.get();
        draft.copyValuesTo(real);
        real.save();
        closeToParent();
    }

    /** 草稿重置为默认值并刷新界面。 */
    private void resetDefaults() {
        draft.resetToDefaults();
        scrollOffset = 0;
        clearAndInit();
    }

    /** 丢弃改动回到上级界面。 */
    private void closeToParent() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    /** 在预设色板里循环切换某段位颜色。 */
    private void cycleColor(String tier) {
        int current = draft.colorOf(tier);
        int index = -1;
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == current) {
                index = i;
                break;
            }
        }
        draft.setColor(tier, PALETTE[(index + 1) % PALETTE.length]);
    }
}

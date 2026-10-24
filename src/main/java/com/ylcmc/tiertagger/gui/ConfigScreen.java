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
 * <p>用 /ylctier 或 ModMenu 里本模组的「配置」按钮打开。所有改动先写在内存草稿
 * （draft）上，只有点底部「保存」才写回配置文件；「取消」/ 顶部「返回」直接丢弃；
 * 「恢复默认」把草稿重置成出厂值。内容区可滚动（右侧 ↑/↓ 按钮），适配窗口偏小的屏幕。
 *
 * <p>每一行用 {@link ButtonWidget} 承担点击（跨 MC 版本稳定），但按钮原生外观被我们在
 * {@link #render} 里画的「不透明面板 + 左标签 + 右值/色块」完全覆盖——这样既保证了
 * 任何主题下文字都有足够对比度，又不会出现「空文字按钮叠半透明浅字」那种看不清的问题。
 */
public class ConfigScreen extends Screen {

    private static final int HEADER_H = 44;   // 顶部标题导航栏高度
    private static final int FOOTER_H = 40;   // 底部确认脚行高度
    private static final int MARGIN = 24;     // 左右留白
    private static final int ROW_H = 24;      // 单行高度
    private static final int ROW_GAP = 4;     // 行间距
    private static final int COL_GAP = 10;    // 段位颜色两列之间的间距
    private static final int SCROLLBAR_W = 6;              // 右侧滚动条宽度（原生风格）
    private static final int SCROLLBAR_GUTTER = SCROLLBAR_W + 8; // 滚动条占用的右侧空间
    private static final int PAD = 8;         // 行内文字左右内边距

    /** 行面板的底色与悬停色（不透明，确保亮/暗主题下都看得清文字）。 */
    private static final int ROW_BG = 0xff26292f;
    private static final int ROW_BG_HOVER = 0xff363b45;
    // MC 的颜色是 ARGB：少了 alpha 就是全透明，画出来等于没画（此前文字与分割线全部不可见就是这个原因）
    private static final int SEPARATOR = 0xff2a2a2e;
    private static final int SCROLLBAR_TRACK = 0xff1b1b1e;
    private static final int SCROLLBAR_THUMB = 0xff5a5f6a;

    /** 打开本界面之前的界面，用于「返回」。 */
    private final Screen parent;

    /** 编辑中的草稿，保存时才写回 TierConfig.get()。 */
    private TierConfig draft;

    /** 内容区已向上滚动的像素量。 */
    private int scrollOffset = 0;

    /** 内容超出可视区的最大滚动量。 */
    private int maxScroll = 0;

    /** 可视区高度，画滚动条滑块时用。 */
    private int viewportH = 0;

    /** 所有内容行（含说明 / 分区 / 设置 / 颜色），绝对坐标（未应用滚动偏移）。 */
    private final List<Item> items = new ArrayList<>();

    private enum Type { NOTE, SECTION, SETTING, COLOR }

    private static final class Item {
        Type type;
        Text label;
        Supplier<Text> value;     // 右侧文字（SETTING 用）
        Supplier<Integer> swatch; // 右侧色块（COLOR 用）
        Runnable onClick;        // 点击回调（SETTING / COLOR 用）
        boolean center = false;  // 说明文字是否居中
        int x, y, w, h;
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
        items.clear();

        int contentTop = HEADER_H;
        int contentBottom = this.height - FOOTER_H;
        int x = MARGIN;
        int fullW = this.width - 2 * MARGIN;

        // 先按整宽排版，算出是否需要滚动；需要的话给右侧滚动按钮让出空间重排。
        List<Item> laid = buildItems(contentTop, x, fullW);
        int visibleH = contentBottom - contentTop;
        viewportH = visibleH;
        maxScroll = Math.max(0, bottomOf(laid) - visibleH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;
        if (maxScroll > 0) {
            laid = buildItems(contentTop, x, fullW - SCROLLBAR_GUTTER);
        }

        items.addAll(laid);
        buildHeader();
        buildFooter();
        addRowButtons(contentTop, contentBottom);
    }

    /** 给可见的可点击行挂上 ButtonWidget，只用于接收点击（外观由 render 自绘）。 */
    private void addRowButtons(int contentTop, int contentBottom) {
        for (Item it : items) {
            if (it.onClick == null) {
                continue;
            }
            int py = it.y - scrollOffset;
            if (py < contentTop || py + it.h > contentBottom) {
                continue;
            }
            ButtonWidget btn = ButtonWidget.builder(Text.empty(), b -> {
                        it.onClick.run();
                        if (client != null && client.currentScreen == ConfigScreen.this) {
                            clearAndInit();
                        }
                    })
                    .dimensions(it.x, py, it.w, it.h)
                    .build();
            addDrawableChild(btn);
        }
    }

    /** 组装所有内容行（不含滚动偏移的绝对坐标）。 */
    private List<Item> buildItems(int contentTop, int x, int contentW) {
        List<Item> list = new ArrayList<>();
        int[] cur = {contentTop + 6};

        // 说明文字
        Item note = new Item();
        note.type = Type.NOTE;
        note.label = Text.literal("显示规则为「只用选中的模式」时，只有优先模式有段位才会显示。");
        note.h = 16; note.x = x; note.w = contentW; note.y = cur[0]; note.center = false;
        list.add(note);
        cur[0] += note.h + 8;

        // 开关 / 选择器
        list.add(setting("总开关",
                () -> Text.literal(draft.enabled ? "开" : "关"),
                () -> draft.enabled = !draft.enabled, x, contentW, cur));
        list.add(setting("Tab 列表显示",
                () -> Text.literal(draft.showOnTab ? "开" : "关"),
                () -> draft.showOnTab = !draft.showOnTab, x, contentW, cur));
        list.add(setting("聊天栏显示",
                () -> Text.literal(draft.showInChat ? "开" : "关"),
                () -> draft.showInChat = !draft.showInChat, x, contentW, cur));
        list.add(setting("显示位置",
                () -> Text.literal(draft.renderLocation.label),
                () -> draft.renderLocation = draft.renderLocation == TierConfig.RenderLocation.LEFT
                        ? TierConfig.RenderLocation.RIGHT : TierConfig.RenderLocation.LEFT,
                x, contentW, cur));
        list.add(setting("优先模式",
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
                x, contentW, cur));
        list.add(setting("显示规则",
                () -> Text.literal(draft.displayRule.label),
                () -> {
                    TierConfig.DisplayRule[] v = TierConfig.DisplayRule.values();
                    draft.displayRule = v[(draft.displayRule.ordinal() + 1) % v.length];
                },
                x, contentW, cur));

        // 段位颜色分区标题
        Item section = new Item();
        section.type = Type.SECTION;
        section.label = Text.literal("段位颜色");
        section.h = 22; section.x = x; section.w = contentW; section.y = cur[0]; section.center = false;
        list.add(section);
        cur[0] += section.h + 6;

        // 段位颜色两列网格
        int colW = (contentW - COL_GAP) / 2;
        int gridStart = cur[0];
        int tierRows = (TierConfig.TIER_NAMES.length + 1) / 2;
        for (int i = 0; i < TierConfig.TIER_NAMES.length; i++) {
            final String tier = TierConfig.TIER_NAMES[i];
            int col = i % 2;
            int r = i / 2;
            Item ci = new Item();
            ci.type = Type.COLOR;
            ci.label = Text.literal(tier);
            ci.swatch = () -> draft.colorOf(tier);
            ci.onClick = () -> {
                if (client != null) {
                    client.setScreen(new ColorSelectScreen(
                            Text.literal("选择 " + tier + " 的颜色"), ConfigScreen.this, draft, tier));
                }
            };
            ci.x = x + col * (colW + COL_GAP);
            ci.y = gridStart + r * (ROW_H + ROW_GAP);
            ci.w = colW;
            ci.h = ROW_H;
            list.add(ci);
        }
        cur[0] = gridStart + tierRows * (ROW_H + ROW_GAP);

        return list;
    }

    private static int bottomOf(List<Item> list) {
        int b = 0;
        for (Item it : list) {
            b = Math.max(b, it.y + it.h);
        }
        return b;
    }

    private static Item setting(String label, Supplier<Text> value, Runnable onClick,
                                int x, int w, int[] cur) {
        Item it = new Item();
        it.type = Type.SETTING;
        it.label = Text.literal(label);
        it.value = value;
        it.onClick = onClick;
        it.x = x; it.w = w; it.h = ROW_H; it.y = cur[0];
        cur[0] += ROW_H + ROW_GAP;
        return it;
    }

    private void buildHeader() {
        int bw = 64, bh = 20;
        int bx = this.width - bw - 8;
        int by = 10;
        addDrawableChild(ButtonWidget.builder(Text.literal("← 返回"), b -> closeToParent())
                .dimensions(bx, by, bw, bh)
                .build());
    }

    private void buildFooter() {
        int totalW = Math.min(this.width - 2 * MARGIN, 360);
        int gap = 8;
        int btnW = (totalW - 2 * gap) / 3;
        int startX = this.width / 2 - totalW / 2;
        int by = this.height - FOOTER_H + (FOOTER_H - 20) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("保存"), b -> save())
                .dimensions(startX, by, btnW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("恢复默认"), b -> resetDefaults())
                .dimensions(startX + btnW + gap, by, btnW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("取消"), b -> closeToParent())
                .dimensions(startX + 2 * (btnW + gap), by, btnW, 20).build());
    }

    /**
     * 滚轮滚动。
     *
     * <p>四个参数的签名在 1.20.4 与 1.21.11 一致，可以安全重写；
     * 拖拽（mouseDragged）两边签名不同（1.20.x 是 double,double,int，1.21.11 换成了 Click），
     * 单份源码没法同时覆盖，所以只做滚轮 + 滚动条。
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll <= 0) {
            return false;
        }
        // 与原版 ScrollableWidget 同向：scrollY -= verticalAmount * step
        scrollBy((int) (-verticalAmount * (ROW_H + ROW_GAP)));
        return true;
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

        // 顶栏与底栏的分隔线
        context.fill(0, HEADER_H - 1, this.width, HEADER_H, SEPARATOR);
        context.fill(0, this.height - FOOTER_H, this.width, this.height - FOOTER_H + 1, SEPARATOR);

        // 标题导航栏
        context.drawText(textRenderer, this.title, MARGIN, 10, 0xffffffff, true);
        int count = YlcTierTaggerClient.SERVICE.size();
        context.drawText(textRenderer,
                Text.literal("数据来源：pvptier.ylcmc.top　当前 " + count + " 人"),
                MARGIN, 26, 0xff9aa0a6, false);

        int contentTop = HEADER_H;
        int contentBottom = this.height - FOOTER_H;

        for (Item it : items) {
            int py = it.y - scrollOffset;
            switch (it.type) {
                case NOTE:
                    int nx = it.center ? this.width / 2 - textRenderer.getWidth(it.label) / 2 : it.x;
                    context.drawText(textRenderer, it.label, nx, py + (it.h - 8) / 2, 0xff9aa0a6, false);
                    break;
                case SECTION:
                    context.drawText(textRenderer, it.label, it.x, py + (it.h - 8) / 2, 0xffc8cdd2, true);
                    context.fill(it.x, py + it.h - 1, it.x + it.w, py + it.h, SEPARATOR);
                    break;
                case SETTING:
                case COLOR:
                    int top = Math.max(py, contentTop);
                    int bot = Math.min(py + it.h, contentBottom);
                    if (bot > top) {
                        boolean hover = mouseX >= it.x && mouseX <= it.x + it.w
                                && mouseY >= py && mouseY <= py + it.h;
                        context.fill(it.x, top, it.x + it.w, bot, hover ? ROW_BG_HOVER : ROW_BG);
                    }
                    // 文字只在整行可见时绘制，避免被底栏截断时露出半截
                    if (py >= contentTop && py + it.h <= contentBottom) {
                        int midY = py + (it.h - 8) / 2;
                        context.drawText(textRenderer, it.label, it.x + PAD, midY, 0xffe6e6e6, false);
                        if (it.swatch != null) {
                            int c = it.swatch.get();
                            int sq = 14;
                            // 配置里存的是 RGB，画之前补上 alpha，否则色块全透明
                            drawSwatch(context, it.x + it.w - sq - PAD, py + (it.h - sq) / 2, sq, 0xFF000000 | c);
                        } else if (it.value != null) {
                            Text v = it.value.get();
                            int tw = textRenderer.getWidth(v);
                            context.drawText(textRenderer, v, it.x + it.w - tw - PAD, midY, 0xffffffff, false);
                        }
                    }
                    break;
            }
        }

        drawScrollbar(context, contentTop, contentBottom);
    }

    /** 画右侧原生风格的滚动条（轨道 + 滑块），替代原来的上下箭头按钮。 */
    private void drawScrollbar(DrawContext context, int contentTop, int contentBottom) {
        if (maxScroll <= 0 || viewportH <= 0) {
            return;
        }
        int trackX = this.width - MARGIN - SCROLLBAR_W;
        int trackTop = contentTop + 2;
        int trackBottom = contentBottom - 2;
        int trackH = trackBottom - trackTop;
        if (trackH <= 0) {
            return;
        }
        context.fill(trackX, trackTop, trackX + SCROLLBAR_W, trackBottom, SCROLLBAR_TRACK);

        int contentH = viewportH + maxScroll;
        int thumbH = Math.max(18, trackH * viewportH / contentH);
        int travel = trackH - thumbH;
        int thumbY = trackTop + (int) ((float) scrollOffset / maxScroll * travel);
        context.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + thumbH, SCROLLBAR_THUMB);
    }

    /** 画一个带 1px 描边的色块。 */
    private static void drawSwatch(DrawContext ctx, int x, int y, int size, int color) {
        ctx.fill(x - 1, y - 1, x + size + 1, y, SEPARATOR);
        ctx.fill(x - 1, y + size, x + size + 1, y + size + 1, SEPARATOR);
        ctx.fill(x - 1, y - 1, x, y + size + 1, SEPARATOR);
        ctx.fill(x + size, y - 1, x + size + 1, y + size + 1, SEPARATOR);
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

}

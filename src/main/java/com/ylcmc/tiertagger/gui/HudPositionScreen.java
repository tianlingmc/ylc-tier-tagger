package com.ylcmc.tiertagger.gui;

import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import com.ylcmc.tiertagger.core.PlayerEntry;
import com.ylcmc.tiertagger.core.PlayerNames;
import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 拖动设置段位 HUD 的位置。
 *
 * <p>为什么不用 {@code mouseClicked} / {@code mouseDragged}：这两个方法在 1.20.x 是
 * {@code (double, double, int, ...)}、1.21.11 换成了 {@code (Click, ...)}，单份源码无法同时重写。
 * 这里改成每帧轮询「左键是否按下 + 鼠标在哪个坐标」（{@link GLFW#glfwGetMouseButton} 与
 * {@code client.mouse}），两个版本都稳定可用。
 *
 * <p>位置按屏幕宽高的比例写回草稿（0~1），点「保存」才会真正落盘。左键按在面板上可拖动，
 * 按在空白处则把面板直接挪到鼠标位置。ESC 退回设置页。
 */
public class HudPositionScreen extends Screen {

    private final ConfigScreen parent;
    private final TierConfig draft;

    private final float originalX;
    private final float originalY;

    private boolean dragging;
    private boolean wasDown;
    private int grabDx;
    private int grabDy;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private List<Text> previewLines = new ArrayList<>();

    public HudPositionScreen(Text title, ConfigScreen parent, TierConfig draft) {
        super(title);
        this.parent = parent;
        this.draft = draft;
        this.originalX = draft.hudX;
        this.originalY = draft.hudY;
    }

    @Override
    protected void init() {
        previewLines = buildPreview();

        int h = 20;
        int w = 80;
        int gap = 8;
        int by = this.height - 28;
        int startX = this.width / 2 - (w * 2 + gap) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("保存"), b -> {
                    // 直接写进真实配置并落盘。设置页那边还要再按一次「保存」才生效，
                    // 很容易让人以为位置没保存成功，所以这里当场生效。
                    TierConfig real = TierConfig.get();
                    real.hudX = draft.hudX;
                    real.hudY = draft.hudY;
                    real.save();
                    backToParent();
                })
                .dimensions(startX, by, w, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("取消"), b -> {
                    draft.hudX = originalX;
                    draft.hudY = originalY;
                    backToParent();
                })
                .dimensions(startX + w + gap, by, w, h).build());
    }

    /** 预览内容：优先用自己真实的段位数据，数据还没加载时用一个示例。 */
    private List<Text> buildPreview() {
        TierConfig cfg = TierConfig.get();
        PlayerEntry entry = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            entry = YlcTierTaggerClient.SERVICE.find(PlayerNames.of(client.player.getGameProfile()));
        }
        if (entry == null) {
            entry = sample();
        }
        return TierHudPanel.buildLines(entry, cfg, 12.0);
    }

    /** 数据还没到时的占位数据，保证预览能画出来。 */
    private static PlayerEntry sample() {
        PlayerEntry e = new PlayerEntry();
        e.name = "Player";
        e.rank = 12;
        e.points = 150;
        e.region = "台湾";
        e.title = "Combat Ace";
        List<TierEntry> tiers = new ArrayList<>();
        tiers.add(tier("axe", "HT2"));
        tiers.add(tier("sword", "HT1"));
        tiers.add(tier("vanilla", "LT4"));
        tiers.add(tier("nethop", "HT2"));
        tiers.add(tier("pot", "LT1"));
        e.tiers = tiers;
        return e;
    }

    private static TierEntry tier(String mode, String tier) {
        TierEntry t = new TierEntry();
        t.mode = mode;
        t.tier = tier;
        return t;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        layout();
        // 注意用 render 给的 mouseX/mouseY：它们是原版已经按界面缩放换算好的坐标。
        // 不要用 client.mouse.getX()，那个是窗口像素，GUI 缩放不是 1 时会整体偏大。
        handleDrag(mouseX, mouseY);
        layout();

        context.drawText(textRenderer, this.title, 24, 12, 0xffffffff, true);
        context.drawText(textRenderer,
                Text.literal("左键按住面板拖动；按在空白处则把面板挪到鼠标位置。位置按屏幕比例保存，点「保存」立即生效。"),
                24, 28, 0xff9aa0a6, false);

        TierHudPanel.drawPanel(context, textRenderer, previewLines, panelX, panelY, panelW, panelH, false);

        // 面板位置数值，方便确认
        context.drawText(textRenderer,
                Text.literal("X " + Math.round(draft.hudX * 100) + "%   Y " + Math.round(draft.hudY * 100) + "%")
                        .copy().styled(s -> s.withColor(Formatting.GRAY)),
                24, 40, 0xff9aa0a6, false);
    }

    /** 根据草稿里的比例算出面板矩形，尺寸与定位都复用 HUD 那边的方法，保证两处一致。 */
    private void layout() {
        panelW = TierHudPanel.boxWidth(textRenderer, previewLines);
        panelH = TierHudPanel.boxHeight(previewLines);
        int[] pos = TierHudPanel.position(draft.hudX, draft.hudY, panelW, panelH, this.width, this.height);
        panelX = pos[0];
        panelY = pos[1];
    }

    /** 每帧轮询左键状态来拖动；鼠标坐标用 render 传进来的（已按界面缩放换算）。 */
    private void handleDrag(int mx, int my) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;

        if (down && !wasDown && !overButton(mx, my)) {
            if (inside(mx, my)) {
                grabDx = mx - panelX;
                grabDy = my - panelY;
            } else {
                // 按在空白处：面板直接以鼠标为中心
                grabDx = panelW / 2;
                grabDy = panelH / 2;
            }
            dragging = true;
        }

        if (dragging && down) {
            int nx = clamp(mx - grabDx, 0, Math.max(0, this.width - panelW));
            int ny = clamp(my - grabDy, 0, Math.max(0, this.height - panelH));
            panelX = nx;
            panelY = ny;
            if (this.width > panelW) {
                draft.hudX = (float) nx / this.width;
            }
            if (this.height > panelH) {
                draft.hudY = (float) ny / this.height;
            }
        }

        if (!down) {
            dragging = false;
        }
        wasDown = down;
    }

    private boolean inside(int mx, int my) {
        return mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH;
    }

    /** 鼠标在按钮上时不开始拖拽，否则点按钮会顺带把面板拖走。 */
    private boolean overButton(int mx, int my) {
        for (Element e : this.children()) {
            if (e instanceof ButtonWidget b
                    && mx >= b.getX() && mx <= b.getX() + b.getWidth()
                    && my >= b.getY() && my <= b.getY() + b.getHeight()) {
                return true;
            }
        }
        return false;
    }

    private void backToParent() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    /** ESC 退回设置页，与其它子界面保持一致。 */
    @Override
    public void close() {
        backToParent();
    }
}

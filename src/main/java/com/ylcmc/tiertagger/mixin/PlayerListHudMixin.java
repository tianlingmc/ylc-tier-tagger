package com.ylcmc.tiertagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import com.ylcmc.tiertagger.core.PlayerEntry;
import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierLabel;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 在 Tab 玩家列表里给玩家名字加上段位标签。
 */
@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
    private Text ylcTierTaggerPrependTier(Text original, PlayerListEntry entry) {
        TierConfig config = TierConfig.get();
        if (!config.enabled || !config.showOnTab) {
            return original;
        }

        GameProfile profile = entry.getProfile();
        if (profile == null) {
            return original;
        }

        PlayerEntry data = YlcTierTaggerClient.SERVICE.find(profile.name());
        if (data == null) {
            return original;
        }
        return TierLabel.apply(original, data, config);
    }
}

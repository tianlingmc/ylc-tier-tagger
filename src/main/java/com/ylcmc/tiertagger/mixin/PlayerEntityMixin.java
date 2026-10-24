package com.ylcmc.tiertagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import com.ylcmc.tiertagger.core.PlayerEntry;
import com.ylcmc.tiertagger.core.PlayerNames;
import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierLabel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 在头顶名牌上给玩家名字加上段位标签。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text ylcTierTaggerPrependTier(Text original) {
        TierConfig config = TierConfig.get();
        if (!config.enabled) {
            return original;
        }

        PlayerEntity self = (PlayerEntity) (Object) this;
        String name = PlayerNames.of(self.getGameProfile());
        if (name == null) {
            return original;
        }

        PlayerEntry entry = YlcTierTaggerClient.SERVICE.find(name);
        if (entry == null) {
            return original;
        }
        return TierLabel.apply(original, entry, config);
    }
}

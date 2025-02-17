package net.jcm.vsch.mixin;

import net.lointain.cosmos.procedures.AerialLightRenderer;
import net.lointain.cosmos.procedures.LightRendererer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Mixin(LightRendererer.class)
public abstract class MixinLightRenderer {

    // getPrivateField fails when Cosmos is de-obfuscated in a dev enviroment, so this is here to stop that
    @ModifyVariable(method = "getPrivateField", remap = false, at = @At("HEAD"), argsOnly = true)
    private static String fixFieldName(String fieldName) {

        // Check if we are dev enviroment or obf enviroment
        if (!FMLEnvironment.production) {
            if (fieldName.equals("f_110009_")) {
                return "passes";
            }
            if (fieldName.equals("f_110054_")) {
                return "effect";
            }
        }

        return fieldName;
    }

}


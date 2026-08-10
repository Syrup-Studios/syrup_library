package net.syrupstudios.syruplibrary.loaders.forge;

//? if forge {
import net.minecraftforge.fml.common.Mod;
import net.syrupstudios.syruplibrary.SyrupLibrary;

/** Forge entrypoint for Syrup Library. */
@Mod(SyrupLibrary.MOD_ID)
public final class ForgeSyrupLibrary {
    public ForgeSyrupLibrary() {
        SyrupLibrary.initialize();
    }
}
//?}

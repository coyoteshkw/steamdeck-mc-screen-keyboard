package com.steamdeck.keyboard;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SteamDeckKeyboard.MODID)
public class SteamDeckKeyboard {
    public static final String MODID = "steamdeckkeyboard";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SteamDeckKeyboard() {
        LOGGER.info("Steam Deck Keyboard mod loaded");
    }
}

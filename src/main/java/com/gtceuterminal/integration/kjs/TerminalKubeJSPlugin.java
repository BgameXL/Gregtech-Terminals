package com.gtceuterminal.integration.kjs;

import dev.latvian.mods.kubejs.KubeJSPlugin;

public class TerminalKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void registerEvents() {
        TerminalEvents.GROUP.register();
    }
}
package com.gtceuterminal.integration.kjs;

import com.gtceuterminal.integration.kjs.events.ComponentEventJS;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;

public class TerminalKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void registerEvents() {
        TerminalEvents.GROUP.register();
    }

    @Override
    public void initStartup() {
        TerminalEvents.COMPONENTS.post(ScriptType.STARTUP, new ComponentEventJS());
    }
}
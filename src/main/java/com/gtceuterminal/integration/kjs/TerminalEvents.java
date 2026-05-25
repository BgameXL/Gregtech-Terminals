package com.gtceuterminal.integration.kjs;

import com.gtceuterminal.integration.kjs.events.ComponentEventJS;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface TerminalEvents {

    EventGroup GROUP = EventGroup.of("GTCEuTerminalEvents");

    EventHandler COMPONENTS = GROUP.startup("components", () -> ComponentEventJS.class);
}
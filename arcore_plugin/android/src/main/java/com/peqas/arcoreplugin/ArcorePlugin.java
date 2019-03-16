package com.peqas.arcoreplugin;

import io.flutter.plugin.common.PluginRegistry;

/**
 * ArcorePlugin
 */
public class ArcorePlugin {
    /**
     * Plugin registration.
     */
    public static void registerWith(PluginRegistry.Registrar registrar) {
        registrar
                .platformViewRegistry()
                .registerViewFactory("plugins.peqas.com/arcore_plugin", new ArcoreViewFactory(registrar.messenger()));
    }

}

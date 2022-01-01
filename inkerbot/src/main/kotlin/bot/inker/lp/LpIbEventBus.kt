package bot.inker.lp

import bot.inker.api.plugin.PluginContainer
import me.lucko.luckperms.common.api.LuckPermsApiProvider
import me.lucko.luckperms.common.event.AbstractEventBus
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbEventBus @Inject constructor(plugin: LuckPermsPlugin,
                   apiProvider: LuckPermsApiProvider
) : AbstractEventBus<PluginContainer>(plugin, apiProvider) {
    override fun checkPlugin(plugin: Any): PluginContainer {
        return plugin as PluginContainer
    }
}
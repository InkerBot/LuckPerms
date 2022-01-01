package bot.inker.lp

import bot.inker.api.plugin.PluginContainer
import me.lucko.luckperms.common.config.generic.adapter.ConfigurateConfigAdapter
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import me.lucko.luckperms.common.util.MoreFiles
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import ninja.leaping.configurate.loader.ConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbConfigurateConfigAdapter @Inject constructor(plugin: LuckPermsPlugin, container: PluginContainer) : ConfigurateConfigAdapter(
    plugin, container.configPath.resolve("luckperms.conf")
) {
    override fun createLoader(path: Path): ConfigurationLoader<out ConfigurationNode> {
        MoreFiles.createDirectoriesIfNotExists(path.parent)
        if (!Files.exists(path)) {
            Files.copy(plugin.bootstrap.getResourceStream("luckperms.conf"),path)
        }
        return HoconConfigurationLoader.builder().setPath(path).build()
    }
}
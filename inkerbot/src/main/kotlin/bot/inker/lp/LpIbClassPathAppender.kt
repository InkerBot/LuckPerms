package bot.inker.lp

import bot.inker.api.plugin.PluginContainer
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbClassPathAppender :ClassPathAppender {
    @Inject
    private lateinit var pluginContainer: PluginContainer

    override fun addJarToClasspath(file: Path) {
        pluginContainer.addDepend(file.toUri().toURL())
    }
}
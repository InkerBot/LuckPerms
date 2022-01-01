package bot.inker.lp

import me.lucko.luckperms.common.plugin.logging.Slf4jPluginLogger
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbPluginLogger @Inject constructor(logger: Logger) : Slf4jPluginLogger(logger)
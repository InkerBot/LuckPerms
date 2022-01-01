package bot.inker.lp

import bot.inker.api.Frame
import me.lucko.luckperms.common.plugin.scheduler.AbstractJavaScheduler
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbSchedulerAdapter : AbstractJavaScheduler() {
    @Inject
    private lateinit var frame: Frame
    override fun sync(): Executor {
        return Executor { command -> frame.execute { command.run() } }
    }
}
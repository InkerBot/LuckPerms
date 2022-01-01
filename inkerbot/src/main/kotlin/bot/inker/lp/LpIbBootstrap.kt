package bot.inker.lp

import bot.inker.api.Frame
import bot.inker.api.InkerBot
import bot.inker.api.event.AutoComponent
import bot.inker.api.event.EventHandler
import bot.inker.api.event.lifestyle.LifecycleEvent
import bot.inker.api.model.Member
import bot.inker.api.plugin.JvmPlugin
import bot.inker.api.plugin.PluginContainer
import bot.inker.lp.manager.*

import com.google.inject.Binder
import com.google.inject.TypeLiteral
import me.lucko.luckperms.common.api.LuckPermsApiProvider
import me.lucko.luckperms.common.calculator.CalculatorFactory
import me.lucko.luckperms.common.command.CommandManager
import me.lucko.luckperms.common.context.manager.ContextManager
import me.lucko.luckperms.common.event.AbstractEventBus
import me.lucko.luckperms.common.messaging.MessagingFactory
import me.lucko.luckperms.common.model.Group
import me.lucko.luckperms.common.model.Track
import me.lucko.luckperms.common.model.User
import me.lucko.luckperms.common.model.manager.group.GroupManager
import me.lucko.luckperms.common.model.manager.track.TrackManager
import me.lucko.luckperms.common.model.manager.user.UserManager
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender
import me.lucko.luckperms.common.plugin.logging.PluginLogger
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener
import net.luckperms.api.LuckPerms
import net.luckperms.api.event.EventBus
import net.luckperms.api.platform.Platform
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.inject.Singleton

@Singleton
@AutoComponent
class LpIbBootstrap : JvmPlugin, LuckPermsBootstrap {
    override fun configure(binder: Binder) {
        binder.bind(LuckPermsBootstrap::class.java).toInstance(this)
        binder.apply {
            bind(PluginLogger::class.java).to(LpIbPluginLogger::class.java)
            bind(LuckPermsPlugin::class.java).to(LpIbPlugin::class.java)
            bind(ClassPathAppender::class.java).to(LpIbClassPathAppender::class.java)
            bind(SchedulerAdapter::class.java).to(LpIbSchedulerAdapter::class.java)

            bind(LuckPerms::class.java).to(LuckPermsApiProvider::class.java)
            bind(LuckPermsApiProvider::class.java).to(LpIbApiProvider::class.java)

            bind(object:TypeLiteral<UserManager<User>>(){}).to(LpIbUserManager::class.java)
            bind(object:TypeLiteral<GroupManager<Group>>(){}).to(LpIbGroupManager::class.java)
            bind(object:TypeLiteral<TrackManager<Track>>(){}).to(LpIbTrackManager::class.java)
            bind(object:TypeLiteral<ContextManager<Member,Member>>(){}).to(LpIbMemberContextManager::class.java)
            bind(CommandManager::class.java).to(LpIbCommandManager::class.java)

            bind(object:TypeLiteral<MessagingFactory<LuckPermsPlugin>>(){}).to(LpIbMessageFactory::class.java)
            bind(CalculatorFactory::class.java).to(LpIbCalculatorFactory::class.java)

            bind(AbstractConnectionListener::class.java).to(LpIbConnectionListener::class.java)

            bind(EventBus::class.java).to(object:TypeLiteral<AbstractEventBus<PluginContainer>>(){})
            bind(object:TypeLiteral<AbstractEventBus<PluginContainer>>(){}).to(LpIbEventBus::class.java)
        }
    }


    @EventHandler
    fun onLoad(event:LifecycleEvent.Initialization){
        loadLatch.countDown()
    }

    @EventHandler
    fun onEnable(event: LifecycleEvent.LoadComplete){
        enableLatch.countDown()
    }

    private val frame: Frame by lazy { InkerBot() }

    private val pluginContainer: PluginContainer by lazy { InkerBot() }

    private val lazyPluginLogger: PluginLogger by lazy { InkerBot() }
    override fun getPluginLogger(): PluginLogger = lazyPluginLogger

    private val schedulerAdapter: LpIbSchedulerAdapter by lazy { InkerBot() }
    override fun getScheduler(): SchedulerAdapter = schedulerAdapter

    private val lazyClassPathAppender: ClassPathAppender by lazy { InkerBot() }
    override fun getClassPathAppender(): ClassPathAppender = lazyClassPathAppender

    private var loadLatch = CountDownLatch(1)
    override fun getLoadLatch(): CountDownLatch = loadLatch

    private var enableLatch = CountDownLatch(1)
    override fun getEnableLatch(): CountDownLatch = enableLatch

    override fun getVersion(): String = pluginContainer.meta.version

    private val startupTime:Instant = Instant.now()
    override fun getStartupTime(): Instant = startupTime

    override fun getType(): Platform.Type = Platform.Type.INKERBOT

    override fun getServerBrand(): String = frame.self.name

    override fun getServerVersion(): String = frame.self.meta.version

    override fun getDataDirectory(): Path = pluginContainer.dataPath

    private val memberRepo: LpIbSenderFactory by lazy { InkerBot() }
    override fun getPlayer(uniqueId: UUID): Optional<Member> = memberRepo.getMember(uniqueId)

    override fun lookupUniqueId(username: String): Optional<UUID> = memberRepo.lookupUniqueId(username)

    override fun lookupUsername(uniqueId: UUID): Optional<String> = memberRepo.lookupUsername(uniqueId)

    override fun getPlayerCount(): Int = memberRepo.getPlayerCount()

    override fun getPlayerList(): List<String> = memberRepo.getOnlineMembers().map(Member::name)
    override fun getOnlinePlayers(): List<UUID> = memberRepo.getOnlineMembers().map { it.identity.uuid }

    override fun isPlayerOnline(uniqueId: UUID): Boolean = memberRepo.isPlayerOnline(uniqueId)
}
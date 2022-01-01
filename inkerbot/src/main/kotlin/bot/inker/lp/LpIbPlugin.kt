package bot.inker.lp

import bot.inker.api.InkerBot
import bot.inker.api.event.AutoComponent
import bot.inker.api.event.EventHandler
import bot.inker.api.event.lifestyle.LifecycleEvent
import bot.inker.api.event.message.ConsoleMessageEvent
import bot.inker.api.plugin.PluginContainer
import bot.inker.api.service.CommandService
import bot.inker.lp.manager.LpIbCommandManager
import bot.inker.lp.manager.LpIbMemberContextManager
import me.lucko.luckperms.common.api.LuckPermsApiProvider
import me.lucko.luckperms.common.calculator.CalculatorFactory
import me.lucko.luckperms.common.command.CommandManager
import me.lucko.luckperms.common.config.LuckPermsConfiguration
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter
import me.lucko.luckperms.common.dependencies.Dependency
import me.lucko.luckperms.common.event.AbstractEventBus
import me.lucko.luckperms.common.messaging.MessagingFactory
import me.lucko.luckperms.common.model.Group
import me.lucko.luckperms.common.model.Track
import me.lucko.luckperms.common.model.User
import me.lucko.luckperms.common.model.manager.group.GroupManager
import me.lucko.luckperms.common.model.manager.track.TrackManager
import me.lucko.luckperms.common.model.manager.user.UserManager
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener
import me.lucko.luckperms.common.sender.Sender
import net.luckperms.api.LuckPerms
import net.luckperms.api.query.QueryOptions
import java.util.*
import java.util.stream.Stream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AutoComponent
class LpIbPlugin: AbstractLuckPermsPlugin() {
    @Inject
    private lateinit var commandService: CommandService

    private val senderFactory: LpIbSenderFactory by lazy { InkerBot() }

    @Inject
    private lateinit var bootstrap: LuckPermsBootstrap
    override fun getBootstrap(): LuckPermsBootstrap {
        return bootstrap
    }

    @Inject
    private lateinit var userManager: UserManager<User>
    override fun getUserManager(): UserManager<User> = userManager

    @Inject
    private lateinit var groupManager: GroupManager<Group>
    override fun getGroupManager(): GroupManager<Group> = groupManager

    @Inject
    private lateinit var trackManager: TrackManager<Track>
    override fun getTrackManager(): TrackManager<Track> = trackManager

    @Inject
    private lateinit var commandManager: CommandManager
    override fun getCommandManager(): CommandManager = commandManager

    @Inject
    private lateinit var connectionListener: AbstractConnectionListener
    override fun getConnectionListener(): AbstractConnectionListener = connectionListener

    private val lazyContextManager: LpIbMemberContextManager by lazy { InkerBot() }
    override fun getContextManager(): LpIbMemberContextManager = lazyContextManager
    override fun setupContextManager() {
        contextManager
    }

    override fun registerPlatformListeners() {
        // Auto registered in InkerBot
    }

    override fun getQueryOptionsForUser(user: User): Optional<QueryOptions> {
        return senderFactory.getMember(user.uniqueId).map { contextManager.getQueryOptions(it) }
    }

    override fun getOnlineSenders(): Stream<Sender> = senderFactory.getOnlineSenders()

    override fun getConsoleSender(): Sender {
        return senderFactory.wrap(ConsoleMessageEvent.of(""))
    }

    override fun setupSenderFactory() {
        // setup by IOC
    }

    @Inject
    private lateinit var configurateConfigAdapter:LpIbConfigurateConfigAdapter
    override fun provideConfigurationAdapter(): ConfigurationAdapter = configurateConfigAdapter
    override fun getConfiguration(): LuckPermsConfiguration {
        return super.getConfiguration()
    }

    @Inject
    private lateinit var messageFactory: LpIbMessageFactory
    override fun provideMessagingFactory(): MessagingFactory<*> = messageFactory

    override fun registerCommands() {
        // InkerBot should register commands in event
    }

    @EventHandler
    fun onInitialization(event: LifecycleEvent.Initialization){
        load()
    }

    @EventHandler
    fun onLoadComplete(event: LifecycleEvent.LoadComplete){
        enable()
    }

    @EventHandler
    fun shutdown(event: LifecycleEvent.ServerStopping){
        disable()
    }

    override fun getGlobalDependencies(): Set<Dependency> {
        return emptySet()
    }

    @EventHandler
    fun onRegisterCommand(event:LifecycleEvent.RegisterCommand){
        event.register("lp"){
            helpHandler {
                commandManager.executeCommand(
                    senderFactory.wrap(it.source),
                    "lp",
                    emptyList()
                )
            }
            describe = "Luckperms"
            argument("input",InkerBot(LpIbCommandManager.LpType::class)){
                describe = "Input"
                executes {
                    commandManager.executeCommand(
                        senderFactory.wrap(it.source),
                        "lp",
                        it.getArgument("input",Array<String>::class.java).toMutableList()
                    ).get()
                }
            }
        }
    }

    override fun testUsernameValidity(username: String): Boolean {
        return senderFactory.testUsernameValidity(username)
    }

    override fun setupManagers() {
        //
    }

    @Inject
    private lateinit var lpIbCalculatorFactory: LpIbCalculatorFactory
    override fun provideCalculatorFactory(): CalculatorFactory = lpIbCalculatorFactory

    override fun setupPlatformHooks() {
        //
    }

    private val eventBus: AbstractEventBus<PluginContainer> by lazy { InkerBot() }
    override fun provideEventBus(apiProvider: LuckPermsApiProvider): AbstractEventBus<PluginContainer> = eventBus

    override fun registerApiOnPlatform(api: LuckPerms) {
        //
    }

    override fun performFinalSetup() {
        //
    }
}
package bot.inker.lp.manager

import bot.inker.api.event.AutoComponent
import bot.inker.api.event.EventContextKey
import bot.inker.api.event.EventHandler
import bot.inker.api.event.Order
import bot.inker.api.event.lifestyle.LifecycleEvent
import bot.inker.api.event.message.MessageEvent
import bot.inker.api.model.Member
import bot.inker.api.util.ResourceKey
import bot.inker.lp.LpIbSenderFactory
import me.lucko.luckperms.common.context.manager.ContextManager
import me.lucko.luckperms.common.context.manager.QueryOptionsSupplier
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import me.lucko.luckperms.common.util.CaffeineFactory
import net.luckperms.api.context.ImmutableContextSet
import net.luckperms.api.query.QueryMode
import net.luckperms.api.query.QueryOptions
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AutoComponent
class LpIbEventContextManager @Inject constructor(plugin: LuckPermsPlugin): ContextManager<MessageEvent,MessageEvent>(plugin,MessageEvent::class.java,MessageEvent::class.java){
    @Inject
    private lateinit var senderFactory:LpIbSenderFactory
    val CONTEXT_CACHE_KEY:EventContextKey<QueryOptions> = EventContextKey.of(ResourceKey.of("luckperms","context-cache"))

    override fun getUniqueId(player: MessageEvent): UUID  = senderFactory.getUniqueId(player)

    override fun getCacheFor(subject: MessageEvent): QueryOptionsSupplier {
        return QueryOptionsSupplier { subject.context.getOrSave<QueryOptions>(CONTEXT_CACHE_KEY){ calculate(subject) } }
    }

    override fun formQueryOptions(subject: MessageEvent, contextSet: ImmutableContextSet): QueryOptions {
        return QueryOptions.builder(QueryMode.CONTEXTUAL).context(contextSet).build()
    }

    override fun invalidateCache(subject: MessageEvent) {
        //
    }

    @EventHandler(order = Order.LATE)
    fun get(event:LifecycleEvent.LoadComplete){
        registerCalculator { target, consumer ->
            consumer.accept("imink","yes")
        }
    }
}
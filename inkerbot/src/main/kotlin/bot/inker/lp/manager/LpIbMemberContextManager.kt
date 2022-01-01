package bot.inker.lp.manager

import bot.inker.api.model.Member
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
class LpIbMemberContextManager @Inject constructor(plugin: LuckPermsPlugin): ContextManager<Member,Member>(plugin,Member::class.java,Member::class.java){
    @Inject
    private lateinit var senderFactory:LpIbSenderFactory
    private val contextsCache = CaffeineFactory.newBuilder()
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build { subject: Member ->
            calculate(subject)
        }

    override fun getUniqueId(player: Member): UUID  = senderFactory.getUniqueId(player)

    override fun getCacheFor(subject: Member): QueryOptionsSupplier {
        return QueryOptionsSupplier { contextsCache.get(subject) }
    }

    override fun formQueryOptions(subject: Member, contextSet: ImmutableContextSet): QueryOptions {
        return QueryOptions.builder(QueryMode.CONTEXTUAL).build()
    }

    override fun invalidateCache(subject: Member) {
        contextsCache.invalidate(subject.identity)
    }
}
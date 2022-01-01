package bot.inker.lp

import bot.inker.api.InkerBot
import bot.inker.api.model.Member
import bot.inker.api.util.Identity
import bot.inker.lp.LpIbSenderFactory
import com.google.common.cache.CacheBuilder
import me.lucko.luckperms.common.context.manager.ContextManager
import me.lucko.luckperms.common.context.manager.QueryOptionsCache
import me.lucko.luckperms.common.context.manager.QueryOptionsSupplier
import me.lucko.luckperms.common.messaging.MessagingFactory
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import net.luckperms.api.context.ImmutableContextSet
import net.luckperms.api.query.QueryMode
import net.luckperms.api.query.QueryOptions
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbMessageFactory @Inject constructor(plugin: LuckPermsPlugin): MessagingFactory<LuckPermsPlugin>(plugin){

}
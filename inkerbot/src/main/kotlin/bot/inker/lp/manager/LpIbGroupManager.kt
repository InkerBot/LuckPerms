package bot.inker.lp.manager

import me.lucko.luckperms.common.model.manager.group.StandardGroupManager
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbGroupManager @Inject constructor(plugin: LuckPermsPlugin): StandardGroupManager(plugin)
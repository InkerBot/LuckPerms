package bot.inker.lp.manager

import me.lucko.luckperms.common.model.manager.user.StandardUserManager
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbUserManager @Inject constructor(plugin: LuckPermsPlugin): StandardUserManager(plugin)
package bot.inker.lp.manager

import me.lucko.luckperms.common.model.manager.track.StandardTrackManager
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbTrackManager @Inject constructor(plugin: LuckPermsPlugin): StandardTrackManager(plugin)
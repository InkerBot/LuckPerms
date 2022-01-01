package bot.inker.lp

import me.lucko.luckperms.common.cacheddata.CacheMetadata
import me.lucko.luckperms.common.calculator.CalculatorFactory
import me.lucko.luckperms.common.calculator.PermissionCalculator
import me.lucko.luckperms.common.calculator.processor.*
import me.lucko.luckperms.common.config.ConfigKeys
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import net.luckperms.api.query.QueryOptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbCalculatorFactory:CalculatorFactory {
    @Inject
    private lateinit var plugin: LpIbPlugin
    override fun build(queryOptions: QueryOptions, metadata: CacheMetadata): PermissionCalculator {
        val processors = ArrayList<PermissionProcessor>()
        processors.add(DirectProcessor())
        if (plugin.configuration.get(ConfigKeys.APPLYING_REGEX)) {
            processors.add(RegexProcessor())
        }
        if (plugin.configuration.get(ConfigKeys.APPLYING_WILDCARDS)) {
            processors.add(WildcardProcessor())
        }
        return PermissionCalculator(plugin, metadata, processors)
    }
}
package bot.inker.lp.manager

import bot.inker.api.event.message.MessageEvent
import bot.inker.api.model.Member
import bot.inker.lp.LpIbSenderFactory
import com.eloli.inkcmd.StringReader
import com.eloli.inkcmd.context.CommandContext
import com.eloli.inkcmd.suggestion.Suggestions
import com.eloli.inkcmd.suggestion.SuggestionsBuilder
import com.eloli.inkcmd.values.ValueType
import me.lucko.luckperms.common.command.CommandManager
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import me.lucko.luckperms.common.sender.Sender
import me.lucko.luckperms.common.sender.SenderFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbCommandManager @Inject constructor(plugin: LuckPermsPlugin) : CommandManager(plugin){
    @Singleton
    class LpType: ValueType<Array<String>> {
        @Inject
        private lateinit var commandManager: LpIbCommandManager
        @Inject
        private lateinit var senderFactory: LpIbSenderFactory
        override fun parse(reader: StringReader): Array<String> {
            val result = reader.remaining.split(" ").toTypedArray()
            reader.cursor = reader.totalLength
            return result
        }

        override fun <S : Any> listSuggestions(
            context: CommandContext<S>,
            builder: SuggestionsBuilder
        ): Suggestions {
            val splits = builder.remaining.split(" ")
            val completions = commandManager.tabCompleteCommand(
                senderFactory.wrap((context.source as MessageEvent).sender),
                splits
            )
            for (completion in completions) {
                builder.suggest(completion)
            }
            return builder.build()
        }
    }
}
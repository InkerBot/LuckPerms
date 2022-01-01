package bot.inker.lp

import bot.inker.api.model.message.MessageComponent
import bot.inker.api.model.message.PlainTextComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import javax.inject.Singleton

@Singleton
class LpIbMessageTranslator {
    private val translator = PlainTextComponentSerializer.plainText()
    fun toInk(component: Component) = PlainTextComponent.of(translator.serialize(component))
    fun toLp(message:MessageComponent) = Component.text(message.toString())
    fun toPlain(component: Component) = translator.serialize(component)
    fun toPlain(message:MessageComponent) = message.toString()
}
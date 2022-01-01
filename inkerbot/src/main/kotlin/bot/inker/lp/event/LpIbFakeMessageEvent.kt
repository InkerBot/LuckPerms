package bot.inker.lp.event

import bot.inker.api.event.EventContext
import bot.inker.api.event.message.MessageEvent
import bot.inker.api.model.Member
import bot.inker.api.model.message.MessageComponent
import bot.inker.api.model.message.PlainTextComponent

class LpIbFakeMessageEvent(override val sender: Member) :MessageEvent {
    override var cancelled: Boolean = false
    override val context: EventContext = EventContext.empty()
    override val message: MessageComponent = PlainTextComponent.of("")
    override fun sendMessage(message: MessageComponent) {
        sender.sendMessage(message)
    }
}
package bot.inker.lp

import bot.inker.api.InkerBot
import bot.inker.api.command.MemberValueType
import bot.inker.api.command.UUIDValueType
import bot.inker.api.event.AutoComponent
import bot.inker.api.event.message.MessageEvent
import bot.inker.api.model.ConsoleSender
import bot.inker.api.model.Member
import bot.inker.api.registry.Registries
import bot.inker.api.service.CommandService
import bot.inker.api.util.Identity
import bot.inker.lp.event.LpIbFakeMessageEvent
import com.eloli.inkcmd.StringReader
import com.eloli.inkcmd.exceptions.CommandSyntaxException
import com.github.benmanes.caffeine.cache.Caffeine
import me.lucko.luckperms.common.locale.TranslationManager
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter
import me.lucko.luckperms.common.sender.Sender
import me.lucko.luckperms.common.sender.SenderFactory
import net.kyori.adventure.text.Component
import net.luckperms.api.util.Tristate
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AutoComponent
open class LpIbSenderFactory @Inject constructor(plugin: LpIbPlugin) : SenderFactory<LpIbPlugin, MessageEvent>(plugin) {
    private val commandService:CommandService by lazy { InkerBot() }
    private val messageTranslator:LpIbMessageTranslator by lazy { InkerBot() }
    private val connectionListener: LpIbConnectionListener by lazy { InkerBot() }
    private val schedulerAdapter: SchedulerAdapter by lazy { InkerBot() }

    private val memberCache = Caffeine.newBuilder()
        .removalListener<UUID,Member> { key, value, cause ->
            if(value != null){
                connectionListener.onLogout(value)
            }
        }
        .expireAfterAccess(60,TimeUnit.SECONDS)
        .executor(schedulerAdapter.async())
        .build<UUID,Member>{
            val optionalMember = Registries.member.get(Identity.of(it))
            if(optionalMember.isEmpty){
                null
            }else{
                val member = optionalMember.get()
                connectionListener.onLogin(member)
                member
            }
        }

    public override fun getUniqueId(event: MessageEvent): UUID = getUniqueId(event.sender)
    fun getUniqueId(member: Member): UUID = member.identity.uuid

    public override fun isConsole(event: MessageEvent): Boolean = event.sender is ConsoleSender

    public override fun performCommand(event: MessageEvent, command: String) = commandService.execute(event.sender,command)

    public override fun getName(event: MessageEvent): String = getName(event.sender)

    public override fun sendMessage(event: MessageEvent, message: Component) {
        event.sendMessage(
            messageTranslator.toInk(
                TranslationManager.render(message)
            )
        )
    }

    fun wrap(sender: Member): Sender {
        return wrap(LpIbFakeMessageEvent(sender))
    }

    fun getOnlineSenders(): Stream<Sender> {
        return getOnlineMembers().stream().map(this::wrap)
    }

    fun getOnlineMembers(): Collection<Member> {
        return memberCache.asMap().map { it.value }
    }

    fun getMember(uniqueId: UUID): Optional<Member> {
        return Optional.ofNullable(
            memberCache.get(uniqueId)
        )
    }

    fun lookupUniqueId(username: String): Optional<UUID> {
        return try {
            val uuid = if(username.startsWith('[')){
                username
            }else{
                username.substringAfterLast(':')
            }
            Optional.of(MemberValueType.of().parse(
                StringReader(uuid)
            ).identity.uuid)
        }catch (e: CommandSyntaxException){
            Optional.empty()
        }
    }
    fun testUsernameValidity(username: String): Boolean {
        try {
            val uuid = if(username.startsWith('[')){
                username
            }else{
                username.substringAfterLast(':')
            }
            UUIDValueType.of().parse(
                StringReader(uuid)
            )
            return true
        }catch (e: CommandSyntaxException){
            return false
        }
    }

    val nameUnsafeMap = mapOf<Any,String>(
        " " to ""
    )
    fun getName(member: Member):String{
        var name = member.name
        for (entry in nameUnsafeMap) {
            val key = entry.key
            name = when(key){
                is String -> {
                    name.replace(key,entry.value)
                }
                is Char -> {
                    name.replace(key.toString(),entry.value)
                }
                is Regex -> {
                    name.replace(key,entry.value)
                }
                else -> {
                    name.replace(key.toString(),entry.value)
                }
            }
        }
        return "$name:${member.identity}"
    }

    fun lookupUsername(uniqueId: UUID): Optional<String> {
        return getMember(uniqueId).map(Member::name)
    }

    fun getPlayerCount(): Int {
        return memberCache.asMap().size
    }

    fun isPlayerOnline(uniqueId: UUID): Boolean {
        return getMember(uniqueId).isPresent
    }

    private val permissionService:LpIbPermissionService by lazy { InkerBot() }
    override fun getPermissionValue(event: MessageEvent, node: String): Tristate {
        return permissionService.checkPermission(event.sender,node)
    }

    override fun hasPermission(event: MessageEvent, node: String): Boolean {
        return getPermissionValue(event,node) == Tristate.TRUE
    }
}
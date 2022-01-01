package bot.inker.lp

import bot.inker.api.InkerBot
import bot.inker.api.event.AutoComponent
import bot.inker.api.event.EventHandler
import bot.inker.api.event.lifestyle.LifecycleEvent
import bot.inker.api.event.message.MessageEvent
import bot.inker.api.model.ConsoleSender
import bot.inker.api.model.Member
import bot.inker.api.permission.PermissionResult
import bot.inker.api.service.PermissionService
import bot.inker.lp.manager.LpIbEventContextManager
import bot.inker.lp.manager.LpIbMemberContextManager
import bot.inker.lp.manager.LpIbUserManager
import me.lucko.luckperms.common.verbose.VerboseCheckTarget
import me.lucko.luckperms.common.verbose.event.PermissionCheckEvent
import net.luckperms.api.util.Tristate
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AutoComponent
class LpIbPermissionService:PermissionService {
    val senderFactory: LpIbSenderFactory by InkerBot.lazy()
    val userManager: LpIbUserManager by InkerBot.lazy()
    val plugin: LpIbPlugin by InkerBot.lazy()
    val memberContextManager: LpIbMemberContextManager by InkerBot.lazy()
    val eventContextManager: LpIbEventContextManager by InkerBot.lazy()

    fun checkPermission(member: Member,permission:String): Tristate {
        if(member is ConsoleSender){
            return Tristate.TRUE
        }
        val user = userManager.getOrMake(senderFactory.getUniqueId(member),senderFactory.getName(member))
        return user.cachedData
            .permissionData
            .checkPermission(permission, PermissionCheckEvent.Origin.PLATFORM_PERMISSION_CHECK)
            .result()
    }

    // TODO: Event Manager
    fun checkPermission(event: MessageEvent,permission:String): Tristate {
        return checkPermission(event.sender, permission)
    }

    @EventHandler
    fun registerService(event:LifecycleEvent.RegisterService){
        event.register {
            bind(PermissionService::class.java).toInstance(this@LpIbPermissionService)
        }
    }

    override fun getPermission(event: MessageEvent, permission: String): PermissionResult {
        return cast(checkPermission(event,permission))
    }

    override fun getPermission(member: Member, permission: String): PermissionResult {
        return cast(checkPermission(member,permission))
    }

    override fun hasPermission(event: MessageEvent, permission: String): Boolean {
        return getPermission(event,permission) == PermissionResult.TRUE
    }

    override fun hasPermission(member: Member, permission: String): Boolean {
        return getPermission(member, permission) == PermissionResult.TRUE
    }

    fun cast(lp:Tristate):PermissionResult{
        return when (lp) {
            Tristate.TRUE -> PermissionResult.TRUE
            Tristate.FALSE -> PermissionResult.FALSE
            Tristate.UNDEFINED -> PermissionResult.UNDEFINED
        }
    }

    fun cast(ink:PermissionResult):Tristate{
        return when (ink) {
            PermissionResult.TRUE -> Tristate.TRUE
            PermissionResult.FALSE -> Tristate.FALSE
            PermissionResult.UNDEFINED -> Tristate.UNDEFINED
        }
    }
}
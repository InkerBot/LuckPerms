/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package bot.inker.lp

import bot.inker.api.InkerBot
import bot.inker.api.model.Member
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LpIbConnectionListener @Inject constructor(private val plugin: LpIbPlugin) : AbstractConnectionListener(plugin) {
    private val senderFactory: LpIbSenderFactory by lazy{ InkerBot() }
    fun onLogin(member: Member) {
        val uuid = senderFactory.getUniqueId(member)
        val name = senderFactory.getName(member)
        if (uniqueConnections.contains(uuid)) {
            this.plugin.logger.warn("Member $member have login twice.")
            onLogout(member)
        }
        try {
            val user = loadUser(
                uuid,
                name
            )
            recordConnection(user.uniqueId)
            this.plugin.eventDispatcher.dispatchPlayerLoginProcess(
                uuid,
                name,
                user
            )
        } catch (ex: Exception) {
            this.plugin.logger.severe("Exception occurred whilst loading data for $member", ex)
            this.plugin.eventDispatcher.dispatchPlayerLoginProcess(member.identity.uuid, member.name, null)
        }
    }

    fun onLogout(member: Member) {
        val uuid = senderFactory.getUniqueId(member)
        handleDisconnect(uuid)
        uniqueConnections.remove(uuid)
    }
}
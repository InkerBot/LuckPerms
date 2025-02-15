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

package me.lucko.luckperms.common.webeditor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.nodemap.MutateResult;
import me.lucko.luckperms.common.node.utils.NodeJsonSerializer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Uuids;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.event.cause.DeletionCause;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates a response from the web editor.
 */
public class WebEditorResponse {

    /**
     * The id of the response payload
     */
    private final String id;

    /**
     * The encoded json object this payload is made up of
     */
    private final JsonObject payload;

    public WebEditorResponse(String id, JsonObject payload) {
        this.id = id;
        this.payload = payload;
    }

    /**
     * Applies the response to storage, sending appropriate informational messages to the sender.
     *
     * @param plugin the plugin
     * @param sender the sender who is applying the session
     */
    public void apply(LuckPermsPlugin plugin, Sender sender, String commandLabel, boolean ignoreSessionWarning) {
        JsonElement sessionIdJson = this.payload.get("sessionId");
        if (sessionIdJson != null) {
            String sessionId = sessionIdJson.getAsString();
            WebEditorSessionStore sessionStore = plugin.getWebEditorSessionStore();

            SessionState state = sessionStore.getSessionState(sessionId);
            switch (state) {
                case COMPLETED:
                    if (!ignoreSessionWarning) {
                        Message.APPLY_EDITS_SESSION_APPLIED_ALREADY.send(sender, this.id, commandLabel);
                        return;
                    }
                    break;
                case NOT_KNOWN:
                    if (!ignoreSessionWarning) {
                        Message.APPLY_EDITS_SESSION_UNKNOWN.send(sender, this.id, commandLabel);
                        return;
                    }
                    break;
                case IN_PROGRESS:
                    sessionStore.markSessionCompleted(sessionId);
                    break;
                default:
                    throw new AssertionError(state);
            }
        }

        Session session = new Session(plugin, sender);
        boolean work = false;

        if (this.payload.has("changes")) {
            JsonArray changes = this.payload.get("changes").getAsJsonArray();
            for (JsonElement change : changes) {
                if (session.applyChange(change.getAsJsonObject())) {
                    work = true;
                }
            }
        }
        if (this.payload.has("userDeletions")) {
            JsonArray userDeletions = this.payload.get("userDeletions").getAsJsonArray();
            for (JsonElement userDeletion : userDeletions) {
                if (session.applyUserDelete(userDeletion)) {
                    work = true;
                }
            }
        }
        if (this.payload.has("groupDeletions")) {
            JsonArray groupDeletions = this.payload.get("groupDeletions").getAsJsonArray();
            for (JsonElement groupDeletion : groupDeletions) {
                if (session.applyGroupDelete(groupDeletion)) {
                    work = true;
                }
            }
        }
        if (this.payload.has("trackDeletions")) {
            JsonArray trackDeletions = this.payload.get("trackDeletions").getAsJsonArray();
            for (JsonElement trackDeletion : trackDeletions) {
                if (session.applyTrackDelete(trackDeletion)) {
                    work = true;
                }
            }
        }

        if (!work) {
            session.getBuilder().append(Message.APPLY_EDITS_TARGET_NO_CHANGES_PRESENT.build()).append(Component.newline());
        }
        sender.sendMessage(session.build());
    }

    /**
     * Represents the application of a given editor session on this platform.
     */
    private static class Session {
        private final LuckPermsPlugin plugin;
        private final Sender sender;
        private final TextComponent.Builder builder = Component.text();

        Session(LuckPermsPlugin plugin, Sender sender) {
            this.plugin = plugin;
            this.sender = sender;
        }

        private boolean applyChange(JsonObject changeInfo) {
            String type = changeInfo.get("type").getAsString();

            if (type.equals("user") || type.equals("group")) {
                return applyHolderChange(changeInfo);
            } else if (type.equals("track")) {
                return applyTrackChange(changeInfo);
            } else {
                builder.append(Message.APPLY_EDITS_UNKNOWN_TYPE.build(type)).append(Component.newline());
                return false;
            }
        }

        private boolean applyHolderChange(JsonObject changeInfo) {
            String type = changeInfo.get("type").getAsString();
            String id = changeInfo.get("id").getAsString();

            PermissionHolder holder;
            if (type.equals("user")) {
                // user
                UUID uuid = Uuids.parse(id);
                if (uuid == null) {
                    builder.append(Message.APPLY_EDITS_TARGET_USER_NOT_UUID.build(id)).append(Component.newline());
                    return false;
                }
                holder = this.plugin.getStorage().loadUser(uuid, null).join();
                if (holder == null) {
                    builder.append(Message.APPLY_EDITS_TARGET_USER_UNABLE_TO_LOAD.build(uuid.toString()));
                    return false;
                }
            } else {
                // group
                holder = this.plugin.getStorage().loadGroup(id).join().orElse(null);
                if (holder == null) {
                    holder = this.plugin.getStorage().createAndLoadGroup(id, CreationCause.WEB_EDITOR).join();
                }
            }

            if (ArgumentPermissions.checkModifyPerms(this.plugin, this.sender, CommandPermission.APPLY_EDITS, holder) || ArgumentPermissions.checkGroup(this.plugin, this.sender, holder, ImmutableContextSetImpl.EMPTY)) {
                builder.append(Message.COMMAND_NO_PERMISSION.build()).append(Component.newline());
                return false;
            }

            Set<Node> nodes = NodeJsonSerializer.deserializeNodes(changeInfo.getAsJsonArray("nodes"));
            MutateResult res = holder.setNodes(DataType.NORMAL, nodes, true);

            if (res.isEmpty()) {
                return false;
            }

            Set<Node> added = res.getAdded();
            Set<Node> removed = res.getRemoved();

            for (Node n : added) {
                LoggedAction.build().source(this.sender).target(holder)
                        .description("webeditor", "add", n.getKey(), n.getValue(), n.getContexts())
                        .build().submit(this.plugin, this.sender);
            }
            for (Node n : removed) {
                LoggedAction.build().source(this.sender).target(holder)
                        .description("webeditor", "remove", n.getKey(), n.getValue(), n.getContexts())
                        .build().submit(this.plugin, this.sender);
            }

            builder.append(Message.APPLY_EDITS_SUCCESS.build(type, holder.getFormattedDisplayName())).append(Component.newline());
            builder.append(Message.APPLY_EDITS_SUCCESS_SUMMARY.build(added.size(), removed.size())).append(Component.newline());
            for (Node n : added) {
                builder.append(Message.APPLY_EDITS_DIFF_ADDED.build(n)).append(Component.newline());
            }
            for (Node n : removed) {
                builder.append(Message.APPLY_EDITS_DIFF_REMOVED.build(n)).append(Component.newline());
            }
            StorageAssistant.save(holder, this.sender, this.plugin);
            return true;
        }

        private boolean applyTrackChange(JsonObject changeInfo) {
            String id = changeInfo.get("id").getAsString();

            Track track = this.plugin.getStorage().loadTrack(id).join().orElse(null);
            if (track == null) {
                track = this.plugin.getStorage().createAndLoadTrack(id, CreationCause.WEB_EDITOR).join();
            }

            if (ArgumentPermissions.checkModifyPerms(this.plugin, this.sender, CommandPermission.APPLY_EDITS, track)) {
                builder.append(Message.COMMAND_NO_PERMISSION.build()).append(Component.newline());
                return false;
            }

            List<String> before = track.getGroups();
            List<String> after = new ArrayList<>();
            changeInfo.getAsJsonArray("groups").forEach(e -> after.add(e.getAsString()));

            if (before.equals(after)) {
                return false;
            }

            Set<String> diffAdded = getAdded(before, after);
            Set<String> diffRemoved = getRemoved(before, after);

            int additions = diffAdded.size();
            int deletions = diffRemoved.size();

            track.setGroups(after);

            if (hasBeenReordered(before, after, diffAdded, diffRemoved)) {
                LoggedAction.build().source(this.sender).target(track)
                        .description("webeditor", "reorder", after)
                        .build().submit(this.plugin, this.sender);
            }
            for (String n : diffAdded) {
                LoggedAction.build().source(this.sender).target(track)
                        .description("webeditor", "add", n)
                        .build().submit(this.plugin, this.sender);
            }
            for (String n : diffRemoved) {
                LoggedAction.build().source(this.sender).target(track)
                        .description("webeditor", "remove", n)
                        .build().submit(this.plugin, this.sender);
            }

            builder.append(Message.APPLY_EDITS_SUCCESS.build("track", Component.text(track.getName()))).append(Component.newline());
            builder.append(Message.APPLY_EDITS_SUCCESS_SUMMARY.build(additions, deletions)).append(Component.newline());
            builder.append(Message.APPLY_EDITS_TRACK_BEFORE.build(before)).append(Component.newline());
            builder.append(Message.APPLY_EDITS_TRACK_AFTER.build(after)).append(Component.newline());

            StorageAssistant.save(track, this.sender, this.plugin);
            return true;
        }

        private boolean applyUserDelete(JsonElement changeInfo) {
            String id = changeInfo.getAsString();

            UUID uuid = Uuids.parse(id);
            if (uuid == null) {
                builder.append(Message.APPLY_EDITS_TARGET_USER_NOT_UUID.build(id)).append(Component.newline());
                return false;
            }

            User user = this.plugin.getStorage().loadUser(uuid, null).join();
            if (user == null) {
                try {
                    this.plugin.getStorage().deletePlayerData(uuid).get();
                } catch (Exception e) {
                    e.printStackTrace();
                    builder.append(Message.DELETE_ERROR.build(Component.text(uuid.toString()))).append(Component.newline());
                }
                return true;
            }

            if (ArgumentPermissions.checkModifyPerms(this.plugin, this.sender, CommandPermission.APPLY_EDITS, user)) {
                builder.append(Message.COMMAND_NO_PERMISSION.build()).append(Component.newline());
                return false;
            }

            user.clearNodes(DataType.NORMAL, null, true);

            try {
                StorageAssistant.save(user, this.sender, this.plugin);
                this.plugin.getStorage().deletePlayerData(user.getUniqueId()).get();
            } catch (Exception e) {
                e.printStackTrace();
                builder.append(Message.DELETE_ERROR.build(user.getFormattedDisplayName())).append(Component.newline());
                return true;
            }

            builder.append(Message.DELETE_SUCCESS.build(user.getFormattedDisplayName())).append(Component.newline());

            LoggedAction.build().source(this.sender).target(user).targetType(Action.Target.Type.USER)
                    .description("webeditor", "delete")
                    .build().submit(this.plugin, this.sender);

            return true;
        }

        private boolean applyGroupDelete(JsonElement changeInfo) {
            String groupName = changeInfo.getAsString();

            if (groupName.equalsIgnoreCase(GroupManager.DEFAULT_GROUP_NAME)) {
                builder.append(Message.DELETE_GROUP_ERROR_DEFAULT.build()).append(Component.newline());
                return true;
            }

            Group group = this.plugin.getStorage().loadGroup(groupName).join().orElse(null);
            if (group == null) {
                return false;
            }

            if (ArgumentPermissions.checkModifyPerms(this.plugin, this.sender, CommandPermission.APPLY_EDITS, group) || ArgumentPermissions.checkGroup(this.plugin, this.sender, group, ImmutableContextSetImpl.EMPTY)) {
                builder.append(Message.COMMAND_NO_PERMISSION.build()).append(Component.newline());
                return false;
            }

            try {
                this.plugin.getStorage().deleteGroup(group, DeletionCause.WEB_EDITOR).get();
            } catch (Exception e) {
                e.printStackTrace();
                builder.append(Message.DELETE_ERROR.build(group.getFormattedDisplayName())).append(Component.newline());
                return true;
            }

            builder.append(Message.DELETE_SUCCESS.build(group.getFormattedDisplayName())).append(Component.newline());

            LoggedAction.build().source(this.sender).target(group)
                    .description("webeditor", "delete")
                    .build().submit(this.plugin, this.sender);

            return true;
        }

        private boolean applyTrackDelete(JsonElement changeInfo) {
            String trackName = changeInfo.getAsString();

            Track track = this.plugin.getStorage().loadTrack(trackName).join().orElse(null);
            if (track == null) {
                return false;
            }

            if (ArgumentPermissions.checkModifyPerms(this.plugin, this.sender, CommandPermission.APPLY_EDITS, track)) {
                builder.append(Message.COMMAND_NO_PERMISSION.build()).append(Component.newline());
                return false;
            }

            try {
                this.plugin.getStorage().deleteTrack(track, DeletionCause.WEB_EDITOR).get();
            } catch (Exception e) {
                e.printStackTrace();
                builder.append(Message.DELETE_ERROR.build(Component.text(track.getName()))).append(Component.newline());
                return true;
            }

            builder.append(Message.DELETE_SUCCESS.build(Component.text(trackName))).append(Component.newline());

            LoggedAction.build().source(this.sender).target(track)
                    .description("webeditor", "delete")
                    .build().submit(this.plugin, this.sender);

            return true;
        }

        private static <T> Set<T> getAdded(Collection<T> before, Collection<T> after) {
            Set<T> added = new LinkedHashSet<>(after);
            added.removeAll(before);
            return added;
        }

        private static <T> Set<T> getRemoved(Collection<T> before, Collection<T> after) {
            Set<T> removed = new LinkedHashSet<>(before);
            removed.removeAll(after);
            return removed;
        }

        private static <T> boolean hasBeenReordered(List<T> before, List<T> after, Collection<T> diffAdded, Collection<T> diffRemoved) {
            after = new ArrayList<>(after);
            before = new ArrayList<>(before);

            after.removeAll(diffAdded);
            before.removeAll(diffRemoved);

            return !before.equals(after);
        }

        public TextComponent.Builder getBuilder() {
            return builder;
        }

        public TextComponent build() {
            return builder.build();
        }
    }
}

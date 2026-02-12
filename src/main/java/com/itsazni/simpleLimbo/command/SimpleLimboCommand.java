package com.itsazni.simpleLimbo.command;

import com.itsazni.simpleLimbo.SimpleLimbo;
import com.itsazni.simpleLimbo.util.MessageUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SimpleLimboCommand implements SimpleCommand {

    private final SimpleLimbo plugin;

    public SimpleLimboCommand(SimpleLimbo plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("simplelimbo.admin")) {
            source.sendMessage(MessageUtil.component("&cYou do not have permission."));
            return;
        }

        if (args.length == 0) {
            sendHelp(source);
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "reload" -> {
                plugin.reload();
                source.sendMessage(MessageUtil.component("&aSimpleLimbo reloaded."));
            }
            case "list" -> {
                String names = String.join(", ", plugin.getLimboManager().getLimboNames());
                source.sendMessage(MessageUtil.component("&eLimbos: &f" + (names.isEmpty() ? "none" : names)));
            }
            case "send" -> handleSend(source, args);
            case "sendall" -> handleSendAll(source, args);
            case "info" -> handleInfo(source, args);
            default -> sendHelp(source);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            return List.of("reload", "list", "send", "sendall", "info");
        }

        if (args.length == 1) {
            return filter(List.of("reload", "list", "send", "sendall", "info"), args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("send") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("sendall"))) {
            if (args[0].equalsIgnoreCase("send")) {
                return filter(plugin.getServer().getAllPlayers().stream().map(Player::getUsername).collect(Collectors.toList()), args[1]);
            }
            return filter(new ArrayList<>(plugin.getLimboManager().getLimboNames()), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("send")) {
            return filter(new ArrayList<>(plugin.getLimboManager().getLimboNames()), args[2]);
        }

        return List.of();
    }

    private void handleSend(CommandSource source, String[] args) {
        if (args.length < 3) {
            source.sendMessage(MessageUtil.component("&cUsage: /simplelimbo send <player> <limbo>"));
            return;
        }

        plugin.getServer().getPlayer(args[1]).ifPresentOrElse(player -> {
            boolean ok = plugin.getLimboManager().sendPlayerToLimbo(player, args[2]);
            if (ok) {
                source.sendMessage(MessageUtil.component("&aSent &f" + player.getUsername() + " &ato limbo &f" + args[2]));
            } else {
                source.sendMessage(MessageUtil.component("&cFailed to send player. Limbo not found."));
            }
        }, () -> source.sendMessage(MessageUtil.component("&cPlayer not found.")));
    }

    private void handleSendAll(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(MessageUtil.component("&cUsage: /simplelimbo sendall <limbo>"));
            return;
        }

        String limbo = args[1];
        int sent = 0;
        for (Player player : plugin.getServer().getAllPlayers()) {
            if (plugin.getLimboManager().sendPlayerToLimbo(player, limbo)) {
                sent++;
            }
        }

        source.sendMessage(MessageUtil.component("&aSent &f" + sent + " &aplayer(s) to limbo &f" + limbo));
    }

    private void handleInfo(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(MessageUtil.component("&cUsage: /simplelimbo info <limbo>"));
            return;
        }

        plugin.getLimboManager().getLimbo(args[1]).ifPresentOrElse(instance -> {
            source.sendMessage(MessageUtil.component("&eLimbo: &f" + instance.getName()));
            source.sendMessage(MessageUtil.component("&ePlayers: &f" + plugin.getLimboManager().getPlayerCount(instance.getName())));
            source.sendMessage(MessageUtil.component("&eDimension: &f" + instance.getConfig().getDimension()));
            source.sendMessage(MessageUtil.component("&eGamemode: &f" + instance.getConfig().getGamemode()));
        }, () -> source.sendMessage(MessageUtil.component("&cLimbo not found.")));
    }

    private void sendHelp(CommandSource source) {
        source.sendMessage(MessageUtil.component("&eSimpleLimbo commands:"));
        source.sendMessage(MessageUtil.component("&7/simplelimbo reload"));
        source.sendMessage(MessageUtil.component("&7/simplelimbo list"));
        source.sendMessage(MessageUtil.component("&7/simplelimbo send <player> <limbo>"));
        source.sendMessage(MessageUtil.component("&7/simplelimbo sendall <limbo>"));
        source.sendMessage(MessageUtil.component("&7/simplelimbo info <limbo>"));
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}

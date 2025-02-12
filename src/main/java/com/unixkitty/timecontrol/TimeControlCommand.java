package com.unixkitty.timecontrol;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.unixkitty.timecontrol.network.ModNetworkDispatcher;
import com.unixkitty.timecontrol.network.packet.ConfigS2CPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class TimeControlCommand
{
    private static final String value_string = "value";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(TimeControl.MODID)
                .requires(source -> source.hasPermission(2));

        registerCommand(Config.DAY_LENGTH_MINUTES, IntegerArgumentType.integer(1, Config.LENGTH_LIMIT), Config.day_length_minutes, command);
        registerCommand(Config.NIGHT_LENGTH_MINUTES, IntegerArgumentType.integer(1, Config.LENGTH_LIMIT), Config.night_length_minutes, command);
        registerCommand(Config.SYNC_TO_SYSTEM_TIME, BoolArgumentType.bool(), Config.sync_to_system_time, command);
        registerCommand(Config.SYNC_TO_SYSTEM_TIME_RATE, IntegerArgumentType.integer(1, Config.SYNC_TO_SYSTEM_TIME_RATE_LIMIT), Config.sync_to_system_time_rate, command);

        dispatcher.register(command);
    }

    private static void registerCommand(String name, ArgumentType<?> argument, Supplier<?> configValueSupplier, LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.then(Commands.literal(name)
                .then(Commands.argument(value_string, argument)
                        .executes(context -> setValue(context, name)))
                .executes(context -> sendFeedback(context, name, configValueSupplier.get(), false)));
    }

    private static int setValue(CommandContext<CommandSourceStack> context, String name)
    {
        Object value = Config.SYNC_TO_SYSTEM_TIME.equals(name) ? BoolArgumentType.getBool(context, value_string) : IntegerArgumentType.getInteger(context, value_string);

        switch (name)
        {
            case Config.DAY_LENGTH_MINUTES -> Config.day_length_minutes.set((Integer) value);
            case Config.NIGHT_LENGTH_MINUTES -> Config.night_length_minutes.set((Integer) value);
            case Config.SYNC_TO_SYSTEM_TIME_RATE -> Config.sync_to_system_time_rate.set((Integer) value);
            case Config.SYNC_TO_SYSTEM_TIME -> Config.sync_to_system_time.set((Boolean) value);
        }

        //Manually sync config so that clients don't need to relogin
        ModNetworkDispatcher.send(new ConfigS2CPacket());

        sendFeedback(context, name, value, true);

        return 0;
    }

    private static int sendFeedback(final CommandContext<CommandSourceStack> context, final String valueName, final Object value, boolean allowLogging)
    {
        context.getSource().sendSuccess(() -> Component.literal(valueName + " = " + value), allowLogging);

        return 0;
    }
}

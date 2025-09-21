package pw.kaboom.commandspy.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public final class StateArgumentType implements
    CustomArgumentType.Converted<@NotNull Boolean, @NotNull String> {

    private static final Collection<String> VALUES = Arrays.asList("on", "enable",
        "off", "disable");

    private static final DynamicCommandExceptionType ERROR_INVALID_VALUE =
        new DynamicCommandExceptionType(o ->
            MessageComponentSerializer.message()
                .serialize(Component.translatable("argument.enum.invalid")
                    .arguments(Component.text(o.toString()))));

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public Boolean convert(final String s) throws CommandSyntaxException {
        switch (s) {
            case "on", "enable" -> {
                return true;
            }
            case "off", "disable" -> {
                return false;
            }

            default -> throw ERROR_INVALID_VALUE.create(s);
        }
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(
        final @NotNull CommandContext<S> context, final @NotNull SuggestionsBuilder builder) {
        for (final String value: VALUES) {
            if (!value.startsWith(builder.getRemainingLowerCase())) continue;

            builder.suggest(value);
        }

        return builder.buildFuture();
    }

    public static boolean getState(final CommandContext<CommandSourceStack> context,
                                   final String name) {
        return context.getArgument(name, Boolean.class);
    }
}

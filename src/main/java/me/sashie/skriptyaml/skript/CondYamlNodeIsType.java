package me.sashie.skriptyaml.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import me.sashie.skriptyaml.debug.SkriptNode;
import me.sashie.skriptyaml.utils.SkriptYamlUtils;
import me.sashie.skriptyaml.utils.yaml.YAMLProcessor;
import org.bukkit.event.Event;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Name("YAML Node Is Type")
@Description("Checks if a YAML node is a specific type (list, value, or node).")
@Examples({
        "if yaml node \"test1.test2\" of \"config\" is value:",
        "    broadcast \"It's a value!\"",
        "else if yaml node \"my.list\" of \"config\" is list:",
        "    broadcast \"It's a list!\"",
        "else if yaml node \"test1\" of \"config\" is node:",
        "    broadcast \"It's a node!\""
})
@Since("1.7.2")
public class CondYamlNodeIsType extends Condition {

    static {
        Skript.registerCondition(CondYamlNodeIsType.class,
                "y[a]ml (node|path) %string% (of|in|from) %string% is [a] (1¦list|2¦node|3¦value)",
                "y[a]ml (node|path) %string% (of|in|from) %string% is(n't| not) [a] (1¦list|2¦node|3¦value)");
    }

    private Expression<String> node;
    private Expression<String> file;
    private int mark;
    private SkriptNode skriptNode;

    @Override
    public boolean check(Event event) {
        final String nodePath = this.node.getSingle(event);
        final String fileName = this.file.getSingle(event);
        if (nodePath == null || fileName == null) return false;

        YAMLProcessor yaml = SkriptYamlUtils.yamlExists(fileName, skriptNode);
        if (yaml == null) {
            return false;
        }

        Object property = yaml.getProperty(nodePath);
        boolean result = false;
        if (property == null) {
            result = false;
        } else if (mark == 1) { // list
            result = property instanceof List;
        } else if (mark == 2) { // node
            result = property instanceof Map;
        } else if (mark == 3) { // value
            result = !(property instanceof List) && !(property instanceof Map);
        }
        return result ^ isNegated();
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        String type = mark == 1 ? "list" : mark == 2 ? "node" : mark == 3 ? "value" : "unknown";
        return "yaml node " + this.node.toString(event, b) + " of " + this.file.toString(event, b) + " is " + type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parse) {
        node = (Expression<String>) exprs[0];
        file = (Expression<String>) exprs[1];
        this.mark = parse.mark;
        this.skriptNode = new SkriptNode(SkriptLogger.getNode());
        setNegated(matchedPattern == 1);
        return true;
    }
} 
package me.sashie.skriptyaml.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import me.sashie.skriptyaml.SkriptYaml;
import me.sashie.skriptyaml.debug.SkriptNode;
import me.sashie.skriptyaml.utils.yaml.YAMLAPI;
import org.bukkit.event.Event;

import javax.annotation.Nullable;

@Name("Change YAML ID")
@Description("Changes the ID of a loaded YAML file or reassigns its file path." +
		"\n  - The first syntax changes the ID of a YAML file. Any changes to the ID are made at your own risk as this can cause issues with any other scripts that use the YAML file." +
		"\n  - The second syntax reassigns the file path of a YAML file. This is useful when a file has been deleted but kept in memory." +
		"\n  - The YAML file must be loaded before changing its ID or file path." +
		"\n  - The new ID must not already be in use by another loaded YAML file.")
@Examples({
		"# Change the ID of a YAML file",
		"change yaml id from \"config\" to \"newconfig\"",
		" ",
		"# Change the ID of a YAML file with variables",
		"set {_oldId} to \"oldconfig\"",
		"set {_newId} to \"newconfig\"",
		"change yaml id from \"%{_oldId}%\" to \"%{_newId}%\"",
		" ",
		"# Reassign the file path of a deleted YAML",
		"change yaml \"config\" file path to \"plugins/MyPlugin/newconfig.yml\"",
		" ",
		"# Change multiple YAML IDs",
		"change yaml id from \"config1\" to \"newconfig1\"",
		"change yaml id from \"config2\" to \"newconfig2\""
})
@Since("1.7.2")
public class EffChangeYamlId extends Effect {

	static {
		Skript.registerEffect(EffChangeYamlId.class,
				"(change|rename|update) y[a]ml id [from] %string% to %string%",
				"(change|rename|update) y[a]ml %string% file path to [(1¦non[(-| )]relative)] %string%");
	}

	private Expression<String> oldId;
	private Expression<String> newId;
	private int mark;
	private SkriptNode skriptNode;
	private int matchedPattern;

	@Override
	protected void execute(@Nullable Event event) {
		if (matchedPattern == 0) {
			// Change YAML ID
			String oldIdValue = this.oldId.getSingle(event);
			String newIdValue = this.newId.getSingle(event);

			if (oldIdValue == null || newIdValue == null) {
				SkriptYaml.warn("Changing the ID of a YAML file requires both the old and new ID to be a valid string " + skriptNode.toString());
				return;
			}

			YAMLAPI.changeId(oldIdValue, newIdValue, skriptNode);
		} else {
			// Reassign file path
			String yamlId = this.oldId.getSingle(event);
			String newFilePath = this.newId.getSingle(event);

			if (yamlId == null || newFilePath == null) {
				SkriptYaml.warn("Reassigning the file path of a YAML file requires both the ID and new file path to be valid strings " + skriptNode.toString());
				return;
			}

			YAMLAPI.reassignFile(yamlId, newFilePath, this.mark == 1, skriptNode);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean b) {
		if (matchedPattern == 0) {
			return "change yaml id from " + this.oldId.toString(event, b) + " to " + this.newId.toString(event, b);
		} else {
			return "change yaml " + this.oldId.toString(event, b) + " file path to " + this.newId.toString(event, b);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parse) {
		this.oldId = (Expression<String>) exprs[0];
		this.newId = (Expression<String>) exprs[1];
		this.skriptNode = new SkriptNode(SkriptLogger.getNode());
		this.mark = parse.mark;
		this.matchedPattern = matchedPattern;
 
		return true;
	}
} 
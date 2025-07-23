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
import me.sashie.skriptyaml.utils.SkriptYamlUtils;
import me.sashie.skriptyaml.utils.yaml.YAMLProcessor;
import org.bukkit.event.Event;
import java.io.File;
import javax.annotation.Nullable;

@Name("Save YAML")
@Description("Saves the current cached YAML elements to file." +
		"\n\t - Using the `[with an indentation of %-number%]` option allows you to save the file with a different amount of spacing between 1 and 10" +
		"\n\t - Option to remove extra lines between nodes" +
		"\n\t - Use `to %string%` or `as %string%` to save to a different file path")
@Examples({
		"save yaml \"config\"",
		"save yaml \"config\" with an indentation of 2",
		"save yaml \"config\" to \"plugins/MyPlugin/backup.yml\"",
		"save yaml \"config\" as \"plugins/MyPlugin/config_backup.yml\" with an indentation of 4"
})
@Since("1.0.0")
public class EffSaveYaml extends Effect {

	static {
		Skript.registerEffect(EffSaveYaml.class,
				"save y[a]ml %strings% [with an indentation of %-number%] [(1¦[and] with(out| no) extra lines between nodes)]",
				"save y[a]ml %strings% (to|as) [(1¦non[(-| )]relative)] [file] %string% [with an indentation of %-number%] [(2¦[and] with(out| no) extra lines between nodes)]");
	}

	private Expression<String> file;
	private Expression<Number> yamlIndent;
	private Expression<String> targetFile;
	private int mark;
	private int matchedPattern;
	private SkriptNode skriptNode;

	@Override
	protected void execute(@Nullable Event event) {
		for (String name : this.file.getAll(event)) {
			YAMLProcessor yaml = SkriptYaml.YAML_STORE.get(name);
			if (yaml == null)
				continue;

			if (matchedPattern == 0) {
				// Save to original file location
				if (yaml.isFileDeleted()) {
					SkriptYaml.warn("Cannot save yaml '" + name + "' because the file has been deleted but kept in memory. Use 'save yaml \"" + name + "\" to \"new/path.yml\"' to save to a new location " + skriptNode.toString());
					continue;
				}

				if (yamlIndent != null)
					yaml.setIndent(this.yamlIndent.getSingle(event).intValue());
				try {
					boolean saved = yaml.save(this.mark == 1 ? false : true);
					if (!saved) {
						SkriptYaml.warn("Failed to save yaml '" + name + "' " + skriptNode.toString());
					}
				} catch (NullPointerException ex) {
					SkriptYaml.warn("The yaml '" + name + "' hasnt been populated yet " + skriptNode.toString());
					ex.printStackTrace();
				}
			} else {
				// Save to different file location
				String targetPath = this.targetFile.getSingle(event);
				if (targetPath == null) {
					SkriptYaml.warn("Target file path cannot be null " + skriptNode.toString());
					continue;
				}

				// Store original file and deleted status
				File originalFile = yaml.getFile();
				boolean wasDeleted = yaml.isFileDeleted();
				boolean saved = false;

				try {
					// Temporarily reassign the file path
					File targetFile = SkriptYamlUtils.getFile(targetPath, mark == 1);
					yaml.reassignFile(targetFile);

					// Set indentation if specified
					if (yamlIndent != null)
						yaml.setIndent(this.yamlIndent.getSingle(event).intValue());

					// Save to the new location
					saved = yaml.save(this.mark == 2 ? false : true);
					if (!saved) {
						SkriptYaml.warn("Failed to save yaml '" + name + "' to '" + targetPath + "' " + skriptNode.toString());
					} else if (wasDeleted) {
						SkriptYaml.warn("Successfully saved deleted yaml '" + name + "' to new location '" + targetPath + "' " + skriptNode.toString());
					}
				} catch (Exception ex) {
					SkriptYaml.warn("Failed to save yaml '" + name + "' to '" + targetPath + "': " + ex.getMessage() + " " + skriptNode.toString());
				} finally {
					// Restore original file path and deleted status
					if (wasDeleted) {
						// If the file was deleted and we successfully saved to a new location,
						// keep the new file path and clear the deleted status
						// This allows future saves to work normally
						if (!saved) {
							// If save failed, restore the original deleted state
							yaml.reassignFile(originalFile);
							yaml.markFileDeleted();
						}
					} else {
						// If the file wasn't deleted, always restore the original path
						yaml.reassignFile(originalFile);
					}
				}
			}
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean b) {
		StringBuilder sb = new StringBuilder("save yaml " + this.file.toString(event, b));

		if (matchedPattern != 0 && targetFile != null) {
			sb.append(" to/as ").append(this.targetFile.toString(event, b));
		}

		if (yamlIndent != null) {
			sb.append(" with an indentation of ").append(this.yamlIndent.toString(event, b));
		}

		if (mark == 1) {
			sb.append(" without extra lines between nodes");
		}

		return sb.toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parse) {
		this.file = (Expression<String>) exprs[0];
		this.matchedPattern = matchedPattern;
		if (matchedPattern == 0) {
			this.yamlIndent = (Expression<Number>) exprs[1];
		} else {
			this.targetFile = (Expression<String>) exprs[1];
			this.yamlIndent = (Expression<Number>) exprs[2];
		}
		this.mark = parse.mark;
		this.skriptNode = new SkriptNode(SkriptLogger.getNode());

		return true;
	}
}
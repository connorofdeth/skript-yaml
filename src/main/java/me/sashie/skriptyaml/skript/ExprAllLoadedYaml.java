package me.sashie.skriptyaml.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.sashie.skriptyaml.SkriptYaml;
import me.sashie.skriptyaml.utils.SkriptYamlUtils;
import me.sashie.skriptyaml.utils.StringUtil;
import me.sashie.skriptyaml.utils.yaml.YAMLProcessor;
import org.bukkit.event.Event;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

@Name("Return All YAML (un)loaded in memory")
@Description("Returns a list of all \"cached\" yaml file IDs or from a certain directory or list of directories." +
		"\n - Can also return list of directories." +
		"\n - Can also return list of unloaded yaml files from a certain directory or list of directories." +
		"\n - To remain consistent with the load effect, the list of unloaded yaml files will not include the server path so that you don't have to use the non-relative option." +
		"\n - Some of the listed unloaded yaml files can include the server path which will require you to use the non-relative option in the yaml load effect.")
@Examples({
		"set {_list::*} to the currently loaded yaml files",
		"broadcast \"%{_list::*}%\"",
		" ",
		"loop the loaded yaml",
		"\tbroadcast loop-value",
		" ",
		"loop the loaded yaml from directory \"plugins\\skript-yaml\"",
		"\tbroadcast loop-value",
		" ",
		"loop the loaded yaml directories",
		"\tbroadcast loop-value",
		" ",
		"loop the unloaded yaml from directory \"plugins\\skript-yaml\"",
		"\tbroadcast loop-value"
})
@Since("1.0.0")
public class ExprAllLoadedYaml extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprAllLoadedYaml.class, String.class, ExpressionType.SIMPLE,
				"[(the|all [(of the|the)])] [currently] loaded y[a]ml [files] [from (director(y|ies) %-strings%|all directories)]",
				"[(the|all [(of the|the)])] [currently] unloaded y[a]ml [files] from director(y|ies) %strings%",
				"[(the|all [(of the|the)])] [currently] loaded y[a]ml directories");
	}

	private Expression<String> directory;
	private int matchedPattern;

	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parse) {
		if (matchedPattern == 0 || matchedPattern == 1)
			directory = (Expression<String>) exprs[0];
		this.matchedPattern = matchedPattern;
		return true;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (matchedPattern == 0)
			return "(the|all [of the]) [currently] loaded y[a]ml [files]" + (directory != null ? " from directory " + directory.toString(event, debug) : "");
		else if (matchedPattern == 1)
			return "(the|all [of the]) [currently] unloaded y[a]ml [files] from directory " + directory.toString(event, debug);
		else
			return "(the|all [of the]) [currently] loaded y[a]ml directories";
	}

	@Override
	@Nullable
	protected String[] get(Event event) {
		if (matchedPattern == 0) {
			if (directory == null) {
				if (SkriptYaml.YAML_STORE.isEmpty())
					return null;
				return SkriptYaml.YAML_STORE.keySet().toArray(new String[0]);
			} else {
				return getYamlFromDirectories(directory.getAll(event));
			}
		} else if (matchedPattern == 1) {
			return getUnloadedYamlFromDirectories(directory.getAll(event));
		} else {
			return getAllDirectories();
		}
	}

	private List<String> filterDirectories(String... directories) {
		List<String> filter = new ArrayList<String>();
		String server = SkriptYamlUtils.getServerPath();
		for (String d : directories) {
			if (d.startsWith(server))
				filter.add(StringUtil.stripLastSeparator(StringUtil.checkSeparator(d)));
			else
				filter.add(server + StringUtil.stripLastSeparator(StringUtil.checkSeparator(d)));
		}
		return filter;
	}
	
	private String[] getYamlFromDirectories(String... directories) {
		List<String> yamlNames = new ArrayList<String>();
		List<String> filter = filterDirectories(directories);

		for (Iterator<Entry<String, YAMLProcessor>> it = SkriptYaml.YAML_STORE.entrySet().iterator(); it.hasNext();) {
			Entry<String, YAMLProcessor> entry = it.next();
			if (filter.contains(entry.getValue().getParentPath())) {
				String id = entry.getKey();
				if (!yamlNames.contains(id))
					yamlNames.add(id);
			}
		}
		if (yamlNames.isEmpty())
			return null;
		else
			return yamlNames.toArray(new String[yamlNames.size()]);
	}

	private String[] getUnloadedYamlFromDirectories(String... directories) {
		List<String> unloadedYamlNames = new ArrayList<String>();
		List<String> filter = filterDirectories(directories);

		// Get all currently loaded YAML files from these directories
		List<String> loadedYamlFiles = new ArrayList<String>();
		for (Iterator<Entry<String, YAMLProcessor>> it = SkriptYaml.YAML_STORE.entrySet().iterator(); it.hasNext();) {
			Entry<String, YAMLProcessor> entry = it.next();
			if (filter.contains(entry.getValue().getParentPath())) {
				String filePath = entry.getValue().getFile().getName();
				if (!loadedYamlFiles.contains(filePath))
					loadedYamlFiles.add(filePath);
			}
		}

		// Check each directory for YAML files
		for (String dirPath : filter) {
			File directory = new File(dirPath);
			if (directory.exists() && directory.isDirectory()) {
				File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml") || name.toLowerCase().endsWith(".yaml"));
				if (files != null) {
					for (File file : files) {
						String fileName = file.getName();
						if (!loadedYamlFiles.contains(fileName) && !unloadedYamlNames.contains(fileName)) {
							// We need to check if the file is in the server path or not
							if (fileName.startsWith(SkriptYamlUtils.getServerPath())) {
								unloadedYamlNames.add(fileName.replace(SkriptYamlUtils.getServerPath(), ""));
							} else {
								unloadedYamlNames.add(fileName);
							}
						}
					}
				}
			}
		}

		if (unloadedYamlNames.isEmpty())
			return null;
		else
			return unloadedYamlNames.toArray(new String[unloadedYamlNames.size()]);
	}

	private String[] getAllDirectories() {
		List<String> yamlDirectories = new ArrayList<String>();
		for (Iterator<Entry<String, YAMLProcessor>> it = SkriptYaml.YAML_STORE.entrySet().iterator(); it.hasNext();) {
			String path = it.next().getValue().getParentPath();
			if (!yamlDirectories.contains(path))
				yamlDirectories.add(path);
		}
		return yamlDirectories.toArray(new String[yamlDirectories.size()]);
	}
	
	@Override
	public void change(Event event, Object[] delta, Changer.ChangeMode mode) {

	}

	@Override
	public Class<?>[] acceptChange(final Changer.ChangeMode mode) {
		return null;
	}
}

package me.sashie.skriptyaml.utils.versions;

import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Expression;
import org.bukkit.event.Event;

import java.lang.reflect.Field;
import java.util.Set;

public class V2_7 extends V2_6 {

	private Field delayedField;

	public V2_7() {
		try {
			delayedField = Delay.class.getDeclaredField("DELAYED");
			delayedField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addDelayedEvent(Event event) {
		try {
			Set<Event> delayed = (Set<Event>) delayedField.get(null);
			delayed.add(event);
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
			e.printStackTrace();
		}

	}

	@Override
	public <T> boolean checkExpression(Expression<T> expr, Event event, Object checker, boolean negated) {
		try {
			Class<?> checkerClass = Class.forName("ch.njol.util.Checker");
			if (!checkerClass.isInstance(checker)) {
				throw new IllegalArgumentException("Checker must be a Checker for legacy Skript");
			}
			return (boolean) expr.getClass()
				.getMethod("check", Event.class, checkerClass, boolean.class)
				.invoke(expr, event, checker, negated);
		} catch (Exception e) {
			throw new RuntimeException("Failed to check Expression with Checker", e);
		}
	}

	@Override
	public <T> Object createChecker(java.util.function.Predicate<T> check) {
		try {
			Class<?> checkerClass = Class.forName("ch.njol.util.Checker");
			return java.lang.reflect.Proxy.newProxyInstance(
				checkerClass.getClassLoader(),
				new Class<?>[]{checkerClass},
				(proxy, method, args) -> {
					if ("check".equals(method.getName()) && args.length == 1) {
						return check.test((T) args[0]);
					}
					throw new UnsupportedOperationException(method.toString());
				}
			);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create Checker proxy", e);
		}
	}
}

package qwertzite.extraexplosions.util.function;

import java.util.Objects;

@FunctionalInterface
public interface Action {
	public static final Action NONE = () -> {};
	
	public abstract void execute();
    default Action andThen(Action after) {
        Objects.requireNonNull(after);
        return () -> { execute(); after.execute(); };
    }
}

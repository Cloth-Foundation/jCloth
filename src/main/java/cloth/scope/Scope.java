package cloth.scope;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Map;

public class Scope {

    @Getter
    private static ArrayList<Map<Scope, Integer>> scopes = new ArrayList<>();

    public Scope() {

    }

    public static void addScope(Map<Scope, Integer> scope) {
        scopes.add(scope);
    }

    public static void removeScope(Map<Scope, Integer> scope) {
        scopes.remove(scope);
    }

    public static void clearScopes() {
        scopes.clear();
    }

}

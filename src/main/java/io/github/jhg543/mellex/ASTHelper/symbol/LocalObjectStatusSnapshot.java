package io.github.jhg543.mellex.ASTHelper.symbol;

/**
 * Created by z089 on 2016/4/19.
 */
public class LocalObjectStatusSnapshot {
    public LocalObjectStatusSnapshot(LocalObjectResolver.Scope scope) {
        this.scope = scope;
    }

    private LocalObjectResolver.Scope scope;

    public LocalObjectResolver.Scope getScope() {
        return scope;
    }
}

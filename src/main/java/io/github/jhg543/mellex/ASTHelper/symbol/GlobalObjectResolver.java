package io.github.jhg543.mellex.ASTHelper.symbol;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.base.Splitter;

import io.github.jhg543.mellex.ASTHelper.plsql.FunctionDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.TableDefinition;
import io.github.jhg543.mellex.util.tuple.Tuple2;

public class GlobalObjectResolver {

	private boolean guessEnabled;
	private Map<String, FunctionDefinition> functions;
	private Function<String, TableDefinition> tableResolver;

	public GlobalObjectResolver(boolean guessEnabled, Function<String, TableDefinition> tableResolver) {
		super();
		this.guessEnabled = guessEnabled;
		this.tableResolver = tableResolver;
	}

	private static Tuple2<String, String> splitLastDot(String name) {
		int pos = name.lastIndexOf('.');
		return Tuple2.of(name.substring(0, pos), name.substring(pos + 1, name.length()));
	}

	public ObjectDefinition searchByName(String name) {
		FunctionDefinition fd = functions.get(name);
		if (fd != null) {
			return fd;
		}

		// TODO what if no dot in name?
		Tuple2<String, String> namesplit = splitLastDot(name);

		TableDefinition td = searchTable(namesplit.getField0());
		if (td == null) {
			return null;
		}
		return td.getColumns().get(namesplit.getField1());
	}

	public TableDefinition searchTable(String name) {
		return tableResolver.apply(name);
	}

	public boolean isGuessEnabled() {
		return guessEnabled;
	}

	public void setGuessEnabled(boolean guessEnabled) {
		this.guessEnabled = guessEnabled;
	}
}

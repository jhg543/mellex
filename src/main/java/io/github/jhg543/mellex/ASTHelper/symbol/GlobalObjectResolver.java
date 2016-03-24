package io.github.jhg543.mellex.ASTHelper.symbol;

import java.util.Map;

import io.github.jhg543.mellex.ASTHelper.plsql.FunctionDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.ObjectDefinition;
import io.github.jhg543.mellex.ASTHelper.plsql.TableDefinition;

public class GlobalObjectResolver {
	
	private boolean guessEnabled;
	private Map<String, FunctionDefinition> functions;
	
	public ObjectDefinition searchByName(String name)
	{
		FunctionDefinition fd = functions.get(name);
		if (fd!=null)
		{
			return fd;
		}
		
		
		return null;
	}
	
	public TableDefinition searchTable(String name)
	{
		return null;
	}

	public boolean isGuessEnabled() {
		return guessEnabled;
	}

	public void setGuessEnabled(boolean guessEnabled) {
		this.guessEnabled = guessEnabled;
	}
}

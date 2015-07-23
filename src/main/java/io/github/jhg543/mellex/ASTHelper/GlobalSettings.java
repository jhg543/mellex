package io.github.jhg543.mellex.ASTHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalSettings {
	
	boolean iscasesensitive = false;

	private static final Logger log = LoggerFactory.getLogger(GlobalSettings.class);
	private static GlobalSettings meta = new GlobalSettings();

	public static boolean isCaseSensitive() {
		return meta.iscasesensitive;
	}

	public static void setCaseSensitive(boolean isCaseSensitive) {
		meta.iscasesensitive = isCaseSensitive;
	}

}

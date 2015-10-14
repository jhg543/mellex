package io.github.jhg543.mellex.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import io.github.jhg543.mellex.ASTHelper.ObjectName;

public class Misc {
	private static final Logger log = LoggerFactory.getLogger(Misc.class);

	public static boolean isvolatile(ObjectName n) {
		if (n != null && n.ns.size() == 2) {
			String a = n.ns.get(1);
			if (a.endsWith("_NEW_DATA") || a.startsWith("TMP_")) {
				return true;
			}
		}
		return false;
	}

	public static String nameSym(String name) {
		int dot = name.indexOf('.');
		if (dot != -1) {
			if (name.charAt(0) == '$') {
				if (name.charAt(1) == '{') {
					return name;
				} else {
					return "${" + name.substring(1, dot) + "}" + name.substring(dot);
				}
			} else {
				return "${" + name.substring(0, dot) + "}" + name.substring(dot);
			}
		} else {
			return name;
		}
	}

	public static boolean isvolatile(String name) {
		List<String> n = Splitter.on('.').splitToList(name);
		if (n != null && n.size() == 2) {
			String a = n.get(1);
			if (a.endsWith("_NEW_DATA") || a.startsWith("TMP_")) {
				return true;
			}
		}
		return false;
	}

	public static String trimPerlScript(Path x) {
		try {
			//TODO detect encoding
			String sqlscript = new String(Files.readAllBytes(x), Charset.forName("GBK"));
			if (x.getFileName().toString().toLowerCase().endsWith(".sql")) {
				return sqlscript;
			}

			if (sqlscript.indexOf("### Below are PSQL scripts ###") > 0) {
				log.info("FILE CONTAINS PSQL KEYWORD " + x.toString());
			}

			int a = sqlscript.indexOf("ENDOFINPUT;");
			int b = sqlscript.lastIndexOf("ENDOFINPUT");
			int a2 = sqlscript.indexOf("--<SqlCode>");
			int b2 = sqlscript.indexOf("--</SqlCode>");
			if (a2 > 0 && a2 > a) {
				a = a2;
			}
			if (b2 > 0 && b2 < b) {
				b = b2;
			}
			if (!(a > 0 && b > 0 && a < b) || sqlscript.indexOf("ENDOFINPUT", a + 1) != sqlscript.lastIndexOf("ENDOFINPUT")) {
				a = sqlscript.indexOf("print \"greenplum connect ok!\";\n    my $sth = $dbh->prepare(\"");
				b = sqlscript.indexOf(");\n$sth->execute;\n$sth->finish;\n$dbh->commit;\n$dbh->disconnect;\nreturn 0;");
				if (!(a > 0 && b > 0 && a < b)) {
					log.error("NO ENDOFINPUT OF MULTIPLE ENDOFINPUT " + x.toString());
					return null;
				} else {
					sqlscript = sqlscript.substring(a + 62, b - 1);
					;
				}

			} else {
				sqlscript = sqlscript.substring(a + 11, b);
			}
			List<String> k = Splitter.on('\n').splitToList(sqlscript);
			// System.out.println(k.size());
			StringBuilder sb = new StringBuilder();
			for (String line : k) {
				String xline = line.trim();
				if (xline.startsWith(".") || xline.startsWith("#") || xline.startsWith("${LOGON")
						|| xline.startsWith("${v_fir_sql}") || xline.startsWith("${v_last_sql}")
						|| xline.startsWith("${tar_logon_str}") || xline.startsWith("\\\\")) {
					sb.append("--");
				}
				sb.append(line);
				sb.append("\n");
			}

			sqlscript = sb.toString();
			BasicFileAttributes attrs = Files.getFileAttributeView(x, BasicFileAttributeView.class).readAttributes();

			if (attrs.size() > 1000000) {
				String c = sqlscript.replaceAll("insert", "");
				if (sqlscript.length() - c.length() > 10000) {
					log.error("FILE TOO MANY INSERTS " + x.toString());
					return null;
				}

			}
			return sqlscript;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}
}

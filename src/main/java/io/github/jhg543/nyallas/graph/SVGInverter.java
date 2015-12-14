package io.github.jhg543.nyallas.graph;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

public class SVGInverter {
	public static void invertSVG(Path source, Path target) {

		try (PrintWriter p = new PrintWriter(target.toFile())) {
			String src = Files.toString(source.toFile(), StandardCharsets.UTF_8);
			int i = 0;
			int state = 0;
			while (i < src.length()) {
				char c = src.charAt(i);
				switch (state) {
				case 0:
					// nothing
					if (c == ' ') {
						state = 1;
					}
					p.append(c);
					i++;
					break;
				case 1:
					// after space
					if ((c == 'x' || c == 'y') && (src.charAt(i + 1) == '=' || src.charAt(i + 2) == '=')) {
						p.append((c == 'x') ? 'y' : 'x');
						i++;
						state = 2;
					}

					else if (c == 'w') {
						p.append("height");
						i += 5;
						state = 2;
					}

					else if (c == 'h') {
						p.append("width");
						i += 6;
						state = 2;
					}

					else if (c == 'p') {
						p.append("points=\"");
						i += 8;
						state = 3;
					}

					else if (c == 'v' && src.charAt(i + 1) == 'i') {
						p.append("viewBox=\"");
						i += 9;
						state = 4;
					} else {
						state = 0;

					}
					break;

				case 2:
					// copy until " "
					int q = 0;
					while (q < 2) {
						char t = src.charAt(i);
						if (t == '"') {
							q++;
						}
						p.append(t);
						i++;
					}
					state = 0;
					break;

				case 3:
				// point = ""
				{
					int lp = src.indexOf('"', i);
					String qq = src.substring(i, lp);
					List<String> coords = Splitter.on(' ').trimResults().omitEmptyStrings().splitToList(qq);
					for (String coord : coords) {
						int rp = coord.indexOf(',');
						p.append(coord.substring(rp + 1));
						p.append(',');
						p.append(coord.substring(0, rp));
						p.append(' ');
					}
					i = lp + 1;

					p.append('"');
					state = 0;
				}
					break;

				case 4:
				// viewbox = ""
				{
					int lp = src.indexOf('"', i);
					String qq = src.substring(i, lp);
					List<String> coords = new ArrayList<>(Splitter.on(' ').trimResults().omitEmptyStrings().splitToList(qq));
					String t = coords.get(3);
					coords.set(3, coords.get(2));
					coords.set(2, t);
					p.append(Joiner.on(' ').join(coords));
					i = lp + 1;

					p.append('"');
					state = 0;
				}
					break;
				default:
					break;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("error inverting svg", e);
		} finally {
		}
	}
}

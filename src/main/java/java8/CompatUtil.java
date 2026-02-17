package java8;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompatUtil {
	public static boolean isBlank(String str) {
		if (str == null)
			return true;
		for (int i = 0; i < str.length(); i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	@SafeVarargs
	public static <T> List<T> listOf(T... elements) {
		if (elements == null || elements.length == 0) {
			return Collections.emptyList();
		}
		List<T> list = new ArrayList<T>(elements.length);
		for (T e : elements) {
			if (e == null) {
				throw new NullPointerException("List.of() does not allow null elements");
			}
			list.add(e);
		}
		return Collections.unmodifiableList(list);
	}

	public static Path pathOf(String first, String... more) {
		return Paths.get(first, more);
	}

	public static Path pathOf(java.net.URI uri) {
		return Paths.get(uri);
	}

	public static String readString(Path path) throws IOException {
		return readString(path, StandardCharsets.UTF_8);
	}

	public static String readString(Path path, Charset charset) throws IOException {
		byte[] bytes = Files.readAllBytes(path);
		return new String(bytes, charset);
	}
}

import java.io.File;

public class Example {
	private String file;

	public Example(String path) {
		file = path;
	}

	public String getPath() {
		if (file == null)
			return null;
		else
			return file;
	}
}

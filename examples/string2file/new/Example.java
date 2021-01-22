import java.io.File;

public class Example {
	private File file;

	public Example(String path) {
		if (path != null) {
			file = new File(path);
		}
	}

	public String getPath() {
		if (file == null)
			return null;
		else
			return file.getAbsolutePath();
	}
}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Example {
	private List<String> f = new ArrayList<>();

	public Example(String[] strings) {
		f.clear();
		if (strings != null && strings.length > 0) {
			f.addAll(Arrays.asList(strings));
		}
	}
}

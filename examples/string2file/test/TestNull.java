import org.junit.Test;

public class TestNull {

	@Test
	public void testNull() {
		System.setProperty("DUMP_ID", "1");
		Example o = new Example(null);
		dsu.pasta.object.processor.ObjectApi.processObject(o, 1);
	}
}

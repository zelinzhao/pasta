import org.junit.Test;

public class TestFile1 {

	@Test
	public void testHome() {
		System.setProperty("DUMP_ID", "1");
		Example o = new Example("/home");
		dsu.pasta.object.processor.ObjectApi.processObject(o, 1);
	}
}

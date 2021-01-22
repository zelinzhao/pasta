import org.junit.Test;

public class TestFile2 {

	@Test
	public void testABC() {
		System.setProperty("DUMP_ID", "1");
		Example o = new Example("/home/a/b/c");
		dsu.pasta.object.processor.ObjectApi.processObject(o, 1);
	}
}

import org.junit.Test;

public class TestSize2 {

	@Test
	public void testSize2() {
		System.setProperty("DUMP_ID", "1");
		Example o = new Example(new String[] { "a", "b" });
		dsu.pasta.object.processor.ObjectApi.processObject(o, 1);
	}
}

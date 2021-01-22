import org.junit.Test;

public class TestSize1 {
	@Test
	public void testSize1() {
		System.setProperty("DUMP_ID", "1");
		Example o = new Example(new String[] { "a" });
		dsu.pasta.object.processor.ObjectApi.processObject(o, 1);
	}

}

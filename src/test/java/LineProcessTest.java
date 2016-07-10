import org.junit.Test;

import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;

public class LineProcessTest {
    //@formatter:off
    @Test public void cleanLineTest() {
        assertEquals("x=x-buy:sh=10:key=key-v:pt=xr:",
                new kv2csv().cleanLine("wrong x=x-buy:sh=10:key=key-v:pt=xr:wrong"));
    }

    @Test public void cleanLineTest2() { assertEquals("t=:x=x buy:sh= 10:key= key v :pt= xr :",
            new kv2csv().cleanLine(":=:  t=:x=x buy:sh= 10:key= key v :pt= xr :   :::=:"));
    }
    @Test public void cleanLineTest3() { assertEquals("aP=mxpv=5:cd[0].v=cd1:cd[1].n=name",
            new kv2csv().cleanLine("aP=mxpv=5:cd[0].v=cd1:cd[1].n=name"));
    }

    @Test public void getMapTest() {
        assertEquals("{t=, x=x buy, sh=10, key=key v, pt=xr}",
                new kv2csv().getMapFromLine("t=:x=x buy:sh= 10:key= key v :pt= xr :").toString());
    }

    @Test public void extractMapTest() {
        kv2csv kv = new kv2csv();
        assertEquals("{}",kv.extractMap("        'wrong'\n").toString());
        assertEquals("{}",kv.extractMap("        =wrong\n").toString());
        assertEquals("{1=3}",kv.extractMap("1=2:1=3").toString());
        assertEquals("{1=1=3}",kv.extractMap("1=1=2:1=1=3").toString());
        assertEquals("==3",kv.extractMap("1===2:1===3").get("1"));
    }
    @Test public void getValueStringTest() {
        kv2csv kv = new kv2csv();
        kv.header = new LinkedHashMap(){{put("1",1);put("2",22);put("3",3);}};
        assertEquals("1,2,3\n",kv.getOutputHeader());
    }

    @Test public void expandColumnTest(){
        kv2csv k = new kv2csv("-b");
        assertEquals("1  ",k.expandColumnIfBeauty(3,"1"));
    }
    @Test public void expandColumnTestNarrow(){
        assertEquals("1",new kv2csv().expandColumnIfBeauty(1,"1"));
    }
    @Test public void tableUpdateTest() {
        kv2csv kv = new kv2csv();
        kv.header = new LinkedHashMap(){{put("1",1);put("2",22);put("3",3);}};
        LinkedHashMap incoming = new LinkedHashMap() {{
            put("1", "qqw");put("2", "7"); put("3", "12345"); put("123456", "1");
        }};
        LinkedHashMap expected = new LinkedHashMap() {{
            put("1", 3); put("2", 22);put("3", 5);put("123456", 6);
        }};
        kv.headerUpdate(incoming);
        assertEquals(expected,kv.header);
    }

    //@formatter:on
}
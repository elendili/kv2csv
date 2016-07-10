import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


public class kv2csvTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private BufferedReader reader;
    private ByteArrayOutputStream os;

    @Before
    public void setUpStreams() throws IOException {
        os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        System.setOut(ps);
    }

    @Test
    public void simple() throws IOException {
        System.out.println("Hello, output!");
        assertEquals("Hello, output!\n", os.toString());
    }

    @Test
    public void total() {
        String actual =
                "aP=mxpv=5:cd[0].v=cd1:cd[1].n=name\n"
                        + "wrong x=x-buy:sh=10:key=key-v:pt=xr:wrong\n"
                        + "wrong x=x-buy:sh=11:pt=x::::key=key-v2:wrong\n"
                +"x=x-buy::key=key-v3:pt=x:sh=10:\n"
                +"file=given file:t=2:e=1\n"
                +"file=C/:t=3:e=0\n"
                +":x=x-sell:\n"
                +"        'wrong'\n"
                +"\n"
                +"''\n"
                +":\n"
                +"key=-9.2:\n"
                +"=wrong"
                ;

        String expected = "aP,cd[0].v,cd[1].n,x,sh,key,pt,file,t,e\n" +
                "mxpv=5,cd1,name,,,,,,,\n" +
                ",,,x-buy,10,key-v,xr,,,\n" +
                ",,,x-buy,11,key-v2,x,,,\n" +
                ",,,x-buy,10,key-v3,x,,,\n" +
                ",,,,,,,given file,2,1\n" +
                ",,,,,,,C/,3,0\n" +
                ",,,x-sell,,,,,,\n" +
                ",,,,,-9.2,,,,\n";
        new kv2csv("kv2").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }


    @Test
    public void totalFollow() {
        String actual =
                "aP=mxpv=5:cd[0].v=cd1:cd[1].n=name\n"
                        +"file=given file:t=2:e=1\n"
                        +"    aP=aPV:    'wrong'\n"
                        +":\n"
                        +"=wrong"
                ;

        String expected = "aP,cd[0].v,cd[1].n\n" +
                "mxpv=5,cd1,name\n" +
                "aP,cd[0].v,cd[1].n,file,t,e\n" +
                ",,,given file,2,1\n" +
                "aPV,,,,," +
                "\n";
        new kv2csv("-f").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void totalFollowBeauty() {
        String actual =
                "aP=mxpv=5:cd[0].v=cd1:cd[1].n=name\n"
                        +"file=given file:t=2:e=1\n"
                        +"    aP=aPV:    'wrong'\n"
                        +"    aP=aPdsdasdasd:    'wrong'\n"
                        +"    aP=a:    'wrong'\n"
                        +"=wrong"
                ;

        String expected = "aP    |cd[0].v|cd[1].n\n" +
                "mxpv=5|cd1    |name   \n" +
                "aP    |cd[0].v|cd[1].n|file      |t|e\n" +
                "      |       |       |given file|2|1\n" +
                "aPV   |       |       |          | | \n" +
                "aP         |cd[0].v|cd[1].n|file      |t|e\n" +
                "aPdsdasdasd|       |       |          | | \n" +
                "a          |       |       |          | | \n";
        new kv2csv("-fb").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }
    @Test
    public void totalBeauty() {
        String actual =
                "aP=mxpv=5:cd[0].v=cd1:cd[1].n=name\n"
                        +"file=given file:t=2:e=1\n"
                        +"    aP=aPV:    'wrong'\n"
                        +"    aP=aPdsdasdasd:    'wrong'\n"
                        +"    aP=a:    'wrong'\n"
                        +"=wrong"
                ;

        String expected = "aP         |cd[0].v|cd[1].n|file      |t|e\n" +
                "mxpv=5     |cd1    |name   |          | | \n" +
                "           |       |       |given file|2|1\n" +
                "aPV        |       |       |          | | \n" +
                "aPdsdasdasd|       |       |          | | \n" +
                "a          |       |       |          | | \n";
        new kv2csv("-b").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }



    @Test
    public void captionsUndefined() {
        String actual ="aP=mxpv=5:cd[0].v=cd1:cd[1].n=name\n";
        String expected = "aP,cd[0].v,cd[1].n\n" +
                "mxpv=5,cd1,name\n";
        new kv2csv("-c").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void captionsDefined() {
        String actual ="aP=mxpv=5:cd[0].v=cd1:cd[1].n=name\n";
        String expected = "aP\n" +
                "mxpv=5\n";
        new kv2csv("-c","'aP'").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }
    @Test
    public void captionsDefinedAndOdered() {
        String actual ="4=v4:3=v3:2=v2:1=v1:::\n";
        String expected = "1,2,3\n" +
                          "v1,v2,v3\n";
        new kv2csv("-c","'1,2,3'").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }
    @Test
    public void inputAndOutputDelimiter() {
        String actual ="4=v4,3=v3,2=v2,1=v1,,,\n";
        String expected = "1;2;3\n" +
                "v1;v2;v3\n";
        new kv2csv("-c","'1,2,3'","-id","','","-od",";")
                .process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }
    @Test
    public void fromFileInResources() throws Exception {
        String inputFileName = "inputTestFile.txt";
        String outputFileName = "outputTestFile.txt";
        ClassLoader classLoader = getClass().getClassLoader();
        URL file = classLoader.getResource(inputFileName);
        String[] inputArgs = new String[]{file.toURI().getPath().toString()};
        String expected = Files.readAllLines(
                Paths.get(classLoader.getResource(outputFileName).toURI()))
                .stream().collect(Collectors.joining(System.lineSeparator()));

        kv2csv.main(inputArgs);
//        kv2csv.main(new String[]{file.toString()});
        assertEquals(expected, os.toString());
    }
    @After
    public void cleanUpStreams() {
        System.setOut(null);
        System.setErr(null);
    }


}
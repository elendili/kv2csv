import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;


public class kv2csvTest {
    private ByteArrayOutputStream os;
    public final static String eol = System.lineSeparator();
    private PrintStream originalOut;

    @Before
    public void setUpStreams() throws IOException {
        os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        originalOut = System.out;
        System.setOut(ps);
    }

    @After
    public void cleanUpStreams() {
        System.setOut(originalOut);
//        System.setErr(originalErr);
    }

    @Test
    public void simple() throws IOException {
        System.out.println("Hello, output!");
        assertEquals("Hello, output!" + eol, os.toString());
    }

    @Test
    public void total() {
        String actual =
                "aP=mbgh=5:kd[0].v=cd1:kd[1].n=name" + eol
                        + "wrong x=x-hut:ty=10:key=key-v:ui=xr:wrong" + eol
                        + "wrong x=x-hut:ty=11:ui=x::::key=key-v2:wrong" + eol
                        + "x=x-hut::key=key-v3:ui=x:ty=10:" + eol
                        + "file=given file:t=2:e=1" + eol
                        + "file=C/:t=3:e=0" + eol
                        + ":x=x-sell:" + eol
                        + "        'wrong'" + eol
                        + "" + eol
                        + "''" + eol
                        + ":" + eol
                        + "key=-9.2:" + eol
                        + "=wrong";

        String expected = "aP,kd[0].v,kd[1].n,x,ty,key,ui,file,t,e" + eol +
                "mbgh=5,cd1,name,,,,,,," + eol +
                ",,,x-hut,10,key-v,xr,,," + eol +
                ",,,x-hut,11,key-v2,x,,," + eol +
                ",,,x-hut,10,key-v3,x,,," + eol +
                ",,,,,,,given file,2,1" + eol +
                ",,,,,,,C/,3,0" + eol +
                ",,,x-sell,,,,,," + eol +
                ",,,,,-9.2,,,," + eol;
        new kv2csv("kv2").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }


    @Test
    public void totalFollow() {
        String actual =
                "aP=mbgh=5:kd[0].v=cd1:kd[1].n=name" + eol
                        + "file=given file:t=2:e=1" + eol
                        + "    aP=aPV:    'wrong'" + eol
                        + ":" + eol
                        + "=wrong";

        String expected = "aP,kd[0].v,kd[1].n" + eol +
                "mbgh=5,cd1,name" + eol +
                "aP,kd[0].v,kd[1].n,file,t,e" + eol +
                ",,,given file,2,1" + eol +
                "aPV,,,,," +
                "" + eol;
        new kv2csv("-f").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void totalFollowBeauty() {
        String actual =
                "aP=mbgh=5:kd[0].v=cd1:kd[1].n=name" + eol
                        + "file=given file:t=2:e=1" + eol
                        + "    aP=aPV:    'wrong'" + eol
                        + "    aP=aPdsdasdasd:    'wrong'" + eol
                        + "    aP=a:    'wrong'" + eol
                        + "=wrong";

        String expected =
                kv2csv.colorizeLine( "aP    |kd[0].v|kd[1].n")+eol +
                        "mbgh=5|cd1    |name   " + eol +
                        kv2csv.colorizeLine("aP    |kd[0].v|kd[1].n|file      |t|e") + eol +
                        "      |       |       |given file|2|1" + eol +
                        "aPV   |       |       |          | | " + eol +
                        kv2csv.colorizeLine("aP         |kd[0].v|kd[1].n|file      |t|e")+ eol +
                        "aPdsdasdasd|       |       |          | | " + eol +
                        "a          |       |       |          | | " + eol;
        new kv2csv("-fb").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void totalBeauty() {
        String actual =
                "aP=mbgh=5:kd[0].v=cd1:kd[1].n=name" + eol
                        + "file=given file:t=2:e=1" + eol
                        + "    aP=aPV:    'wrong'" + eol
                        + "    aP=aPdsdasdasd:    'wrong'" + eol
                        + "    aP=a:    'wrong'" + eol
                        + "=wrong";

        String expected = kv2csv.colorizeLine("aP         |kd[0].v|kd[1].n|file      |t|e") + eol +
                "mbgh=5     |cd1    |name   |          | | " + eol +
                "           |       |       |given file|2|1" + eol +
                "aPV        |       |       |          | | " + eol +
                "aPdsdasdasd|       |       |          | | " + eol +
                "a          |       |       |          | | " + eol;
        new kv2csv("-b").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void totalNames() {
        String actual =
                "aP=mbgh=5:kd[0].v=cd1:kd[1].n=name" + eol
                        + "wrong x=x-hut:ty=10:key=key-v:ui=xr:wrong";
        String expected = "aP,kd[0].v,kd[1].n,x,ty,key,ui" + eol;
        new kv2csv("-n").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void totalFollowBeautyNames() {
        String actual =
                "aP=mbgh=5:kd[0].v=cd1:kd[1].n=name" + eol
                        + "file=given file:t=2:e=1" + eol
                        + "    aP=aPV:    'wrong'" + eol
                        + "    aP=aPdsdasdasd:    'wrong'" + eol
                        + "    aP=a:    'wrong'" + eol
                        + "=wrong";

        String expected =
                        kv2csv.colorizeLine("aP|kd[0].v|kd[1].n" ) + eol +
                        kv2csv.colorizeLine("aP|kd[0].v|kd[1].n|file|t|e" ) + eol +
                        kv2csv.colorizeLine("aP|kd[0].v|kd[1].n|file|t|e" ) + eol;
        new kv2csv("-fbn").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void withDefaultTime() {
        String actual =
                "2016-07-11 07:34:00,095 [Module 123123] INFO 13123: :key=value:" + eol +
                        "2016-07-11 07:37:05,024 [123123] DEBUG 123123: key2=value2:" + eol;

        String expected =
                "time,key,key2" + eol +
                        "2016-07-11 07:34:00 095,value," + eol +
                        "2016-07-11 07:37:05 024,,value2" + eol;

        new kv2csv().process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }


    @Test
    @Ignore
    public void longLine() {
        AtomicInteger l =new AtomicInteger(80);
        List<String> tagValues = Stream.generate(() -> ((char)l.incrementAndGet()) +"="+ ((char)l.get()))
                .limit(50).collect(toList());
        String actual = String.join(":",tagValues);
        originalOut.println(actual);

        new kv2csv("-").process(new ByteArrayInputStream(actual.getBytes()), os);
        originalOut.println(os.toString());
        assertEquals(50, os.toString().split("\n")[0].split(",").length);
    }

    @Test
    public void withProperlyDefinedTime() {
        String actual =
                "2016-07-11 07:34:00,095 [Module 123123] INFO 13123: :key=value:" + eol +
                        "2016-07-11 07:37:05,024 [123123] DEBUG 123123: key2=value2:" + eol;

        String expected =
                "time,key,key2" + eol +
                        "2016-07-11 07:34,value," + eol +
                        "2016-07-11 07:37,,value2" + eol;

        new kv2csv("-t", "'\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}'").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }


    @Test
    public void withTimeInBeautyMode() {
        String actual =
                "2016-07-11 07:34:00,095 [Module 123123] INFO 13123: :key=value:" + eol +
                        "2016-07-11 07:37:05,024 [123123] DEBUG 123123: key2=value2:" + eol;

        String expected =
                kv2csv.colorizeLine("time                   |key  |key2  ") + eol +
                                    "2016-07-11 07:34:00,095|value|      " + eol +
                                    "2016-07-11 07:37:05,024|     |value2" + eol;

        new kv2csv("-b").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test(expected = PatternSyntaxException.class)
    public void withImProperlyDefinedTime() {
        new kv2csv("-t", "'\\d{4 '").process(new ByteArrayInputStream("".getBytes()), os);
    }


    @Test
    public void withProperlyDefinedTimeWhichCantFind() {
        String actual = "2016-07-11 07:34:00,095 [Module 123123] INFO 13123: :key=value:" ;
        String expected = "key" + eol +
                        "value" + eol;
        new kv2csv("-t", "'\\d{10}'").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void captionsUndefined() {
        String actual = "aP=mbgh=5:kd[0].v=cd1:kd[1].n=name" + eol;
        String expected = "aP,kd[0].v,kd[1].n" + eol +
                "mbgh=5,cd1,name" + eol;
        new kv2csv("-c").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }
    @Test
    public void captionsUndefinedWithTime() {
        String actual = "2016-07-11 07:34:00,095 aP=mbgh=5:kd[0].v=cd1:kd[1].n=name" + eol;
        String expected = "time,aP,kd[0].v,kd[1].n" + eol +
                "2016-07-11 07:34:00 095,mbgh=5,cd1,name" + eol;
        new kv2csv("-c").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void captionsDefined() {
        String actual = "aP=mbgh=5:aPP=a:aP*=apV" + eol;
        String expected = "aP" + eol +
                "mbgh=5" + eol;
        new kv2csv("-c", "'aP'").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }
    @Test
    public void support() {
        System.setOut(originalOut);
        new kv2csv("-support").process(new ByteArrayInputStream("".getBytes()), os);
    }


    @Test
    public void captionsDefinedWithTime() {
        String actual = "2016-07-11 07:34:00,095 aP=mbgh=5:aPP=a:aP*=apV" + eol;
        String expected = "time,aP" + eol +
                "2016-07-11 07:34:00 095,mbgh=5" + eol;
        new kv2csv("-c", "'aP,ti.*'").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }


    @Test
    public void captionsDefinedAsRegexp() {
        String actual = "aP=mbgh=5:aPP=a:aP*=apV" + eol;
        String expected = "aP,aPP,aP*" + eol + "mbgh=5,a,apV" + eol;
        new kv2csv("-c", "'aP.*'").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void excludeCaptionsDefinedAsRegexp() {
        String actual = "aP=mbgh=5:aPP=a:aP*=apV:a1=1:ap=2" + eol;
        String expected = "aP,aPP,aP*" + eol + "mbgh=5,a,apV" + eol;
        new kv2csv("-c", "'a.*'", "-x", "a[^P]").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void onlyExcludeCaptionsDefinedAsRegexp() {
        String actual = "aP=mbgh=5:aPP=a:aP*=apV:a1=1:ap=2" + eol;
        String expected = "a1,ap" + eol + "1,2" + eol;
        new kv2csv("-x", "aP.*").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void captionsDefinedAndOdered() {
        String actual = "4=v4:3=v3:2=v2:1=v1:::" + eol;
        String expected = "1,2,3" + eol +
                "v1,v2,v3" + eol;
        new kv2csv("-c", "'1,2,3'").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void captionsDefinedAsEmpty() {
        String actual = "4=v4:3=v3:2=v2:1=v1:::" + eol;
        String expected = "4,3,2,1" + eol +
                "v4,v3,v2,v1" + eol;
        new kv2csv("-c", "' '").process(new ByteArrayInputStream(actual.getBytes()), os);
        assertEquals(expected, os.toString());
    }

    @Test
    public void inputAndOutputDelimiter() {
        String actual = "4=v4,3=v3,2=v2,1=v1,,," + eol;
        String expected = "1;2;3" + eol +
                "v1;v2;v3" + eol;
        new kv2csv("-c", "'1,2,3'", "-id", "','", "-od", ";")
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



}
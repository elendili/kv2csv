import org.junit.Test;

import java.io.*;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;


public class kv2csvConcurrentTest {
    public final static String eol = System.lineSeparator();

    final PipedOutputStream pos = new PipedOutputStream();
    final PipedInputStream pis = new PipedInputStream();


    final Runnable readerFromStream = () -> {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(pis));
//            OutputStreamWriter out = new OutputStreamWriter(System.out);
//            while(pis.available()==-1 && in.ready() ){}
            for (String line; (line = in.readLine()) != null; ) {
                System.out.print(line + eol);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    final Runnable writerToStream = () -> {
        Stream.generate(() -> "a=b:c=d:e=f:g=h" + eol)
                .limit(4)
                .forEach(s -> {
                            try {
                                pos.write(s.getBytes());
                                Thread.sleep(200);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                );
        try {
            pos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };


    @Test
    public void totalFollowBeauty() throws IOException, InterruptedException {
        pis.connect(pos);
        String expected = kv2csv.colorizeLine("a|c|e|g") + eol;
        expected += String.join("", Collections.nCopies(4, "b|d|f|h" + eol));
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(writerToStream);
//        executorService.execute(readerFromStream);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new kv2csv("-fb").process(pis, os);
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);

        assertEquals(expected, os.toString());
        System.out.println(os.toString());
    }

}
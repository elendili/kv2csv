import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Usage:  java -cp target/test-classes/ ForRealTest | java -cp target/classes/ kv2csv -fb
 */

public class ForRealTest {

    public static String randomLetter() {
        Random r = new Random();
        return "" + ((char) (r.nextInt(26) + 30));
    }

    public static void main(String... args) {
        Stream.generate(() -> String.join(":",
                Stream.generate(() -> randomLetter() + "=" + randomLetter())
                        .limit(10).collect(Collectors.toList())))
                .limit(1000)
                .forEach(s -> {
                            try {
                                System.out.println(s);
                                Thread.sleep(10 * new Random().nextInt(20));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                );
    }

}
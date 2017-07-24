import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Usage:
java -cp target/test-classes/ ForRealTest | java -cp target/classes/ kv2csv -fb
A|X|H|p|T|k|E|F|7|6|8|z|q|Q|x|Z|G|r|D|2|J|m|B|L|Y|y|1|t|C|u|i|w|s|N|S|e|5|j|K|U|c|h|o|9|v|g|W|0|d|l|4|P|M|b|3|O|V|f|I|_|R|n|a

 */

public class ForRealTest {

    public static String randomLetter() {
        Random r = new Random(); String s;
        do {
            s = "" + ((char) (r.nextInt(200) + 30));
        } while (!s.matches("\\p{Graph}"));
        return s;
    }

    public static void main(String... args) {
        Stream.generate(() -> String.join(":",
                Stream.generate(() -> randomLetter()+randomLetter() + "=" + randomLetter())
                        .limit(50).collect(Collectors.toList())))
                .limit(300)
                .forEach(s -> {
                            try {
                                System.out.println(s);
                                Thread.sleep(10 * new Random().nextInt(10));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                );
    }

}
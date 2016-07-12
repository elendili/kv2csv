import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class kv2csv {
    List<LinkedHashMap<String, String>> list = new LinkedList<>();
    LinkedHashMap<String, Integer> header = new LinkedHashMap<>();
    public final static String eol = System.lineSeparator();
    boolean headerUpdated = true;
    final Arguments args;
    final Pattern cleanLinePattern;


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";


    public kv2csv() {
        this(new Arguments());
    }

    public kv2csv(String... string) {
        this(new Arguments(string));
    }

    public kv2csv(Arguments arguments) {
        this.args = arguments;
        String id = args.inputDelimiter;
        cleanLinePattern = Pattern.compile("^.*?(([^" + id + "=\\s]+=[^" + id + "]*" + id + "*)+).*$", Pattern.CASE_INSENSITIVE);
    }


    public void process(InputStream inputStream, OutputStream outputStream) {
        LinkedHashMap<String, String> map;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
             OutputStreamWriter out = new OutputStreamWriter(outputStream);
        ) {
            for (String line; in.ready() && (line = in.readLine()) != null; ) {
                map = extractMap(line);
                map = filterByCaptions(map);
                if (map.size() > 0) {
                    headerUpdate(map);
                    if (args.follow) {
                        if (headerUpdated) {
                            out.append(getOutputHeader());
                            headerUpdated = false;
                        }
                        if (!args.names)
                            out.append(getValueString(map)).append(eol);
                    } else list.add(map);
                }
            }
            if (!args.follow) {
                out.append(getOutputHeader());
                if (!args.names) printValuesTable(out);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected LinkedHashMap<String, String> filterByCaptions(final LinkedHashMap<String, String> map) {
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>(map);

        if (args.captions != null) {
            LinkedHashMap<String, String> lhm = new LinkedHashMap<>();
            for (String regexpKey : args.captions) {
                toReturn.keySet().stream()
                        .filter(inKey -> inKey.matches(regexpKey))
                        .forEach(inKey -> lhm.put(inKey, map.get(inKey)));
            }
            toReturn = lhm;
        }

        if (args.exclusiveCaptions != null) {
            LinkedHashMap<String, String> lhm = new LinkedHashMap<>();
            for (String regexpKey : args.exclusiveCaptions) {
                toReturn.keySet().stream()
                        .filter(inKey -> !inKey.matches(regexpKey))
                        .forEach(inKey -> lhm.put(inKey, map.get(inKey)));
            }
            toReturn = lhm;
        }

        return toReturn;
    }

    public void printValuesTable(OutputStreamWriter out) throws IOException {
        list.stream().map(e -> getValueString(e))
                .forEach(e -> {
                    try {
                        out.append(e);
                        out.append(eol);
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                });
    }

    protected String getOutputHeader() {
        String s = header.keySet().stream()
                .map(e -> args.names ? e : expandColumnIfBeauty(header.get(e), e))
                .collect(Collectors.joining(args.outDelimiter));
        if (args.beauty) s = ANSI_GREEN + s + ANSI_RESET;
        return s.concat(eol);
    }

    public String getValueString(final LinkedHashMap<String, String> map) {
        return header.keySet().stream().map(e -> expandColumnIfBeauty(header.get(e),
                map.containsKey(e) ? map.get(e) : ""))
                .collect(
                        Collectors.joining(args.outDelimiter));
    }

    public String expandColumnIfBeauty(int width, String value) {
        if (args.beauty) {
            int spaceWidth = width - value.length();
            if (spaceWidth < 1) return value;
            else return value + String.format("%" + spaceWidth + "s", " ");
        } else return value;
    }

    public void headerUpdate(final LinkedHashMap<String, String> map) {
        // find current widths
        Map<String, Integer> intMap = map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Math.max(e.getKey().length(),
                                e.getValue().length()),
                        (e1, e2) -> e2,
                        LinkedHashMap::new
                ));
        LinkedHashMap oldHeader = (LinkedHashMap) header.clone();
        // merge maximum widths
        header = Stream.of(header, intMap).flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Integer::max,
                        LinkedHashMap::new));
        headerUpdated = !oldHeader.equals(header);
    }

    public LinkedHashMap<String, String> extractMap(String line) {
        line = cleanLine(line);
        return getMapFromLine(line);
    }

    public final String cleanLine(String line) {
        if (line.contains("=") && line.contains(args.inputDelimiter)) {
            Matcher m = cleanLinePattern.matcher(line);
            return m.find() ? m.group(1) : line;
        } else return "";
    }

    public static void main(String[] args) throws Exception {
        Arguments arguments = new Arguments(args);
        kv2csv k = new kv2csv(arguments);
        if (arguments.filePath == null)
            k.process(System.in, System.out);
        else {
            k.process(inputStreamFromFile(arguments.filePath), System.out);
        }

        // file mode
        // piped mode
    }

    protected static InputStream inputStreamFromFile(String filePath) {
        File initialFile = new File(filePath);
        if (initialFile.exists()) try {
            return new FileInputStream(initialFile);
        } catch (FileNotFoundException ignored) {
        }
        else {
            System.err.println("File '" + filePath + "' not found.");
            System.exit(1);
        }
        return null;
    }

    protected LinkedHashMap<String, String> getMapFromLine(String line) {
        LinkedHashMap<String, String> map = Arrays.stream(line.split(args.inputDelimiter))
                .filter(e -> !e.isEmpty())
                .map(e -> e.split("=", 2))
                .filter(ar -> !ar[0].trim().isEmpty())
                .collect(Collectors.toMap(ar -> ar[0].trim(),
                        ar -> ar.length > 1 ? ar[1].trim() : "",
                        (v1, v2) -> v2,
                        LinkedHashMap::new));
        return map;
    }

    // ======================

    static final class Arguments {

        boolean follow = false;
        boolean beauty = false;
        boolean names = false;
        String outDelimiter = ",";
        String inputDelimiter = ":";
        LinkedList<String> captions;
        LinkedList<String> exclusiveCaptions;
        String filePath;

        public Arguments(String... args) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    if (arg.contains("h")) {
                        printHelp();
                    }
                    if (arg.contains("f")) this.follow = true;
                    if (arg.contains("b")) this.beauty = true;
                    if (arg.contains("n")) this.names = true;
                    if (arg.contains("id") && (i + 1 < args.length)) {
                        this.inputDelimiter = cleanFromQuotes(args[i + 1]);
                        i++;
                    }
                    if (arg.contains("od") && (i + 1 < args.length)) {
                        this.outDelimiter = cleanFromQuotes(args[i + 1]);
                        i++;
                    }
                    if ((arg.contains("c") || arg.contains("k")) && (i + 1 < args.length)) {
                        this.captions = getNotEmptyListFromArgumentOrNull(args[i + 1]);
                        i++;
                    }
                    if (arg.contains("x") && (i + 1 < args.length)) {
                        this.exclusiveCaptions = getNotEmptyListFromArgumentOrNull(args[i + 1]);
                        i++;
                    }
                } else {
                    filePath = arg;
                }

            }
            if (this.beauty) outDelimiter = "|";
        }
    }

    protected static void printHelp() {
        String help = "Tool converts key-value line in csv table." + eol +
                "Usage:" + eol +
                "  kv2csv [options] [file]" + eol +
                "Options:" + eol +
                "  -id    define input delimiter, default is colon ':'" + eol +
                "  -od    define output csv delimiter, default is comma ','" + eol +
                "  -b     output is human readable table" + eol +
                "  -f     follows for input and prints output line for every input line" + eol +
                "         print header, if keys input list was updated" + eol +
                "  -n     prints only headers " + eol +
                "  -c -k  define desired keys/columns for output, default: all keys " + eol +
                "         keys list could be separated by comma, space, colon, semicolon" +
                "         regexp could be used for key definition." + eol +
                "  -h     print this help and exit." + eol +
                "Examples:" + eol +
                "  java kv2csv -b file        (converts file and prints in human readable table in stdout)" + eol +
                "  java kv2csv -c 'id,object' (gets lines from stdin and prints csv table with only 2 columns: id and object) " + eol +
                "  java kv2csv -f             (gets lines from stdin and prints csv table line by line) " +
                "  java kv2csv -c 'i.*'       (prints csv table with columns which started from 'i') ";
        System.out.println(help);
        System.exit(0);
    }

    public static String cleanFromQuotes(String s) {
        return s.replaceAll("\"|\'", "").trim();
    }

    public static LinkedList<String> getNotEmptyListFromArgumentOrNull(String arg) {
        String c = cleanFromQuotes(arg);
        if (c.isEmpty()) return null;
        else {
            String[] a = c.split(",|\\s|;|:");
            return new LinkedList<>(Arrays.asList(a));
        }
    }
}



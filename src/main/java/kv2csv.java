import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class kv2csv {
    List<LinkedHashMap<String, String>> list = new LinkedList<>();
    LinkedHashMap<String, Integer> header = new LinkedHashMap<>();
    final static String eol = System.lineSeparator();
    boolean headerUpdated = true;
    final Arguments args;
    final Pattern cleanLinePattern;

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
                        out.append(getValueString(map)).append(eol);
                    } else list.add(map);
                }
            }
            if (!args.follow) {
                printTable(out);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected LinkedHashMap<String, String> filterByCaptions(LinkedHashMap<String, String> map) {
        if (args.captions != null) map.keySet().retainAll(args.captions);
        return map;
    }

    public void printTable(OutputStreamWriter out) throws IOException {
        out.append(getOutputHeader());
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
        String s = getHeaderStream()
                .map(e -> expandColumnIfBeauty(header.get(e), e))
                .collect(Collectors.joining(args.outDelimiter));
        return s.concat(eol);
    }

    Stream<String> getHeaderStream() {
        return args.captions != null ? args.captions.stream() : header.keySet().stream();
    }

    public String getValueString(final LinkedHashMap<String, String> map) {
        return getHeaderStream().map(e -> expandColumnIfBeauty(header.get(e),
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
            k.process(inputStreamFromFile(arguments.filePath),System.out);
        }

        // file mode
        // piped mode
    }

    protected static InputStream inputStreamFromFile(String filePath) {
        File initialFile = new File(filePath);
        if (initialFile.exists()) try {
            return new FileInputStream(initialFile);
        } catch (FileNotFoundException ignored) {}
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
        String outDelimiter = ",";
        String inputDelimiter = ":";
        LinkedList<String> captions;
        String filePath;

        public Arguments(String... args) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    if (arg.contains("f")) this.follow = true;
                    if (arg.contains("b")) this.beauty = true;
                    if (arg.contains("id") && (i + 1 < args.length)) {
                        this.inputDelimiter = cleanFromQuotes(args[i + 1]);
                        i++;
                    }
                    if (arg.contains("od") && (i + 1 < args.length)) {
                        this.outDelimiter = cleanFromQuotes(args[i + 1]);
                        i++;
                    }
                    if ((arg.contains("c") || arg.contains("k")) && (i + 1 < args.length)) {
                        this.captions = getListFromArgument(args[i + 1]);
                        i++;
                    }
                } else {
                    filePath = arg;
                }

            }
            if (this.beauty) outDelimiter = "|";
        }
    }

    public static String cleanFromQuotes(String s) {
        return s.replaceAll("\"|\'", "");
    }

    public static LinkedList<String> getListFromArgument(String arg) {
        String c = cleanFromQuotes(arg);
        String[] a = c.split(",|\\s|;|:");
        return new LinkedList<>(Arrays.asList(a));
    }
}



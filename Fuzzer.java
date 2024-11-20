import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.*;

public class Fuzzer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Fuzzer.java \"<command_to_fuzz>\"");
            System.exit(1);
        }
        String commandToFuzz = args[0];
        String workingDirectory = "./";

        if (!Files.exists(Paths.get(workingDirectory, commandToFuzz))) {
            throw new RuntimeException("Could not find command '%s'.".formatted(commandToFuzz));
        }

        String seedInput = "<html a=\"value\">...</html>";

        ProcessBuilder builder = getProcessBuilderForCommand(commandToFuzz, workingDirectory);
        System.out.printf("Command: %s\n", builder.command());

        List<String> mutatedInputs = getMutatedInputs(seedInput);

        runCommand(builder, seedInput, mutatedInputs);
    }

    private static ProcessBuilder getProcessBuilderForCommand(String command, String workingDirectory) {
        ProcessBuilder builder = new ProcessBuilder();
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }
        builder.directory(new File(workingDirectory));
        builder.redirectErrorStream(true); // redirect stderr to stdout
        return builder;
    }

    private static void runCommand(ProcessBuilder builder, String seedInput, List<String> mutatedInputs) {
        Stream.concat(Stream.of(seedInput), mutatedInputs.stream()).forEach(
                input -> {
                    try {
                        Process process = builder.start();

                        System.out.println("Input: " + input);
                        OutputStream streamToCommand = process.getOutputStream();
                        streamToCommand.write(input.getBytes());
                        streamToCommand.flush();
                        streamToCommand.close();

                        int exitCode = process.waitFor();
                        System.out.printf("Exit code: %s\n", exitCode);

                        InputStream streamFromCommand = process.getInputStream();
                        String output = readStreamIntoString(streamFromCommand);
                        streamFromCommand.close();
                        System.out.printf("Output: %s\n", output);

                        if (exitCode != 0) {
                            System.out.println("Non-zero exit code detected!");
                        }

                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }


                }
        );
    }

    private static String readStreamIntoString(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines()
                .map(line -> line + System.lineSeparator())
                .collect(Collectors.joining());
    }

    private static List<String> getMutatedInputs(String seedInput) {
        List<String> mutations = new ArrayList<>();

        // Tag mutations
//        mutations.add(seedInput.replace("<html>", "")); // Remove opening <html> tag
//        mutations.add(seedInput.replace("</html>", "")); // Remove closing </html> tag
//        mutations.add(seedInput.replace("<body>", "<html>")); // Replace <body> with <html>
//        mutations.add(seedInput + "</unmatched>"); // Add unmatched closing tag

        // Attribute mutations
//        mutations.add(seedInput.replace("a=\"value\"", "a==")); // Invalid attribute format
//        mutations.add(seedInput.replace("a=\"value\"", "")); // Remove attribute entirely
//        mutations.add(seedInput.replace("a=\"value\"", "a=\"")); // Incomplete attribute

        // Content mutations
//        mutations.add(seedInput + "RandomText"); // Add unexpected random text
        mutations.add(seedInput.replace("...", "LONGTEXT".repeat(10))); // Long content injection
        mutations.add(seedInput.replace("value", "Longvaluetext")); // Long content injection
        mutations.add(seedInput.replace("<html", "<" + "html".repeat(10)));
        mutations.add(seedInput.replace("<html a=\"value\">", "<html a=\"value\">".repeat(10)));

//        mutations.add(seedInput.replace("<html", "<htmll")); // Typo in tag

        // Structure mutations
//        mutations.add("<html>".repeat(100)); // Deep nesting
//        mutations.add(seedInput + "<unclosed"); // Add an unclosed tag
//        mutations.add(seedInput.replace("</body>", "")); // Missing closing </body>

        // Completely invalid input
//        mutations.add("RandomBinaryData".repeat(5)); // Random non-HTML input
//        mutations.add("<><<<<<<"); // Garbage HTML

//        // get rid of opening tabs
//        mutations.add(seedInput.replaceAll("<[a-zA-Z]+ [a-zA-Z]+=\".+\">", "</no open>"));
//
//        mutations.add(seedInput.replace(">", "<"));
//        // try all kinds of different symbols
//        // Inject special symbols
//        mutations.add(seedInput.replace("<html>", "<html @>")); // Add special symbol
//        mutations.add(seedInput.replace("value", "value$#")); // Add special symbols in attribute value
//        mutations.add(seedInput.replace(">", "!>")); // Replace `>` with a symbol
//        mutations.add(seedInput.replace("<", "[[")); // Replace `<` with a symbol
//        mutations.add(seedInput + "<!DOCTYPE>"); // Add an unexpected special HTML declaration
//
//        // Inject Unicode characters
//        mutations.add(seedInput.replace("value", "value🔥")); // Add emoji in attribute value
//        mutations.add(seedInput + "\uFFFF"); // Add invalid Unicode character
//        mutations.add(seedInput.replace("a=\"value\"", "a=\"\u263A\"")); // Insert Unicode symbol
//
//        // Break structure with unescaped symbols
//        mutations.add(seedInput.replace("value", "&invalid;")); // Insert invalid HTML entity
//        mutations.add(seedInput.replace("<body>", "<body &>")); // Add symbol in tag
//
//        // Use excessive whitespace
//        mutations.add(seedInput.replace("<html>", "<html    >")); // Add excessive spaces
//        mutations.add(seedInput.replace(">", ">\t\n")); // Add tabs and newlines
//        mutations.add(seedInput + "   "); // Add trailing spaces

//        mutations.add(seedInput.replace("<", "{").replace(">", "}")); // Replace < > with curly brackets
//        mutations.add(seedInput.replace("<", "«").replace(">", "»")); // Replace < > with angled brackets
//
//        mutations.add(seedInput.replace("<html>", "<<html>>")); // Double wrap tag
//        mutations.add(seedInput.replace(">", ">###")); // Add junk after closing tag
//
//        mutations.add(seedInput.substring(0, 5) + "!@#$%" + seedInput.substring(5)); // Insert special symbols mid-string


        return mutations;
    }

//
//
//private static List<String> getMutatedInputs(String seedInput) {
//    List<String> mutations = new ArrayList<>();
//
//    // Apply dynamic tag mutations
//    mutations.addAll(mutateTags(seedInput));
//
//    // Apply dynamic attribute mutations
//    mutations.addAll(mutateAttributes(seedInput));
//
//    // Apply dynamic content mutations
//    mutations.addAll(mutateContent(seedInput));
//
//    // Add random mutations
//    mutations.addAll(generateRandomMutations(seedInput, 5));
//
//    return mutations;
//}
//
//    private static List<String> mutateTags(String input) {
//        List<String> mutations = new ArrayList<>();
//        Pattern tagPattern = Pattern.compile("<(/?\\w+).*?>"); // Match opening/closing tags
//        Matcher matcher = tagPattern.matcher(input);
//
//        while (matcher.find()) {
//            String tag = matcher.group(1); // Extract the tag name
//
//            // Replace tag with random string
//            mutations.add(input.replace("<" + tag, "<randomTag"));
//
//            // Remove the tag
//            mutations.add(input.replace("<" + tag, ""));
//        }
//        return mutations;
//    }
//
//    private static List<String> mutateAttributes(String input) {
//        List<String> mutations = new ArrayList<>();
//        Pattern attrPattern = Pattern.compile("(\\w+)=\"(.*?)\""); // Match key="value"
//        Matcher matcher = attrPattern.matcher(input);
//
//        while (matcher.find()) {
//            String attribute = matcher.group(0); // The full attribute: key="value"
//
//            // Modify attribute value
//            mutations.add(input.replace(attribute, matcher.group(1) + "=\"randomValue\""));
//
//            // Remove the attribute
//            mutations.add(input.replace(attribute, ""));
//        }
//        return mutations;
//    }
//
//    private static List<String> mutateContent(String input) {
//        List<String> mutations = new ArrayList<>();
//        Pattern contentPattern = Pattern.compile(">(.*?)<"); // Match content between > and <
//        Matcher matcher = contentPattern.matcher(input);
//
//        while (matcher.find()) {
//            String content = matcher.group(1); // Extract the content
//
//            // Replace content with random text
//            mutations.add(input.replace(content, "RandomContent"));
//
//            // Remove the content
//            mutations.add(input.replace(content, ""));
//        }
//        return mutations;
//    }
//
//    private static List<String> generateRandomMutations(String seedInput, int count) {
//        List<String> mutations = new ArrayList<>();
//        for (int i = 0; i < count; i++) {
//            mutations.add(insertRandomCharacters(seedInput));
//            mutations.add(removeRandomCharacters(seedInput));
//            mutations.add(randomString(50)); // Completely random input
//        }
//        return mutations;
//    }
//
//    private static String insertRandomCharacters(String input) {
//        Random random = new Random();
//        int index = random.nextInt(input.length());
//        return input.substring(0, index) + randomString(5) + input.substring(index);
//    }
//
//    private static String removeRandomCharacters(String input) {
//        Random random = new Random();
//        int index = random.nextInt(input.length());
//        return input.substring(0, index) + input.substring(index + 1);
//    }
//
//    private static String randomString(int length) {
//        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
//        StringBuilder sb = new StringBuilder();
//        Random random = new Random();
//        for (int i = 0; i < length; i++) {
//            sb.append(chars.charAt(random.nextInt(chars.length())));
//        }
//        return sb.toString();
//    }
}

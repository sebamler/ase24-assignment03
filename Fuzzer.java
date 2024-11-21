import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        // Valid seed input
        String seedInput = "<html a=\"value\">...</html>";
//        String seedInput = "<body var=\"stuff\"><tag>some Content in here</tag></body>";

        ProcessBuilder builder = getProcessBuilderForCommand(commandToFuzz, workingDirectory);
        System.out.printf("Command: %s\n", builder.command());

        // Generate mutations dynamically
        List<String> mutatedInputs = getMutatedInputs(seedInput);

        // Run all inputs
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
        builder.redirectErrorStream(true); // Redirect stderr to stdout
        return builder;
    }

    private static void runCommand(ProcessBuilder builder, String seedInput, List<String> mutatedInputs) {
        try (BufferedWriter logWriter = new BufferedWriter(new FileWriter("non_zero_exit_codes.log", true))) { // Open log file
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
                            System.out.println("\u001B[31m" + "Non-zero exit code detected!" + "\u001B[0m");

                            // Log the input and output to the file
                            String logMessage = String.format("Input: %s%nleads to a non-zero exit code with %noutput: %s.%n", input, output);
                            logWriter.write(logMessage); // Write to the file
                            logWriter.flush(); // Ensure it is saved

                            // exit with 1 (non-zero) if one tof the generated inputs triggered a non-zero exit for the program
                            System.exit(1);
                        }

                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to open log file", e);
        }
        // exit with 0, if all runs returned 0
        System.exit(0);
    }

    private static String readStreamIntoString(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines()
                .map(line -> line + System.lineSeparator())
                .collect(Collectors.joining());
    }

    private static List<String> getMutatedInputs(String seedInput) {
        List<String> mutations = new ArrayList<>();
//        mutations.addAll(mutateTags(seedInput));        // Dynamic tag mutations
//        mutations.addAll(mutateAttributes(seedInput)); // Dynamic attribute mutations
//        mutations.addAll(mutateContent(seedInput));    // Dynamic content mutations
//        mutations.add("<html a=\"value\">..........................................................................................");
//        mutations.add("<html a=\"value\">..........................................................................................</html>");
        return mutations;
    }

    private static List<String> mutateTags(String input) {
        List<String> mutations = new ArrayList<>();
        Pattern tagPattern = Pattern.compile("<(/?\\w+).*?>"); // Match tags
        Matcher matcher = tagPattern.matcher(input);

        while (matcher.find()) {
            String tag = matcher.group(1); // Extract the tag name

            // Add random characters to the tag name
            String tagWithRandomChar = input.replace(tag, addRandomChar(tag));
            mutations.add(tagWithRandomChar);

            // Delete random characters from the tag name
            mutations.add(input.replace(tag, deleteRandomChar(tag)));

            // Swap random characters in the tag name
            mutations.add(input.replace(tag, swapRandomChar(tag)));

            // Duplicate the entire tag
            mutations.add(input.replace(tag, tagWithRandomChar.repeat(20)));
            mutations.add(input.replace(tag, tag.repeat(20)));

            // Add opening < at the beginning
            mutations.add(input.replace(tag, "<"+ tag));
        }

        return mutations;
    }

    private static List<String> mutateAttributes(String input) {
        List<String> mutations = new ArrayList<>();
        Pattern attrPattern = Pattern.compile("(\\w+)=\"(.*?)\""); // Match attributes
        Matcher matcher = attrPattern.matcher(input);

        while (matcher.find()) {
            String value = matcher.group(2);    // Extract the value

            // Add random characters to the value
            String valueWithRandomChar = input.replace(value, addRandomChar(value));
            mutations.add(valueWithRandomChar);

            // Delete random characters from the value
            mutations.add(input.replace(value, deleteRandomChar(value)));

            // Duplicate the value
            mutations.add(input.replace(value, valueWithRandomChar.repeat(10)));
            mutations.add(input.replace(value, value.repeat(10)));
        }

        return mutations;
    }

    private static List<String> mutateContent(String input) {
        List<String> mutations = new ArrayList<>();
        Pattern contentPattern = Pattern.compile(">(.*?)<"); // Match content between tags
        Matcher matcher = contentPattern.matcher(input);

        while (matcher.find()) {
            String content = matcher.group(1); // Extract the content

            // Add random characters to the content
            String contentWithRandomChar = input.replace(content, addRandomChar(content));
            mutations.add(contentWithRandomChar);

            // Delete random characters from the content
            mutations.add(input.replace(content, deleteRandomChar(content)));

            // Duplicate random segments of the content
            mutations.add(input.replace(content, contentWithRandomChar.repeat(30)));
            mutations.add(input.replace(content, content.repeat(30)));

            // Shuffle the content
            mutations.add(input.replace(content, shuffleSegments(content)));
        }

        return mutations;
    }

    private static String addRandomChar(String input) {
        Random random = new Random();
        int index = random.nextInt(input.length() + 1);
        char randomChar = randomChar();
        return input.substring(0, index) + randomChar + input.substring(index);
    }

    private static String deleteRandomChar(String input) {
        if (input.isEmpty()) return input;
        Random random = new Random();
        int index = random.nextInt(input.length());
        return input.substring(0, index) + input.substring(index + 1);
    }

    private static String swapRandomChar(String input) {
        if (input.length() < 2) return input;
        Random random = new Random();
        int index1 = random.nextInt(input.length());
        int index2 = random.nextInt(input.length());
        char[] chars = input.toCharArray();
        char temp = chars[index1];
        chars[index1] = chars[index2];
        chars[index2] = temp;
        return new String(chars);
    }

    private static String shuffleSegments(String input) {
        List<String> segments = Arrays.asList(input.split(" "));
        Collections.shuffle(segments);
        return String.join(" ", segments);
    }

    private static char randomChar() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=<>?/|";
        Random random = new Random();
        return chars.charAt(random.nextInt(chars.length()));
    }
}

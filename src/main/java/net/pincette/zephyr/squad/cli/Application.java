package net.pincette.zephyr.squad.cli;

import static java.lang.System.exit;
import static java.util.Arrays.stream;
import static java.util.logging.Logger.getGlobal;
import static java.util.stream.Collectors.joining;
import static net.pincette.util.Collections.set;
import static net.pincette.zephyr.squad.cli.Application.VERSION;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;
import net.pincette.zephyr.squad.JUnit;
import net.pincette.zephyr.squad.Uploader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

@Command(
    mixinStandardHelpOptions = true,
    version = VERSION,
    subcommands = {HelpCommand.class},
    description = "The command-line for Pincette Zephyr Squad.")
public class Application implements Callable<Integer> {
  static final String VERSION = "1.0";

  @Option(
      names = {"-c", "--components"},
      arity = "1..*",
      description =
          "The components to which the test cases belong. When tests in this list are not "
              + "present then they are marked as unexecuted.")
  private String[] components;

  @Option(
      names = {"-e", "--epics"},
      arity = "1..*",
      description =
          "The epics to which the test cases belong. When tests in this list are not "
              + "present then they are marked as unexecuted.")
  private String[] epics;

  @Option(
      names = {"-f", "--files"},
      required = true,
      arity = "1..*",
      description = "The files to upload to Zephyr Squad.")
  private File[] files;

  @Option(
      names = {"-j", "--jira-endpoint"},
      required = true,
      description = "The JIRA REST API endpoint.")
  private String jira;

  @Option(
      names = {"-p", "--password"},
      required = true,
      description = "The password for the JIRA REST API endpoint.")
  private String password;

  @Option(
      names = {"-pr", "--project"},
      required = true,
      description = "The name of the JIRA project.")
  private String project;

  @Option(
      names = {"-pv", "--project-version"},
      description = "The name of the version of the JIRA project.")
  private String projectVersion;

  @Option(
      names = {"-u", "--username"},
      required = true,
      description = "The username for the JIRA REST API endpoint.")
  private String username;

  @Option(
      names = {"-z", "--zephyr-endpoint"},
      required = true,
      description = "The Zephyr Squad REST API endpoint.")
  private String zephyr;

  public static void main(final String[] args) {
    Optional.of(new CommandLine(new Application()).execute(args))
        .filter(code -> code != 0)
        .ifPresent(System::exit);
  }

  public Integer call() {
    upload();
    exit(0);
    return 0;
  }

  private void upload() {
    final Uploader uploader =
        new Uploader()
            .withProject(project)
            .withVersion(projectVersion)
            .withEpics(epics != null ? set(epics) : null)
            .withComponents(components != null ? set(components) : null)
            .withJiraEndpoint(jira)
            .withZephyrEndpoint(zephyr)
            .withUsername(username)
            .withPassword(password);

    getGlobal()
        .info(() -> "Uploading " + stream(files).map(File::getAbsolutePath).collect(joining(", ")));
    uploader.upload(JUnit.loadTestcases(files)).toCompletableFuture().join();
  }
}

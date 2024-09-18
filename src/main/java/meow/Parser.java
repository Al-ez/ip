package meow;

import java.util.ArrayList;

import java.io.IOException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;

/**
 * Handles parsing of user input and execution of commands in the Meow application.
 * Supports various commands such as adding/deleting tasks, marking/unmarking tasks,
 * as well as viewing outstanding tasks.
 */
public class Parser {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static Ui ui = new Ui();
    public static LocalDate parseDateTime(String input) throws DateTimeException, DateTimeParseException {
        return LocalDate.parse(input, FORMATTER);
    }

    private LocalDate parseDate(String date) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(date, formatter);
    }

  /**
     * Reads a single user input command and executes the corresponding task management action.
     *
     * @param input The user input command.
     * @param taskList The list of tasks to operate on.
     * @param ui The UI instance to handle user interactions.
     * @param storage The storage instance for saving and loading tasks.
     * @throws IOException If an I/O error occurs during saving.
     * @throws MeowException If an invalid command or task operation is encountered.
     * @throws DateTimeParseException If the date format is incorrect.
     */
    public String read(String input, TaskList taskList, Ui ui, Storage storage)
            throws IOException, MeowException, DateTimeParseException {
        try {
            if (input.trim().equals("bye")) {
                return ui.getGoodbyeMessage();
            }

            if (input.trim().equals("list")) {
                if (taskList.getTaskCount() == 0) {
                    return ui.getMessage("No outstanding tasks. MEOW!");
                } else {
                    return taskList.stringify();
                }
            } else if (input.startsWith("mark")) {
                if (input.startsWith("mark ")) {
                    int index = Integer.parseInt(input.substring(5).trim()) - 1;
                    if (index >= 0 && index < taskList.getTaskCount()) {
                        Task task = taskList.getTask(index);
                        task.mark();
                        storage.saveTasks(taskList.getTasks());
                        return ui.getMessage("Nice! I've marked this task as done:\n" + task);
                    } else {
                        throw new MeowException("GRRR! Invalid task number, you only have " + taskList.getTaskCount()
                                + (taskList.getTaskCount() == 1 ? " task." : " tasks."));
                    }
                } else {
                    throw new MeowException("Please specify which task to mark. Example: mark 1");
                }
            } else if (input.startsWith("unmark")) {
                if (input.startsWith("unmark ")) {
                    int index = Integer.parseInt(input.substring(7).trim()) - 1;
                    if (index >= 0 && index < taskList.getTaskCount()) {
                        Task task = taskList.getTask(index);
                        task.unMark();
                        storage.saveTasks(taskList.getTasks());
                        return ui.getMessage("OK, I've marked this task as not done yet:\n" + task);
                    } else {
                        throw new MeowException("GRRR! Invalid task number, you only have " + taskList.getTaskCount()
                                + (taskList.getTaskCount() == 1 ? " task." : " tasks."));
                    }
                } else {
                    throw new MeowException("Please specify which task to unmark. Example: unmark 1");
                }
            } else if (input.startsWith("deadline")) {
                if (input.startsWith("deadline ")) {
                    String[] parts = input.substring(9).split(" /by ");
                    if (parts.length == 2) {
                        try {
                            LocalDate by = parseDate(parts[1].trim());
                            taskList.addTask(new Deadline(parts[0], by));
                            storage.saveTasks(taskList.getTasks());
                            return taskList.addTaskMessage();
                        } catch (DateTimeException | MeowException e) {
                            return e.getMessage();
                        }
                    } else {
                        throw new MeowException("Invalid deadline format. "
                                + "Example: deadline return book /by yyyy-mm-dd");
                    }
                } else {
                    throw new MeowException("Invalid deadline format. Example: deadline return book /by yyyy-mm-dd");
                }
            } else if (input.startsWith("event")) {
                if (input.startsWith("event ")) {
                    String[] parts = input.substring(6).split(" /from | /to ");
                    if (parts.length == 3) {
                        try {
                            LocalDate from = parseDate(parts[1].trim());
                            LocalDate to = parseDate(parts[2].trim());
                            if (from.isBefore(to)) {
                                taskList.addTask(new Event(parts[0], from, to));
                                storage.saveTasks(taskList.getTasks());
                                return taskList.addTaskMessage();
                            } else {
                                throw new MeowException("GRRR! Start date cannot be after End date");
                            }
                        } catch (DateTimeException | MeowException e) {
                            return e.getMessage();
                        }
                    } else {
                        throw new MeowException("Invalid event format. "
                                + "Example: event gym workout /from yyyy-mm-dd /to yyyy-mm-dd");
                    }
                } else {
                    throw new MeowException("Invalid event format. "
                            + "Example: event gym workout /from yyyy-mm-dd /to yyyy-mm-dd");
                }
            } else if (input.startsWith("todo")) {
                if (input.startsWith("todo ")) {
                    String description = input.substring(5).trim();
                    taskList.addTask(new ToDo(description));
                    storage.saveTasks(taskList.getTasks());
                    return taskList.addTaskMessage();
                } else {
                    throw new MeowException("Invalid todo format. Example: todo eat lunch");
                }
            } else if (input.startsWith("delete")) {
                if (input.startsWith("delete ")) {
                    int index = Integer.parseInt(input.substring(7).trim()) - 1;
                    if (index >= 0 && index < taskList.getTaskCount()) {
                        Task removedTask = taskList.getTask(index);
                        taskList.deleteTask(index);
                        storage.saveTasks(taskList.getTasks());
                        return taskList.deleteTaskMessage(removedTask);
                    } else {
                        throw new MeowException("GRRR! Invalid task number, you only have " + taskList.getTaskCount()
                                + (taskList.getTaskCount() == 1 ? " task." : " tasks."));
                    }
                } else {
                    throw new MeowException("Invalid delete format. Example: delete 1");
                }
            } else if (input.startsWith("find")) {
                if (input.startsWith("find ")) {
                    String keyword = input.substring(5).trim();
                    TaskList filteredList = new TaskList(new ArrayList<>());

                    for (int i = 0; i < taskList.getTaskCount(); i++) {
                        Task currentTask = taskList.getTask(i);
                        if (currentTask.getDescription().toLowerCase().contains(keyword.toLowerCase())) {
                            filteredList.addTask(currentTask);
                        }
                    }
                    if (filteredList.getTaskCount() == 0) {
                        return ui.getMessage("No tasks match your search. ROWR!");
                    } else {
                        return filteredList.stringify();
                    }
                } else {
                    throw new MeowException("Invalid find format. Example: find book");
                }
            } else if (input.equals("sort")) {
                if (taskList.getTaskCount() == 0) {
                    return ui.getMessage("No outstanding tasks. MEOW!");
                } else {
                    TaskList sortedList = taskList.sort();
                    return sortedList.stringify();
                }
            } else {
                return ui.getMessage("Whatchu sayin bruh?");
            }
        } catch (IndexOutOfBoundsException e) {
            return ui.getMessage("Invalid task number.");
        } catch (DateTimeParseException e) {
            return ui.getMessage("Invalid date format.");
        }
    }
}

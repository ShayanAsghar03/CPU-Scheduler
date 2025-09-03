import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PSDQSchedulerGUI extends JFrame {

    private JPanel inputPanel;
    private JButton enterProcessButton, calculateButton, clearButton;
    private JTextArea outputArea;
    private int processCount = 0;
    private ArrayList<JTextField[]> processFields = new ArrayList<>();
    private JComboBox<String> algorithmSelector;
    private boolean needPriority = false, needQuantum = false;
    private JTextField quantumField;
    private GanttChartPanel ganttChartPanel; // New: Gantt Chart Panel

    public PSDQSchedulerGUI() {
        setTitle("CPU Scheduling Algorithms with Gantt Chart");
        setSize(1200, 700); // Increased size to accommodate Gantt chart
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        algorithmSelector = new JComboBox<>(new String[]{"FCFS", "SJF (Non-Preemptive)", "SJF (Preemptive)", "Priority (Preemptive)", "Priority (Non-Preemptive)", "Round Robin", "PSDQ (Proposed)"});
        topPanel.add(new JLabel("Select Algorithm: "));
        topPanel.add(algorithmSelector);

        enterProcessButton = new JButton("Enter Processes");
        topPanel.add(enterProcessButton);

        calculateButton = new JButton("Calculate");
        calculateButton.setEnabled(false); // Initially disabled until processes are entered
        topPanel.add(calculateButton);

        clearButton = new JButton("Clear");
        topPanel.add(clearButton);

        quantumField = new JTextField(5);
        quantumField.setEnabled(false); // Initially disabled
        topPanel.add(new JLabel("Quantum:"));
        topPanel.add(quantumField);

        add(topPanel, BorderLayout.NORTH);

        // Main split pane for Input Panel (left) and Results/Gantt (right)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.25); // Input panel takes 25% of width

        inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        mainSplitPane.setLeftComponent(new JScrollPane(inputPanel)); // Scrollable input panel

        // Right side split pane for Output Area (top) and Gantt Chart (bottom)
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setResizeWeight(0.6); // Output area takes 60% of height

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        rightSplitPane.setTopComponent(new JScrollPane(outputArea)); // Scrollable output area

        ganttChartPanel = new GanttChartPanel(); // Initialize the Gantt chart panel
        rightSplitPane.setBottomComponent(ganttChartPanel);

        mainSplitPane.setRightComponent(rightSplitPane);
        add(mainSplitPane, BorderLayout.CENTER); // Add the main split pane to the frame's center

        // Add action listeners
        algorithmSelector.addActionListener(e -> updateAlgorithmRequirements());
        enterProcessButton.addActionListener(e -> promptProcessCount());
        calculateButton.addActionListener(e -> calculateScheduling());
        clearButton.addActionListener(e -> clearAll());

        // Initial update based on default selected algorithm
        updateAlgorithmRequirements();
    }

    /**
     * Updates UI elements based on the selected scheduling algorithm.
     * For example, enables/disables the priority and quantum input fields.
     */
    private void updateAlgorithmRequirements() {
        String selected = (String) algorithmSelector.getSelectedItem();
        needPriority = selected.contains("Priority") || selected.equals("PSDQ (Proposed)");
        needQuantum = selected.equals("Round Robin");

        quantumField.setEnabled(needQuantum);

        // Re-render process fields to show/hide priority field if processes are already entered
        if (processCount > 0) {
            setProcessFields(processCount); // This will rebuild the input panel
        }
    }

    /**
     * Prompts the user to enter the number of processes.
     * If a valid number is entered, it sets up the process input fields.
     */
    private void promptProcessCount() {
        String input = JOptionPane.showInputDialog(this, "Enter number of processes:");
        if (input != null) {
            try {
                processCount = Integer.parseInt(input);
                if (processCount <= 0) {
                    throw new NumberFormatException("Process count must be positive.");
                }
                setProcessFields(processCount);
                calculateButton.setEnabled(true); // Enable calculate button after processes are set up
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number of processes. Please enter a positive integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Sets up the input fields for each process (Arrival Time, Burst Time, and optional Priority).
     *
     * @param count The number of processes to create fields for.
     */
    private void setProcessFields(int count) {
        inputPanel.removeAll(); // Clear existing fields
        processFields.clear(); // Clear existing references

        for (int i = 0; i < count; i++) {
            JPanel panel = new JPanel();
            panel.add(new JLabel("P" + (i + 1) + " AT:"));
            JTextField atField = new JTextField(3);
            panel.add(atField);

            panel.add(new JLabel("BT:"));
            JTextField btField = new JTextField(3);
            panel.add(btField);

            JTextField priorityField = new JTextField(3);
            if (needPriority) {
                panel.add(new JLabel("Priority:"));
                panel.add(priorityField);
            }

            // Store the text fields in an array for easy retrieval
            // Index 0: AT, Index 1: BT, Index 2: Priority (will always exist, but may not be on panel)
            processFields.add(new JTextField[]{atField, btField, priorityField});
            inputPanel.add(panel);
        }

        revalidate(); // Re-layout the panel
        repaint(); // Repaint the panel
    }

    /**
     * Performs the CPU scheduling calculation based on user inputs and selected algorithm.
     * Includes extensive input validation to provide specific error messages.
     */
    private void calculateScheduling() {
        outputArea.setText(""); // Clear previous output
        ganttChartPanel.clearChart(); // Clear previous Gantt chart
        try {
            // --- Input Validation ---

            // Validate Quantum Field if needed (for Round Robin)
            String selected = (String) algorithmSelector.getSelectedItem();
            int quantum = 0;
            if (needQuantum) {
                String quantumText = quantumField.getText().trim();
                if (quantumText.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Quantum field cannot be empty for Round Robin.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    quantum = Integer.parseInt(quantumText);
                    if (quantum <= 0) {
                        JOptionPane.showMessageDialog(this, "Quantum must be a positive integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number for Quantum. Please enter an integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            int[] at = new int[processCount];
            int[] bt = new int[processCount];
            int[] priority = new int[processCount];

            // Validate and parse process fields
            for (int i = 0; i < processCount; i++) {
                JTextField[] fields = processFields.get(i);
                String atText = fields[0].getText().trim();
                String btText = fields[1].getText().trim();
                // priorityText will always exist, but may be empty if priority is not needed
                String priorityText = (fields.length > 2 && fields[2] != null) ? fields[2].getText().trim() : "";

                if (atText.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Arrival Time (AT) for Process P" + (i + 1) + " cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (btText.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Burst Time (BT) for Process P" + (i + 1) + " cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (needPriority && priorityText.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Priority for Process P" + (i + 1) + " cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    at[i] = Integer.parseInt(atText);
                    bt[i] = Integer.parseInt(btText);
                    // Arrival time can be 0 or positive, Burst time must be positive
                    if (at[i] < 0 || bt[i] <= 0) {
                        JOptionPane.showMessageDialog(this, "Invalid values for Process P" + (i + 1) + ". Arrival Time (AT) must be non-negative, Burst Time (BT) must be positive.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number for Arrival Time (AT) or Burst Time (BT) in Process P" + (i + 1) + ". Please enter integers.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (needPriority) {
                    try {
                        priority[i] = Integer.parseInt(priorityText);
                        if (priority[i] < 0) { // Priorities are typically non-negative
                            JOptionPane.showMessageDialog(this, "Priority for Process P" + (i + 1) + " must be non-negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid number for Priority in Process P" + (i + 1) + ". Please enter an integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    priority[i] = 0; // Default priority if not needed by the algorithm
                }
            }

            // --- If all validations pass, proceed with scheduling ---
            Scheduler scheduler = new Scheduler(at, bt, priority, processCount);

            if (selected.equals("FCFS")) scheduler.fcfs();
            else if (selected.equals("SJF (Non-Preemptive)")) scheduler.sjf(false);
            else if (selected.equals("SJF (Preemptive)")) scheduler.sjf(true);
            else if (selected.equals("Priority (Preemptive)")) scheduler.priority(true);
            else if (selected.equals("Priority (Non-Preemptive)")) scheduler.priority(false);
            else if (selected.equals("Round Robin")) scheduler.roundRobin(quantum); // Use validated quantum
            else if (selected.equals("PSDQ (Proposed)")) scheduler.psdq();

            outputArea.setText(scheduler.getResult());
            ganttChartPanel.drawChart(scheduler.getGanttData()); // Draw the Gantt chart

        } catch (Exception e) {
            // Catch any unexpected errors that might occur after validation (less likely now)
            JOptionPane.showMessageDialog(this, "An unexpected error occurred during calculation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); // Print stack trace for debugging purposes
        }
    }

    /**
     * Clears all input fields, output area, and resets the process count.
     */
    private void clearAll() {
        inputPanel.removeAll();
        outputArea.setText("");
        ganttChartPanel.clearChart(); // Clear Gantt chart
        processFields.clear();
        processCount = 0;
        calculateButton.setEnabled(false); // Disable calculate button until new processes are entered
        quantumField.setText(""); // Clear quantum field
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PSDQSchedulerGUI().setVisible(true));
    }
}

/**
 * Scheduler class contains implementations of various CPU scheduling algorithms.
 */
class Scheduler {
    private int[] at, bt, priority;
    private int n;
    private StringBuilder result = new StringBuilder(); // Stores the results for display
    private List<GanttEntry> ganttData; // Stores data for the Gantt chart

    public Scheduler(int[] at, int[] bt, int[] priority, int n) {
        this.at = at;
        this.bt = bt;
        this.priority = priority;
        this.n = n;
        this.ganttData = new ArrayList<>(); // Initialize Gantt data list
    }

    /**
     * Implements the First-Come, First-Served (FCFS) scheduling algorithm.
     */
    public void fcfs() {
        int[] ct = new int[n]; // Completion Time
        int[] wt = new int[n]; // Waiting Time
        int[] tat = new int[n]; // Turnaround Time
        int time = 0; // Current time

        // Sort processes by arrival time for FCFS
        // Create an array of process indices to sort them based on AT
        Integer[] processIndices = new Integer[n];
        for (int i = 0; i < n; i++) {
            processIndices[i] = i;
        }

        Arrays.sort(processIndices, Comparator.comparingInt(i -> at[i]));

        for (int i : processIndices) {
            int startTime = Math.max(time, at[i]); // Actual start time of execution
            time = startTime + bt[i];
            ct[i] = time;
            tat[i] = ct[i] - at[i];
            wt[i] = tat[i] - bt[i];

            // Log for Gantt chart
            ganttData.add(new GanttEntry(i + 1, startTime, time)); // Process ID is 1-based
        }
        appendResults(ct, tat, wt);
    }

    /**
     * Implements the Shortest Job First (SJF) scheduling algorithm.
     * Can operate in preemptive or non-preemptive mode.
     *
     * @param preemptive True for preemptive SJF, false for non-preemptive.
     */
    public void sjf(boolean preemptive) {
        int[] ct = new int[n];
        int[] wt = new int[n];
        int[] tat = new int[n];
        int[] rem = Arrays.copyOf(bt, n); // Remaining burst time
        boolean[] done = new boolean[n]; // Track completed processes

        int time = 0;
        int completed = 0;
        int lastExecutedProcess = -1; // To merge consecutive blocks for Gantt chart
        int blockStartTime = 0;

        while (completed < n) {
            int shortestJobIndex = -1;
            int minRemainingTime = Integer.MAX_VALUE;

            // Find the process with the shortest remaining time among arrived and not completed processes
            for (int i = 0; i < n; i++) {
                if (at[i] <= time && !done[i] && rem[i] < minRemainingTime && rem[i] > 0) {
                    minRemainingTime = rem[i];
                    shortestJobIndex = i;
                }
            }

            // Log previous block if process changed or no process was found
            if (lastExecutedProcess != -1 && shortestJobIndex != lastExecutedProcess) {
                ganttData.add(new GanttEntry(lastExecutedProcess + 1, blockStartTime, time));
            }

            if (shortestJobIndex == -1) {
                if (lastExecutedProcess != -1) { // No process ready, but previous one was running
                    ganttData.add(new GanttEntry(lastExecutedProcess + 1, blockStartTime, time));
                    lastExecutedProcess = -1; // Reset as CPU is now idle
                }
                time++; // No process ready, advance time
                blockStartTime = time; // Update block start time for potential idle period or next process
                continue;
            }

            if (shortestJobIndex != lastExecutedProcess) {
                blockStartTime = time; // New block starts now
                lastExecutedProcess = shortestJobIndex;
            }

            if (preemptive) {
                rem[shortestJobIndex]--; // Execute for one unit of time
                time++;

                if (rem[shortestJobIndex] == 0) { // Process completed
                    ct[shortestJobIndex] = time; // Completion time is current time
                    tat[shortestJobIndex] = ct[shortestJobIndex] - at[shortestJobIndex];
                    wt[shortestJobIndex] = tat[shortestJobIndex] - bt[shortestJobIndex];
                    done[shortestJobIndex] = true;
                    completed++;
                    // Log the completed block
                    ganttData.add(new GanttEntry(shortestJobIndex + 1, blockStartTime, time));
                    lastExecutedProcess = -1; // Reset as this process is done
                }
            } else { // Non-preemptive
                int startTime = Math.max(time, at[shortestJobIndex]);
                if (lastExecutedProcess == -1 || lastExecutedProcess != shortestJobIndex) {
                    blockStartTime = startTime;
                }
                lastExecutedProcess = shortestJobIndex;


                time = Math.max(time, at[shortestJobIndex]); // Advance time to arrival if not arrived yet
                int currentBlockStart = time; // For Gantt chart
                time += rem[shortestJobIndex]; // Execute whole burst time
                ct[shortestJobIndex] = time;
                tat[shortestJobIndex] = ct[shortestJobIndex] - at[shortestJobIndex];
                wt[shortestJobIndex] = tat[shortestJobIndex] - bt[shortestJobIndex];
                rem[shortestJobIndex] = 0; // Mark as done
                done[shortestJobIndex] = true;
                completed++;
                // Log the completed block for non-preemptive
                ganttData.add(new GanttEntry(shortestJobIndex + 1, currentBlockStart, time));
                lastExecutedProcess = -1; // Reset after a full non-preemptive execution
                blockStartTime = time; // Update block start for next potential process
            }
        }
        // If a process was running and the loop finished, log the last block
        if (lastExecutedProcess != -1) {
            ganttData.add(new GanttEntry(lastExecutedProcess + 1, blockStartTime, time));
        }
        appendResults(ct, tat, wt);
    }

    /**
     * Implements the Priority scheduling algorithm.
     * Can operate in preemptive or non-preemptive mode. Lower priority number means higher priority.
     *
     * @param preemptive True for preemptive Priority, false for non-preemptive.
     */
    public void priority(boolean preemptive) {
        int[] ct = new int[n];
        int[] wt = new int[n];
        int[] tat = new int[n];
        int[] rem = Arrays.copyOf(bt, n); // Remaining burst time
        boolean[] done = new boolean[n]; // Track completed processes

        int time = 0;
        int completed = 0;
        int lastExecutedProcess = -1;
        int blockStartTime = 0;

        while (completed < n) {
            int highestPriorityIndex = -1;
            int minPriorityValue = Integer.MAX_VALUE;

            // Find the process with the highest priority (lowest value) among arrived and not completed processes
            for (int i = 0; i < n; i++) {
                if (at[i] <= time && !done[i] && priority[i] < minPriorityValue && rem[i] > 0) {
                    minPriorityValue = priority[i];
                    highestPriorityIndex = i;
                }
            }

            // Log previous block if process changed or no process was found
            if (lastExecutedProcess != -1 && highestPriorityIndex != lastExecutedProcess) {
                ganttData.add(new GanttEntry(lastExecutedProcess + 1, blockStartTime, time));
            }

            if (highestPriorityIndex == -1) {
                if (lastExecutedProcess != -1) {
                    ganttData.add(new GanttEntry(lastExecutedProcess + 1, blockStartTime, time));
                    lastExecutedProcess = -1;
                }
                time++; // No process ready, advance time
                blockStartTime = time;
                continue;
            }

            if (highestPriorityIndex != lastExecutedProcess) {
                blockStartTime = time;
                lastExecutedProcess = highestPriorityIndex;
            }


            if (preemptive) {
                rem[highestPriorityIndex]--; // Execute for one unit of time
                time++;

                if (rem[highestPriorityIndex] == 0) { // Process completed
                    ct[highestPriorityIndex] = time; // Completion time is current time
                    tat[highestPriorityIndex] = ct[highestPriorityIndex] - at[highestPriorityIndex];
                    wt[highestPriorityIndex] = tat[highestPriorityIndex] - bt[highestPriorityIndex];
                    done[highestPriorityIndex] = true;
                    completed++;
                    ganttData.add(new GanttEntry(highestPriorityIndex + 1, blockStartTime, time));
                    lastExecutedProcess = -1;
                }
            } else { // Non-preemptive
                int startTime = Math.max(time, at[highestPriorityIndex]);
                if (lastExecutedProcess == -1 || lastExecutedProcess != highestPriorityIndex) {
                    blockStartTime = startTime;
                }
                lastExecutedProcess = highestPriorityIndex;

                time = Math.max(time, at[highestPriorityIndex]); // Advance time to arrival if not arrived yet
                int currentBlockStart = time;
                time += rem[highestPriorityIndex]; // Execute whole burst time
                ct[highestPriorityIndex] = time;
                tat[highestPriorityIndex] = ct[highestPriorityIndex] - at[highestPriorityIndex];
                wt[highestPriorityIndex] = tat[highestPriorityIndex] - bt[highestPriorityIndex];
                rem[highestPriorityIndex] = 0; // Mark as done
                done[highestPriorityIndex] = true;
                completed++;
                ganttData.add(new GanttEntry(highestPriorityIndex + 1, currentBlockStart, time));
                lastExecutedProcess = -1;
                blockStartTime = time;
            }
        }
        if (lastExecutedProcess != -1) {
            ganttData.add(new GanttEntry(lastExecutedProcess + 1, blockStartTime, time));
        }
        appendResults(ct, tat, wt);
    }

    /**
     * Implements the Round Robin (RR) scheduling algorithm.
     *
     * @param quantum The time quantum for each process.
     */
    public void roundRobin(int quantum) {
        int[] ct = new int[n];
        int[] wt = new int[n];
        int[] tat = new int[n];
        int[] rem = Arrays.copyOf(bt, n); // Remaining burst time

        Queue<Integer> q = new LinkedList<>();
        boolean[] inQueue = new boolean[n]; // Tracks if a process is currently in the ready queue

        int time = 0;
        int completed = 0;

        // Main scheduling loop
        while (completed < n) {

            // Add processes that have arrived by 'time', have remaining burst time,
            // and are not already in the queue. This captures initial arrivals
            // and processes that arrive while others are running or waiting.
            for (int i = 0; i < n; i++) {
                if (at[i] <= time && rem[i] > 0 && !inQueue[i]) {
                    q.add(i);
                    inQueue[i] = true;
                }
            }

            if (q.isEmpty()) {
                // If no processes are in the queue, advance time to the next process arrival.
                // This prevents an infinite loop if there's a gap in arrival times.
                int nextArrivalTime = Integer.MAX_VALUE;
                boolean foundNext = false;
                for (int i = 0; i < n; i++) {
                    // Check if process is not completed and arrives after current time
                    if (rem[i] > 0 && at[i] > time) {
                        nextArrivalTime = Math.min(nextArrivalTime, at[i]);
                        foundNext = true;
                    }
                }

                if (foundNext) {
                    time = nextArrivalTime;
                } else {
                    System.err.println("Round Robin: No processes in queue and no future arrivals. Exiting loop.");
                    break;
                }
                continue; // Continue to re-check queue with advanced time
            }

            int currentProcess = q.poll(); // Get process from front of queue
            inQueue[currentProcess] = false; // Mark as no longer in queue, as it's now executing

            int executionTime = Math.min(quantum, rem[currentProcess]);
            int startTime = time; // Start time for this slice

            rem[currentProcess] -= executionTime;
            time += executionTime;

            // Log this execution slice
            ganttData.add(new GanttEntry(currentProcess + 1, startTime, time));

            // After execution, if the process is completed, update stats and increment completed count.
            // Otherwise, if it still has remaining burst time, add it back to the end of the queue.
            if (rem[currentProcess] == 0) { // Process completed
                ct[currentProcess] = time;
                tat[currentProcess] = ct[currentProcess] - at[currentProcess];
                wt[currentProcess] = tat[currentProcess] - bt[currentProcess];
                completed++;
            } else {
                q.add(currentProcess); // Add back to queue if not completed
                inQueue[currentProcess] = true; // Mark as back in queue
            }
        }
        appendResults(ct, tat, wt);
    }


    /**
     * Implements the Proposed PSDQ (Priority-Shortest Dynamic Quantum) scheduling algorithm.
     * This simplified version sorts ready processes by priority, then by remaining burst time.
     * It dynamically calculates a quantum based on the average remaining burst time of ready processes.
     */
    public void psdq() {
        int[] ct = new int[n];
        int[] tat = new int[n];
        int[] wt = new int[n];
        int[] rem = Arrays.copyOf(bt, n);
        boolean[] done = new boolean[n];

        int time = 0;
        int completed = 0;
        int lastExecutedProcess = -1;
        int blockStartTime = 0;

        while (completed < n) {
            ArrayList<Integer> readyProcesses = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (at[i] <= time && !done[i] && rem[i] > 0) {
                    readyProcesses.add(i);
                }
            }

            // Log previous block if process changed or no process was found
            if (lastExecutedProcess != -1 && (readyProcesses.isEmpty() || readyProcesses.get(0) != lastExecutedProcess)) {
                ganttData.add(new GanttEntry(lastExecutedProcess + 1, blockStartTime, time));
            }


            if (readyProcesses.isEmpty()) {
                // If no processes are ready, advance time to the next process arrival.
                int nextArrivalTime = Integer.MAX_VALUE;
                boolean foundNext = false;
                for (int i = 0; i < n; i++) {
                    if (!done[i] && at[i] > time) {
                        nextArrivalTime = Math.min(nextArrivalTime, at[i]);
                        foundNext = true;
                    }
                }
                if (foundNext) {
                    time = nextArrivalTime;
                } else {
                    System.err.println("PSDQ: No processes ready and no future arrivals. Exiting loop.");
                    break;
                }
                lastExecutedProcess = -1; // CPU is idle
                blockStartTime = time;
                continue; // Continue to re-check for newly ready processes
            }

            // Sort ready processes: first by Priority (lower value = higher priority),
            // then by Remaining Burst Time (shortest remaining job)
            readyProcesses.sort(Comparator.comparingInt((Integer i) -> priority[i])
                                          .thenComparingInt(i -> rem[i]));

            // Calculate dynamic time quantum (tq) based on ready processes
            int totalRemainingBurst = 0;
            for (int i : readyProcesses) {
                totalRemainingBurst += rem[i];
            }
            // Ensure tq is at least 1 to avoid division by zero or infinite loops
            int tq = Math.max(1, totalRemainingBurst / readyProcesses.size());

            // Select the highest priority process (which is the first after sorting)
            int currentProcessIndex = readyProcesses.get(0);

            if (currentProcessIndex != lastExecutedProcess) {
                blockStartTime = time; // New block starts now
                lastExecutedProcess = currentProcessIndex;
            }

            // Determine execution slice:
            // If remaining burst is very small (<= tq/2), execute completely.
            // Otherwise, execute for a quantum slice (tq).
            int executionSlice = (rem[currentProcessIndex] <= tq / 2) ? rem[currentProcessIndex] : Math.min(tq, rem[currentProcessIndex]);

            rem[currentProcessIndex] -= executionSlice;
            time += executionSlice;

            if (rem[currentProcessIndex] == 0) { // Process completed
                ct[currentProcessIndex] = time;
                tat[currentProcessIndex] = ct[currentProcessIndex] - at[currentProcessIndex];
                wt[currentProcessIndex] = tat[currentProcessIndex] - bt[currentProcessIndex];
                done[currentProcessIndex] = true;
                completed++;
                // Log the completed block
                ganttData.add(new GanttEntry(currentProcessIndex + 1, blockStartTime, time));
                lastExecutedProcess = -1; // Reset as this process is done
            }
            // If not completed, it will be considered again in the next iteration if it's still the highest priority
        }
        // If a process was running and the loop finished, log the last block
        if (lastExecutedProcess != -1) {
            ganttData.add(new GanttEntry(lastExecutedProcess + 1, blockStartTime, time));
        }
        appendResults(ct, tat, wt);
    }

    /**
     * Appends the calculated results (CT, TAT, WT) to the result StringBuilder.
     *
     * @param ct  Completion Times array.
     * @param tat Turnaround Times array.
     * @param wt  Waiting Times array.
     */
    private void appendResults(int[] ct, int[] tat, int[] wt) {
        result.append("Process\tAT\tBT\tCT\tTAT\tWT\n");
        for (int i = 0; i < n; i++) {
            result.append("P" + (i + 1) + "\t" + at[i] + "\t" + bt[i] + "\t" + ct[i] + "\t" + tat[i] + "\t" + wt[i] + "\n");
        }

        // Calculate and append averages
        double avgWt = Arrays.stream(wt).average().orElse(0.0);
        double avgTat = Arrays.stream(tat).average().orElse(0.0);

        result.append("\nAverage Waiting Time: " + String.format("%.2f", avgWt) + "\n");
        result.append("Average Turnaround Time: " + String.format("%.2f", avgTat) + "\n");
    }

    /**
     * Returns the formatted scheduling results.
     *
     * @return A string containing the scheduling results table and averages.
     */
    public String getResult() {
        return result.toString();
    }

    /**
     * Returns the list of Gantt chart entries.
     *
     * @return A list of GanttEntry objects representing process execution.
     */
    public List<GanttEntry> getGanttData() {
        return ganttData;
    }
}

/**
 * Represents a single block of execution for a process in the Gantt chart.
 */
class GanttEntry {
    private int processId; // 1-based process ID
    private int startTime;
    private int endTime;

    public GanttEntry(int processId, int startTime, int endTime) {
        this.processId = processId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getProcessId() {
        return processId;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }
}

/**
 * Custom JPanel for drawing the Gantt chart.
 */
class GanttChartPanel extends JPanel {
    private List<GanttEntry> ganttEntries = new ArrayList<>();
    private int maxTime = 0;
    private Map<Integer, Color> processColors = new HashMap<>(); // To assign consistent colors to processes
    private static final Color[] DEFAULT_COLORS = {
            new Color(255, 99, 71),   // Tomato
            new Color(60, 179, 113),  // MediumSeaGreen
            new Color(65, 105, 225),  // RoyalBlue
            new Color(255, 165, 0),   // Orange
            new Color(138, 43, 226),  // BlueViolet
            new Color(0, 191, 255),   // DeepSkyBlue
            new Color(255, 20, 147),  // DeepPink
            new Color(0, 128, 0),     // Green
            new Color(255, 215, 0),   // Gold
            new Color(128, 0, 0)      // Maroon
    };
    private int colorIndex = 0;

    public GanttChartPanel() {
        setPreferredSize(new Dimension(800, 250)); // Default preferred size
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("Gantt Chart"));
    }

    public void drawChart(List<GanttEntry> entries) {
        this.ganttEntries = entries;
        this.maxTime = entries.stream()
                .mapToInt(GanttEntry::getEndTime)
                .max()
                .orElse(0);
        // Ensure some minimum time for drawing if maxTime is 0 or very small
        if (maxTime == 0 && !entries.isEmpty()) {
            maxTime = 1; // For single-point processes
        } else if (maxTime == 0 && entries.isEmpty()) {
            maxTime = 10; // Default for empty chart to show some axis
        }

        // Assign colors dynamically if not already assigned
        processColors.clear();
        colorIndex = 0;
        for (GanttEntry entry : entries) {
            if (!processColors.containsKey(entry.getProcessId())) {
                processColors.put(entry.getProcessId(), DEFAULT_COLORS[colorIndex % DEFAULT_COLORS.length]);
                colorIndex++;
            }
        }

        repaint(); // Redraw the chart
    }

    public void clearChart() {
        ganttEntries.clear();
        processColors.clear();
        maxTime = 0;
        colorIndex = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (ganttEntries.isEmpty()) {
            g2d.setColor(Color.GRAY);
            g2d.drawString("No Gantt Chart data available. Please run a simulation.", 50, getHeight() / 2);
            return;
        }

        int padding = 20; // Padding from panel edges
        int chartHeight = getHeight() - 2 * padding;
        int barHeight = 40; // Height of the single Gantt bar
        int chartY = padding + (chartHeight / 2) - (barHeight / 2); // Y position for the single bar

        // Calculate chart width
        int chartWidth = getWidth() - 2 * padding;

        // Calculate pixels per time unit
        double unitWidth = (double) chartWidth / maxTime;

        // Store unique time points for labels
        Set<Integer> timePoints = new TreeSet<>();
        timePoints.add(0); // Always include time 0

        // Draw Gantt bars and process labels
        // Sort entries by startTime to ensure correct drawing order
        List<GanttEntry> sortedEntries = new ArrayList<>(ganttEntries);
        sortedEntries.sort(Comparator.comparingInt(GanttEntry::getStartTime));

        int currentTime = 0; // Track the current position on the timeline

        for (GanttEntry entry : sortedEntries) {
            // Handle idle time (gap) before the current process starts
            if (entry.getStartTime() > currentTime) {
                int idleStartPx = padding + (int) (currentTime * unitWidth);
                int idleEndPx = padding + (int) (entry.getStartTime() * unitWidth);
                int idleWidthPx = idleEndPx - idleStartPx;

                if (idleWidthPx > 0) {
                    g2d.setColor(Color.LIGHT_GRAY); // Color for idle time
                    g2d.fillRect(idleStartPx, chartY, idleWidthPx, barHeight);
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawRect(idleStartPx, chartY, idleWidthPx, barHeight);

                    // Draw "Idle" text if space allows
                    String idleText = "Idle";
                    int idleTextWidth = g2d.getFontMetrics().stringWidth(idleText);
                    if (idleWidthPx > idleTextWidth + 10) {
                        g2d.setColor(Color.DARK_GRAY);
                        g2d.drawString(idleText, idleStartPx + (idleWidthPx - idleTextWidth) / 2, chartY + barHeight / 2 + 5);
                    }
                }
            }

            // Draw process segment
            int xStart = padding + (int) (entry.getStartTime() * unitWidth);
            int xEnd = padding + (int) (entry.getEndTime() * unitWidth);
            int barWidth = xEnd - xStart;

            if (barWidth <= 0) { // Skip if duration is 0 or negative
                currentTime = entry.getEndTime(); // Advance currentTime even if bar is skipped
                continue;
            }

            g2d.setColor(processColors.getOrDefault(entry.getProcessId(), Color.GRAY));
            g2d.fillRect(xStart, chartY, barWidth, barHeight);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(xStart, chartY, barWidth, barHeight); // Draw border

            // Draw process ID on top of the bar
            String processLabel = "P" + entry.getProcessId();
            int labelWidth = g2d.getFontMetrics().stringWidth(processLabel);
            g2d.setColor(Color.BLACK);
            // Center the label horizontally, place it slightly above the bar
            g2d.drawString(processLabel, xStart + (barWidth - labelWidth) / 2, chartY - 5);

            // Add start and end times to the set of unique time points
            timePoints.add(entry.getStartTime());
            timePoints.add(entry.getEndTime());

            currentTime = entry.getEndTime();
        }

        // Draw time axis labels and tick marks below the bars
        int xAxisY = chartY + barHeight + 15; // Y position for time axis labels

        for (int t : timePoints) {
            int x = padding + (int) (t * unitWidth);
            g2d.drawLine(x, chartY + barHeight, x, chartY + barHeight + 5); // Tick mark
            String timeLabel = String.valueOf(t);
            int stringWidth = g2d.getFontMetrics().stringWidth(timeLabel);
            g2d.drawString(timeLabel, x - stringWidth / 2, xAxisY); // Label
        }
        
        // Draw overall horizontal line for time axis
        g2d.setColor(Color.BLACK);
        g2d.drawLine(padding, chartY + barHeight, padding + chartWidth, chartY + barHeight);

        // No legend needed with processes labeled directly
    }
}

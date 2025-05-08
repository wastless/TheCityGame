import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ClientService extends JFrame {
    private GameInterface gameService;
    private String playerName;
    private String currentGameId;
    private JTextArea gameLog;
    private JTextField cityInput;
    private JButton submitButton;
    private JButton connectButton;
    private JButton startButton;
    private JTextField nameField;
    private JTextField serverField;
    private JTextField portField;
    private JComboBox<String> serverComboBox;
    private JPanel gamePanel;
    private JPanel connectionPanel;
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JLabel currentPlayerLabel;
    private JLabel lastCityLabel;
    private Timer turnTimer;
    private Timer updateTimer;
    private int timeLeft;
    private String lastPlayersList = "";
    private String lastStatus = "";
    private JTextArea playersList;
    private JPanel lobbyPanel;
    private boolean isFirstPlayer = false;

    public ClientService() {
        setTitle("Игра Города");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 330);
        setLayout(new BorderLayout());

        createConnectionPanel();
        createMainPanel();
        
        // Создаем таймер для обновления состояния игры
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (gameService != null && currentGameId != null) {
                    SwingUtilities.invokeLater(() -> updateGameState());
                }
            }
        }, 0, 1000);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (gameService != null && playerName != null) {
                    try {
                        gameService.disconnectPlayer(playerName);
                        gameService.clearPlayerState(playerName);
                    } catch (Exception e) {
                        System.err.println("Ошибка при отключении: " + e.getMessage());
                    }
                }
                if (updateTimer != null) {
                    updateTimer.cancel();
                }
            }
        });
    }

    private void createConnectionPanel() {
        connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JLabel nameLabel = new JLabel("Имя игрока:");
        nameField = new JTextField(15);
        connectButton = new JButton("Подключиться");
        startButton = new JButton("Старт");
        startButton.setEnabled(false);
        
        connectionPanel.add(nameLabel);
        connectionPanel.add(nameField);
        connectionPanel.add(connectButton);
        connectionPanel.add(startButton);
        
        add(connectionPanel, BorderLayout.NORTH);

        connectButton.addActionListener(e -> {
            playerName = nameField.getText();
            String serverAddress = "thecitygame.onrender.com";
            int httpPort = 8080;
            int rmiPort = httpPort + 1; // RMI порт на 1 больше HTTP порта
            
            try {
                if (playerName == null || playerName.trim().isEmpty()) {
                    showError("Имя игрока не может быть пустым");
                    return;
                }
                
                if (gameService != null) {
                    try {
                        gameService.clearPlayerState(playerName);
                    } catch (Exception ex) {}
                }
                
                updateStatus("Подключение к " + serverAddress + ":" + rmiPort + "...");
                System.out.println("Attempting to connect to " + serverAddress + ":" + rmiPort);
                
                // Устанавливаем системные свойства для RMI
                System.setProperty("java.rmi.server.useCodebaseOnly", "false");
                System.setProperty("java.rmi.server.codebase", "file:./");
                
                Registry registry = LocateRegistry.getRegistry(serverAddress, rmiPort);
                System.out.println("Registry found, looking up GameService...");
                
                gameService = (GameInterface) registry.lookup("GameService");
                System.out.println("GameService found, attempting to connect player...");
                
                if (gameService.connectPlayer(playerName)) {
                    updateStatus("Подключено к " + serverAddress + ":" + rmiPort);
                    connectButton.setEnabled(false);
                    nameField.setEnabled(false);
                    showAvailableGames();
                } else {
                    showError("Игрок с таким именем уже существует");
                }
            } catch (Exception ex) {
                String errorMessage = "Ошибка подключения к " + serverAddress + ":" + rmiPort + ": " + ex.getMessage();
                System.err.println(errorMessage);
                ex.printStackTrace();
                showError(errorMessage);
            }
        });

        startButton.addActionListener(e -> {
            if (startButton.getText().equals("Заново")) {
                resetGame();
            } else {
                try {
                    if (gameService.startGame(currentGameId)) {
                        startButton.setEnabled(false);
                        updateGameState();
                    } else {
                        showError("Не удалось начать игру");
                    }
                } catch (Exception ex) {
                    showError("Ошибка: " + ex.getMessage());
                }
            }
        });
    }

    private void resetGame() {
        // Отключаем текущего игрока
        if (gameService != null && playerName != null) {
            try {
                gameService.disconnectPlayer(playerName);
                gameService.clearPlayerState(playerName);
            } catch (Exception e) {
                System.err.println("Ошибка при отключении: " + e.getMessage());
            }
        }

        // Сбрасываем состояние
        gameService = null;
        playerName = null;
        currentGameId = null;
        isFirstPlayer = false;
        lastPlayersList = "";
        lastStatus = "";

        // Сбрасываем таймеры
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }

        // Очищаем и разблокируем поля ввода
        nameField.setText("");
        serverField.setText("");
        portField.setText("1099");
        nameField.setEnabled(true);
        serverField.setEnabled(true);
        portField.setEnabled(true);
        connectButton.setEnabled(true);
        startButton.setText("Старт");
        startButton.setEnabled(false);
        submitButton.setEnabled(false);
        cityInput.setText("");

        // Очищаем информационные поля
        playersList.setText("");
        currentPlayerLabel.setText("Не подключено");
        lastCityLabel.setText("Последний город: -");
        timeLabel.setText("--:--");
        statusLabel.setText("Статус: Не подключено");
    }

    private void createMainPanel() {
        // Создаем главную панель с BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Создаем центральную панель с основной информацией
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        
        // Создаем панель для основной информации (текущий игрок, город, время)
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Игровая информация"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Настраиваем шрифты для основной информации
        Font largeFont = new Font("Arial", Font.BOLD, 16);
        
        currentPlayerLabel = new JLabel("Текущий игрок: -");
        currentPlayerLabel.setFont(largeFont);
        
        lastCityLabel = new JLabel("Последний город: -");
        lastCityLabel.setFont(largeFont);
        
        timeLabel = new JLabel("Время: --:--");
        timeLabel.setFont(largeFont);
        
        infoPanel.add(currentPlayerLabel);
        infoPanel.add(lastCityLabel);
        infoPanel.add(timeLabel);
        
        centerPanel.add(infoPanel, BorderLayout.NORTH);

        // --- Нижняя панель (ввод + статус) с GridBagLayout ---
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Подпись «Введите город.» отдельной строкой
        JLabel cityLabel = new JLabel("Введите город.");
        bottomPanel.add(cityLabel, gbc);

        // Строка ввода и кнопка в одной строке
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        cityInput = new JTextField();
        cityInput.setFont(new Font("Arial", Font.PLAIN, 14));
        bottomPanel.add(cityInput, gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        submitButton = new JButton("Отправить");
        submitButton.setEnabled(false);
        bottomPanel.add(submitButton, gbc);

        // Ошибка под полем ввода (на всю ширину)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        JLabel errorLabel = new JLabel("");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        bottomPanel.add(errorLabel, gbc);

        // Статус отдельной строкой под вводом (на всю ширину)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        statusLabel = new JLabel("Статус: Не подключено");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        bottomPanel.add(statusLabel, gbc);

        centerPanel.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Создаем правую панель для списка игроков
        JPanel playersPanel = new JPanel(new BorderLayout());
        playersPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Участники"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        playersPanel.setPreferredSize(new Dimension(200, 0));

        playersList = new JTextArea();
        playersList.setEditable(false);
        playersList.setFont(new Font("Arial", Font.PLAIN, 14));
        playersList.setBackground(new Color(245, 245, 245));
        JScrollPane scrollPane = new JScrollPane(playersList);
        playersPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(playersPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);

        submitButton.addActionListener(e -> {
            if (currentGameId != null) {
                String city = cityInput.getText();
                try {
                    if (gameService.submitCity(currentGameId, playerName, city)) {
                        cityInput.setText("");
                        errorLabel.setText("");
                        if (turnTimer != null) {
                            turnTimer.cancel();
                            turnTimer = null;
                        }
                        updateGameState();
                    } else {
                        errorLabel.setText("Неверный город или город уже был использован");
                    }
                } catch (Exception ex) {
                    errorLabel.setText("Ошибка: " + ex.getMessage());
                }
            }
        });

        cityInput.addActionListener(e -> submitButton.doClick());
    }

    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Статус: " + status);
        });
    }

    private void showError(String error) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Ошибка: " + error);
        });
    }

    private void appendToLog(String message) {
        SwingUtilities.invokeLater(() -> {
            gameLog.append(message + "\n");
            gameLog.setCaretPosition(gameLog.getDocument().getLength());
        });
    }

    private void showAvailableGames() {
        try {
            List<String> games = gameService.getAvailableGames();
            if (games.isEmpty()) {
                currentGameId = gameService.createNewGame();
                if (gameService.joinGame(currentGameId, playerName)) {
                    updateStatus("В игре");
                    isFirstPlayer = true;
                    lastPlayersList = "";
                    lastStatus = "";
                    SwingUtilities.invokeLater(this::updateGameState);
                } else {
                    showError("Не удалось присоединиться");
                }
            } else {
                currentGameId = games.get(0);
                if (gameService.joinGame(currentGameId, playerName)) {
                    updateStatus("В игре");
                    isFirstPlayer = false;
                    lastPlayersList = "";
                    lastStatus = "";
                    SwingUtilities.invokeLater(this::updateGameState);
                } else {
                    showError("Не удалось присоединиться");
                }
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
        timeLeft = 30;
        updateTimeLabel();
        turnTimer = new Timer();
        turnTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    timeLeft--;
                    SwingUtilities.invokeLater(() -> {
                        updateTimeLabel();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        timeLabel.setText("Время вышло!");
                        if (turnTimer != null) {
                            turnTimer.cancel();
                            turnTimer = null;
                        }
                        try {
                            if (gameService != null && currentGameId != null) {
                                gameService.checkPlayerTimeout(currentGameId, playerName);
                                updateGameState();
                            }
                        } catch (Exception e) {
                            showError("Ошибка проверки таймаута: " + e.getMessage());
                        }
                    });
                }
            }
        }, 0, 1000);
    }

    private void updateTimeLabel() {
        timeLabel.setText(String.format("Осталось времени: %02d:%02d", timeLeft / 60, timeLeft % 60));
    }

    private void updateGameState() {
        try {
            if (gameService == null || currentGameId == null) {
                return;
            }

            GameState state = gameService.getGameState(currentGameId);
            if (state == null) {
                showError("Не удалось получить состояние игры");
                return;
            }

            final String currentPlayer = state.getCurrentPlayer();
            final List<String> players = state.getPlayers();
            final boolean isGameOver = state.isGameOver();
            final String winner = state.getWinner();
            final String lastCity = state.getLastCity();
            final boolean isGameStarted = state.isGameStarted();

            SwingUtilities.invokeLater(() -> {
                try {
                    StringBuilder playersText = new StringBuilder();
                    if (players != null) {
                        for (String player : players) {
                            if (currentPlayer != null && player.equals(currentPlayer)) {
                                playersText.append("► ").append(player).append(" (ходит)").append("\n");
                            } else {
                                playersText.append("• ").append(player).append("\n");
                            }
                        }
                    }
                    String newPlayersList = playersText.toString();
                    if (!newPlayersList.equals(lastPlayersList)) {
                        playersList.setText(newPlayersList);
                        lastPlayersList = newPlayersList;
                    }

                    if (!isGameStarted && isFirstPlayer) {
                        startButton.setEnabled(true);
                        currentPlayerLabel.setText("Нажмите 'Старт' чтобы начать игру");
                    } else {
                        startButton.setEnabled(false);
                    }

                    if (isGameOver) {
                        currentPlayerLabel.setText("ИГРА ОКОНЧЕНА! Победитель: " + winner);
                        submitButton.setEnabled(false);
                        startButton.setText("Заново");
                        startButton.setEnabled(true);
                        if (turnTimer != null) {
                            turnTimer.cancel();
                            turnTimer = null;
                        }
                        timeLabel.setText("ИГРА ЗАВЕРШЕНА");
                    } else if (isGameStarted && currentPlayer != null) {
                        // Проверяем, остался ли текущий игрок в игре
                        if (players != null && !players.contains(currentPlayer)) {
                            // Если текущий игрок вышел, передаем ход следующему
                            try {
                                gameService.passTurn(currentGameId);
                            } catch (Exception e) {
                                showError("Ошибка передачи хода: " + e.getMessage());
                            }
                        } else {
                            currentPlayerLabel.setText("Сейчас ходит: " + currentPlayer);
                            submitButton.setEnabled(currentPlayer.equals(playerName));
                            if (currentPlayer.equals(playerName)) {
                                if (turnTimer == null) {
                                    startTurnTimer();
                                }
                            } else {
                                if (turnTimer != null) {
                                    turnTimer.cancel();
                                    turnTimer = null;
                                }
                                timeLabel.setText("Ожидание хода...");
                            }
                        }
                    } else if (!isGameStarted) {
                        currentPlayerLabel.setText("Ожидание начала игры...");
                        submitButton.setEnabled(false);
                        if (turnTimer != null) {
                            turnTimer.cancel();
                            turnTimer = null;
                        }
                        timeLabel.setText("--:--");
                    }

                    if (lastCity != null) {
                        lastCityLabel.setText("Последний город: " + lastCity);
                    }
                } catch (Exception e) {
                    showError("Ошибка обновления интерфейса: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            showError("Ошибка обновления: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientService client = new ClientService();
            client.setVisible(true);
        });
    }
}

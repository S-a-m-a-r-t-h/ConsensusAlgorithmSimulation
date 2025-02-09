// Implement the new formula for PoCV Reputation
// Change initial reputation to 50, increments to 10 and decrements to 5
// Include dynamic confidance factor allocation
//Include the option to take transaction input from a dataset
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

class Node implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int nodeId;
    private final KeyPair keyPair;
    private double reputationScore;
    private double energyConsumption;

    public Node(int nodeId, double initialReputationScore) throws NoSuchAlgorithmException {
        this.nodeId = nodeId;
        this.keyPair = generateKeyPair();
        this.reputationScore = initialReputationScore;
        this.energyConsumption = 0.0;
    }

    public int getNodeId() {
        return nodeId;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public double getReputationScore() {
        return reputationScore;
    }

    public void adjustReputation(double delta) {
        reputationScore = Math.max(0, reputationScore + delta);
    }

    public void consumeEnergy(double amount) {
        energyConsumption += amount;
        System.out.println("Node " + nodeId + " consumed " + amount + " energy. Total: " + energyConsumption);
        if (energyConsumption > 100) { // Penalty for high energy usage
            adjustReputation(-0.1);
            System.out.println("Node " + nodeId + " penalized for excessive energy consumption.");
        }
    }

    public boolean validateTransaction(Transaction transaction) {
        Random random = new Random();
        consumeEnergy(0.02); // Simulate energy consumption
        boolean valid = transaction.verifySignature();

        // Reputation-based validation weighting
        if (valid && random.nextDouble() < (reputationScore)) {
            adjustReputation(0.01); // Reward for successful validation
            return true;
        } else {
            adjustReputation(-0.05); // Penalize for failed validation
            return false;
        }
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        return keyGen.generateKeyPair();
    }
}

// Class to manage nodes with persistence
class PersistentNodeManager {
    private final Map<Integer, Node> nodes = new ConcurrentHashMap<>();
    private int nextNodeId = 1;

    private static final String FILE_NAME = "nodes.dat";

    public PersistentNodeManager() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
        loadNodes();
    }

    public void addNode(double initialReputationScore) throws NoSuchAlgorithmException, IOException {
        Node newNode = new Node(nextNodeId, initialReputationScore);
        nodes.put(nextNodeId, newNode);
        saveNodes();
        System.out.println("Node " + nextNodeId + " added to the network.");
        nextNodeId++;
    }

    public void removeNode(int nodeId) throws IOException {
        if (nodes.remove(nodeId) != null) {
            saveNodes();
            System.out.println("Node " + nodeId + " removed from the network.");
        } else {
            System.out.println("Node " + nodeId + " does not exist.");
        }
    }

    public Collection<Node> getAllNodes() {
        return nodes.values();
    }

    private void saveNodes() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(nodes);
            oos.writeInt(nextNodeId);
        }
    }

    private void loadNodes() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Map<Integer, Node> loadedNodes = (Map<Integer, Node>) ois.readObject();
                nodes.putAll(loadedNodes);
                nextNodeId = ois.readInt();
            }
        }
    }
}

// Class to represent a Transaction
class Transaction implements Serializable {
    private final String data;
    private final PublicKey senderPublicKey;
    private final byte[] signature;
    private final long timestamp;
    private final List<PublicKey> validationSignatures;

    public Transaction(String data, PublicKey senderPublicKey, byte[] signature, long timestamp) {
        this.data = data;
        this.senderPublicKey = senderPublicKey;
        this.signature = signature;
        this.timestamp = timestamp;
        this.validationSignatures = new ArrayList<>();
    }

    public boolean verifySignature() {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(senderPublicKey);
            sig.update(data.getBytes());
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public void addValidationSignature(PublicKey validatorKey) {
        validationSignatures.add(validatorKey);
    }

    public int getValidationCount() {
        return validationSignatures.size();
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return data;
    }
}

// Class to represent a Block
class Block implements Serializable {
    private final int index;
    private final String previousHash;
    private final String hash;
    private final List<Transaction> transactions;
    private final long timestamp;

    public Block(int index, String previousHash, List<Transaction> transactions, long timestamp) {
        this.index = index;
        this.previousHash = previousHash;
        this.transactions = transactions;
        this.timestamp = timestamp;
        this.hash = calculateHash();
    }

    private String calculateHash() {
        return Integer.toString((transactions.toString() + previousHash + timestamp + index).hashCode());
    }

    public int getTransactionCount() {
        return transactions.size();
    }

    public int getIndex() {
        return index;
    }

    public String getHash() {
        return hash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public List<Transaction> getTransactions(){
        return transactions;
    }
}

// Class to manage the blockchain with persistence
class PersistentBlockchain {
    private final Deque<Block> chain = new LinkedList<>();

    private static final String FILE_NAME = "blockchain.dat";

    public PersistentBlockchain() throws IOException, ClassNotFoundException {
        loadBlockchain();
    }

    private void loadBlockchain() throws IOException, ClassNotFoundException {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                List<Block> loadedChain = (List<Block>) ois.readObject();
                chain.addAll(loadedChain);
            }
        } else {
            chain.add(new Block(0, "0", new ArrayList<>(), System.currentTimeMillis())); // Genesis block
        }
    }

    public synchronized Block getLastBlock() {
        return chain.peekLast();
    }

    public synchronized void addBlock(Block block) throws IOException {
        chain.addLast(block);
        saveBlockchain();
    }

    public synchronized void displayBlockchain() {
        System.out.println("\n===== Blockchain =====");
        for (Block block : chain) {
            System.out.println("--------------------------------------------------");
            System.out.println("Block Index: " + block.getIndex());
            System.out.println("Timestamp: " + block.getTimestamp());
            System.out.println("Transaction Count: " + block.getTransactionCount());
            System.out.println("Previous Hash: " + block.getPreviousHash());
            System.out.println("Current Hash: " + block.getHash());
            System.out.println("Transactions:");
            for (Transaction transaction : block.getTransactions()) {
                System.out.println("  - " + transaction.toString());
            }
        }
        System.out.println("--------------------------------------------------");
    }


    private void saveBlockchain() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(new ArrayList<>(chain));
        }
    }
}

class Metrics {
    private long totalTransactions = 0;
    private long faultyTransactions = 0;
    private long totalValidationTime = 0;
    private long totalBlocks = 0;
    private long totalBlockTime = 0;

    public synchronized void recordTransaction(boolean isFaulty) {
        totalTransactions++;
        if (isFaulty) faultyTransactions++;
    }

    public synchronized void addValidationTime(long time) {
        totalValidationTime += time;
    }

    public synchronized void recordBlock(long blockTime) {
        totalBlocks++;
        totalBlockTime += blockTime;
    }

    public void printReport() {
        System.out.println("\n===== Metrics Report =====");
        System.out.println("Total Transactions Processed: " + totalTransactions);
        System.out.println("Faulty Transactions: " + faultyTransactions);
        System.out.println("Average Validation Time: " +
                (totalTransactions > 0 ? totalValidationTime / totalTransactions : 0) + " ms");
        System.out.println("Average Block Formation Time: " +
                (totalBlocks > 0 ? totalBlockTime / totalBlocks : 0) + " ms");
        System.out.println("Throughput (TPS): " +
                (totalBlockTime > 0 ? (totalTransactions / (totalBlockTime / 1000.0)) : 0));
    }

}

class TransactionSimulator {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Queue<Transaction> transactionQueue;
    private final PersistentNodeManager nodeManager;
    private final Random random = new Random();
    private boolean isRunning = false;

    public TransactionSimulator(Queue<Transaction> transactionQueue, PersistentNodeManager nodeManager) {
        this.transactionQueue = transactionQueue;
        this.nodeManager = nodeManager;
    }

    public void startSimulation(int tps) {
        if (isRunning) return;
        isRunning = true;
        System.out.println("Transaction simulation started with " + tps + " TPS.");
        executor.scheduleAtFixedRate(this::generateTransaction, 0, 1000 / tps, TimeUnit.MILLISECONDS);
    }

    private void generateTransaction() {
        try {
            Collection<Node> nodes = nodeManager.getAllNodes();
            if (nodes.isEmpty()) {
                System.out.println("No nodes available to generate transactions. Add nodes to start simulation.");
                return;
            }

            long timestamp = System.currentTimeMillis();
            int sensorId = random.nextInt(100);
            String data = String.format("SensorID: %d, Timestamp: %d, Temp: %.2f°C, Humidity: %.2f%%",
                    sensorId, timestamp, random.nextDouble(15, 40), random.nextDouble(20, 80));

            Node sender = new ArrayList<>(nodes).get(random.nextInt(nodes.size()));

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(sender.getPrivateKey());
            signature.update(data.getBytes());
            byte[] signedData = signature.sign();

            Transaction transaction = new Transaction(data, sender.getPublicKey(), signedData, timestamp);

            if (random.nextDouble() < 0.05) { // 5% chance of faulty transaction
                System.out.println("Generated faulty transaction.");
                transactionQueue.offer(new Transaction("INVALID DATA", sender.getPublicKey(), signedData, timestamp));
            } else {
                transactionQueue.offer(transaction);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopSimulation() {
        executor.shutdownNow();
        isRunning = false;
        System.out.println("Transaction simulation stopped.");
    }
}

public class PoCVSimulation {

    private final Random random = new Random(); // Define random at the class level

    public static void main(String[] args) throws Exception {
        PersistentBlockchain blockchain = new PersistentBlockchain();
        PersistentNodeManager nodeManager = new PersistentNodeManager();
        Queue<Transaction> transactionQueue = new ConcurrentLinkedQueue<>();
        Metrics metrics = new Metrics();

        PoCVSimulation simulation = new PoCVSimulation(); // Create an instance of the class
        TransactionSimulator transactionSimulator = new TransactionSimulator(transactionQueue, nodeManager);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== PoCV Simulation Menu ===");
            System.out.println("1. Add Node");
            System.out.println("2. Remove Node");
            System.out.println("3. View Blockchain");
            System.out.println("4. Start Transaction Simulation");
            System.out.println("5. Stop Transaction Simulation");
            System.out.println("6. Process Transactions");
            System.out.println("7. View Metrics");
            System.out.println("8. Exit");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1 -> {
                    System.out.println("Adding a new node...");
                    double defaultReputation = 0.8;
                    nodeManager.addNode(defaultReputation);
                }
                case 2 -> {
                    System.out.print("Enter Node ID to remove: ");
                    int nodeId = scanner.nextInt();
                    nodeManager.removeNode(nodeId);
                }
                case 3 -> blockchain.displayBlockchain();
                case 4 -> {
                    System.out.print("Enter transactions per second (TPS): ");
                    int tps = scanner.nextInt();
                    transactionSimulator.startSimulation(tps);
                }
                case 5 -> transactionSimulator.stopSimulation();
                case 6 -> {
                    System.out.println("Processing transactions...");
                    simulation.processTransactions(nodeManager, transactionQueue, blockchain, metrics, 5, 5000); // Example
                }
                case 7 -> metrics.printReport();
                case 8 -> {
                    System.out.println("Exiting simulation...");
                    transactionSimulator.stopSimulation();
                    return;
                }
                default -> System.out.println("Invalid choice! Try again.");
            }
        }
    }

    public void processTransactions(PersistentNodeManager nodeManager,
                                    Queue<Transaction> transactionQueue,
                                    PersistentBlockchain blockchain,
                                    Metrics metrics,
                                    int transactionsPerBlock,
                                    long blockIntervalMillis) throws IOException {
        List<Transaction> currentBatch = new ArrayList<>();
        Collection<Node> nodes = nodeManager.getAllNodes();
        long lastBlockTime = System.currentTimeMillis();

        while (!transactionQueue.isEmpty()) {
            Transaction transaction = transactionQueue.poll();
            long startTime = System.currentTimeMillis();

            int consensusThreshold = calculateConsensusThreshold(nodes);
            int validCount = 0;

            for (Node node : nodes) {
                if (node.validateTransaction(transaction)) {
                    validCount++;
                    if (validCount >= consensusThreshold) break;
                }
            }

            long validationTime = System.currentTimeMillis() - startTime;
            metrics.addValidationTime(validationTime);
            metrics.recordTransaction(validCount < consensusThreshold);

            if (validCount >= consensusThreshold) {
                currentBatch.add(transaction);
            }

            if (currentBatch.size() >= transactionsPerBlock ||
                    (System.currentTimeMillis() - lastBlockTime) >= blockIntervalMillis) {
                Block newBlock = new Block(
                        blockchain.getLastBlock().getIndex() + 1,
                        blockchain.getLastBlock().getHash(),
                        new ArrayList<>(currentBatch),
                        System.currentTimeMillis()
                );
                blockchain.addBlock(newBlock);
                metrics.recordBlock(System.currentTimeMillis() - lastBlockTime);
                currentBatch.clear();
                lastBlockTime = System.currentTimeMillis();
            }
        }
    }

    private int calculateConsensusThreshold(Collection<Node> nodes) {
        double totalReputation = nodes.stream().mapToDouble(Node::getReputationScore).sum();
        return Math.max(1, (int) Math.ceil((totalReputation / nodes.size()) * 0.7)); // 70% threshold
    }

}


